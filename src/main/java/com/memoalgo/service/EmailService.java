package com.memoalgo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * EmailService — sends OTP emails via whatever JavaMailSender is
 * configured (Resend's SMTP relay — see spring.mail.* in application.yaml
 * / application-{profile}.yml). Swapping email providers later is a
 * config change here, not a rewrite, as long as the new provider also
 * speaks SMTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    public void sendRegistrationOtp(String toEmail, String code) {
        send(toEmail, code, "complete your MemoAlgo registration");
    }

    public void sendPasswordResetOtp(String toEmail, String code) {
        send(toEmail, code, "reset your MemoAlgo password");
    }

    private void send(String toEmail, String code, String action) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Your MemoAlgo verification code");
        message.setText(
                "Your verification code is: " + code + "\n\n"
                        + "Use this code to " + action + ". "
                        + "This code expires in " + otpExpiryMinutes + " minutes.\n\n"
                        + "If you didn't request this, you can safely ignore this email."
        );
        mailSender.send(message);
        log.info("OTP email sent to {}", toEmail);
    }
}