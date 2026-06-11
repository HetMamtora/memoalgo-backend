package com.memoalgo.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * ProblemTagId — Composite primary key for ProblemTag entity.
 *
 * JPA requires composite keys to be Serializable.
 * This class represents the (problemId, tagId) pair that uniquely identifies a problem-tag association.
 *
 * Must implement equals() and hashCode() for proper JPA behavior.
 */
@EqualsAndHashCode
@NoArgsConstructor
public class ProblemTagId implements Serializable {

    public UUID problemId;
    public UUID tagId;

    public ProblemTagId(UUID problemId, UUID tagId) {
        this.problemId = problemId;
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProblemTagId that)) return false;
        return Objects.equals(problemId, that.problemId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(problemId, tagId);
    }
}