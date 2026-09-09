package com.flyship.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.math.BigDecimal;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    public boolean sendOTP(String email, String otp) {
        log.info("[Email Service] Sending OTP {} to {}", otp, email);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("\"FlyShip Logistics\" <no-reply@flyship.com>");
            helper.setTo(email);
            helper.setSubject("Your OTP Code");
            helper.setText(
                    String.format("Your OTP code is %s. It expires in 10 minutes.", otp),
                    String.format("<b>Your OTP code is %s</b>. It expires in 10 minutes.", otp)
            );

            mailSender.send(message);
            log.info("OTP email sent successfully to {}", email);
            return true;
        } catch (MessagingException e) {
            log.error("Error sending email: ", e);
            return false;
        }
    }

    public boolean sendNewQuoteNotification(String shipperEmail, Long shipmentId, String origin, String destination,
                                             BigDecimal amount, String currency) {
        String subject = "New quote received for your shipment #" + shipmentId;
        String plain = String.format(
                "You've received a new quote of %s %s for your shipment from %s to %s. Log in to FlyShip to review it.",
                currency, amount, origin, destination);
        String html = String.format(
                "You've received a new quote of <b>%s %s</b> for your shipment from <b>%s</b> to <b>%s</b>. " +
                        "Log in to FlyShip to review it.",
                currency, amount, origin, destination);
        return sendNotification(shipperEmail, subject, plain, html);
    }

    public boolean sendQuoteAcceptedNotification(String travelerEmail, Long shipmentId, String origin, String destination,
                                                  BigDecimal amount, String currency) {
        String subject = "Your quote was accepted for shipment #" + shipmentId;
        String plain = String.format(
                "Your quote of %s %s for the shipment from %s to %s has been accepted. Log in to FlyShip for details.",
                currency, amount, origin, destination);
        String html = String.format(
                "Your quote of <b>%s %s</b> for the shipment from <b>%s</b> to <b>%s</b> has been accepted. " +
                        "Log in to FlyShip for details.",
                currency, amount, origin, destination);
        return sendNotification(travelerEmail, subject, plain, html);
    }

    public boolean sendShipmentStatusChangeNotification(String recipientEmail, Long shipmentId, String origin,
                                                          String destination, String status) {
        String subject = "Shipment #" + shipmentId + " status update: " + status;
        String plain = String.format(
                "Your shipment from %s to %s is now %s. Log in to FlyShip for details.",
                origin, destination, status);
        String html = String.format(
                "Your shipment from <b>%s</b> to <b>%s</b> is now <b>%s</b>. Log in to FlyShip for details.",
                origin, destination, status);
        return sendNotification(recipientEmail, subject, plain, html);
    }

    private boolean sendNotification(String toEmail, String subject, String plainText, String htmlText) {
        log.info("[Email Service] Sending notification '{}' to {}", subject, toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("\"FlyShip Logistics\" <no-reply@flyship.com>");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, htmlText);

            mailSender.send(message);
            log.info("Notification email sent successfully to {}", toEmail);
            return true;
        } catch (MessagingException e) {
            log.error("Error sending notification email: ", e);
            return false;
        }
    }
}
