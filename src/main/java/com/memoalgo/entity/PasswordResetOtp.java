package com.memoalgo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * PasswordResetOtp — a password-reset attempt awaiting email verification.
 *
 * Unlike PendingRegistration, this points at a real, already-existing
 * User (you can't reset the password of an account that doesn't exist).
 * The new password is hashed immediately on request and only copied
 * onto the real User row once the OTP is verified — the plaintext
 * password is never persisted anywhere, even temporarily.
 *
 * One row per user — a fresh reset request (or resend) for the same
 * user replaces the existing row.
 */
@Entity
@Table(
        name = "password_reset_otps",
        uniqueConstraints = @UniqueConstraint(name = "uq_password_reset_user", columnNames = "user_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetOtp extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_password_reset_otps_user")
    )
    private User user;

    @Column(name = "new_password_hash", nullable = false, length = 255)
    private String newPasswordHash;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "otp_expires_at", nullable = false)
    private Instant otpExpiresAt;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;
}