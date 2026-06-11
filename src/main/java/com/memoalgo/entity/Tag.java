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

import java.util.HashSet;
import java.util.Set;

/**
 * Tag entity — user-created tags for organizing problems.
 *
 * User-scoped: each user has their own tag namespace.
 * Two users can both have a tag called "revisit" without collision.
 * Constraint: (user_id, name) is unique, not just name.
 *
 * Fields:
 * - name: tag name (e.g., "revisit", "tricky", "slow-approach")
 *
 * Relationships:
 * - ManyToOne with User: which user owns this tag
 * - ManyToMany with Problem (via ProblemTag): which problems have this tag
 */

@Entity
@Table(
        name ="tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_tags_user_name",
                columnNames = {"user_id", "name"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag extends BaseEntity{

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tags_user")
    )
    private User user;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @OneToMany(mappedBy = "tag")
    @Builder.Default
    private Set<ProblemTag> problemTags = new HashSet<>();
}
