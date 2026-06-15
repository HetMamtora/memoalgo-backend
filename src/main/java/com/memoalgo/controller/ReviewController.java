package com.memoalgo.controller;

import com.memoalgo.dto.request.ReviewRequest;
import com.memoalgo.dto.response.ReviewResponse;
import com.memoalgo.dto.response.ReviewSessionResponse;
import com.memoalgo.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ReviewController — endpoints for daily review sessions.
 *
 * Routes:
 *   GET  /api/v1/reviews/due            → get today's review queue
 *   POST /api/v1/reviews/{problemId}    → submit rating for a problem
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Daily spaced repetition review sessions")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/due")
    @Operation(
            summary = "Get today's review queue",
            description = "Returns all problems due for review today, oedered by due date ascending"
    )
    public ResponseEntity<ReviewSessionResponse> getDueProblems(){
        return ResponseEntity.ok(reviewService.getDueProblems());
    }

    @PostMapping("/{problemId}")
    @Operation(
            summary = "Submit a review rating",
            description = "Rate recall quality 1=Again, 3=Hard, 4=Good, 5=Easy." +
                    "SM-2 algorithm calculates and stores next review date."
    )
    public ResponseEntity<ReviewResponse> submitReview(
            @PathVariable UUID problemId,
            @Valid @RequestBody ReviewRequest request){

        return ResponseEntity.ok(reviewService.submitReview(problemId, request));
    }
}
