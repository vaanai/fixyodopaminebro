package app.quiet.data.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import quiet.core.enforce.EnforcementMemory
import quiet.core.enforce.SurfacePass
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EnforcementMemorySynchronizerTest {
    private val synchronizer = EnforcementMemorySynchronizer()

    @Before
    fun resetHolder() {
        publishHolder(breakUntil = 42L, passes = emptyList())
    }

    @Test
    fun assignOutsideLock_losesPassPublishedBetweenMergeAndAssign() {
        val slot = MemorySlot(EnforcementMemory())
        publishHolder(breakUntil = 42L, passes = emptyList())

        synchronizer.mergeFromHolderBroken(
            getMemory = { slot.memory },
            setMemory = { slot.memory = it },
            betweenLockAndAssign = {
                synchronizer.commitUpdated(
                    setMemory = { slot.memory = it },
                    updated = EnforcementMemory(
                        passes = listOf(SurfacePass("instagram.reels", 9_999L)),
                        breakUntilEpochMs = 42L,
                        breaksRemainingToday = 2,
                        breakAllowancePerDay = 3,
                        breakDayKey = "day",
                    ),
                )
            },
        )

        assertTrue(slot.memory.passes.isEmpty())
    }

    @Test
    fun concurrentHeartbeatMergeAndPassPublish_doNotDropEachOther() {
        val slot = MemorySlot(EnforcementMemory())
        val executor = Executors.newFixedThreadPool(4)
        val rounds = 40
        val done = CountDownLatch(rounds * 2)
        val errors = Collections.synchronizedList(mutableListOf<Throwable>())

        repeat(rounds) {
            executor.execute {
                try {
                    synchronizer.mergeFromHolder(
                        getMemory = { slot.memory },
                        setMemory = { slot.memory = it },
                    )
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    done.countDown()
                }
            }
            executor.execute {
                try {
                    publishHolder(
                        breakUntil = 42L,
                        passes = listOf("instagram.reels" to 9_999L),
                    )
                    synchronizer.commitUpdated(
                        setMemory = { slot.memory = it },
                        updated = EnforcementMemory(
                            passes = listOf(SurfacePass("instagram.reels", 9_999L)),
                            breakUntilEpochMs = 42L,
                            breaksRemainingToday = 2,
                            breakAllowancePerDay = 3,
                            breakDayKey = "day",
                        ),
                    )
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    done.countDown()
                }
            }
        }

        assert(done.await(10, TimeUnit.SECONDS))
        executor.shutdown()
        assert(errors.isEmpty())

        synchronizer.mergeFromHolder(
            getMemory = { slot.memory },
            setMemory = { slot.memory = it },
        )
        assertEquals(42L, slot.memory.breakUntilEpochMs)
        val pass = slot.memory.passes.firstOrNull { it.surfaceId == "instagram.reels" }
        assertNotNull(pass)
        assertEquals(9_999L, pass!!.untilEpochMs)
    }

    private fun publishHolder(breakUntil: Long?, passes: List<Pair<String, Long>>) {
        PolicySnapshotHolder.update(
            RuntimePolicySnapshot(
                loaded = true,
                settingsLoadFailed = false,
                presetId = "balanced",
                surfacePolicies = emptyMap(),
                friendsReels = emptyMap(),
                quietHoursEnabled = false,
                quietHoursStartMinutes = 0,
                quietHoursEndMinutes = 0,
                breaksPerDay = 3,
                breaksRemainingToday = 2,
                breakDayKey = "day",
                breakUntilEpochMs = breakUntil,
                gateMinutes = 10,
                passes = passes,
            ),
        )
    }

    private class MemorySlot(var memory: EnforcementMemory)
}
