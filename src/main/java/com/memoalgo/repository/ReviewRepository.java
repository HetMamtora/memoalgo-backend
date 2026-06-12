package com.memoalgo.repository;

import com.memoalgo.entity.Problem;
import com.memoalgo.entity.Review;
import com.memoalgo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("""
            SELECT r from Review r
            WHERE r.user = :user
            AND r.nextReviewDate <= :date
            ORDER BY r.nextReviewDate ASC
            """)
    List<Review> findDueToday(@Param("user") User user, @Param("date")LocalDate date);

    Optional<Review> findByProblemAndUser(Problem problem, User user);

    @Query("""
            SELECT COUNT(r) FROM Review r
            WHERE r.user = :user
            AND r.nextReviewDate <= CURRENT_DATE
            """)
    Long countDueReviews(@Param("user") User user);
}
