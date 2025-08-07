package com.astro.shared.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Async("taskExecutor")
    public void sendPasswordResetEmail(String to, String token, String resetUrlBase) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("AstroURL - Password Reset Request");

            String resetLink = resetUrlBase + "?token=" + token;
            String emailBody = String.format(
                    "Hello,\n\nYou have requested to reset your password for your AstroURL account.\n\n" +
                            "Please click the link below to set a new password:\n%s\n\n" +
                            "This link will expire in 15 minutes.\n\n" +
                            "If you did not request a password reset, please ignore this email.\n\n" +
                            "Thanks,\nThe AstroURL Team",
                    resetLink
            );

            message.setText(emailBody);
            mailSender.send(message);
            logger.info("Password reset email sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}", to, e);
        }
    }
}