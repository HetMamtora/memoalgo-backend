package com.memoalgo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * PendingRegistration — a registration attempt awaiting email verification.
 *
 * No real User row exists until the OTP is verified. This row holds
 * everything needed to create one: the already-hashed password, the
 * chosen username, and the (also hashed) OTP with its expiry.
 *
 * One row per email — a fresh registration attempt (or a resend) for the
 * same email replaces the existing row rather than creating a duplicate,
 * so there's never more than one active code per email at a time.
 */
@Entity
@Table(name = "pending_registrations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "otp_expires_at", nullable = false)
    private Instant otpExpiresAt;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;
}