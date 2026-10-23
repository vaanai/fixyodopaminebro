package app.quiet.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log

/**
 * Silences the video playing behind a block screen. Taking audio focus makes Instagram and TikTok
 * pause on their own; muting the media stream catches the ones that ignore focus.
 */
class MediaPauser(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val prefs = context.getSharedPreferences("media", Context.MODE_PRIVATE)
    private var request: AudioFocusRequest? = null

    /** If the service died while muted, give the sound back. */
    fun recover() {
        if (prefs.getBoolean(KEY_MUTED, false)) unmute()
    }

    fun engage() {
        if (request == null) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build(),
                )
                .setOnAudioFocusChangeListener { }
                .build()
            runCatching { audio?.requestAudioFocus(req) }
            request = req
        }
        if (!prefs.getBoolean(KEY_MUTED, false)) {
            val alreadyMuted = audio?.isStreamMute(AudioManager.STREAM_MUSIC) == true
            if (!alreadyMuted) {
                runCatching { audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0) }
                    .onSuccess { prefs.edit().putBoolean(KEY_MUTED, true).apply() }
                    .onFailure { Log.w("Quiet", "mute failed", it) }
            }
        }
    }

    fun release() {
        request?.let { runCatching { audio?.abandonAudioFocusRequest(it) } }
        request = null
        if (prefs.getBoolean(KEY_MUTED, false)) unmute()
    }

    private fun unmute() {
        runCatching { audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0) }
        prefs.edit().putBoolean(KEY_MUTED, false).apply()
    }

    private companion object {
        const val KEY_MUTED = "we_muted"
    }
}
