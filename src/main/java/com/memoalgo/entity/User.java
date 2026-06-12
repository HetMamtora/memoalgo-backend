package com.memoalgo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.parsing.Problem;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity — represents a registered user.
 *
 * Fields:
 * - email: unique, used for login
 * - username: unique, display name
 * - passwordHash: BCrypt hashed password (never store plaintext!)
 * - isActive: soft-delete flag (false = inactive/deleted user)
 * - lastActiveAt: timestamp of last login (for engagement tracking)
 *
 * Relationships:
 * - OneToMany with Problem: user owns problems
 * - OneToMany with Review: user has review state for each problem
 * - OneToMany with Tag: user creates tags
 * - OneToMany with ReviewHistory: logs all reviews by this user
 */

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity{

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    @OneToMany(mappedBy = "user", targetEntity = Problem.class)
    @Builder.Default
    private Set<Problem> problems = new HashSet<>();

    @OneToMany(mappedBy = "user", targetEntity = Review.class)
    @Builder.Default
    private Set<Review> reviews = new HashSet<>();

    @OneToMany(mappedBy = "user", targetEntity = Tag.class)
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "user", targetEntity = ReviewHistory.class)
    @Builder.Default
    private Set<ReviewHistory> reviewHistories = new HashSet<>();
}
