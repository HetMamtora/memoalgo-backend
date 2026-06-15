package com.memoalgo.service;

import com.memoalgo.dto.response.StatsResponse;
import com.memoalgo.entity.ReviewHistory;
import com.memoalgo.entity.User;
import com.memoalgo.repository.ProblemRepository;
import com.memoalgo.repository.ReviewHistoryRepository;
import com.memoalgo.repository.ReviewRepository;
import com.memoalgo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * StatsService — computes dashboard statistics from ReviewHistory.
 *
 * All stats are derived from the append-only ReviewHistory table.
 * This is a read-only service — no writes, only aggregation.
 *
 * Streak algorithm:
 *   Walk backwards day by day from today.
 *   Each day that has at least one review increments the streak.
 *   First gap breaks the streak.
 *
 * Retention rate:
 *   (reviews rated 4 or 5) / (total reviews) * 100
 *   Reviews where user recalled correctly without "Again".
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final ProblemRepository problemRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final SecurityUtils securityUtils;

    public StatsResponse getStats() {
        User currentUser = securityUtils.getCurrentUser();

        // Total active problems in library
        long totalProblems = problemRepository
                .findByUserAndIsActiveTrue(currentUser).size();

        // Problems due today
        long dueToday = reviewRepository.countDueReviews(currentUser);

        // All review history for this user
        List<ReviewHistory> allHistory =
                reviewHistoryRepository.findByUserOrderByReviewedAtDesc(currentUser);

        long totalReviews = allHistory.size();

        // Retention rate: percentage of Good (4) or Easy (5) ratings
        double retentionRate = totalReviews == 0 ? 0.0 :
                allHistory.stream()
                        .filter(rh -> rh.getQuality() >= 4)
                        .count() * 100.0 / totalReviews;

        // Current streak (consecutive days with reviews)
        int currentStreak = calculateStreak(allHistory);

        // Problems grouped by difficulty
        Map<String, Long> problemsByDifficulty =
                problemRepository.findByUserAndIsActiveTrue(currentUser).stream()
                        .collect(Collectors.groupingBy(
                                p -> p.getDifficulty(),
                                Collectors.counting()
                        ));

        // Problems grouped by topic name
        Map<String, Long> problemsByTopic =
                problemRepository.findByUserAndIsActiveTrue(currentUser).stream()
                        .filter(p -> p.getTopic() != null)
                        .collect(Collectors.groupingBy(
                                p -> p.getTopic().getName(),
                                Collectors.counting()
                        ));

        return StatsResponse.builder()
                .totalProblems(totalProblems)
                .dueToday(dueToday)
                .currentStreak(currentStreak)
                .totalReviews(totalReviews)
                .retentionRate(Math.round(retentionRate * 10.0) / 10.0)
                .problemsByDifficulty(problemsByDifficulty)
                .problemsByTopic(problemsByTopic)
                .build();
    }

    /**
     * Streak calculation algorithm.
     *
     * 1. Extract the unique set of dates on which the user reviewed anything
     * 2. Walk backwards from today
     * 3. Each consecutive day with a review = +1 streak
     * 4. First day with no review = stop
     *
     * Edge case: if user hasn't reviewed today yet but reviewed yesterday,
     * streak is still alive (don't penalize for not reviewing yet today).
     */
    private int calculateStreak(List<ReviewHistory> history) {
        if (history.isEmpty()) return 0;

        // Collect all unique dates that had at least one review
        var reviewDates = history.stream()
                .map(rh -> rh.getReviewedAt()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate())
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate checkDate = LocalDate.now();

        // If no review today, start checking from yesterday
        // (streak is still alive — today isn't over)
        if (!reviewDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1);
        }

        // Walk backwards until a gap is found
        while (reviewDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }
}