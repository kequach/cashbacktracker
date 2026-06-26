package com.cashbacktracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MilestonePolicyTest {
    @Test
    fun calculatesProgressTowardNextMilestoneFromTotalPaidAmount() {
        val progress = MilestonePolicy.calculateProgress(
            paidTotalMinor = 2_100L,
            milestonesMinor = listOf(500L, 1_000L, 2_500L),
        )

        assertEquals(2_500L, progress.nextMilestoneMinor)
        assertEquals(400L, progress.remainingMinor)
        assertEquals(0.84f, progress.progress, 0.001f)
    }

    @Test
    fun doesNotResetProgressToZeroWhenMilestoneWasJustReached() {
        val progress = MilestonePolicy.calculateProgress(
            paidTotalMinor = 1_000L,
            milestonesMinor = listOf(500L, 1_000L, 2_500L),
        )

        assertEquals(2_500L, progress.nextMilestoneMinor)
        assertEquals(1_500L, progress.remainingMinor)
        assertEquals(0.4f, progress.progress, 0.001f)
    }

    @Test
    fun returnsToLowerMilestoneWhenPaidAmountDecreases() {
        val beforeDecrease = MilestonePolicy.calculateProgress(
            paidTotalMinor = 2_600L,
            milestonesMinor = listOf(500L, 1_000L, 2_500L, 5_000L),
        )
        val afterDecrease = MilestonePolicy.calculateProgress(
            paidTotalMinor = 2_400L,
            milestonesMinor = listOf(500L, 1_000L, 2_500L, 5_000L),
        )

        assertEquals(5_000L, beforeDecrease.nextMilestoneMinor)
        assertEquals(2_500L, afterDecrease.nextMilestoneMinor)
        assertEquals(100L, afterDecrease.remainingMinor)
        assertEquals(0.96f, afterDecrease.progress, 0.001f)
    }

    @Test
    fun showsCompleteProgressAfterAllMilestonesAreReached() {
        val progress = MilestonePolicy.calculateProgress(
            paidTotalMinor = 120_000L,
            milestonesMinor = listOf(500L, 1_000L, 100_000L),
        )

        assertNull(progress.nextMilestoneMinor)
        assertEquals(0L, progress.remainingMinor)
        assertEquals(1f, progress.progress, 0.001f)
    }
}
