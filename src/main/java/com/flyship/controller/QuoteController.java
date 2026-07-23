package com.flyship.controller;

import com.flyship.entity.Quote;
import com.flyship.security.AuthenticatedUser;
import com.flyship.service.QuoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    @Autowired
    private QuoteService quoteService;

    @PostMapping
    public ResponseEntity<?> createQuote(@RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            Long shipmentId = Long.valueOf(body.get("shipment_id").toString());
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            LocalDate deliveryDate = body.get("delivery_date") != null
                    ? LocalDate.parse(body.get("delivery_date").toString()) : null;
            String currency = body.get("currency") != null ? body.get("currency").toString() : "USD";
            String message = body.get("message") != null ? body.get("message").toString() : null;

            Quote quote = quoteService.createQuote(shipmentId, amount, deliveryDate, currency, message, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", quote.getId(), "shipment_id", quote.getShipmentId(),
                    "amount", quote.getAmount(), "currency", quote.getCurrency(),
                    "status", quote.getStatus().name(), "message", quote.getMessage() != null ? quote.getMessage() : ""
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shipment/{shipmentId}")
    public ResponseEntity<?> getQuotesByShipment(@PathVariable Long shipmentId,
                                                  @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(quoteService.getQuotesByShipment(shipmentId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptQuote(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(quoteService.acceptQuote(id, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdrawQuote(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            String reason = body.get("reason");
            if (reason == null || reason.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
            return ResponseEntity.ok(quoteService.withdrawQuote(id, user.getId(), reason));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
