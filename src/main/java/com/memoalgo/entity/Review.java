package com.memoalgo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Review entity — the LIVE SM-2 state for a single problem per user.
 *
 * One row per (problem, user) pair. This is the heart of spaced repetition.
 *
 * SM-2 Fields:
 * - easeFactor: starts at 2.5, adjusted per review (min 1.3)
 * - intervalDays: days until next review (1 → 3 → 7 → 14 → ...)
 * - repetitionCount: how many successful reviews
 * - nextReviewDate: the date this problem surfaces in the queue
 * - lastReviewedAt: when the user last rated this problem
 *
 * The SM-2 algorithm (Day 6) updates these fields based on user's rating.
 *
 * Relationships:
 * - ManyToOne with Problem: which problem
 * - ManyToOne with User: which user
 * - OneToMany with ReviewHistory: append-only log of all reviews
 */

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reviews_problem_user",
                columnNames = {"problem_id", "user_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "problem_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_problem")
    )
    private Problem problem;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_user")
    )
    private User user;

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal easeFactor = new BigDecimal("2.50");

    @Column(name = "interval_days", nullable = false)
    @Builder.Default
    private Integer intervalDays = 1;

    @Column(name = "repetition_count", nullable = false)
    @Builder.Default
    private Integer repetitionCount = 0;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @OneToMany(mappedBy = "review", targetEntity = ReviewHistory.class)
    @Builder.Default
    private Set<ReviewHistory> histories = new HashSet<>();
}