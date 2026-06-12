package com.memoalgo.repository;

import com.memoalgo.entity.Tag;
import com.memoalgo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByUser(User user);

    Optional<Tag> findByUserAndName(User user, String name);
}
