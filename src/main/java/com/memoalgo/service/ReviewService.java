package com.memoalgo.service;

import com.memoalgo.algorithm.SpacedRepetitionAlgorithm;
import com.memoalgo.dto.request.ReviewRequest;
import com.memoalgo.dto.response.ProblemResponse;
import com.memoalgo.dto.response.ReviewResponse;
import com.memoalgo.dto.response.ReviewSessionResponse;
import com.memoalgo.entity.Problem;
import com.memoalgo.entity.Review;
import com.memoalgo.entity.ReviewHistory;
import com.memoalgo.entity.User;
import com.memoalgo.exception.ResourceNotFoundException;
import com.memoalgo.repository.ProblemRepository;
import com.memoalgo.repository.ProblemTagRepository;
import com.memoalgo.repository.ReviewHistoryRepository;
import com.memoalgo.repository.ReviewRepository;
import com.memoalgo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ReviewService — orchestrates spaced repetition review sessions.
 *
 * Depends on SpacedRepetitionAlgorithm INTERFACE, not SM2Algorithm directly.
 * This is the Strategy Pattern in action — swap the algorithm, nothing here changes.
 *
 * Two core operations:
 *
 * 1. getDueProblems():
 *    Fetches all reviews where nextReviewDate <= today.
 *    Returns problems sorted by due date (oldest first = highest priority).
 *    This is the Priority Queue concept: problems waiting longest go first.
 *
 * 2. submitReview():
 *    Accepts user's quality rating.
 *    Runs SM-2 calculation.
 *    Updates Review (live state) + appends ReviewHistory (permanent log).
 *    Two writes in one @Transactional — both succeed or both rollback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final ProblemRepository problemRepository;
    private final ProblemTagRepository problemTagRepository;
    private final SpacedRepetitionAlgorithm spacedRepetitionAlgorithm;
    private final SecurityUtils securityUtils;

    /**
     * Get all problems due for review today (or overdue).
     *
     * DSA concept: Priority Queue behaviour —
     * problems due longest ago have highest priority.
     * SQL ORDER BY nextReviewDate ASC implements this ordering.
     */
    @Transactional(readOnly = true)
    public ReviewSessionResponse getDueProblems() {
        User currentUser = securityUtils.getCurrentUser();
        List<Review> dueReviews = reviewRepository.findDueToday(currentUser, LocalDate.now());

        List<ProblemResponse> dueProblems = dueReviews.stream()
                .map(review -> {
                    Problem problem = review.getProblem();
                    var tags = Set.copyOf(problemTagRepository.findByProblem(problem));
                    return ProblemResponse.fromEntity(problem, tags);
                })
                .collect(Collectors.toList());

        return ReviewSessionResponse.builder()
                .dueCount(dueProblems.size())
                .dueProblems(dueProblems)
                .build();
    }

    /**
     * Submit a review rating for a problem.
     *
     * Flow:
     * 1. Load the Review state for this problem
     * 2. Run SM-2 algorithm with user's quality rating
     * 3. Update Review entity with new state
     * 4. Append a ReviewHistory entry (permanent audit log)
     * 5. Return updated ReviewResponse
     *
     * @Transactional: both the Review update and ReviewHistory insert
     * happen in a single DB transaction. If either fails, both roll back.
     * This prevents partial state (state updated but history missing).
     */
    @Transactional
    public ReviewResponse submitReview(UUID problemId, ReviewRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // Load the problem and validate ownership
        Problem problem = problemRepository.findByIdAndUser(problemId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", problemId));

        // Get or create Review state (create on first review of this problem)
        Review review = reviewRepository.findByProblemAndUser(problem, currentUser)
                .orElseGet(() -> createInitialReview(problem, currentUser));

        // Capture before-state for history log
        BigDecimal easeFactorBefore = review.getEaseFactor();
        int intervalBefore = review.getIntervalDays();

        // Run the SM-2 algorithm — pure function, no side effects
        SpacedRepetitionAlgorithm.AlgorithmResult result =
                spacedRepetitionAlgorithm.calculate(review, request.getQuality());

        // Update Review with new SM-2 state
        review.setEaseFactor(BigDecimal.valueOf(result.easeFactor()));
        review.setIntervalDays(result.intervalDays());
        review.setRepetitionCount(result.repetitionCount());
        review.setNextReviewDate(LocalDate.now().plusDays(result.intervalDays()));
        review.setLastReviewedAt(Instant.now());
        Review savedReview = reviewRepository.save(review);

        // Append to ReviewHistory (permanent, append-only log)
        ReviewHistory history = ReviewHistory.builder()
                .review(savedReview)
                .user(currentUser)
                .quality(request.getQuality())
                .easeFactorBefore(easeFactorBefore)
                .easeFactorAfter(savedReview.getEaseFactor())
                .intervalBefore(intervalBefore)
                .intervalAfter(result.intervalDays())
                .reviewedAt(Instant.now())
                .build();
        reviewHistoryRepository.save(history);

        log.info("Review submitted: problem='{}' quality={} nextReview={}",
                problem.getTitle(), request.getQuality(), savedReview.getNextReviewDate());

        return ReviewResponse.fromEntity(savedReview);
    }

    /**
     * Create an initial Review record for a problem being reviewed for the first time.
     * Sets all SM-2 values to defaults and nextReviewDate to today.
     */
    private Review createInitialReview(Problem problem, User user) {
        Review review = Review.builder()
                .problem(problem)
                .user(user)
                .nextReviewDate(LocalDate.now())
                .build();
        return reviewRepository.save(review);
    }
}