package com.memoalgo.repository;

import com.memoalgo.entity.ReviewHistory;
import com.memoalgo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ReviewHistoryRepository — Data access for ReviewHistory (append-only log).
 *
 * Used for:
 * - Streak calculation
 * - Retention rate / accuracy metrics
 * - Heatmap / activity charts
 * - All stats on the stats page
 */
@Repository
public interface ReviewHistoryRepository extends JpaRepository<ReviewHistory, UUID> {

    /**
     * Get all reviews for a user, ordered by date.
     * Used for streak, heatmap, and analytics.
     */
    List<ReviewHistory> findByUserOrderByReviewedAtDesc(User user);

    /**
     * Count reviews within a date range (for heatmap/weekly stats).
     */
    @Query("""
        SELECT COUNT(rh) FROM ReviewHistory rh
        WHERE rh.user = :user
        AND rh.reviewedAt BETWEEN :start AND :end
        """)
    Long countReviewsBetween(
            @Param("user") User user,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * Get reviews from the last N days for streak and heatmap.
     */
    @Query("""
        SELECT rh FROM ReviewHistory rh
        WHERE rh.user = :user
        AND rh.reviewedAt >= :since
        ORDER BY rh.reviewedAt DESC
        """)
    List<ReviewHistory> findRecentReviews(@Param("user") User user, @Param("since") Instant since);
}