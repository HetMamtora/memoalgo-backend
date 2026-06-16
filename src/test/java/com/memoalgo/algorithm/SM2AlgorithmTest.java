package com.memoalgo.algorithm;

import com.memoalgo.entity.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * SM2AlgorithmTest — Unit tests for the SM-2 spaced repetition algorithm.
 *
 * Testing approach:
 * - UNIT test (no Spring, no DB, no mock injection needed)
 * - SM2Algorithm is a pure function: same inputs → same outputs
 * - Uses AssertJ for readable assertions
 *
 * Test structure uses @Nested classes to group related tests:
 * - IntervalTests: verify interval progression (1 → 6 → growing)
 * - EaseFactorTests: verify EF adjustment per quality rating
 * - RepetitionCountTests: verify counter advances and resets
 * - EdgeCaseTests: boundary conditions and invalid input
 */
@DisplayName("SM-2 Algorithm")
class SM2AlgorithmTest {

    private SM2Algorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new SM2Algorithm();
    }

    // ─────────────────────────────────────────────────────
    // Helper: build a Review with specific SM-2 state
    // ─────────────────────────────────────────────────────
    private Review buildReview(double easeFactor, int intervalDays, int repetitionCount) {
        return Review.builder()
                .easeFactor(BigDecimal.valueOf(easeFactor))
                .intervalDays(intervalDays)
                .repetitionCount(repetitionCount)
                .nextReviewDate(LocalDate.now())
                .build();
    }

    // ─────────────────────────────────────────────────────
    // INTERVAL TESTS
    // ─────────────────────────────────────────────────────
    @Nested
    @DisplayName("Interval progression")
    class IntervalTests {

        @Test
        @DisplayName("First successful review → interval = 1 day")
        void firstSuccessfulReview_shouldReturnIntervalOfOne() {
            Review review = buildReview(2.50, 1, 0); // repetitionCount = 0

            var result = algorithm.calculate(review, 4);

            assertThat(result.intervalDays()).isEqualTo(1);
            assertThat(result.repetitionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Second successful review → interval = 6 days")
        void secondSuccessfulReview_shouldReturnIntervalOfSix() {
            Review review = buildReview(2.50, 1, 1); // repetitionCount = 1

            var result = algorithm.calculate(review, 4);

            assertThat(result.intervalDays()).isEqualTo(6);
            assertThat(result.repetitionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Third review (Good, EF=2.5, prev=6) → interval = 15 days")
        void thirdReview_quality4_EF250_shouldReturn15Days() {
            // round(6 * 2.50) = 15
            Review review = buildReview(2.50, 6, 2);

            var result = algorithm.calculate(review, 4);

            assertThat(result.intervalDays()).isEqualTo(15);
        }

        @Test
        @DisplayName("Fourth review (Good, EF=2.5, prev=15) → interval = 37 days")
        void fourthReview_quality4_EF250_shouldReturn37Days() {
            // round(15 * 2.50) = 37 (rounds 37.5 to 38, actually)
            // Let's verify: 15 * 2.50 = 37.5, Math.round(37.5) = 38
            Review review = buildReview(2.50, 15, 3);

            var result = algorithm.calculate(review, 4);

            assertThat(result.intervalDays()).isEqualTo(38); // Math.round(37.5) = 38
        }

        @Test
        @DisplayName("Failed review (quality=1) resets interval to 1 regardless of previous interval")
        void failedReview_shouldResetIntervalToOne() {
            Review review = buildReview(2.50, 93, 10); // Long-established card

            var result = algorithm.calculate(review, 1);

            assertThat(result.intervalDays()).isEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────
    // EASE FACTOR TESTS
    // ─────────────────────────────────────────────────────
    @Nested
    @DisplayName("Ease factor adjustments")
    class EaseFactorTests {

        @Test
        @DisplayName("Quality 5 (Easy) → EF increases by +0.10")
        void quality5_shouldIncreaseEaseFactorBy010() {
            Review review = buildReview(2.50, 1, 0);

            var result = algorithm.calculate(review, 5);

            // EF = 2.50 + (0.1 - (5-5) * (0.08 + (5-5) * 0.02)) = 2.50 + 0.10 = 2.60
            assertThat(result.easeFactor()).isEqualTo(2.60);
        }

        @Test
        @DisplayName("Quality 4 (Good) → EF stays same (within rounding)")
        void quality4_shouldKeepEaseFactorSame() {
            Review review = buildReview(2.50, 1, 0);

            var result = algorithm.calculate(review, 4);

            // EF = 2.50 + (0.1 - (5-4) * (0.08 + (5-4) * 0.02)) = 2.50 + (0.1 - 0.10) = 2.50
            assertThat(result.easeFactor()).isEqualTo(2.50);
        }

        @Test
        @DisplayName("Quality 3 (Hard) → EF decreases by 0.14")
        void quality3_shouldDecreaseEaseFactorBy014() {
            Review review = buildReview(2.50, 1, 0);

            var result = algorithm.calculate(review, 3);

            // EF = 2.50 + (0.1 - (5-3) * (0.08 + (5-3) * 0.02))
            //    = 2.50 + (0.1 - 2 * (0.08 + 2 * 0.02))
            //    = 2.50 + (0.1 - 2 * 0.12) = 2.50 + (0.1 - 0.24) = 2.50 - 0.14 = 2.36
            assertThat(result.easeFactor()).isEqualTo(2.36);
        }

        @Test
        @DisplayName("Quality 1 (Again) → EF penalized heavily")
        void quality1_shouldDecreaseEaseFactorSignificantly() {
            Review review = buildReview(2.50, 1, 0);

            var result = algorithm.calculate(review, 1);

            // EF = 2.50 + (0.1 - (5-1) * (0.08 + (5-1) * 0.02))
            //    = 2.50 + (0.1 - 4 * (0.08 + 0.08)) = 2.50 + (0.1 - 4 * 0.16)
            //    = 2.50 + (0.1 - 0.64) = 2.50 - 0.54 = 1.96
            assertThat(result.easeFactor()).isEqualTo(1.96);
        }

        @Test
        @DisplayName("EF should never drop below 1.3 (minimum floor)")
        void easeFactorShouldNeverGoBelowMinimum() {
            // Card already at minimum EF, worst possible rating
            Review review = buildReview(1.3, 1, 0);

            var result = algorithm.calculate(review, 0);

            assertThat(result.easeFactor())
                    .isGreaterThanOrEqualTo(1.3)
                    .as("EF should never go below the 1.3 minimum");
        }

        @Test
        @DisplayName("EF should never drop below 1.3 after many consecutive failures")
        void manyFailures_easeFactorShouldRemainAboveFloor() {
            Review review = buildReview(2.50, 1, 0);

            // Simulate 10 consecutive failures
            for (int i = 0; i < 10; i++) {
                var result = algorithm.calculate(review, 0);
                review.setEaseFactor(BigDecimal.valueOf(result.easeFactor()));
                review.setIntervalDays(result.intervalDays());
                review.setRepetitionCount(result.repetitionCount());

                assertThat(result.easeFactor())
                        .isGreaterThanOrEqualTo(1.3);
            }
        }
    }

    // ─────────────────────────────────────────────────────
    // REPETITION COUNT TESTS
    // ─────────────────────────────────────────────────────
    @Nested
    @DisplayName("Repetition count")
    class RepetitionCountTests {

        @ParameterizedTest
        @ValueSource(ints = {3, 4, 5})
        @DisplayName("Quality >= 3 always advances repetition count")
        void qualityAbove3_shouldAdvanceRepetitionCount(int quality) {
            Review review = buildReview(2.50, 6, 2);

            var result = algorithm.calculate(review, quality);

            assertThat(result.repetitionCount()).isEqualTo(3);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2})
        @DisplayName("Quality < 3 always resets repetition count to 0")
        void qualityBelow3_shouldResetRepetitionCountToZero(int quality) {
            Review review = buildReview(2.50, 93, 10); // Well-established card

            var result = algorithm.calculate(review, quality);

            assertThat(result.repetitionCount()).isEqualTo(0);
        }
    }

    // ─────────────────────────────────────────────────────
    // EDGE CASE & BOUNDARY TESTS
    // ─────────────────────────────────────────────────────
    @Nested
    @DisplayName("Edge cases and boundary conditions")
    class EdgeCaseTests {

        @Test
        @DisplayName("Quality -1 should throw IllegalArgumentException")
        void negativeQuality_shouldThrowException() {
            Review review = buildReview(2.50, 1, 0);

            assertThatThrownBy(() -> algorithm.calculate(review, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("-1");
        }

        @Test
        @DisplayName("Quality 6 should throw IllegalArgumentException")
        void qualityAboveFive_shouldThrowException() {
            Review review = buildReview(2.50, 1, 0);

            assertThatThrownBy(() -> algorithm.calculate(review, 6))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("6");
        }

        @Test
        @DisplayName("Quality 0 is valid (complete blackout) - should not throw")
        void qualityZero_shouldBeValidAndReset() {
            Review review = buildReview(2.50, 1, 0);

            var result = algorithm.calculate(review, 0);

            assertThat(result.intervalDays()).isEqualTo(1);
            assertThat(result.repetitionCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Quality 5 is valid (perfect recall) - should not throw")
        void qualityFive_shouldBeValid() {
            Review review = buildReview(2.50, 1, 0);

            var result = algorithm.calculate(review, 5);

            assertThat(result.intervalDays()).isEqualTo(1);
            assertThat(result.repetitionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Simulate full SM-2 progression: verify exponential interval growth")
        void fullProgression_shouldShowExponentialGrowth() {
            // This tests the real-world behaviour of SM-2 over time
            // with consistent "Good" (4) ratings
            Review review = buildReview(2.50, 1, 0);

            // n=0 → 1 day
            var r1 = algorithm.calculate(review, 4);
            assertThat(r1.intervalDays()).isEqualTo(1);

            // n=1 → 6 days
            review.setRepetitionCount(r1.repetitionCount());
            review.setIntervalDays(r1.intervalDays());
            review.setEaseFactor(BigDecimal.valueOf(r1.easeFactor()));
            var r2 = algorithm.calculate(review, 4);
            assertThat(r2.intervalDays()).isEqualTo(6);

            // n=2 → round(6 * 2.50) = 15 days
            review.setRepetitionCount(r2.repetitionCount());
            review.setIntervalDays(r2.intervalDays());
            review.setEaseFactor(BigDecimal.valueOf(r2.easeFactor()));
            var r3 = algorithm.calculate(review, 4);
            assertThat(r3.intervalDays()).isEqualTo(15);

            // Each interval is longer than the previous — exponential growth confirmed
            assertThat(r2.intervalDays()).isGreaterThan(r1.intervalDays());
            assertThat(r3.intervalDays()).isGreaterThan(r2.intervalDays());
        }
    }
}