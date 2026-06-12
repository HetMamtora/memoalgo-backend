package com.memoalgo.repository;

import com.memoalgo.entity.Problem;
import com.memoalgo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    Optional<Problem> findByIdAndUser(UUID problemId, User user);

    List<Problem> findByUser(User user);

    List<Problem> findByUserAndIsActiveTrue(User user);
}
