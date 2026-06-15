package com.memoalgo.algorithm;

import com.memoalgo.entity.Review;

/**
 * SpacedRepetitionAlgorithm — Strategy interface for review scheduling.
 *
 * Design Pattern: Strategy
 * Defines a family of algorithms (SM-2, FSRS, Leitner system),
 * encapsulates each one, and makes them interchangeable.
 *
 * The ReviewService depends on THIS interface, not on SM2Algorithm directly.
 * To swap the algorithm: change the @Primary bean, write a new implementation.
 * Zero changes needed in ReviewService.
 *
 * This is exactly how production scheduling systems are built.
 * Anki, SuperMemo, and RemNote all use this pattern internally.
 */
public interface SpacedRepetitionAlgorithm {

    /**
     * Calculate the next review schedule given current state and user rating.
     *
     * @param review  the current SM-2 state of the problem
     * @param quality the user's self-rating (0-5 scale)
     * @return        AlgorithmResult with new ease, interval, repetition count
     */
    AlgorithmResult calculate(Review review, int quality);

    /**
     * AlgorithmResult — immutable value object carrying the computed outcome.
     *
     * Java 16+ records are immutable data carriers with auto-generated
     * equals(), hashCode(), toString(), and compact constructors.
     * Perfect for value objects that carry results but have no behavior.
     *
     * @param easeFactor       new ease factor after this review
     * @param intervalDays     days until next review
     * @param repetitionCount  updated successful repetition counter
     */
    record AlgorithmResult(
            double easeFactor,
            int intervalDays,
            int repetitionCount
    ) {}
}