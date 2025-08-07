package com.astro.shared.service;

import com.astro.user.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Async("taskExecutor")
    public void sendPasswordResetEmail(User user, String token, String resetUrlBase) {
        try {
            String to = user.getEmail();
            String resetLink = resetUrlBase + "?token=" + token;

            Context context = new Context();
            context.setVariable("username", user.getUsername());
            context.setVariable("resetLink", resetLink);

            String htmlContent = templateEngine.process("emails/password-reset", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("AstroURL - Password Reset Request");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            logger.info("Password reset HTML email sent successfully to {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send password reset HTML email to {}", user.getEmail(), e);
        }
    }
}