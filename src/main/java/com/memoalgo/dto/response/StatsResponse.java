package com.memoalgo.dto.response;

import lombok.*;

import java.util.Map;

/**
 * StatsResponse — returned by GET /api/v1/stats
 *
 * Powers the dashboard and stats page.
 *
 * Fields:
 * - totalProblems:      total active problems in the user's library
 * - dueToday:           problems due for review today
 * - currentStreak:      consecutive days with at least 1 review
 * - totalReviews:       all-time review count
 * - retentionRate:      % of reviews rated Good (4) or Easy (5)
 * - problemsByDifficulty: {"EASY": 80, "MEDIUM": 120, "HARD": 40}
 * - problemsByTopic:    {"Arrays & Strings": 30, "Trees": 25, ...}
 * - retentionByTopic:   {"Arrays & Strings": 91.0, "Graphs": 38.0, ...}
 *                        -- % rated Good/Easy, per topic. Only includes
 *                        topics with at least 1 review (a 0/0 rate is
 *                        meaningless, not 0%).
 * - reviewsByDay:       {"2026-06-20": 5, "2026-06-19": 3, ...}
 *                        -- review count per day for the last 35 days,
 *                        only includes days with >= 1 review (missing
 *                        day = 0 reviews, frontend fills the gaps)
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {

    private long totalProblems;
    private long dueToday;
    private int currentStreak;
    private long totalReviews;
    private double retentionRate;
    private Map<String, Long> problemsByDifficulty;
    private Map<String, Long> problemsByTopic;
    private Map<String, Double> retentionByTopic;
    private Map<String, Long> reviewsByDay;
}
