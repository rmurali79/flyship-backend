package com.flyship.service;

import com.flyship.entity.*;
import com.flyship.entity.Quote.QuoteStatus;
import com.flyship.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private UserRepository userRepository;

    @Transactional
    public Map<String, Object> createReview(Long shipmentId, Long reviewerId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5)
            throw new RuntimeException("Rating must be between 1 and 5");

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));
        if (shipment.getStatus() != Shipment.ShipmentStatus.delivered)
            throw new RuntimeException("Shipment must be delivered before it can be reviewed");

        Long travelerId = quoteRepository.findByShipmentId(shipmentId).stream()
                .filter(q -> q.getStatus() == QuoteStatus.accepted)
                .map(Quote::getTravelerId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No accepted traveler found for this shipment"));

        Long revieweeId;
        if (reviewerId.equals(shipment.getShipperId())) {
            revieweeId = travelerId;
        } else if (reviewerId.equals(travelerId)) {
            revieweeId = shipment.getShipperId();
        } else {
            throw new RuntimeException("You are not authorized to review this shipment");
        }

        if (reviewRepository.findByShipmentIdAndReviewerId(shipmentId, reviewerId).isPresent())
            throw new RuntimeException("You've already reviewed this shipment");

        Review review = new Review();
        review.setShipmentId(shipmentId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setRating(rating);
        review.setComment(comment);
        review = reviewRepository.save(review);

        return toMap(review);
    }

    public List<Map<String, Object>> getReviewsForShipment(Long shipmentId) {
        List<Review> reviews = reviewRepository.findByShipmentId(shipmentId);

        List<Long> userIds = reviews.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getReviewerId(), r.getRevieweeId()))
                .distinct().toList();
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        return reviews.stream().map(r -> {
            Map<String, Object> map = toMap(r);
            User reviewer = usersById.get(r.getReviewerId());
            if (reviewer != null) map.put("reviewer_name", reviewer.getName());
            return map;
        }).toList();
    }

    public Map<String, Object> getUserRatingSummary(Long userId) {
        List<Object[]> summaryRows = reviewRepository.getRatingSummary(userId);
        Object[] summary = summaryRows.isEmpty() ? null : summaryRows.get(0);
        Double avg = summary != null && summary[0] != null ? ((Number) summary[0]).doubleValue() : null;
        Long count = summary != null && summary[1] != null ? ((Number) summary[1]).longValue() : 0L;

        List<Review> received = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId);
        Map<Long, User> reviewersById = userRepository.findAllById(
                received.stream().map(Review::getReviewerId).distinct().toList()
        ).stream().collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> reviews = received.stream().map(r -> {
            Map<String, Object> map = toMap(r);
            User reviewer = reviewersById.get(r.getReviewerId());
            if (reviewer != null) map.put("reviewer_name", reviewer.getName());
            return map;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("average_rating", avg);
        result.put("review_count", count);
        result.put("reviews", reviews);
        return result;
    }

    private Map<String, Object> toMap(Review r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("shipment_id", r.getShipmentId());
        map.put("reviewer_id", r.getReviewerId());
        map.put("reviewee_id", r.getRevieweeId());
        map.put("rating", r.getRating());
        map.put("comment", r.getComment());
        map.put("createdAt", r.getCreatedAt());
        return map;
    }
}
