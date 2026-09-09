package com.flyship.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = Mockito.mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));

        emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
    }

    @Test
    void sendNewQuoteNotification_sendsMailAndReturnsTrue() {
        boolean result = emailService.sendNewQuoteNotification(
                "shipper@example.com", 10L, "NYC", "LON", new BigDecimal("50.00"), "USD");

        assertTrue(result);
        verify(mailSender).send(Mockito.any(MimeMessage.class));
    }

    @Test
    void sendQuoteAcceptedNotification_sendsMailAndReturnsTrue() {
        boolean result = emailService.sendQuoteAcceptedNotification(
                "traveler@example.com", 10L, "NYC", "LON", new BigDecimal("75.00"), "USD");

        assertTrue(result);
        verify(mailSender).send(Mockito.any(MimeMessage.class));
    }

    @Test
    void sendShipmentStatusChangeNotification_sendsMailAndReturnsTrue() {
        boolean result = emailService.sendShipmentStatusChangeNotification(
                "shipper@example.com", 10L, "NYC", "LON", "in_transit");

        assertTrue(result);
        verify(mailSender).send(Mockito.any(MimeMessage.class));
    }
}
