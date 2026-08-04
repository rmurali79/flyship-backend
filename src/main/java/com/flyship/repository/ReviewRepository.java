package com.flyship.repository;

import com.flyship.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByShipmentId(Long shipmentId);
    List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);
    Optional<Review> findByShipmentIdAndReviewerId(Long shipmentId, Long reviewerId);

    @Query("SELECT AVG(r.rating), COUNT(r) FROM Review r WHERE r.revieweeId = :userId")
    List<Object[]> getRatingSummary(@Param("userId") Long userId);

    @Query("SELECT r.revieweeId, AVG(r.rating), COUNT(r) FROM Review r WHERE r.revieweeId IN :userIds GROUP BY r.revieweeId")
    List<Object[]> getRatingSummaries(@Param("userIds") List<Long> userIds);
}
