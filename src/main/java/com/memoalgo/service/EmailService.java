/*package com.memoalgo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;*/

/**
 * EmailService — sends OTP emails via whatever JavaMailSender is
 * configured (Resend's SMTP relay — see spring.mail.* in application.yaml
 * / application-{profile}.yml). Swapping email providers later is a
 * config change here, not a rewrite, as long as the new provider also
 * speaks SMTP.
 */
/*@Slf4j
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
}*/

package com.memoalgo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * EmailService — sends OTP emails via Resend's HTTPS API, not SMTP.
 *
 * Why not SMTP: Railway blocks all outbound SMTP ports (25/465/587/2525)
 * on every plan below Pro -- enforced at the network/firewall level, so
 * no amount of timeout tuning fixes it (this is why the request just
 * hung instead of failing fast). Resend's REST API runs over plain
 * HTTPS (443), which is never blocked anywhere, so this is also the
 * more portable choice generally -- works unchanged on Render, Fly.io,
 * or anywhere else, unlike SMTP which is host-specific.
 *
 * Uses RestClient (Spring 6.1+/Boot 3.2+, included in spring-boot-
 * starter-web) -- no new dependency needed.
 */
@Slf4j
@Service
public class EmailService {

    private final RestClient restClient;
    private final String fromAddress;
    private final int otpExpiryMinutes;

    public EmailService(
            @Value("${app.resend.api-key}") String resendApiKey,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.otp.expiry-minutes}") int otpExpiryMinutes
    ) {
        this.fromAddress = fromAddress;
        this.otpExpiryMinutes = otpExpiryMinutes;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + resendApiKey)
                .build();
    }

    public void sendRegistrationOtp(String toEmail, String code) {
        send(toEmail, code, "complete your MemoAlgo registration");
    }

    public void sendPasswordResetOtp(String toEmail, String code) {
        send(toEmail, code, "reset your MemoAlgo password");
    }

    private void send(String toEmail, String code, String action) {
        String text =
                "Your verification code is: " + code + "\n\n"
                        + "Use this code to " + action + ". "
                        + "This code expires in " + otpExpiryMinutes + " minutes.\n\n"
                        + "If you didn't request this, you can safely ignore this email.";

        Map<String, Object> body = Map.of(
                "from", fromAddress,
                "to", List.of(toEmail),
                "subject", "Your MemoAlgo verification code",
                "text", text
        );

        restClient.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("OTP email sent to {}", toEmail);
    }
}