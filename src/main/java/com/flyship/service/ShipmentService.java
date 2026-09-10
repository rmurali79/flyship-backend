package com.flyship.service;

import com.flyship.entity.*;
import com.flyship.entity.Shipment.ShipmentStatus;
import com.flyship.entity.WalletLock.LockType;
import com.flyship.entity.WalletLock.LockStatus;
import com.flyship.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShipmentService {

    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ShipmentHistoryRepository shipmentHistoryRepository;
    @Autowired private WalletLockRepository walletLockRepository;
    @Autowired private TravelPlanRepository travelPlanRepository;
    @Autowired private WalletService walletService;
    @Autowired private EmailService emailService;
    @Autowired private NotificationService notificationService;

    @Transactional
    public Shipment createShipment(Shipment shipment, Long userId, String userRole) {
        if ("traveler".equals(userRole)) throw new RuntimeException("Travelers cannot create shipments");
        shipment.setShipperId(userId);
        shipment.setStatus(ShipmentStatus.pending);
        shipment = shipmentRepository.save(shipment);

        if (shipment.getMaxBudget() != null && shipment.getMaxBudget().compareTo(BigDecimal.ZERO) > 0) {
            String currency = shipment.getEscrowCurrency() != null ? shipment.getEscrowCurrency() : "USD";
            walletService.lockFunds(userId, currency, shipment.getMaxBudget(), LockType.shipment_budget, shipment.getId());
        }

        ShipmentHistory history = new ShipmentHistory();
        history.setShipmentId(shipment.getId());
        history.setStatus(ShipmentStatus.pending);
        history.setDescription("Shipment created (Budget Locked)");
        shipmentHistoryRepository.save(history);

        return shipment;
    }

    public List<Map<String, Object>> getAllShipments(Long userId, String role, boolean matched) {
        if ("traveler".equals(role) && matched) {
            List<TravelPlan> plans = travelPlanRepository.findByTravelerId(userId);
            if (plans.isEmpty()) return Collections.emptyList();
            Set<Long> seen = new HashSet<>();
            List<Map<String, Object>> result = new ArrayList<>();
            for (TravelPlan plan : plans) {
                List<Shipment> shipments = shipmentRepository
                        .findByOriginAndDestinationAndStatusAndReachLatestByGreaterThanEqual(
                                plan.getOrigin(), plan.getDestination(), ShipmentStatus.pending, plan.getEndDate());
                for (Shipment s : shipments) {
                    if (exceedsBaggageCapacity(s, plan)) continue;
                    if (seen.add(s.getId())) result.add(shipmentToMap(s));
                }
            }
            return result;
        }
        return shipmentRepository.findByStatus(ShipmentStatus.pending).stream()
                .map(this::shipmentToMap).collect(Collectors.toList());
    }

    // A traveler shouldn't see shipments whose parcel weight exceeds the
    // baggage capacity they declared for this itinerary. Plans/shipments
    // without a weight/capacity on file are left unfiltered (nothing to compare).
    private boolean exceedsBaggageCapacity(Shipment shipment, TravelPlan plan) {
        return shipment.getWeight() != null
                && plan.getAvailableBaggageKg() != null
                && shipment.getWeight().compareTo(plan.getAvailableBaggageKg()) > 0;
    }

    public List<Map<String, Object>> getMyShipments(Long userId) {
        return shipmentRepository.findByShipperId(userId).stream()
                .map(this::shipmentToMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMyDeliveries(Long userId) {
        List<Quote> quotes = quoteRepository.findByTravelerIdAndStatus(userId, Quote.QuoteStatus.accepted);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Quote q : quotes) {
            Shipment shipment = shipmentRepository.findById(q.getShipmentId()).orElse(null);
            if (shipment != null) {
                Map<String, Object> map = shipmentToMap(shipment);
                map.put("quote_amount", q.getAmount());
                map.put("quote_id", q.getId());
                result.add(map);
            }
        }
        return result;
    }

    public Map<String, Object> getShipmentById(Long id) {
        Shipment shipment = shipmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Shipment not found"));
        Map<String, Object> map = shipmentToMap(shipment);
        map.put("History", shipmentHistoryRepository.findByShipmentIdOrderByTimestampAsc(id));
        return map;
    }

    @Transactional
    public ShipmentHistory updateStatus(Long shipmentId, String status, String description, String location, Long userId) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElseThrow(() -> new RuntimeException("Shipment not found"));
        ShipmentStatus newStatus = ShipmentStatus.valueOf(status);

        Quote acceptedQuote = null;
        if (newStatus == ShipmentStatus.in_transit || newStatus == ShipmentStatus.delivered) {
            acceptedQuote = quoteRepository.findByShipmentId(shipmentId).stream()
                    .filter(q -> q.getStatus() == Quote.QuoteStatus.accepted).findFirst()
                    .orElseThrow(() -> new RuntimeException("No accepted traveler found for this shipment"));
            if (!acceptedQuote.getTravelerId().equals(userId))
                throw new RuntimeException("Only the accepted traveler can update this shipment's status");

            if (newStatus == ShipmentStatus.in_transit && shipment.getStatus() != ShipmentStatus.accepted)
                throw new RuntimeException("Shipment must be accepted before it can be marked in transit");
            if (newStatus == ShipmentStatus.delivered && shipment.getStatus() != ShipmentStatus.in_transit)
                throw new RuntimeException("Shipment must be in transit before it can be marked delivered");
        }

        shipment.setStatus(newStatus);
        shipmentRepository.save(shipment);

        if (acceptedQuote != null) {
            notifyStatusChange(shipment, acceptedQuote.getTravelerId(), newStatus.name());
        }

        ShipmentHistory history = new ShipmentHistory();
        history.setShipmentId(shipmentId); history.setStatus(newStatus);
        history.setDescription(description); history.setLocation(location);
        return shipmentHistoryRepository.save(history);
    }

    private void notifyStatusChange(Shipment shipment, Long travelerId, String status) {
        userRepository.findById(shipment.getShipperId()).ifPresent(shipper -> {
            emailService.sendShipmentStatusChangeNotification(shipper.getEmail(), shipment.getId(),
                    shipment.getOrigin(), shipment.getDestination(), status);
            notifyStatusChangeInApp(shipper.getId(), shipment, status);
        });
        userRepository.findById(travelerId).ifPresent(traveler -> {
            emailService.sendShipmentStatusChangeNotification(traveler.getEmail(), shipment.getId(),
                    shipment.getOrigin(), shipment.getDestination(), status);
            notifyStatusChangeInApp(traveler.getId(), shipment, status);
        });
    }

    private void notifyStatusChangeInApp(Long userId, Shipment shipment, String status) {
        notificationService.create(userId, Notification.NotificationType.shipment_status_change,
                "Shipment #" + shipment.getId() + " status update: " + status,
                String.format("Your shipment from %s to %s is now %s.",
                        shipment.getOrigin(), shipment.getDestination(), status),
                shipment.getId());
    }

    @Transactional
    public Map<String, String> cancelShipment(Long shipmentId, Long userId) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElseThrow(() -> new RuntimeException("Shipment not found"));
        if (!shipment.getShipperId().equals(userId)) throw new RuntimeException("Unauthorized");
        if (shipment.getStatus() == ShipmentStatus.cancelled || shipment.getStatus() == ShipmentStatus.delivered)
            throw new RuntimeException("Cannot cancel");

        if (shipment.getStatus() == ShipmentStatus.accepted) {
            Quote acceptedQuote = quoteRepository.findByShipmentId(shipmentId).stream()
                    .filter(q -> q.getStatus() == Quote.QuoteStatus.accepted).findFirst()
                    .orElseThrow(() -> new RuntimeException("Accepted quote data corrupt"));

            WalletLock budgetLock = walletLockRepository.findByReferenceIdAndTypeAndStatus(
                    shipmentId, LockType.shipment_budget, LockStatus.active).orElse(null);
            if (budgetLock != null) walletService.applyCancellationPenalty(budgetLock, acceptedQuote.getTravelerId());

            walletLockRepository.findByReferenceIdAndTypeAndStatus(
                    acceptedQuote.getId(), LockType.quote_collateral, LockStatus.active).ifPresent(ql ->
                    walletService.unlockFunds(acceptedQuote.getTravelerId(),
                            acceptedQuote.getCurrency() != null ? acceptedQuote.getCurrency() : "USD",
                            LockType.quote_collateral, acceptedQuote.getId()));

        } else if (shipment.getStatus() == ShipmentStatus.pending) {
            String currency = shipment.getEscrowCurrency() != null ? shipment.getEscrowCurrency() : "USD";
            walletService.unlockFunds(userId, currency, LockType.shipment_budget, shipmentId);
        }

        shipment.setStatus(ShipmentStatus.cancelled);
        shipmentRepository.save(shipment);

        ShipmentHistory history = new ShipmentHistory();
        history.setShipmentId(shipmentId); history.setStatus(ShipmentStatus.cancelled);
        history.setDescription("Shipment cancelled by shipper");
        shipmentHistoryRepository.save(history);

        return Map.of("message", "Shipment cancelled");
    }

    @Transactional
    public Map<String, String> deleteShipment(Long shipmentId, Long userId, DisputeReason reasonCategory, String reasonDetail) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));
        if (!shipment.getShipperId().equals(userId)) throw new RuntimeException("Unauthorized");
        if (shipment.getStatus() == ShipmentStatus.delivered || shipment.getStatus() == ShipmentStatus.deleted)
            throw new RuntimeException("Cannot delete this shipment");

        if (shipment.getStatus() == ShipmentStatus.accepted) {
            Quote acceptedQuote = quoteRepository.findByShipmentId(shipmentId).stream()
                    .filter(q -> q.getStatus() == Quote.QuoteStatus.accepted).findFirst().orElse(null);
            if (acceptedQuote != null) {
                WalletLock budgetLock = walletLockRepository.findByReferenceIdAndTypeAndStatus(
                        shipmentId, LockType.shipment_budget, LockStatus.active).orElse(null);
                if (budgetLock != null) walletService.applyCancellationPenalty(budgetLock, acceptedQuote.getTravelerId());

                walletLockRepository.findByReferenceIdAndTypeAndStatus(
                        acceptedQuote.getId(), LockType.quote_collateral, LockStatus.active).ifPresent(ql ->
                        walletService.unlockFunds(acceptedQuote.getTravelerId(),
                                acceptedQuote.getCurrency() != null ? acceptedQuote.getCurrency() : "USD",
                                LockType.quote_collateral, acceptedQuote.getId()));
            }
        } else if (shipment.getStatus() == ShipmentStatus.pending || shipment.getStatus() == ShipmentStatus.cancelled) {
            String currency = shipment.getEscrowCurrency() != null ? shipment.getEscrowCurrency() : "USD";
            walletService.unlockFunds(userId, currency, LockType.shipment_budget, shipmentId);
        }

        List<Quote> pendingQuotes = quoteRepository.findByShipmentIdAndStatusNot(shipmentId, Quote.QuoteStatus.rejected);
        for (Quote q : pendingQuotes) {
            if (q.getStatus() == Quote.QuoteStatus.pending) {
                walletService.unlockFunds(q.getTravelerId(),
                        q.getCurrency() != null ? q.getCurrency() : "USD",
                        LockType.quote_collateral, q.getId());
            }
            q.setStatus(Quote.QuoteStatus.rejected);
            quoteRepository.save(q);
        }

        shipment.setStatus(ShipmentStatus.deleted);
        shipment.setCancellationReasonCategory(reasonCategory);
        shipment.setCancellationReason(reasonDetail);
        shipmentRepository.save(shipment);

        ShipmentHistory history = new ShipmentHistory();
        history.setShipmentId(shipmentId);
        history.setStatus(ShipmentStatus.deleted);
        history.setDescription("Shipment deleted by shipper: " + reasonCategory
                + (reasonDetail != null && !reasonDetail.isBlank() ? " - " + reasonDetail : ""));
        shipmentHistoryRepository.save(history);

        return Map.of("message", "Shipment deleted");
    }

    private Map<String, Object> shipmentToMap(Shipment s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId()); map.put("shipperId", s.getShipperId());
        map.put("origin", s.getOrigin()); map.put("destination", s.getDestination());
        map.put("details", s.getDetails()); map.put("item_description", s.getItemDescription());
        map.put("photo_url", s.getPhotoUrl()); map.put("weight", s.getWeight());
        map.put("dimension_length", s.getDimensionLength()); map.put("dimension_width", s.getDimensionWidth());
        map.put("dimension_height", s.getDimensionHeight()); map.put("max_budget", s.getMaxBudget());
        map.put("shipment_arrangement", s.getShipmentArrangement());
        map.put("collection_point", s.getCollectionPoint());
        map.put("delivery_recipient_name", s.getDeliveryRecipientName());
        map.put("delivery_address", s.getDeliveryAddress());
        map.put("delivery_arrangement", s.getDeliveryArrangement());
        map.put("status", s.getStatus()); map.put("reach_latest_by", s.getReachLatestBy());
        map.put("escrow_amount", s.getEscrowAmount()); map.put("escrow_currency", s.getEscrowCurrency());
        map.put("cancellation_reason", s.getCancellationReason());
        map.put("cancellation_reason_category", s.getCancellationReasonCategory());
        map.put("createdAt", s.getCreatedAt()); map.put("updatedAt", s.getUpdatedAt());
        User shipper = userRepository.findById(s.getShipperId()).orElse(null);
        if (shipper != null) map.put("Shipper", Map.of("name", shipper.getName()));
        return map;
    }
}
