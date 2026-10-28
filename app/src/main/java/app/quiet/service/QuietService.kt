package app.quiet.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings as AndroidSettings
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import app.quiet.ads.SkipWaitActivity
import app.quiet.BuildConfig
import app.quiet.MainActivity
import app.quiet.QuietApp
import app.quiet.data.Store
import app.quiet.ui.components.plural
import org.json.JSONArray
import org.json.JSONObject
import quiet.core.detect.Detector
import quiet.core.detect.Reading
import quiet.core.engine.Command
import quiet.core.engine.Cover
import quiet.core.engine.CoverReason
import quiet.core.engine.Decision
import quiet.core.engine.Engine
import quiet.core.engine.EngineInput
import quiet.core.engine.EventType
import quiet.core.engine.ScrollInfo
import quiet.core.rules.AppRules
import quiet.core.stats.StatsMath
import quiet.core.stats.TickTarget
import quiet.core.stats.plusCount
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Watches which app is in front, reads the screen of supported apps, asks the [Engine] what to do,
 * and draws the result. Also the clock for usage tracking: one tick a second while the screen is on.
 */
class QuietService : AccessibilityService(), CoverActions {
    private val store: Store get() = QuietApp.store
    private val engine = Engine()
    private val reader = TreeReader()
    private lateinit var overlays: Overlays
    private lateinit var media: MediaPauser
    private val main = Handler(Looper.getMainLooper())

    private var evalPending = false
    private var lastEvalAt = 0L
    private var foreground: String? = null
    private var foregroundSince = 0L
    private var enteredFromApp = false
    private var lastApp: String? = null
    private var lastAppSince = 0L
    private var appBefore: String? = null
    private var cameFrom: String? = null
    private var nudgesSent = 0
    private var lastTick = 0L
    private var carryMs = 0L
    private var lastDecision = Decision.NONE
    private var lastRules: AppRules? = null
    private var lastScroll: ScrollInfo? = null
    private var lastScrollPkg: String? = null
    private var screenOn = true
    private var dumpRequested = false
    private var lastLoggedSurface: String? = null
    private val genericUnlocks = mutableMapOf<String, Long>()
    private val skippedWaits = mutableMapOf<String, Long>()
    /** When an unlock wait was started, per app, so leaving the cover (or the ad screen) doesn't restart it. */
    private val waitStarts = mutableMapOf<String, Long>()
    private var receiversRegistered = false
    private var debugReceiverRegistered = false
    private var lastCoverTapAt = 0L

    private val evalRunnable = Runnable { evaluateSafely() }
    private val tickRunnable = object : Runnable {
        override fun run() {
            tickSafely()
            main.postDelayed(this, TICK_MS)
        }
    }

