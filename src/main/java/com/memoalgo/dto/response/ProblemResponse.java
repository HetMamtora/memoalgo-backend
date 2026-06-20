package com.memoalgo.dto.response;

import com.memoalgo.entity.Problem;
import com.memoalgo.entity.ProblemTag;
import com.memoalgo.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ProblemResponse — returned by all problem endpoints.
 *
 * Includes topic name (not just ID) so frontend doesn't need
 * a second API call to display "Arrays & Strings" next to a problem.
 *
 * Tags are returned as a Set of strings for easy frontend rendering.
 *
 * nextReviewDate is null until the problem has been reviewed at least
 * once — a Review row is only created on first review (see the Review
 * entity's javadoc), so there's genuinely nothing to report before that.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemResponse {

    private UUID id;
    private String title;
    private String url;
    private String difficulty;
    private String notes;
    private UUID topicId;
    private String topicName;
    private Set<String> tags;
    private LocalDate nextReviewDate;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Static factory: convert a Problem entity + its ProblemTag set
     * into a clean ProblemResponse DTO.
     *
     * We pass problemTags separately because we may load them
     * independently to avoid N+1 queries in list operations.
     *
     * @param review the problem's Review row, or null if it has never
     *               been reviewed yet (nextReviewDate will be null)
     */
    public static ProblemResponse fromEntity(Problem problem, Set<ProblemTag> problemTags, Review review) {
        Set<String> tagNames = problemTags.stream()
                .map(pt -> pt.getTag().getName())
                .collect(Collectors.toSet());

        return ProblemResponse.builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .url(problem.getUrl())
                .difficulty(problem.getDifficulty())
                .notes(problem.getNotes())
                .topicId(
                        problem.getTopic() != null
                                ? problem.getTopic().getId()
                                : null
                )
                .topicName(
                        problem.getTopic() != null
                                ? problem.getTopic().getName()
                                : null
                )
                .tags(tagNames)
                .nextReviewDate(review != null ? review.getNextReviewDate() : null)
                .createdAt(problem.getCreatedAt())
                .updatedAt(problem.getUpdatedAt())
                .build();
    }
}