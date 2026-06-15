package com.memoalgo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * ReviewSessionResponse — returned by GET /api/v1/reviews/due
 *
 * Wraps the list of due problems plus session metadata
 * so the frontend knows how many cards are in the queue.
 *
 * The frontend uses dueCount to show "7 problems due today"
 * and the progress bar during the session.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSessionResponse {

    private int dueCount;
    private List<ProblemResponse> dueProblems;
}
