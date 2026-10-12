package app.quiet.data.policy

import app.quiet.data.prefs.PresetIds
import quiet.core.rules.Policy

object PresetDefinitions {
    val feedSurfaces = listOf(
        "instagram.home_feed",
        "instagram.reels",
        "tiktok.for_you",
        "tiktok.following",
    )
    val storiesSurfaces = listOf("instagram.stories")
    val exploreSurfaces = listOf("instagram.explore", "tiktok.discover")
    val friendsSurfaces = listOf("instagram.dm_shared_reel", "tiktok.dm_shared_video")

    fun policiesForPreset(presetId: String): Map<String, Policy> {
        val map = mutableMapOf<String, Policy>()
        when (presetId) {
            PresetIds.STRICT -> {
                feedSurfaces.forEach { map[it] = Policy.BLOCK }
                storiesSurfaces.forEach { map[it] = Policy.BLOCK }
                exploreSurfaces.forEach { map[it] = Policy.BLOCK }
            }
            PresetIds.LIGHT -> {
                feedSurfaces.forEach { map[it] = Policy.GATE }
                storiesSurfaces.forEach { map[it] = Policy.ALLOW }
                exploreSurfaces.forEach { map[it] = Policy.ALLOW }
            }
            else -> {
                feedSurfaces.forEach { map[it] = Policy.BLOCK }
                storiesSurfaces.forEach { map[it] = Policy.GATE }
                exploreSurfaces.forEach { map[it] = Policy.GATE }
            }
        }
        return map
    }

    fun breaksForPreset(presetId: String): Int = when (presetId) {
        PresetIds.STRICT -> 1
        PresetIds.LIGHT -> 5
        else -> 3
    }

    fun quietHoursForPreset(presetId: String): Pair<Boolean, Int> = when (presetId) {
        PresetIds.STRICT -> Pair(true, 22 * 60)
        else -> Pair(false, 22 * 60)
    }

    fun friendsReelsForPreset(presetId: String): Map<String, Policy> = when (presetId) {
        PresetIds.LIGHT -> friendsSurfaces.associateWith { Policy.ALLOW }
        else -> friendsSurfaces.associateWith { Policy.BLOCK }
    }
}
