package com.memoalgo.service;

import com.memoalgo.dto.request.ProblemRequest;
import com.memoalgo.dto.response.ProblemResponse;
import com.memoalgo.entity.Problem;
import com.memoalgo.entity.ProblemTag;
import com.memoalgo.entity.Review;
import com.memoalgo.entity.ProblemTagId;
import com.memoalgo.entity.Tag;
import com.memoalgo.entity.Topic;
import com.memoalgo.entity.User;
import com.memoalgo.exception.ResourceNotFoundException;
import com.memoalgo.repository.*;
import com.memoalgo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ProblemService — core business logic for problem management.
 *
 * Key design decisions:
 *
 * 1. OWNERSHIP ENFORCEMENT: every method calls securityUtils.getCurrentUser()
 *    and scopes all DB operations to that user. Users cannot touch other
 *    users' problems — this is enforced at the service layer, not just the DB.
 *
 * 2. SOFT DELETE: delete() sets isActive=false, not an SQL DELETE.
 *    Data is preserved for historical review stats even after "deletion".
 *
 * 3. TAG HANDLING: tags are user-scoped. On create/update we either
 *    find an existing tag or create a new one, then link to the problem.
 *    On update we clear all existing tag links and rebuild them.
 *
 * 4. TOPIC RESOLUTION: if topicId is provided, we validate it exists
 *    before setting it. If null, the problem has no topic (that's fine).
 *
 * 5. REVIEW LOOKUP (nextReviewDate on ProblemResponse): a Problem doesn't
 *    own its scheduling state — Review does, as a separate row. createProblem()
 *    now creates this row itself (see APP_ZONE comment below for why), so
 *    every active problem has exactly one Review from the moment it's
 *    created onward — getAllProblems/getProblemById/updateProblem's lookups
 *    via ReviewRepository will always find it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TopicRepository topicRepository;
    private final TagRepository tagRepository;
    private final ProblemTagRepository problemTagRepository;
    private final ReviewRepository reviewRepository;
    private final SecurityUtils securityUtils;

    // Used for computing next_review_date. Explicit on purpose: LocalDate.now()
    // with no argument uses the JVM's *default* timezone, which depends on
    // the host platform/container image, not on anything we control in code.
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Get all active problems for the authenticated user.
     * Optional filter by difficulty (EASY, MEDIUM, HARD) or topicId.
     */
    @Transactional(readOnly = true)
    public List<ProblemResponse> getAllProblems(String difficulty, UUID topicId) {
        User currentUser = securityUtils.getCurrentUser();

        List<Problem> problems = problemRepository.findByUserAndIsActiveTrue(currentUser);

        // Apply optional filters in memory (fast enough for 200-300 problems)
        return problems.stream()
                .filter(p -> difficulty == null || p.getDifficulty().equalsIgnoreCase(difficulty))
                .filter(p -> topicId == null
                        || (p.getTopic() != null && p.getTopic().getId().equals(topicId)))
                .map(p -> {
                    Set<ProblemTag> tags = Set.copyOf(problemTagRepository.findByProblem(p));
                    Review review = reviewRepository.findByProblemAndUser(p, currentUser).orElse(null);
                    return ProblemResponse.fromEntity(p, tags, review);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a single problem by ID — validates ownership.
     */
    @Transactional(readOnly = true)
    public ProblemResponse getProblemById(UUID problemId) {
        User currentUser = securityUtils.getCurrentUser();

        Problem problem = problemRepository.findByIdAndUser(problemId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", problemId));

        Set<ProblemTag> tags = Set.copyOf(problemTagRepository.findByProblem(problem));
        Review review = reviewRepository.findByProblemAndUser(problem, currentUser).orElse(null);
        return ProblemResponse.fromEntity(problem, tags, review);
    }

    /**
     * Create a new problem for the authenticated user.
     *
     * Flow:
     * 1. Resolve topic (optional)
     * 2. Build and save Problem entity
     * 3. Resolve tags (find or create) and link them
     * 4. Create the initial Review row, so the problem enters the
     *    spaced-repetition schedule immediately
     * 5. Return DTO
     */
    @Transactional
    public ProblemResponse createProblem(ProblemRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // Resolve topic if provided
        Topic topic = resolveTopicIfProvided(request.getTopicId());

        // Build and persist problem
        Problem problem = Problem.builder()
                .user(currentUser)
                .topic(topic)
                .title(request.getTitle().trim())
                .url(request.getUrl() != null ? request.getUrl().trim() : null)
                .difficulty(request.getDifficulty().toUpperCase())
                .notes(request.getNotes())
                .isActive(true)
                .build();

        Problem savedProblem = problemRepository.save(problem);

        // Handle tags
        Set<ProblemTag> savedTags = resolveAndLinkTags(request.getTags(), savedProblem, currentUser);

        // Create the initial Review row. Without this, the problem can
        // never appear in the due-review queue (that query reads from
        // `reviews`, not `problems`) -- and since the Review Session UI
        // only ever surfaces problems that are already in that queue,
        // there'd be no normal way to submit a first review either.
        // ease_factor/interval_days/repetition_count use the entity's
        // own defaults (2.50/1/0); next_review_date is "tomorrow", which
        // matches interval_days=1 (SM-2's first interval).
        Review initialReview = Review.builder()
                .problem(savedProblem)
                .user(currentUser)
                .nextReviewDate(LocalDate.now(APP_ZONE).plusDays(1))
                .build();
        Review savedReview = reviewRepository.save(initialReview);

        log.info("Problem created: '{}' by user: {}", savedProblem.getTitle(), currentUser.getEmail());

        return ProblemResponse.fromEntity(savedProblem, savedTags, savedReview);
    }

    /**
     * Update an existing problem — validates ownership.
     *
     * Tags are fully replaced on update (clear all, then add new set).
     * This is simpler and correct for our scale.
     */
    @Transactional
    public ProblemResponse updateProblem(UUID problemId, ProblemRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Problem problem = problemRepository.findByIdAndUser(problemId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", problemId));

        // Resolve topic
        Topic topic = resolveTopicIfProvided(request.getTopicId());

        // Update fields
        problem.setTitle(request.getTitle().trim());
        problem.setUrl(request.getUrl() != null ? request.getUrl().trim() : null);
        problem.setDifficulty(request.getDifficulty().toUpperCase());
        problem.setNotes(request.getNotes());
        problem.setTopic(topic);

        Problem updatedProblem = problemRepository.save(problem);

        // Replace all tags: delete existing, then add new ones
        problemTagRepository.deleteByProblem(updatedProblem);
        Set<ProblemTag> updatedTags = resolveAndLinkTags(
                request.getTags(), updatedProblem, currentUser);

        log.info("Problem updated: '{}' by user: {}", updatedProblem.getTitle(), currentUser.getEmail());

        // Every active problem has a Review row from the moment it's
        // created (see createProblem)
        Review review = reviewRepository.findByProblemAndUser(updatedProblem, currentUser).orElse(null);

        return ProblemResponse.fromEntity(updatedProblem, updatedTags, review);
    }

    /**
     * Soft-delete a problem — sets isActive=false.
     * Data is preserved for review history and stats.
     */
    @Transactional
    public void deleteProblem(UUID problemId) {
        User currentUser = securityUtils.getCurrentUser();

        Problem problem = problemRepository.findByIdAndUser(problemId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", problemId));

        problem.setIsActive(false);
        problemRepository.save(problem);

        log.info("Problem soft-deleted: '{}' by user: {}", problem.getTitle(), currentUser.getEmail());
    }

    // ── Private helpers ───────────────────────────────────────

    /**
     * Resolve topic from UUID if provided.
     * Returns null if topicId is null (problem without a topic is valid).
     */
    private Topic resolveTopicIfProvided(UUID topicId) {
        if (topicId == null) return null;

        return topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));
    }

    /**
     * Resolve tags: find existing or create new, then link to problem.
     *
     * Tags are user-scoped. "revisit" for user A is separate from
     * "revisit" for user B (enforced by uq_tags_user_name constraint).
     */
    private Set<ProblemTag> resolveAndLinkTags(
            Set<String> tagNames, Problem problem, User user) {

        if (tagNames == null || tagNames.isEmpty()) {
            return Set.of();
        }

        return tagNames.stream()
                .map(name -> name.toLowerCase().trim())
                .filter(name -> !name.isBlank())
                .map(name -> {
                    // Find existing tag or create it
                    Tag tag = tagRepository.findByUserAndName(user, name)
                            .orElseGet(() -> tagRepository.save(
                                    Tag.builder()
                                            .user(user)
                                            .name(name)
                                            .build()
                            ));

                    // Link tag to problem via junction table
                    return problemTagRepository.save(
                            ProblemTag.builder()
                                    .id(new ProblemTagId(problem.getId(), tag.getId()))
                                    .problem(problem)
                                    .tag(tag)
                                    .build()
                    );
                })
                .collect(Collectors.toSet());
    }
}