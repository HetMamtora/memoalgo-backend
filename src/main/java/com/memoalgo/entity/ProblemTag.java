package com.memoalgo.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProblemTag entity — junction table for many-to-many relationship.
 *
 * Allows a problem to have many tags, and a tag to be applied to many problems.
 * Example:
 *   Problem "Two Sum" can have tags: ["array", "hashmap", "easy"]
 *   Tag "hashmap" can be applied to many problems.
 *
 * Uses composite primary key (problemId, tagId) to prevent duplicate associations.
 *
 * Relationships:
 * - ManyToOne with Problem
 * - ManyToOne with Tag
 */

@Entity
@Table(name = "problem_tags")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemTag {

    @EmbeddedId
    private ProblemTagId id = new ProblemTagId();

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "problem_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_problem_tags_problem")
    )
    @MapsId("problemId")
    private Problem problem;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "tag_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_problem_tags_tag")
    )
    @MapsId("tagId")
    private Tag tag;
}
