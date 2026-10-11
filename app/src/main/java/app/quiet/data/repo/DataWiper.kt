package app.quiet.data.repo

import app.quiet.data.policy.PolicyRepository
import app.quiet.data.prefs.QuietPreferences

class DataWiper(
    private val preferences: QuietPreferences,
    private val events: EventRepository,
    private val library: LibraryRepository,
    private val policies: PolicyRepository,
) {
    suspend fun deleteAllData() {
        preferences.clearAll()
        events.deleteAll()
        library.deleteAll()
        policies.applyPreset(app.quiet.data.prefs.PresetIds.BALANCED)
    }
}
