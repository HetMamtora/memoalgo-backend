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
}
