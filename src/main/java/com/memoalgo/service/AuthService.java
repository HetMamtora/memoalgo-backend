package com.memoalgo.service;

import com.memoalgo.dto.request.InitiatePasswordResetRequest;
import com.memoalgo.dto.request.LoginRequest;
import com.memoalgo.dto.request.RegisterRequest;
import com.memoalgo.dto.request.VerifyOtpRequest;
import com.memoalgo.dto.response.AuthResponse;
import com.memoalgo.dto.response.OtpResponse;
import com.memoalgo.entity.PasswordResetOtp;
import com.memoalgo.entity.PendingRegistration;
import com.memoalgo.entity.User;
import com.memoalgo.exception.ConflictException;
import com.memoalgo.exception.OtpException;
import com.memoalgo.exception.ResourceNotFoundException;
import com.memoalgo.repository.PasswordResetOtpRepository;
import com.memoalgo.repository.PendingRegistrationRepository;
import com.memoalgo.repository.UserRepository;
import com.memoalgo.security.JwtTokenProvider;
import com.memoalgo.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * AuthService — registration is now a two-step OTP-gated flow rather
 * than a single call. No User row exists between "initiate" and a
 * successful "verify" — see PendingRegistration's javadoc. Password
 * reset follows the identical shape, staged in PasswordResetOtp instead.
 * login() is unchanged from before this feature.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts}")
    private int otpMaxAttempts;

    @Value("${app.otp.resend-cooldown-seconds}")
    private long resendCooldownSeconds;

    // ───────────────────────── Registration ─────────────────────────

    /**
     * Step 1 of registration: validate, hash the password, generate and
     * email an OTP, and stage everything in PendingRegistration. No User
     * row is created yet — that only happens in verifyRegistration().
     * Calling this again for the same email (e.g. "resend code") replaces
     * the existing pending row rather than creating a duplicate.
     */
    @Transactional
    public OtpResponse initiateRegistration(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("This username is already taken");
        }

        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElseGet(() -> PendingRegistration.builder().email(email).build());

        enforceResendCooldown(pending.getUpdatedAt());

        String otp = otpService.generateCode();
        pending.setUsername(request.getUsername());
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pending.setOtpHash(otpService.hash(otp));
        pending.setOtpExpiresAt(Instant.now().plusSeconds(otpExpiryMinutes * 60L));
        pending.setAttemptCount(0);
        pendingRegistrationRepository.save(pending);

        emailService.sendRegistrationOtp(email, otp);
        log.info("Registration OTP sent to {}", email);

        return OtpResponse.builder()
                .message("Verification code sent to your email.")
                .email(email)
                .expiresInMinutes(otpExpiryMinutes)
                .build();
    }

    /**
     * Step 2 of registration: verify the OTP and, only on success, create
     * the real User row and log them in immediately — matching exactly
     * what the old single-step register() used to return.
     */
    @Transactional
    public AuthResponse verifyRegistration(VerifyOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElseThrow(OtpException::noPendingRequest);

        validateOtp(
                pending.getOtpExpiresAt(),
                pending.getOtpHash(),
                pending.getAttemptCount(),
                request.getOtp(),
                () -> {
                    pending.setAttemptCount(pending.getAttemptCount() + 1);
                    pendingRegistrationRepository.save(pending);
                }
        );

        // Re-check uniqueness — guards against a race where someone else
        // claimed this email/username between initiate and verify.
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByUsername(pending.getUsername())) {
            throw new ConflictException("This username is already taken");
        }

        User user = User.builder()
                .email(email)
                .username(pending.getUsername())
                .passwordHash(pending.getPasswordHash())
                .isActive(true)
                .lastActiveAt(Instant.now())
                .build();
        User savedUser = userRepository.save(user);

        pendingRegistrationRepository.delete(pending);
        log.info("New user registered (OTP verified): {}", savedUser.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);

        return AuthResponse.builder()
                .accessToken(token)
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .build();
    }

    // ───────────────────────── Password reset ─────────────────────────

    /**
     * Step 1 of password reset: hash the requested new password and
     * stage it alongside a fresh OTP. The real password is untouched
     * until verifyPasswordReset() succeeds.
     */
    @Transactional
    public OtpResponse initiatePasswordReset(InitiatePasswordResetRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        PasswordResetOtp resetOtp = passwordResetOtpRepository.findByUser(user)
                .orElseGet(() -> PasswordResetOtp.builder().user(user).build());

        enforceResendCooldown(resetOtp.getUpdatedAt());

        String otp = otpService.generateCode();
        resetOtp.setNewPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        resetOtp.setOtpHash(otpService.hash(otp));
        resetOtp.setOtpExpiresAt(Instant.now().plusSeconds(otpExpiryMinutes * 60L));
        resetOtp.setAttemptCount(0);
        passwordResetOtpRepository.save(resetOtp);

        emailService.sendPasswordResetOtp(email, otp);
        log.info("Password reset OTP sent to {}", email);

        return OtpResponse.builder()
                .message("Verification code sent to your email.")
                .email(email)
                .expiresInMinutes(otpExpiryMinutes)
                .build();
    }

    /**
     * Step 2 of password reset: verify the OTP and, only on success,
     * overwrite the real User's password hash. Logs them in immediately
     * with a fresh token afterward.
     */
    @Transactional
    public AuthResponse verifyPasswordReset(VerifyOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        PasswordResetOtp resetOtp = passwordResetOtpRepository.findByUser(user)
                .orElseThrow(OtpException::noPendingRequest);

        validateOtp(
                resetOtp.getOtpExpiresAt(),
                resetOtp.getOtpHash(),
                resetOtp.getAttemptCount(),
                request.getOtp(),
                () -> {
                    resetOtp.setAttemptCount(resetOtp.getAttemptCount() + 1);
                    passwordResetOtpRepository.save(resetOtp);
                }
        );

        user.setPasswordHash(resetOtp.getNewPasswordHash());
        userRepository.save(user);
        passwordResetOtpRepository.delete(resetOtp);
        log.info("Password reset completed for: {}", email);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);

        return AuthResponse.builder()
                .accessToken(token)
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }

    // ───────────────────────── Login (unchanged) ─────────────────────────

    public AuthResponse login(LoginRequest request){
        String email = request.getEmail().toLowerCase().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastActiveAt(Instant.now());
            userRepository.save(user);
        });

        User user = userRepository.findByEmail(email).orElseThrow();
        log.info("User logged in: {}", email);

        return AuthResponse.builder()
                .accessToken(token)
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }

    // ───────────────────────── Shared OTP helpers ─────────────────────────

    /**
     * Blocks re-initiating too soon after the last OTP was sent for this
     * email/user, relying on BaseEntity's auto-managed updatedAt as the
     * "last sent" timestamp rather than adding a dedicated column.
     */
    private void enforceResendCooldown(Instant lastUpdatedAt) {
        if (lastUpdatedAt == null) return; // brand-new row, nothing to cool down from

        Instant cooldownEnds = lastUpdatedAt.plusSeconds(resendCooldownSeconds);
        if (Instant.now().isBefore(cooldownEnds)) {
            long secondsRemaining = cooldownEnds.getEpochSecond() - Instant.now().getEpochSecond();
            throw OtpException.cooldownActive(Math.max(secondsRemaining, 1));
        }
    }

    /**
     * Shared by both verify flows: checks expiry, attempt limit, and the
     * code itself, in that order. onWrongAttempt is invoked (and the
     * caller is expected to persist the incremented count) only when the
     * code is present but simply wrong — not on expiry or attempt-limit,
     * since those are already terminal states.
     */
    private void validateOtp(
            Instant expiresAt,
            String otpHash,
            int attemptCount,
            String suppliedOtp,
            Runnable onWrongAttempt
    ) {
        if (Instant.now().isAfter(expiresAt)) {
            throw OtpException.expired();
        }
        if (attemptCount >= otpMaxAttempts) {
            throw OtpException.tooManyAttempts();
        }
        if (!otpService.matches(suppliedOtp, otpHash)) {
            onWrongAttempt.run();
            throw OtpException.invalidCode();
        }
    }
}