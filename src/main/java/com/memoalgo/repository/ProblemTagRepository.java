package com.memoalgo.repository;

import com.memoalgo.entity.Problem;
import com.memoalgo.entity.ProblemTag;
import com.memoalgo.entity.ProblemTagId;
import com.memoalgo.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemTagRepository extends JpaRepository<ProblemTag, ProblemTagId> {

    List<ProblemTag> findByProblem(Problem problem);

    List<ProblemTag> findByTag(Tag tag);

    Optional<ProblemTag> findByProblemAndTag(Problem problem, Tag tag);

    void deleteByProblem(Problem problem);
}
