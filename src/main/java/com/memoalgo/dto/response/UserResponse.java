package com.memoalgo.dto.response;

import com.memoalgo.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * UserResponse — returned by GET /api/v1/users/me
 *
 * Deliberately excludes passwordHash and any internal fields.
 * This is what the frontend account menu reads to display
 * email, username, and member-since info without relying on
 * stale localStorage data from login time.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String username;
    private Instant createdAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .createdAt(user.getCreatedAt())
                .build();
    }
}