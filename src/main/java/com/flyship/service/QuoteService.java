package com.flyship.service;

import com.flyship.entity.*;
import com.flyship.entity.Quote.QuoteStatus;
import com.flyship.entity.WalletLock.LockType;
import com.flyship.entity.WalletLock.LockStatus;
import com.flyship.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuoteService {

    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletLockRepository walletLockRepository;
    @Autowired private WalletService walletService;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private EmailService emailService;
    @Autowired private NotificationService notificationService;

    @Transactional
    public Quote createQuote(Long shipmentId, BigDecimal amount, LocalDate deliveryDate,
                             String currency, String message, Long travelerId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (shipment.getMaxBudget() != null && amount.compareTo(shipment.getMaxBudget()) > 0)
            throw new RuntimeException("Quote amount cannot exceed budget of $" + shipment.getMaxBudget());

        if (deliveryDate != null && shipment.getReachLatestBy() != null && deliveryDate.isAfter(shipment.getReachLatestBy()))
            throw new RuntimeException("Delivery date must be on or before " + shipment.getReachLatestBy());

        String curr = currency != null ? currency : "USD";

        Quote quote = new Quote();
        quote.setShipmentId(shipmentId); quote.setTravelerId(travelerId);
        quote.setAmount(amount); quote.setDeliveryDate(deliveryDate);
        quote.setCurrency(curr); quote.setMessage(message);
        quote.setStatus(QuoteStatus.pending);
        quote = quoteRepository.save(quote);

        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            walletService.lockFunds(travelerId, curr, amount, LockType.quote_collateral, quote.getId());
        }

        Quote savedQuote = quote;
        userRepository.findById(shipment.getShipperId()).ifPresent(shipper -> {
            emailService.sendNewQuoteNotification(shipper.getEmail(), shipment.getId(),
                    shipment.getOrigin(), shipment.getDestination(), savedQuote.getAmount(), savedQuote.getCurrency());
            notificationService.create(shipper.getId(), Notification.NotificationType.new_quote,
                    "New quote received for your shipment #" + shipment.getId(),
                    String.format("You've received a new quote of %s %s for your shipment from %s to %s.",
                            savedQuote.getCurrency(), savedQuote.getAmount(), shipment.getOrigin(), shipment.getDestination()),
                    shipment.getId());
        });

        return quote;
    }

    public List<Map<String, Object>> getQuotesByShipment(Long shipmentId) {
        List<Quote> quotes = quoteRepository.findByShipmentId(shipmentId);

        List<Long> travelerIds = quotes.stream().map(Quote::getTravelerId).distinct().toList();
        Map<Long, User> travelersById = userRepository.findAllById(travelerIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        Map<Long, Object[]> ratingsByTravelerId = travelerIds.isEmpty() ? Map.of() :
                reviewRepository.getRatingSummaries(travelerIds).stream()
                        .collect(java.util.stream.Collectors.toMap(row -> (Long) row[0], row -> row));

        return quotes.stream().map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId()); map.put("shipment_id", q.getShipmentId());
            map.put("traveler_id", q.getTravelerId()); map.put("amount", q.getAmount());
            map.put("currency", q.getCurrency()); map.put("message", q.getMessage());
            map.put("delivery_date", q.getDeliveryDate()); map.put("status", q.getStatus());
            map.put("withdrawal_reason", q.getWithdrawalReason());
            map.put("createdAt", q.getCreatedAt()); map.put("updatedAt", q.getUpdatedAt());
            User traveler = travelersById.get(q.getTravelerId());
            if (traveler != null) {
                Map<String, Object> tm = new HashMap<>();
                tm.put("name", traveler.getName()); tm.put("profile_picture", traveler.getProfilePicture());
                Object[] rating = ratingsByTravelerId.get(q.getTravelerId());
                tm.put("average_rating", rating != null && rating[1] != null ? ((Number) rating[1]).doubleValue() : null);
                tm.put("review_count", rating != null && rating[2] != null ? ((Number) rating[2]).longValue() : 0L);
                map.put("Traveler", tm);
            }
            return map;
        }).toList();
    }

    @Transactional
    public Map<String, Object> acceptQuote(Long quoteId, Long userId) {
        Quote quote = quoteRepository.findById(quoteId).orElseThrow(() -> new RuntimeException("Quote not found"));
        Shipment shipment = shipmentRepository.findById(quote.getShipmentId()).orElseThrow(() -> new RuntimeException("Shipment not found"));
        if (!shipment.getShipperId().equals(userId)) throw new RuntimeException("Unauthorized");
        if (shipment.getStatus() != Shipment.ShipmentStatus.pending && shipment.getStatus() != Shipment.ShipmentStatus.accepted)
            throw new RuntimeException("Shipment not available");

        // Ensure Shipper Budget Locked
        WalletLock budgetLock = walletLockRepository.findByReferenceIdAndTypeAndStatus(
                shipment.getId(), LockType.shipment_budget, LockStatus.active).orElse(null);
        if (budgetLock == null && shipment.getMaxBudget() != null) {
            String currency = shipment.getEscrowCurrency() != null ? shipment.getEscrowCurrency() : "USD";
            walletService.lockFunds(shipment.getShipperId(), currency, shipment.getMaxBudget(), LockType.shipment_budget, shipment.getId());
        }

        // Ensure Traveler Collateral Locked
        WalletLock quoteLock = walletLockRepository.findByReferenceIdAndTypeAndStatus(
                quote.getId(), LockType.quote_collateral, LockStatus.active).orElse(null);
        if (quoteLock == null) {
            String curr = quote.getCurrency() != null ? quote.getCurrency() : "USD";
            walletService.lockFunds(quote.getTravelerId(), curr, quote.getAmount(), LockType.quote_collateral, quote.getId());
        }

        quote.setStatus(QuoteStatus.accepted); quoteRepository.save(quote);
        shipment.setStatus(Shipment.ShipmentStatus.accepted); shipmentRepository.save(shipment);

        userRepository.findById(quote.getTravelerId()).ifPresent(traveler -> {
            emailService.sendQuoteAcceptedNotification(traveler.getEmail(), shipment.getId(),
                    shipment.getOrigin(), shipment.getDestination(), quote.getAmount(), quote.getCurrency());
            notificationService.create(traveler.getId(), Notification.NotificationType.quote_accepted,
                    "Your quote was accepted for shipment #" + shipment.getId(),
                    String.format("Your quote of %s %s for the shipment from %s to %s has been accepted.",
                            quote.getCurrency(), quote.getAmount(), shipment.getOrigin(), shipment.getDestination()),
                    shipment.getId());
        });

        // Reject other quotes and release locks
        List<Quote> otherQuotes = quoteRepository.findByShipmentIdAndIdNotAndStatus(shipment.getId(), quote.getId(), QuoteStatus.pending);
        for (Quote other : otherQuotes) {
            other.setStatus(QuoteStatus.rejected); quoteRepository.save(other);
            walletService.unlockFunds(other.getTravelerId(),
                    other.getCurrency() != null ? other.getCurrency() : "USD",
                    LockType.quote_collateral, other.getId());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Quote accepted");
        response.put("quote_id", quote.getId());
        response.put("status", quote.getStatus().name());
        return response;
    }

    @Transactional
    public Map<String, Object> withdrawQuote(Long quoteId, Long travelerId, String reason) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found"));
        if (!quote.getTravelerId().equals(travelerId)) throw new RuntimeException("Unauthorized");
        if (quote.getStatus() == QuoteStatus.withdrawn || quote.getStatus() == QuoteStatus.rejected)
            throw new RuntimeException("Quote already " + quote.getStatus());

        Shipment shipment = shipmentRepository.findById(quote.getShipmentId())
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        boolean wasAccepted = quote.getStatus() == QuoteStatus.accepted;

        walletService.unlockFunds(travelerId,
                quote.getCurrency() != null ? quote.getCurrency() : "USD",
                LockType.quote_collateral, quote.getId());

        quote.setStatus(QuoteStatus.withdrawn);
        quote.setWithdrawalReason(reason);
        quoteRepository.save(quote);

        if (wasAccepted) {
            shipment.setStatus(Shipment.ShipmentStatus.pending);
            shipmentRepository.save(shipment);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Quote withdrawn");
        response.put("quote_id", quote.getId());
        response.put("shipment_status", shipment.getStatus().name());
        return response;
    }
}
