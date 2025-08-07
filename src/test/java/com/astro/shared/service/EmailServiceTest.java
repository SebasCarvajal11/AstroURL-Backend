package com.astro.shared.service;

import com.astro.user.model.User;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Manually inject the value that would be provided by @Value in a real environment
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@astrourl.com");
    }

    @Test
    void sendPasswordResetEmail_shouldProcessTemplateAndSendHtmlEmail() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        String token = "test-token";
        String resetUrlBase = "http://localhost/reset";
        String expectedHtml = "<html><body>Hello, testuser!</body></html>";

        when(templateEngine.process(eq("emails/password-reset"), any(Context.class))).thenReturn(expectedHtml);

        // This cast is necessary because MimeMessage is an interface
        MimeMessage mimeMessage = new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendPasswordResetEmail(user, token, resetUrlBase);

        // Then
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getSubject()).isEqualTo("AstroURL - Password Reset Request");
        assertThat(sentMessage.getAllRecipients()[0].toString()).isEqualTo("test@example.com");
        assertThat(sentMessage.getContent().toString()).contains("Hello, testuser!");
        assertThat(sentMessage.getFrom()[0].toString()).isEqualTo("no-reply@astrourl.com");
    }
}