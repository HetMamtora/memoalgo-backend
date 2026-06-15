package com.memoalgo.algorithm;

import com.memoalgo.entity.Review;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SM2Algorithm — SuperMemo 2 spaced repetition implementation.
 *
 * ═══════════════════════════════════════════════════════════
 * THE SM-2 ALGORITHM (1987, Piotr Wozniak)
 * ═══════════════════════════════════════════════════════════
 *
 * Core concept: the "forgetting curve" — memory decays exponentially
 * over time, but each successful recall at the right moment strengthens
 * the memory trace and extends the interval before next review.
 *
 * THREE state variables per problem:
 *
 * 1. easeFactor (EF): float, min 1.3, starts at 2.5
 *    How "easy" this problem is for you. Higher = longer intervals.
 *    Adjusted DOWN on bad recall, UP on easy recall.
 *    Formula: EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))
 *
 * 2. intervalDays (I): int, starts at 1
 *    Days until the next review.
 *    After first success:    I = 1
 *    After second success:   I = 6
 *    After third+ success:   I = round(prevInterval * EF)
 *
 * 3. repetitionCount (n): int, starts at 0
 *    Consecutive successful recalls. Resets to 0 on "Again" (quality < 3).
 *
 * QUALITY SCALE (0-5):
 *   0 = Complete blackout
 *   1 = Wrong answer (Again in UI)
 *   2 = Wrong but looked familiar
 *   3 = Correct with significant difficulty (Hard in UI)
 *   4 = Correct after hesitation (Good in UI)
 *   5 = Perfect recall (Easy in UI)
 *
 * UI → quality mapping:
 *   Again → 1  Hard → 3  Good → 4  Easy → 5
 *
 * RULE: if quality < 3, reset repetitions (start over from interval=1).
 *       if quality >= 3, advance the interval schedule.
 *
 * DSA connection: this algorithm uses a Priority Queue conceptually —
 * the problem with the earliest nextReviewDate has highest priority.
 * ReviewRepository.findDueToday() simulates this with SQL ORDER BY.
 *
 * Reference: https://www.supermemo.com/en/archives1990-2015/english/ol/sm2
 * ═══════════════════════════════════════════════════════════
 */
@Component
public class SM2Algorithm implements SpacedRepetitionAlgorithm {

    // SM-2 constants
    private static final double MIN_EASE_FACTOR = 1.3;
    private static final double INITIAL_EASE_FACTOR = 2.5;
    private static final int FIRST_INTERVAL = 1;
    private static final int SECOND_INTERVAL = 6;

    @Override
    public AlgorithmResult calculate(Review review, int quality) {
        validateQuality(quality);

        double currentEF = review.getEaseFactor().doubleValue();
        int currentInterval = review.getIntervalDays();
        int currentRepetition = review.getRepetitionCount();

        if (quality < 3) {
            // Failed recall — reset repetition counter and restart from interval 1
            // EF is still updated (penalized) to reflect difficulty
            double newEF = calculateNewEaseFactor(currentEF, quality);
            return new AlgorithmResult(newEF, FIRST_INTERVAL, 0);
        }

        // Successful recall — advance the schedule
        double newEF = calculateNewEaseFactor(currentEF, quality);
        int newInterval = calculateNewInterval(currentInterval, currentRepetition, newEF);
        int newRepetition = currentRepetition + 1;

        return new AlgorithmResult(newEF, newInterval, newRepetition);
    }

    /**
     * SM-2 ease factor formula.
     *
     * EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))
     *
     * This creates a parabolic adjustment:
     *   q=5 (Easy):  EF increases by +0.10
     *   q=4 (Good):  EF stays ~same   +0.00
     *   q=3 (Hard):  EF decreases     -0.14
     *   q=2:         EF decreases     -0.32
     *   q=1 (Again): EF decreases     -0.54
     *   q=0:         EF decreases     -0.80
     *
     * Always clamped to minimum 1.3 (prevents infinite scheduling).
     */
    private double calculateNewEaseFactor(double currentEF, int quality) {
        double delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02);
        double newEF = currentEF + delta;
        // Clamp to minimum — EF never goes below 1.3
        newEF = Math.max(MIN_EASE_FACTOR, newEF);
        // Round to 2 decimal places (matches DB column precision DECIMAL(4,2))
        return BigDecimal.valueOf(newEF)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * SM-2 interval formula.
     *
     * n=0 (first review):  I = 1 day
     * n=1 (second review): I = 6 days
     * n≥2 (subsequent):    I = round(prevInterval * EF)
     *
     * Example progression for EF=2.5:
     *   1 → 6 → 15 → 37 → 93 → 232 days
     */
    private int calculateNewInterval(int currentInterval, int repetitionCount, double newEF) {
        return switch (repetitionCount) {
            case 0 -> FIRST_INTERVAL;
            case 1 -> SECOND_INTERVAL;
            default -> (int) Math.round(currentInterval * newEF);
        };
    }

    private void validateQuality(int quality) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException(
                    "Quality must be between 0 and 5, got: " + quality
            );
        }
    }
}