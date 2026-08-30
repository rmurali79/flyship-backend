package com.flyship.controller;

import com.flyship.entity.*;
import com.flyship.repository.*;
import com.flyship.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReviewRepository reviewRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@AuthenticationPrincipal AuthenticatedUser user) {
        try {
            Long userId = user.getId();
            String role = user.getRole();

            BigDecimal totalSpends = BigDecimal.ZERO;
            BigDecimal totalEarnings = BigDecimal.ZERO;
            long itemsShipped = 0;
            long tripsDone = 0;

            if ("shipper".equals(role)) {
                // Total Spends: Sum of completed payments for my shipments
                List<Payment> payments = paymentRepository.findByStatus("completed");
                for (Payment p : payments) {
                    Shipment s = shipmentRepository.findById(p.getShipmentId()).orElse(null);
                    if (s != null && s.getShipperId().equals(userId)) {
                        totalSpends = totalSpends.add(p.getAmount());
                    }
                }

                // Items Shipped
                itemsShipped = shipmentRepository.countByShipperIdAndStatus(userId, Shipment.ShipmentStatus.delivered);

            } else if ("traveler".equals(role)) {
                // Earnings: Sum of completed payments where quote traveler is me
                List<Payment> payments = paymentRepository.findByStatus("completed");
                for (Payment p : payments) {
                    if (p.getQuoteId() != null) {
                        Quote q = quoteRepository.findById(p.getQuoteId()).orElse(null);
                        if (q != null && q.getTravelerId().equals(userId)) {
                            totalEarnings = totalEarnings.add(p.getAmount());
                        }
                    }
                }

                // Trips Done
                List<Quote> acceptedQuotes = quoteRepository.findByTravelerIdAndStatus(userId, Quote.QuoteStatus.accepted);
                for (Quote q : acceptedQuotes) {
                    Shipment s = shipmentRepository.findById(q.getShipmentId()).orElse(null);
                    if (s != null && s.getStatus() == Shipment.ShipmentStatus.delivered) {
                        tripsDone++;
                    }
                }
            }

            List<Object[]> ratingSummaryRows = reviewRepository.getRatingSummary(userId);
            Object[] ratingSummary = ratingSummaryRows.isEmpty() ? null : ratingSummaryRows.get(0);
            Double averageRating = ratingSummary != null && ratingSummary[0] != null
                    ? ((Number) ratingSummary[0]).doubleValue() : null;
            long reviewCount = ratingSummary != null && ratingSummary[1] != null
                    ? ((Number) ratingSummary[1]).longValue() : 0L;

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSpends", totalSpends);
            stats.put("totalEarnings", totalEarnings);
            stats.put("itemsShipped", itemsShipped);
            stats.put("tripsDone", tripsDone);
            stats.put("averageRating", averageRating);
            stats.put("reviewCount", reviewCount);

            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal AuthenticatedUser authUser) {
        try {
            User user = userRepository.findById(authUser.getId()).orElseThrow(() -> new RuntimeException("User not found"));
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("name", user.getName());
            profile.put("email", user.getEmail());
            profile.put("role", user.getRole().name());
            profile.put("profile_picture", user.getProfilePicture());
            profile.put("country_code", user.getCountryCode());
            profile.put("mobile_number", user.getMobileNumber());
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal AuthenticatedUser authUser) {
        try {
            User user = userRepository.findById(authUser.getId()).orElseThrow(() -> new RuntimeException("User not found"));
            if (body.containsKey("name") && body.get("name") != null && !body.get("name").isBlank()) {
                user.setName(body.get("name"));
            }
            if (body.containsKey("profile_picture")) {
                user.setProfilePicture(body.get("profile_picture"));
            }
            if (body.containsKey("country_code")) {
                user.setCountryCode(body.get("country_code"));
            }
            if (body.containsKey("mobile_number")) {
                user.setMobileNumber(body.get("mobile_number"));
            }
            if (body.containsKey("role") && body.get("role") != null && !body.get("role").isBlank()) {
                user.setRole(User.UserRole.valueOf(body.get("role")));
            }
            userRepository.save(user);

            Map<String, Object> result = new HashMap<>();
            result.put("id", user.getId());
            result.put("name", user.getName());
            result.put("email", user.getEmail());
            result.put("role", user.getRole().name());
            result.put("profile_picture", user.getProfilePicture());
            result.put("country_code", user.getCountryCode());
            result.put("mobile_number", user.getMobileNumber());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
