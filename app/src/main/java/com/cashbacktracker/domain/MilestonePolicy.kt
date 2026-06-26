package com.cashbacktracker.domain

data class MilestoneProgress(
    val nextMilestoneMinor: Long?,
    val remainingMinor: Long,
    val progress: Float,
)

object MilestonePolicy {
    val DEFAULT_MILESTONES_MINOR = listOf(
        500L,
        1_000L,
        2_500L,
        5_000L,
        10_000L,
        15_000L,
        25_000L,
        50_000L,
        75_000L,
        100_000L,
    )

    fun calculateProgress(
        paidTotalMinor: Long,
        milestonesMinor: List<Long>,
    ): MilestoneProgress {
        val positiveMilestones = milestonesMinor
            .filter { it > 0L }
            .sorted()
        val safePaidTotal = paidTotalMinor.coerceAtLeast(0L)
        val nextMilestone = positiveMilestones.firstOrNull { it > safePaidTotal }

        if (nextMilestone == null) {
            return MilestoneProgress(
                nextMilestoneMinor = null,
                remainingMinor = 0L,
                progress = 1f,
            )
        }

        return MilestoneProgress(
            nextMilestoneMinor = nextMilestone,
            remainingMinor = (nextMilestone - safePaidTotal).coerceAtLeast(0L),
            progress = (safePaidTotal.toFloat() / nextMilestone.toFloat()).coerceIn(0f, 1f),
        )
    }
}
