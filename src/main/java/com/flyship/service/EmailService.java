package com.flyship.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

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
}
