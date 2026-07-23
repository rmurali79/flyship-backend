package com.flyship.repository;

import com.flyship.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserIdAndStatus(Long userId, String status);
    List<Payment> findByShipmentIdAndStatus(Long shipmentId, String status);
    List<Payment> findByStatus(String status);
}
