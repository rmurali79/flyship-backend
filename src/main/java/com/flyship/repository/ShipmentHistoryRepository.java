package com.flyship.repository;

import com.flyship.entity.ShipmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShipmentHistoryRepository extends JpaRepository<ShipmentHistory, Long> {
    List<ShipmentHistory> findByShipmentIdOrderByTimestampAsc(Long shipmentId);
}
