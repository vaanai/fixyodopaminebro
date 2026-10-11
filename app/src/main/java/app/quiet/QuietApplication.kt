package app.quiet

import android.app.Application
import androidx.room.Room
import app.quiet.data.db.QuietDatabase
import app.quiet.data.policy.EnforcementStateRepository
import app.quiet.data.policy.PolicyRepository
import app.quiet.data.policy.PolicySnapshotSync
import app.quiet.data.prefs.QuietPreferences
import app.quiet.data.repo.EventRepository
import app.quiet.data.repo.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import quiet.core.QuietTiming
import java.util.concurrent.TimeUnit

class QuietApplication : Application() {
    lateinit var database: QuietDatabase
        private set
    lateinit var preferences: QuietPreferences
        private set
    lateinit var eventRepository: EventRepository
        private set
    lateinit var libraryRepository: LibraryRepository
        private set
    lateinit var policyRepository: PolicyRepository
        private set
    lateinit var enforcementStateRepository: EnforcementStateRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, QuietDatabase::class.java, "quiet.db").build()
        preferences = QuietPreferences(this)
        eventRepository = EventRepository(database.enforcementEventDao())
        libraryRepository = LibraryRepository(database.libraryItemDao())
        policyRepository = PolicyRepository(preferences)
        enforcementStateRepository = EnforcementStateRepository(preferences)
        appScope.launch {
            policyRepository.seedBalancedIfNeeded()
            PolicySnapshotSync.refresh(preferences)
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(QuietTiming.EVENT_PRUNE_DAYS.toLong())
            try {
                eventRepository.pruneOlderThan(cutoff)
            } catch (_: Exception) {
            }
        }
    }
}
