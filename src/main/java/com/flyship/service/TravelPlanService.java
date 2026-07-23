package com.flyship.service;

import com.flyship.entity.*;
import com.flyship.entity.Quote.QuoteStatus;
import com.flyship.entity.Shipment.ShipmentStatus;
import com.flyship.entity.WalletLock.LockType;
import com.flyship.entity.WalletLock.LockStatus;
import com.flyship.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TravelPlanService {

    @Autowired private TravelPlanRepository travelPlanRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private WalletLockRepository walletLockRepository;
    @Autowired private WalletService walletService;

    public TravelPlan createPlan(TravelPlan plan, Long travelerId) {
        plan.setTravelerId(travelerId);
        return travelPlanRepository.save(plan);
    }

    public List<TravelPlan> getMyPlans(Long travelerId) {
        return travelPlanRepository.findByTravelerIdOrderByStartDateAsc(travelerId);
    }

    @Transactional
    public Map<String, Object> cancelPlan(Long planId, Long travelerId) {
        TravelPlan plan = travelPlanRepository.findById(planId).orElseThrow(() -> new RuntimeException("Travel Plan not found"));
        if (!plan.getTravelerId().equals(travelerId)) throw new RuntimeException("Unauthorized");

        List<Quote> acceptedQuotes = quoteRepository.findByTravelerIdAndStatus(travelerId, QuoteStatus.accepted);
        List<Quote> affectedQuotes = acceptedQuotes.stream().filter(q -> {
            Shipment s = shipmentRepository.findById(q.getShipmentId()).orElse(null);
            return s != null && s.getOrigin().equals(plan.getOrigin()) && s.getDestination().equals(plan.getDestination());
        }).collect(Collectors.toList());

        for (Quote quote : affectedQuotes) {
            Shipment shipment = shipmentRepository.findById(quote.getShipmentId()).orElse(null);
            if (shipment == null) continue;

            walletLockRepository.findByReferenceIdAndTypeAndStatus(quote.getId(), LockType.quote_collateral, LockStatus.active)
                    .ifPresent(lock -> walletService.applyCancellationPenalty(lock, shipment.getShipperId()));

            walletLockRepository.findByReferenceIdAndTypeAndStatus(shipment.getId(), LockType.shipment_budget, LockStatus.active)
                    .ifPresent(bl -> {
                        String currency = shipment.getEscrowCurrency() != null ? shipment.getEscrowCurrency() : "USD";
                        walletService.unlockFunds(shipment.getShipperId(), currency, LockType.shipment_budget, shipment.getId());
                    });

            quote.setStatus(QuoteStatus.rejected); quoteRepository.save(quote);
            shipment.setStatus(ShipmentStatus.pending); shipmentRepository.save(shipment);
        }

        travelPlanRepository.delete(plan);
        return Map.of("message", "Travel plan cancelled", "affected_shipments", affectedQuotes.size());
    }
}
