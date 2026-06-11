package com.memoalgo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

/**
 * Problem entity — a DSA problem added and tracked by a user.
 *
 * Fields:
 * - title: problem name (e.g., "Two Sum", "Binary Tree Level Order")
 * - url: external link (LeetCode, HackerRank, etc.)
 * - difficulty: EASY, MEDIUM, HARD (enum in real app, string here for simplicity)
 * - notes: user's personal notes/hints for this problem
 * - isActive: soft-delete flag
 *
 * Relationships:
 * - ManyToOne with User: problem owner
 * - ManyToOne with Topic: which topic this belongs to
 * - OneToOne with Review: SM-2 state (created automatically on first review)
 * - ManyToMany with Tag (via ProblemTag junction table)
 */

@Entity
@Table(name = "problems")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem extends BaseEntity{

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_problems_user")
    )
    private User user;

    @ManyToOne
    @JoinColumn(
            name = "topic_id",
            foreignKey = @ForeignKey(name = "fk_problems_topic")
    )
    private Topic topic;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "difficulty", nullable = false, length = 10)
    private String difficulty;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "problem")
    @Builder.Default
    private Set<Review> reviews = new HashSet<>();

    @OneToMany(mappedBy = "problem")
    @Builder.Default
    private Set<ProblemTag> problemTags = new HashSet<>();

}
