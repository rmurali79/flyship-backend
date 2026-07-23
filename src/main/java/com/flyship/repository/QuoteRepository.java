package com.flyship.repository;

import com.flyship.entity.Quote;
import com.flyship.entity.Quote.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    List<Quote> findByShipmentId(Long shipmentId);
    List<Quote> findByTravelerIdAndStatus(Long travelerId, QuoteStatus status);
    List<Quote> findByShipmentIdAndStatusNot(Long shipmentId, QuoteStatus status);
    List<Quote> findByShipmentIdAndIdNotAndStatus(Long shipmentId, Long excludeId, QuoteStatus status);
}
