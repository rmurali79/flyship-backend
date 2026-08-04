package com.flyship.controller;

import com.flyship.security.AuthenticatedUser;
import com.flyship.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody Map<String, Object> body,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            Long shipmentId = Long.valueOf(body.get("shipment_id").toString());
            Integer rating = Integer.valueOf(body.get("rating").toString());
            String comment = body.get("comment") != null ? body.get("comment").toString() : null;

            Map<String, Object> review = reviewService.createReview(shipmentId, user.getId(), rating, comment);
            return ResponseEntity.status(HttpStatus.CREATED).body(review);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shipment/{shipmentId}")
    public ResponseEntity<?> getReviewsForShipment(@PathVariable Long shipmentId) {
        try {
            return ResponseEntity.ok(reviewService.getReviewsForShipment(shipmentId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserRatingSummary(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(reviewService.getUserRatingSummary(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
