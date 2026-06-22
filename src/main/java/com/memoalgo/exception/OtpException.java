package com.memoalgo.exception;

/**
 * OtpException — covers every OTP failure mode (wrong code, expired,
 * too many attempts, resend too soon, no pending request). Extends the
 * existing AppException, so it's handled by GlobalExceptionHandler's
 * existing handleAppException — no separate handler needed.
 */
public class OtpException extends AppException {

    public OtpException(String message, int httpStatusCode) {
        super(message, httpStatusCode);
    }

    public static OtpException invalidCode() {
        return new OtpException("Incorrect verification code.", 400);
    }

    public static OtpException expired() {
        return new OtpException(
                "This verification code has expired. Request a new one.", 400);
    }

    public static OtpException tooManyAttempts() {
        return new OtpException(
                "Too many incorrect attempts. Request a new code.", 400);
    }

    public static OtpException cooldownActive(long secondsRemaining) {
        return new OtpException(
                "Please wait " + secondsRemaining + "s before requesting another code.", 429);
    }

    public static OtpException noPendingRequest() {
        return new OtpException(
                "No pending request found for this email. Start over.", 400);
    }
}