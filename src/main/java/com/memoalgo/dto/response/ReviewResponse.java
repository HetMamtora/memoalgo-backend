package com.memoalgo.dto.response;

import com.memoalgo.entity.Review;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ReviewResponse — returned after submitting a review rating.
 *
 * Shows the user the updated SM-2 state so the frontend can display
 * "Next review in 6 days" on the completion screen.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID problemId;
    private String problemTitle;
    private BigDecimal easeFactor;
    private Integer intervalDays;
    private Integer repetitionCount;
    private LocalDate nextReviewDate;

    public static ReviewResponse fromEntity(Review review){
        return ReviewResponse.builder()
                .problemId(review.getProblem().getId())
                .problemTitle(review.getProblem().getTitle())
                .easeFactor(review.getEaseFactor())
                .intervalDays(review.getIntervalDays())
                .repetitionCount(review.getRepetitionCount())
                .nextReviewDate(review.getNextReviewDate())
                .build();
    }
}
