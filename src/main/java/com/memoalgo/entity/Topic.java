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
 * Topic entity — hierarchical DSA topics.
 *
 * Self-referential: parent_topic_id creates a tree structure.
 * Example:
 *   Trees (parent = null)
 *     ├─ Binary Trees (parent = Trees)
 *     │   ├─ BST (parent = Binary Trees)
 *     │   └─ AVL (parent = Binary Trees)
 *     └─ N-ary Trees (parent = Trees)
 *
 * Querying subtrees uses a recursive CTE (covered in stats queries on Day 7).
 *
 * Fields:
 * - name: topic name (unique globally)
 * - description: optional explanation
 * - parentTopic: self-reference to parent (null if root topic)
 *
 * Relationships:
 * - OneToMany with Problem: problems belong to a topic
 * - OneToMany with Topic: child topics
 */

@Entity
@Table(name = "topics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topic extends BaseEntity{
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(
            name = "parent_topic_id",
            foreignKey = @ForeignKey(name = "fk_topics_parent")
    )
    private Topic parentTopic;

    @OneToMany(mappedBy = "parentTopic")
    @Builder.Default
    private Set<Topic> childTopics = new HashSet<>();

    @OneToMany(mappedBy = "topic")
    @Builder.Default
    private Set<Problem> problems = new HashSet<>();
}
