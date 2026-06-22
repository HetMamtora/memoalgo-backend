package com.memoalgo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OtpResponse — returned by every "initiate" endpoint (register or
 * password-reset) to confirm a code was sent.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {
    private String message;
    private String email;
    private int expiresInMinutes;
}