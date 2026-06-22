package com.memoalgo.controller;

import com.memoalgo.dto.request.InitiatePasswordResetRequest;
import com.memoalgo.dto.request.LoginRequest;
import com.memoalgo.dto.request.RegisterRequest;
import com.memoalgo.dto.request.VerifyOtpRequest;
import com.memoalgo.dto.response.AuthResponse;
import com.memoalgo.dto.response.OtpResponse;
import com.memoalgo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register (OTP-verified), login, and password reset (OTP-verified)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/initiate")
    @Operation(summary = "Start registration -- sends a 6-digit code to the given email")
    public ResponseEntity<OtpResponse> initiateRegistration(
            @Valid @RequestBody RegisterRequest request) {

        OtpResponse response = authService.initiateRegistration(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/verify")
    @Operation(summary = "Verify the registration code and create the account")
    public ResponseEntity<AuthResponse> verifyRegistration(
            @Valid @RequestBody VerifyOtpRequest request) {

        AuthResponse response = authService.verifyRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT token")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset/initiate")
    @Operation(summary = "Start a password reset -- sends a 6-digit code to the given email")
    public ResponseEntity<OtpResponse> initiatePasswordReset(
            @Valid @RequestBody InitiatePasswordResetRequest request) {

        OtpResponse response = authService.initiatePasswordReset(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset/verify")
    @Operation(summary = "Verify the reset code and apply the new password")
    public ResponseEntity<AuthResponse> verifyPasswordReset(
            @Valid @RequestBody VerifyOtpRequest request) {

        AuthResponse response = authService.verifyPasswordReset(request);
        return ResponseEntity.ok(response);
    }
}