package com.flyship.service;

import com.flyship.entity.User;
import com.flyship.entity.Wallet;
import com.flyship.repository.UserRepository;
import com.flyship.repository.WalletRepository;
import com.flyship.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private EmailService emailService;

    @Transactional
    public Map<String, String> register(String name, String email, String password, String role,
                                        String profilePicture, String countryCode, String mobileNumber) {
        if (userRepository.existsByEmail(email)) throw new RuntimeException("Email already in use");

        String otp = String.valueOf((int) (100000 + Math.random() * 900000));

        User user = new User();
        user.setName(name); user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(User.UserRole.valueOf(role));
        user.setProfilePicture(profilePicture);
        user.setCountryCode(countryCode); user.setMobileNumber(mobileNumber);
        user.setIsActive(false); user.setOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        user = userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId()); wallet.setCurrency("USD");
        wallet.setBalance(new BigDecimal("1000.00")); wallet.setLockedBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);

        emailService.sendOTP(email, otp);
        Map<String, String> r = new HashMap<>();
        r.put("message", "Registration successful. Please check your email for OTP.");
        r.put("email", email);
        return r;
    }

    public Map<String, String> verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (Boolean.TRUE.equals(user.getIsActive())) throw new RuntimeException("User already activated");
        if (!otp.equals(user.getOtp())) throw new RuntimeException("Invalid OTP");
        if (LocalDateTime.now().isAfter(user.getOtpExpiresAt())) throw new RuntimeException("OTP expired");
        user.setIsActive(true); user.setOtp(null); user.setOtpExpiresAt(null);
        userRepository.save(user);
        return Map.of("message", "Account activated successfully. You can now login.");
    }

    public Map<String, String> resendOtp(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (Boolean.TRUE.equals(user.getIsActive())) throw new RuntimeException("User already activated");
        String otp = String.valueOf((int) (100000 + Math.random() * 900000));
        user.setOtp(otp); user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        emailService.sendOTP(email, otp);
        return Map.of("message", "OTP resent successfully");
    }

    public Map<String, Object> login(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) throw new RuntimeException("Invalid credentials");
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Account not activated. Please verify your email.");
            err.put("email", user.getEmail()); err.put("requires_activation", true);
            throw new AccountNotActivatedException(err);
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId()); userInfo.put("name", user.getName());
        userInfo.put("email", user.getEmail()); userInfo.put("role", user.getRole().name());
        userInfo.put("profile_picture", user.getProfilePicture());
        Map<String, Object> r = new HashMap<>();
        r.put("token", token); r.put("user", userInfo);
        return r;
    }

    public static class AccountNotActivatedException extends RuntimeException {
        private final Map<String, Object> details;
        public AccountNotActivatedException(Map<String, Object> details) { super("Account not activated"); this.details = details; }
        public Map<String, Object> getDetails() { return details; }
    }
}
