package com.memoalgo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * ReviewHistory entity — append-only log of every review.
 *
 * Purpose: Track user's review history for statistics, streaks, heatmaps.
 * Never updated or deleted — only appended to.
 *
 * Every time a user rates a problem, a new row is inserted here.
 * The Review table (above) updates its SM-2 state, but history keeps a permanent record.
 *
 * Fields:
 * - quality: 0=Blackout, 1=Wrong, 2=Wrong+hint, 3=Hard, 4=Good, 5=Perfect
 *   (UI maps: Again=1, Hard=3, Good=4, Easy=5)
 * - easeFactor*: the SM-2 ease before/after this review
 * - interval*: the interval before/after this review
 * - reviewedAt: timestamp of when this review happened
 *
 * Relationships:
 * - ManyToOne with Review: which review state this belongs to
 * - ManyToOne with User: for efficient user history queries
 */

@Entity
@Table(name = "review_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewHistory extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "review_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_history_review")
    )
    private Review review;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_history_user")
    )
    private User user;

    @Column(name = "quality", nullable = false)
    private Integer quality; // 0-5

    @Column(name = "ease_factor_before", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactorBefore;

    @Column(name = "ease_factor_after", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactorAfter;

    @Column(name = "interval_before", nullable = false)
    private Integer intervalBefore;

    @Column(name = "interval_after", nullable = false)
    private Integer intervalAfter;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;
}