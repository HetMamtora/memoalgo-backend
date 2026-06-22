-- ============================================================
-- MemoAlgo — V2 Add OTP-gated registration & password reset
-- ============================================================


-- ── PENDING_REGISTRATIONS ───────────────────────────────────
-- A registration attempt awaiting email verification. No row in
-- `users` exists until the OTP here is verified -- see
-- AuthService.verifyRegistration(). One row per email; a fresh
-- attempt (or resend) for the same email replaces the existing row.
CREATE TABLE pending_registrations (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    username        VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    otp_hash        VARCHAR(255) NOT NULL,
    otp_expires_at  TIMESTAMPTZ  NOT NULL,
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_pending_registrations_email UNIQUE (email),
    CONSTRAINT chk_pending_registrations_attempt_count CHECK (attempt_count >= 0)
);


-- ── PASSWORD_RESET_OTPS ──────────────────────────────────────
-- A password-reset attempt awaiting email verification, for an
-- existing user. The new password is hashed immediately and only
-- copied onto users.password_hash once the OTP is verified -- see
-- AuthService.verifyPasswordReset(). One row per user.
CREATE TABLE password_reset_otps (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    new_password_hash   VARCHAR(255) NOT NULL,
    otp_hash            VARCHAR(255) NOT NULL,
    otp_expires_at      TIMESTAMPTZ  NOT NULL,
    attempt_count       INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_password_reset_otps_user_id UNIQUE (user_id),
    CONSTRAINT chk_password_reset_otps_attempt_count CHECK (attempt_count >= 0)
);

-- No extra indexes needed: the UNIQUE constraints above already create
-- an implicit index on email / user_id, which is exactly the lookup
-- pattern findByEmail() / findByUser() use. Matches the "don't
-- over-index" philosophy from V1's INDEXES section.