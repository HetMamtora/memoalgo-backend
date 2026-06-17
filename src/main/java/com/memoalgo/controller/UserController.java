package com.memoalgo.controller;

import com.memoalgo.dto.response.UserResponse;
import com.memoalgo.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UserController — current user info endpoint.
 *
 * Route:
 *   GET /api/v1/users/me → returns the authenticated user's profile
 *
 * Used by the frontend account menu (sidebar avatar dropdown on desktop,
 * top-bar avatar on mobile) to display email/username reliably,
 * including after a page refresh — instead of relying solely on
 * data cached in localStorage at login time.
 *
 * No new service layer needed — SecurityUtils.getCurrentUser() already
 * does the JWT → User entity resolution used throughout the app.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Current user profile")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(UserResponse.fromEntity(securityUtils.getCurrentUser()));
    }
}