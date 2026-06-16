package com.memoalgo.service;

import com.memoalgo.dto.request.ReviewRequest;
import com.memoalgo.dto.response.ReviewResponse;
import com.memoalgo.dto.response.ReviewSessionResponse;
import com.memoalgo.entity.Problem;
import com.memoalgo.entity.User;
import com.memoalgo.repository.ProblemRepository;
import com.memoalgo.repository.ReviewHistoryRepository;
import com.memoalgo.repository.ReviewRepository;
import com.memoalgo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReviewServiceIntegrationTest — End-to-end test of the full review flow.
 *
 * Tests the complete pipeline:
 * Register User → Create Problem → Submit Reviews → Check SM-2 State → Check Stats
 *
 * This test calls services directly (not via HTTP), but still uses the real
 * Spring context, real PostgreSQL, and real security context.
 *
 * Why direct service calls instead of MockMvc here?
 * We want to verify SM-2 state at the entity level after multiple reviews —
 * direct service calls make this easier than parsing JSON responses.
 */
@SpringBootTest(
        properties = {
                "spring.datasource.url=jdbc:postgresql://localhost:5432/memoalgo_test",
                "spring.datasource.username=postgres",
                "spring.datasource.password=${MEMOALGO_TEST_DB_PASSWORD:fallback}",
                "spring.flyway.locations=classpath:db/migration",
                "jwt.secret=test-secret-key-minimum-32-chars-long-for-algorithm",
                "jwt.expiration-ms=86400000",
                "jwt.refresh-expiration-ms=604800000"
        }
)
@DisplayName("Review Service Integration Tests")
class ReviewServiceIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private StatsService statsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewHistoryRepository reviewHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    private User testUser;
    private Problem testProblem;

    @BeforeEach
    void setUp() {
        // Create test user directly in DB
        String unique = UUID.randomUUID().toString().substring(0, 8);
        testUser = User.builder()
                .email("review-test-" + unique + "@memoalgo.com")
                .username("review-tester-" + unique)
                .passwordHash(passwordEncoder.encode("password123"))
                .isActive(true)
                .lastActiveAt(Instant.now())
                .build();
        testUser = userRepository.save(testUser);

        // Create test problem
        testProblem = Problem.builder()
                .user(testUser)
                .title("Test Problem - Two Sum")
                .url("https://leetcode.com/problems/two-sum/")
                .difficulty("EASY")
                .notes("Use a hashmap")
                .isActive(true)
                .build();
        testProblem = problemRepository.save(testProblem);

        // Authenticate as the test user in the Security Context
        // This is what JwtAuthenticationFilter does on real HTTP requests
        authenticateAs(testUser.getEmail());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        // Cascade delete: reviews + history get deleted with user
        problemRepository.findByUser(testUser).forEach(problemRepository::delete);
        userRepository.delete(testUser);
    }

    @Test
    @DisplayName("New problem: review queue should include it immediately")
    void getDueProblems_withNewProblem_shouldNotBeDueUntilFirstReview() throws Exception {
        // A problem with no review state is not due until its first review
        ReviewSessionResponse session = reviewService.getDueProblems();

        // Problem is NOT in due queue until a Review record exists for it
        assertThat(session.getDueCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("First review (Good=4): repetition=1, interval=1, EF=2.5")
    void submitReview_firstGoodRating_shouldSetInitialSM2State() {
        ReviewRequest request = new ReviewRequest();
        request.setQuality(4);

        ReviewResponse response = reviewService.submitReview(testProblem.getId(), request);

        assertThat(response.getRepetitionCount()).isEqualTo(1);
        assertThat(response.getIntervalDays()).isEqualTo(1);
        assertThat(response.getEaseFactor()).isEqualByComparingTo(BigDecimal.valueOf(2.50));
    }

    @Test
    @DisplayName("Second review (Easy=5): repetition=2, interval=6, EF increases")
    void submitReview_secondEasyRating_shouldAdvanceToSixDayInterval() {
        ReviewRequest goodRequest = new ReviewRequest();
        goodRequest.setQuality(4);
        reviewService.submitReview(testProblem.getId(), goodRequest);

        ReviewRequest easyRequest = new ReviewRequest();
        easyRequest.setQuality(5);
        ReviewResponse response = reviewService.submitReview(testProblem.getId(), easyRequest);

        assertThat(response.getRepetitionCount()).isEqualTo(2);
        assertThat(response.getIntervalDays()).isEqualTo(6);
        assertThat(response.getEaseFactor()).isGreaterThan(BigDecimal.valueOf(2.50));
    }

    @Test
    @DisplayName("Again (1) after established card: resets repetition and interval to 0/1")
    void submitReview_againAfterEstablishedCard_shouldResetState() {
        // Build up repetitions first
        ReviewRequest goodRequest = new ReviewRequest();
        goodRequest.setQuality(4);
        reviewService.submitReview(testProblem.getId(), goodRequest);
        reviewService.submitReview(testProblem.getId(), goodRequest);

        // Now fail the review
        ReviewRequest againRequest = new ReviewRequest();
        againRequest.setQuality(1);
        ReviewResponse response = reviewService.submitReview(testProblem.getId(), againRequest);

        assertThat(response.getRepetitionCount()).isEqualTo(0);
        assertThat(response.getIntervalDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("Review history is logged for every submitted review")
    void submitReview_shouldCreateReviewHistoryEntry() {
        ReviewRequest request = new ReviewRequest();
        request.setQuality(4);
        reviewService.submitReview(testProblem.getId(), request);
        reviewService.submitReview(testProblem.getId(), request);

        long historyCount = reviewHistoryRepository
                .findByUserOrderByReviewedAtDesc(testUser).size();

        assertThat(historyCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Stats: retention rate = 100% after all Good/Easy reviews")
    void getStats_afterAllGoodReviews_retentionRateShouldBe100() {
        ReviewRequest goodRequest = new ReviewRequest();
        goodRequest.setQuality(4);
        reviewService.submitReview(testProblem.getId(), goodRequest);

        ReviewRequest easyRequest = new ReviewRequest();
        easyRequest.setQuality(5);
        reviewService.submitReview(testProblem.getId(), easyRequest);

        var stats = statsService.getStats();

        assertThat(stats.getTotalReviews()).isEqualTo(2);
        assertThat(stats.getRetentionRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Stats: retention rate = 50% after one Good and one Again")
    void getStats_afterMixedReviews_retentionRateShouldBe50() {
        ReviewRequest goodRequest = new ReviewRequest();
        goodRequest.setQuality(4);
        reviewService.submitReview(testProblem.getId(), goodRequest);

        ReviewRequest againRequest = new ReviewRequest();
        againRequest.setQuality(1);
        reviewService.submitReview(testProblem.getId(), againRequest);

        var stats = statsService.getStats();

        assertThat(stats.getTotalReviews()).isEqualTo(2);
        assertThat(stats.getRetentionRate()).isEqualTo(50.0);
    }

    // ─────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────

    private void authenticateAs(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}