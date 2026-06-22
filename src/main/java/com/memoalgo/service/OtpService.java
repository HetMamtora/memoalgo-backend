package com.memoalgo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * OtpService — generates and verifies 6-digit OTP codes.
 *
 * Deliberately reuses the existing PasswordEncoder (BCrypt) bean to hash
 * codes the same way passwords are hashed — no new crypto dependency,
 * and a leaked DB doesn't hand over usable codes even within their
 * (short) expiry window.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Generates a 6-digit code, zero-padded ("042913", not "42913"). */
    public String generateCode() {
        int code = RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    public String hash(String rawCode) {
        return passwordEncoder.encode(rawCode);
    }

    public boolean matches(String rawCode, String hashedCode) {
        return passwordEncoder.matches(rawCode, hashedCode);
    }
}