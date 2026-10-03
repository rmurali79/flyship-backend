package com.flyship.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
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

    @Test
    void sendOTP_returnsFalseInsteadOfThrowingWhenSmtpAuthFails() {
        doThrow(new MailAuthenticationException("Authentication failed"))
                .when(mailSender).send(Mockito.any(MimeMessage.class));

        boolean result = emailService.sendOTP("user@example.com", "123456");

        assertFalse(result);
    }

    @Test
    void sendNewQuoteNotification_returnsFalseInsteadOfThrowingWhenSmtpAuthFails() {
        doThrow(new MailAuthenticationException("Authentication failed"))
                .when(mailSender).send(Mockito.any(MimeMessage.class));

        boolean result = emailService.sendNewQuoteNotification(
                "shipper@example.com", 10L, "NYC", "LON", new BigDecimal("50.00"), "USD");

        assertFalse(result);
    }

    @Test
    void notificationsAreBrandedPeerPost() throws Exception {
        org.mockito.ArgumentCaptor<MimeMessage> sent = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        emailService.sendShipmentStatusChangeNotification("shipper@example.com", 7L, "Berlin", "Chennai", "delivered");
        verify(mailSender).send(sent.capture());

        MimeMessage message = sent.getValue();
        message.saveChanges();
        jakarta.mail.internet.InternetAddress from = (jakarta.mail.internet.InternetAddress) message.getFrom()[0];
        org.junit.jupiter.api.Assertions.assertEquals("PeerPost", from.getPersonal());
        org.junit.jupiter.api.Assertions.assertEquals("no-reply@peerpost.online", from.getAddress());
        java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
        message.writeTo(raw);
        String body = raw.toString(java.nio.charset.StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("Log in to PeerPost"), body);
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("FlyShip"), body);
    }
}
