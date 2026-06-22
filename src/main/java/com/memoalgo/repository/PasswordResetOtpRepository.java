package com.memoalgo.repository;

import com.memoalgo.entity.PasswordResetOtp;
import com.memoalgo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, UUID> {

    Optional<PasswordResetOtp> findByUser(User user);

    void deleteByUser(User user);
}