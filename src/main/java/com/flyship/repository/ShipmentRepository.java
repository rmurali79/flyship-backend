package com.flyship.repository;

import com.flyship.entity.Shipment;
import com.flyship.entity.Shipment.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByStatus(ShipmentStatus status);
    List<Shipment> findByShipperId(Long shipperId);
    List<Shipment> findByOriginAndDestinationAndStatusAndReachLatestByGreaterThanEqual(
            String origin, String destination, ShipmentStatus status, LocalDate date);
    long countByShipperIdAndStatus(Long shipperId, ShipmentStatus status);
}