    // ---- lifecycle ----

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlays = Overlays(this, this)
        media = MediaPauser(this).also { it.recover() }
        registerReceivers()
        lastTick = System.currentTimeMillis()
        main.post(tickRunnable)
        LiveState.update { it.copy(connected = true, lastHeartbeat = System.currentTimeMillis()) }
        Log.i(TAG, "service connected")
        returnAfterTurnOn()
    }

    /** Switched on from Quiet's own screens: come back to Quiet instead of staying in Android's settings. */
    private fun returnAfterTurnOn() {
        if (SystemClock.elapsedRealtime() > QuietApp.returnAfterTurnOnUntil) return
        QuietApp.returnAfterTurnOnUntil = 0L
        runCatching { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        if (instance == null) return
        instance = null
        main.removeCallbacksAndMessages(null)
        runCatching { overlays.hideAll() }
        runCatching { media.release() }
        if (receiversRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiversRegistered = false
        }
        if (debugReceiverRegistered) {
            runCatching { unregisterReceiver(debugReceiver) }
            debugReceiverRegistered = false
        }
        store.flushNow()
        LiveState.update { it.copy(connected = false) }
    }

    override fun onInterrupt() {}

    // ---- events ----

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString()
        if (pkg == null) {
            // Window changes (an app closing to the home screen, a window opening on top) come with no
            // package. They're often the only sign the screen under a cover has changed.
            if (e.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED && (lastRules != null || lastDecision.cover != null)) scheduleEval()
            return
        }
        if (pkg == packageName) return
        try {
            val supported = store.rules.value.forPackage(pkg) != null
            when (e.eventType) {
                AccessibilityEvent.TYPE_VIEW_SCROLLED -> if (supported) onScroll(e, pkg)
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> Unit
                else -> if (!supported) return
            }
            scheduleEval()
        } catch (t: Throwable) {
            Log.w(TAG, "event failed", t)
        }
    }

    private fun onScroll(e: AccessibilityEvent, pkg: String) {
        val now = System.currentTimeMillis()
        // The video pager is the one scrolling thing that fills most of the screen.
        val src = runCatching { e.source }.getOrNull()
        val rect = android.graphics.Rect()
        src?.getBoundsInScreen(rect)
        val fromPager = rect.height() >= screenHeight() * 0.5
        lastScroll = ScrollInfo(now, fromPager, if (fromPager) e.fromIndex else -1)
        lastScrollPkg = pkg
        LiveState.update { it.copy(lastScrollAt = now) }
    }

    private fun scheduleEval() {
        if (evalPending) return
        evalPending = true
        val wait = (EVAL_THROTTLE_MS - (System.currentTimeMillis() - lastEvalAt)).coerceIn(EVAL_MIN_DELAY_MS, EVAL_THROTTLE_MS)
        main.postDelayed(evalRunnable, wait)
    }

    // ---- the main loop ----

    private fun evaluateSafely() {
        evalPending = false
        lastEvalAt = System.currentTimeMillis()
        try {
            evaluate()
        } catch (t: Throwable) {
            // Fail open: never leave a cover up because of a bug.
            Log.e(TAG, "evaluate failed", t)
            overlays.hideAll()
            media.release()
        }
    }

    private fun evaluate() {
        val now = System.currentTimeMillis()
        val fg = currentForeground()
        onForeground(fg, now)
        if (!screenOn) return
        // Nothing is ever drawn over Quiet's own screens: above all the ad screen and the ad it shows.
        if (SkipWaitActivity.isStarting || ownScreenActive()) {
            overlays.hideAll()
            media.release()
            // The Today screen shows unlocks and passes from this; keep it current while Quiet is in front.
            LiveState.update { it.copy(engine = engine.snapshot()) }
            return
        }
        val settings = store.settings.value
        val rules = fg?.let { store.rules.value.forPackage(it) }

        if (rules == null) {
            // No readable app in front (mid-transition) isn't leaving: keep the app's state.
            if (fg != null) {
                if (lastRules != null) engine.onLeftApp()
                lastRules = null
            }
            val cover = genericLimitCover(fg, now)
            applyDecision(Decision(cover = cover), null, null)
            LiveState.update { it.copy(foreground = fg, reading = null, decision = lastDecision, engine = engine.snapshot()) }
            return
        }
        lastRules = rules

        val root = findRoot(fg)
        val (w, h) = screenSize()
        val result = root?.let { reader.read(it, fg, w, h, rules.tabWords) }
        val reading = result?.let { Detector.read(rules, it.tree) } ?: Reading.unknown(rules.app)
        val scroll = lastScroll?.takeIf { lastScrollPkg == fg }
        val decision = engine.evaluate(
            EngineInput(now, ZoneId.systemDefault(), rules, reading, settings, store.today(now), scroll, enteredFromApp),
        )
        applyDecision(decision, rules, result)

        if (settings.verboseLogging && decision.events.any { it.type == EventType.VIDEO }) {
            Log.i(TAG, "video ${rules.app}.${reading.surface} truncated=${reading.truncated} scroll=${scroll?.index} sig=${reading.signature.take(40)}")
        }
        if (settings.verboseLogging && reading.surface != lastLoggedSurface) {
            lastLoggedSurface = reading.surface
            Log.i(TAG, "screen ${rules.app}.${reading.surface} kind=${reading.kind} signals=${reading.signals} cover=${decision.cover?.reason} held=${decision.held} nodes=${result?.tree?.nodes?.size} truncated=${reading.truncated}")
        }
        if (dumpRequested && result != null) {
            dumpRequested = false
            dump(result.tree, reading)
        }
        LiveState.update {
            it.copy(
                foreground = fg,
                reading = reading,
                decision = decision,
                engine = engine.snapshot(),
                nodeCount = result?.tree?.nodes?.size ?: 0,
                walkMs = result?.millis ?: 0,
            )
        }
    }

    private fun applyDecision(d: Decision, rules: AppRules?, result: TreeReader.Result?) {
        val s = store.settings.value
        val cover = d.cover
        if (cover != null) {
            val now = System.currentTimeMillis()
            overlays.showCover(
                CoverModel(
                    cover,
                    s.accent,
                    todayLine(),
                    adSkip = s.adSkipsWait,
                    waitSkipped = (skippedWaits[cover.app] ?: 0L) > now,
                    waitStartedAt = waitStarts[cover.app]?.takeIf { now - it < SKIPPED_WAIT_KEEP_MS },
                    returnTo = cameFrom?.takeIf { cover.fromLink }?.let { appLabel(it) },
                ),
            )
            if (s.pauseVideo) media.engage()
            overlays.showPill(null)
        } else {
            overlays.hideCover()
            media.release()
            overlays.showPill(d.pill?.let { PillModel(it.text, it.urgent, s.accent) })
        }
        d.command?.let { execute(it, rules, result) }
        // A held decision is a guess about a screen that couldn't be read; look again shortly.
        if (d.held) main.postDelayed({ scheduleEval() }, HELD_RECHECK_MS)
        if (d.events.isNotEmpty()) {
            store.record(d.events) { id -> store.rules.value.apps[id]?.displayName ?: id }
            if (s.haptics && d.events.any { it.type == EventType.COVER_SHOWN }) buzz()
        }
        lastDecision = d
    }

    private fun execute(cmd: Command, rules: AppRules?, result: TreeReader.Result?) {
        when (cmd) {
            is Command.Back -> performGlobalAction(GLOBAL_ACTION_BACK)
            is Command.Navigate -> {
                if (rules == null || result == null) return
                val ok = clickNav(rules, result, cmd.target)
                Log.i(TAG, "auto-skip ${cmd.app} -> ${cmd.target}: ${if (ok) "clicked" else "target not found"}")
                if (ok) main.postDelayed({ scheduleEval() }, 400)
            }
        }
    }

    private fun clickNav(rules: AppRules, result: TreeReader.Result, target: String): Boolean {
        val node = Detector.findNavNode(rules, result.tree, target) ?: return false
        val info = result.platform.getOrNull(node.index) ?: return false
        return runCatching { info.performAction(AccessibilityNodeInfo.ACTION_CLICK) }.getOrDefault(false)
    }

    // ---- ticks: time tracking ----

    private fun tickSafely() {
        try {
            tick()
        } catch (t: Throwable) {
            Log.w(TAG, "tick failed", t)
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val elapsed = (now - lastTick).coerceIn(0, MAX_TICK_GAP_MS)
        lastTick = now
        LiveState.update { it.copy(lastHeartbeat = now) }
        if (!screenOn || isLocked()) {
            carryMs = 0
            return
        }
        val fg = foreground ?: return
        val s = store.settings.value
        val supported = store.rules.value.forPackage(fg)
        val tracked = supported != null || s.tracked.any { it.packageName == fg }
        if (tracked) {
            carryMs += elapsed
            val secs = (carryMs / 1000).toInt()
            carryMs %= 1000
            if (secs > 0) {
                val d = lastDecision.takeIf { supported != null && lastRules == supported }
                val target = TickTarget(
                    packageName = fg,
                    tracked = true,
                    surfaceKey = d?.surfaceKey,
                    isFeed = d != null && d.isFeed && d.cover == null,
                    isViewer = d != null && d.isViewer && d.cover == null,
                    chargesBudget = d != null && d.chargesBudget && d.cover == null,
                )
                val hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
                store.mutateToday(now) { StatsMath.addTime(it, target, secs, hour) }
            }
            maybeNudge(fg, now, s.nudgeEveryMinutes)
        }
        // Re-check every second inside supported or limited apps: passes expire, allowances run out.
        if (supported != null || s.tracked.any { it.packageName == fg && it.dailyLimitMinutes > 0 }) scheduleEval()
    }

    private fun maybeNudge(fg: String, now: Long, every: Int) {
        if (every <= 0) return
        val minutes = ((now - foregroundSince) / 60_000).toInt()
        if (minutes >= every * (nudgesSent + 1)) {
            nudgesSent++
            val s = store.settings.value
            overlays.toast(PillModel("You've been on ${labelFor(fg)} for ${plural(minutes, "minute")}.", false, s.accent))
        }
    }

    private fun onForeground(fg: String?, now: Long) {
        if (fg == foreground) return
        foreground = fg
        foregroundSince = now
        nudgesSent = 0
        if (fg == null) return
        // Straight from another app rather than the home screen: most likely a link or a share. Moments
        // with no readable app in front (a cold start's splash) don't count as leaving.
        if (fg != lastApp) {
            // Another window flickering in front for a moment and going again isn't arriving from it.
            val bounce = fg == appBefore && now - lastAppSince < BOUNCE_MS
            enteredFromApp = !bounce && lastApp?.let { !isHome(it) } ?: false
            cameFrom = lastApp.takeIf { enteredFromApp }
            if (store.settings.value.verboseLogging) Log.i(TAG, "front $fg from $lastApp link=$enteredFromApp")
            appBefore = lastApp
            lastApp = fg
            lastAppSince = now
        }
        val s = store.settings.value
        val tracked = store.rules.value.forPackage(fg) != null || s.tracked.any { it.packageName == fg }
        if (tracked) store.mutateToday(now) { it.copy(opens = it.opens.plusCount(fg)) }
    }

    // ---- generic per-app limits (apps without detection rules) ----

    private fun genericLimitCover(fg: String?, now: Long): Cover? {
        fg ?: return null
        val s = store.settings.value
        if (!s.protectionOn) return null
        val tracked = s.tracked.firstOrNull { it.packageName == fg && it.dailyLimitMinutes > 0 } ?: return null
        if ((genericUnlocks[fg] ?: 0L) > now) return null
        val today = store.today(now)
        if ((today.appSeconds[fg] ?: 0) < tracked.dailyLimitMinutes * 60) return null
        val focus = Engine.isFocusActive(s, now, ZoneId.systemDefault())
        val unlocksLeft = (s.unlocksPerDay - today.unlocks).coerceAtLeast(0)
        return Cover(
            reason = CoverReason.APP_LIMIT,
            app = fg,
            appName = tracked.label,
            surface = null,
            surfaceLabel = tracked.label,
            navTargets = emptyList(),
            offerBack = false,
            canUnlock = s.canUnlock(focus) && unlocksLeft > 0,
            unlockMinutes = s.unlockMinutes,
            unlockVideos = 0,
            unlockWaitSeconds = s.unlockWaitSeconds,
            unlocksLeft = unlocksLeft,
            pauseSeconds = s.pauseSeconds,
            pauseMinutes = s.pauseMinutes,
            bottomPx = null,
        )
    }

    private fun isGeneric(cover: Cover) = cover.app.contains('.')

    // ---- cover buttons ----

    /**
     * One tap, one action. A tap that seems to do nothing gets tapped again, and a second Back on top of
     * the first leaves the app.
     */
    private fun tapTaken(): Boolean {
        val t = SystemClock.uptimeMillis()
        if (t - lastCoverTapAt < COVER_TAP_GAP_MS) return false
        lastCoverTapAt = t
        return true
    }

    override fun onNav(cover: Cover, target: String) {
        if (!tapTaken()) return
        navTo(target)
    }

    /** Taps the app's own tab for [target]; Back when it can't be found on this screen. */
    private fun navTo(target: String) {
        val rules = lastRules
        val fg = foreground
        val clicked = if (rules != null && fg != null) {
            findRoot(fg)?.let { root ->
                val (w, h) = screenSize()
                clickNav(rules, reader.read(root, fg, w, h, rules.tabWords), target)
            } ?: false
        } else {
            false
        }
        if (!clicked) {
            val label = rules?.navTarget(target)?.label ?: target
            overlays.toast(PillModel("Couldn't find $label on this screen. Going back instead.", false, store.settings.value.accent))
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        main.postDelayed({ scheduleEval() }, 350)
    }

    override fun onBack(cover: Cover) {
        if (!tapTaken()) return
        // Inside an app a link opened, Back often has nowhere to go: return to the app the link came from.
        val launch = cameFrom?.takeIf { cover.fromLink }?.let { packageManager.getLaunchIntentForPackage(it) }
        val returned = launch != null && runCatching { startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (!returned) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            main.postDelayed({ afterBack(cover) }, BACK_CHECK_MS)
        }
        main.postDelayed({ scheduleEval() }, 350)
    }

    /**
     * Back normally closes the video and lands where it was opened from (the chat). If the same cover is
     * still up, Back had nothing to close: the screen was a feed, where another Back would leave the app.
     * Take the way off the feed instead (TikTok's Inbox).
     */
    private fun afterBack(cover: Cover) {
        val still = lastDecision.takeIf { !it.held }?.cover ?: return
        if (still.app != cover.app || still.reason != cover.reason) return
        val rules = lastRules?.takeIf { it.app == cover.app } ?: return
        val target = rules.safeTarget ?: return
        val fg = foreground ?: return
        val root = findRoot(fg) ?: return
        val (w, h) = screenSize()
        val ok = clickNav(rules, reader.read(root, fg, w, h, rules.tabWords), target)
        Log.i(TAG, "back left ${cover.app} covered; ${rules.navTarget(target)?.label ?: target}: ${if (ok) "clicked" else "not found"}")
        if (ok) main.postDelayed({ scheduleEval() }, 350)
    }

    override fun onLeave(cover: Cover) {
        if (!tapTaken()) return
        if (cover.reason == CoverReason.PAUSE) {
            // "Not now" on a pause turns away from that one screen, not the whole app.
            store.mutateToday { it.copy(pausesLeft = it.pausesLeft + 1) }
            val away = cover.navTargets.firstOrNull { it.id != cover.surface }?.id
            if (away != null) {
                navTo(away)
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
                main.postDelayed({ scheduleEval() }, 350)
            }
            return
        }
        overlays.hideCover()
        media.release()
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onWaitStarted(cover: Cover) {
        waitStarts[cover.app] = System.currentTimeMillis()
    }

    override fun onUnlock(cover: Cover, videos: Boolean) {
        val now = System.currentTimeMillis()
        val s = store.settings.value
        when {
            isGeneric(cover) -> genericUnlocks[cover.app] = now + s.unlockMinutes * 60_000L
            videos -> engine.unlockVideos(cover.app, s.unlockVideos, now)
            else -> engine.unlock(cover.app, s.unlockMinutes, now)
        }
        store.mutateToday(now) { it.copy(unlocks = it.unlocks + 1) }
        skippedWaits.remove(cover.app)
        waitStarts.remove(cover.app)
        store.addLog(cover.appName, if (videos) "Unlocked ${plural(s.unlockVideos, "more video")}" else "Unlocked for ${s.unlockMinutes} min")
        overlays.hideCover()
        media.release()
        scheduleEval()
    }

    override fun onSkipWaitWithAd(cover: Cover) {
        val back = foreground ?: cover.app.takeIf { it.contains('.') } ?: ""
        SkipWaitActivity.starting()
        overlays.hideCover()
        media.release()
        runCatching {
            startActivity(SkipWaitActivity.intent(this, cover.app, cover.appName, back).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { Log.w(TAG, "couldn't open the skip-wait screen", it) }
    }

    /** One ad was watched for [app]: its next block screen goes straight to the unlock choices. */
    fun skipWait(app: String) {
        skippedWaits[app] = System.currentTimeMillis() + SKIPPED_WAIT_KEEP_MS
        waitStarts.remove(app)
        scheduleEval()
    }

    override fun onContinue(cover: Cover) {
        val s = store.settings.value
        val surface = cover.surface ?: return
        engine.continueAfterPause(cover.app, surface, s.pauseMinutes, System.currentTimeMillis())
        store.mutateToday { it.copy(pausesContinued = it.pausesContinued + 1) }
        overlays.hideCover()
        media.release()
        scheduleEval()
    }

    // ---- used by the app UI ----

    fun relock(app: String) {
        engine.relock(app)
        genericUnlocks.remove(app)
        LiveState.update { it.copy(engine = engine.snapshot()) }
        scheduleEval()
    }

    fun unlockedUntil(app: String): Long? = engine.unlockedUntil(app, System.currentTimeMillis())

    /** Unlock from inside Quiet (Today screen), same rules as the cover's button. */
    fun unlockFromApp(app: String, videos: Boolean): Boolean {
        val now = System.currentTimeMillis()
        val s = store.settings.value
        val today = store.today(now)
        val focus = Engine.isFocusActive(s, now, ZoneId.systemDefault())
        if (!s.canUnlock(focus) || today.unlocks >= s.unlocksPerDay) return false
        if (videos) engine.unlockVideos(app, s.unlockVideos, now) else engine.unlock(app, s.unlockMinutes, now)
        store.mutateToday(now) { it.copy(unlocks = it.unlocks + 1) }
        val name = store.rules.value.apps[app]?.displayName ?: app
        store.addLog(name, if (videos) "Unlocked ${plural(s.unlockVideos, "more video")}" else "Unlocked for ${s.unlockMinutes} min")
        waitStarts.remove(app)
        LiveState.update { it.copy(engine = engine.snapshot()) }
        scheduleEval()
        return true
    }

    fun requestDump() {
        dumpRequested = true
        scheduleEval()
    }

    fun refresh() = scheduleEval()

    // ---- helpers ----

    /** One of Quiet's own activities (or the ad on top of one) is the active app window. Never our overlays. */
    private fun ownScreenActive(): Boolean = runCatching {
        windows.any { w ->
            w.type == AccessibilityWindowInfo.TYPE_APPLICATION && w.isActive && w.root?.packageName?.toString() == packageName
        }
    }.getOrDefault(false)

    private fun currentForeground(): String? {
        val fromWindows = runCatching {
            windows.asSequence()
                .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                .mapNotNull { it.root?.packageName?.toString() }
                .firstOrNull { it != packageName }
        }.getOrNull()
        return fromWindows ?: rootInActiveWindow?.packageName?.toString()?.takeIf { it != packageName }
    }

    /**
     * The app's main window. Apps float popups as windows of their own above it (TikTok's inbox-count
     * bubble is one), so the biggest window of the app is read, not the top one. Equal sizes keep the
     * top one, so a full-screen dialog still wins over what's under it.
     */
    private fun findRoot(pkg: String): AccessibilityNodeInfo? {
        val rect = android.graphics.Rect()
        val fromWindows = runCatching {
            windows
                .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                .sortedByDescending { w -> w.getBoundsInScreen(rect); rect.width().toLong() * rect.height() }
                .firstOrNull { it.root?.packageName?.toString() == pkg }
                ?.let { if (Build.VERSION.SDK_INT >= 33) it.getRoot(TreeReader.PREFETCH) else it.root }
        }.getOrNull()
        return fromWindows ?: rootInActiveWindow?.takeIf { it.packageName?.toString() == pkg }
    }

    @Suppress("DEPRECATION")
    private fun screenSize(): Pair<Int, Int> {
        val wm = getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.maximumWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val m = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }
    }

    private fun screenHeight(): Int = screenSize().second

    /** The home screen app. Settings also answers the home intent (its fallback home while the phone boots), so only the default counts. */
    private fun isHome(pkg: String): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val default = runCatching {
            packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        }.getOrNull()
        // With no default picked this resolves to the system chooser ("android"); then any home app but Settings counts.
        if (default != null && default != "android") return pkg == default
        val all = runCatching { packageManager.queryIntentActivities(home, 0).map { it.activityInfo.packageName } }.getOrDefault(emptyList())
        return pkg in all && pkg != "com.android.settings"
    }

    private fun isLocked(): Boolean = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

    private fun appLabel(pkg: String): String? = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun labelFor(pkg: String): String =
        store.rules.value.forPackage(pkg)?.displayName
            ?: store.settings.value.tracked.firstOrNull { it.packageName == pkg }?.label
            ?: pkg

    private fun todayLine(): String? {
        val blocks = store.today().blocks
        return if (blocks >= 2) "$blocks feeds turned away today" else null
    }

    private fun buzz() {
        val effect = VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE)
        runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(effect)
            }
        }
    }

    private fun dump(tree: quiet.core.model.UiTree, reading: Reading) {
        val dir = store.dumpsDir() ?: return
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "${reading.app}-$stamp.json")
        val nodes = JSONArray()
        for (n in tree.nodes) {
            nodes.put(
                JSONObject().apply {
                    put("i", n.index)
                    put("p", n.parent)
                    put("d", n.depth)
                    n.id?.let { put("id", it) }
                    n.text?.let { put("text", it) }
                    n.desc?.let { put("desc", it) }
                    n.hint?.let { put("hint", it) }
                    n.cls?.let { put("cls", it) }
                    if (n.selected) put("selected", true)
                    if (tree.isSelectedNear(n)) put("selectedNear", true)
                    if (n.clickable) put("clickable", true)
                    if (n.scrollable) put("scrollable", true)
                    if (n.editable) put("editable", true)
                    if (!n.visible) put("visible", false)
                    put("b", "${n.left},${n.top},${n.right},${n.bottom}")
                },
            )
        }
        val json = JSONObject().apply {
            put("package", tree.packageName)
            put("screen", "${tree.screenWidth}x${tree.screenHeight}")
            put("truncated", tree.truncated)
            put("surface", reading.surface ?: JSONObject.NULL)
            put("kind", reading.kind?.name ?: JSONObject.NULL)
            put("signals", JSONArray(reading.signals.sorted()))
            put("signature", reading.signature)
            put("tabBarTop", reading.tabBarTop ?: JSONObject.NULL)
            put("atTop", reading.atTop)
            put("nodes", nodes)
        }
        runCatching { file.writeText(json.toString(1)) }
            .onSuccess {
                Log.i(TAG, "dump saved ${file.absolutePath}")
                LiveState.update { it.copy(lastDump = file.absolutePath) }
                overlays.toast(PillModel("Saved screen dump: ${file.name}", false, store.settings.value.accent), 2500)
            }
            .onFailure { Log.w(TAG, "dump failed", it) }
    }

    // ---- receivers ----

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    store.flushNow()
                    // Nothing stays muted or drawn while the phone is locked: music on the lock screen
                    // must play. The next look after unlocking puts back whatever is still needed.
                    overlays.hideAll()
                    media.release()
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    lastTick = System.currentTimeMillis()
                    scheduleEval()
                }
                Intent.ACTION_USER_PRESENT -> scheduleEval()
            }
        }
    }

    /**
     * Debug hooks for adb, e.g.
     * `adb shell am broadcast -a app.quiet.DEBUG --es cmd dump -p app.quiet`
     * Commands: dump, reload, state, relock. Debug builds only: any app could send these.
     */
    private val debugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getStringExtra("cmd")) {
                "dump" -> requestDump()
                "reload" -> {
                    store.reloadRules()
                    Log.i(TAG, "rules reloaded: ${store.rules.value.sources} errors=${store.rules.value.errors}")
                    scheduleEval()
                }
                "relock" -> lastRules?.let { relock(it.app) }
                else -> Log.i(TAG, "state ${LiveState.state.value.copy(decision = null)} decision=${lastDecision.cover?.key}")
            }
        }
    }

    private fun registerReceivers() {
        val screen = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, screen, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiversRegistered = true
        if (BuildConfig.DEBUG) {
            ContextCompat.registerReceiver(this, debugReceiver, IntentFilter(DEBUG_ACTION), ContextCompat.RECEIVER_EXPORTED)
            debugReceiverRegistered = true
        }
    }

    companion object {
        const val TAG = Store.TAG
        const val DEBUG_ACTION = "app.quiet.DEBUG"
        private const val TICK_MS = 1_000L
        private const val MAX_TICK_GAP_MS = 5_000L
        private const val EVAL_THROTTLE_MS = 250L
        private const val BOUNCE_MS = 2_000L
        private const val EVAL_MIN_DELAY_MS = 40L
        private const val HELD_RECHECK_MS = 120L
        private const val COVER_TAP_GAP_MS = 800L
        /** Long enough for Back to close a video and the screen under it to be read. */
        private const val BACK_CHECK_MS = 900L
        /** How long a wait skipped with an ad stays skipped if no unlock is picked. */
        private const val SKIPPED_WAIT_KEEP_MS = 10 * 60_000L

        @Volatile
        var instance: QuietService? = null
            private set

        /** Whether the user has switched the service on in system settings (it may still be starting). */
        fun isEnabled(context: Context): Boolean {
            val enabled = AndroidSettings.Secure.getString(
                context.contentResolver,
                AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return enabled.split(':').any { it.startsWith(context.packageName + "/") }
        }
    }
}
