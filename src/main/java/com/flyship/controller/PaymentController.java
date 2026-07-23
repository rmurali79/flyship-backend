package com.flyship.controller;

import com.flyship.entity.Payment;
import com.flyship.entity.Quote;
import com.flyship.repository.PaymentRepository;
import com.flyship.repository.QuoteRepository;
import com.flyship.security.AuthenticatedUser;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        return ResponseEntity.ok(Map.of("publishableKey", stripePublishableKey));
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody Map<String, Object> body,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            Long quoteId = Long.valueOf(body.get("quote_id").toString());
            Quote quote = quoteRepository.findById(quoteId)
                    .orElseThrow(() -> new RuntimeException("Quote not found"));

            BigDecimal amount = quote.getAmount();
            BigDecimal fee = amount.multiply(new BigDecimal("0.10"));
            BigDecimal total = amount.add(fee);
            long totalCents = total.multiply(new BigDecimal("100")).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(totalCents)
                    .setCurrency(quote.getCurrency() != null ? quote.getCurrency().toLowerCase() : "usd")
                    .putMetadata("quote_id", quote.getId().toString())
                    .putMetadata("shipment_id", quote.getShipmentId().toString())
                    .putMetadata("user_id", user.getId().toString())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            return ResponseEntity.ok(Map.of(
                    "clientSecret", paymentIntent.getClientSecret(),
                    "amount", total.doubleValue()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, Object> body,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            String paymentIntentId = body.get("payment_intent_id").toString();
            Long quoteId = Long.valueOf(body.get("quote_id").toString());

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            if (!"succeeded".equals(paymentIntent.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Payment not succeeded"));
            }

            Quote quote = quoteRepository.findById(quoteId)
                    .orElseThrow(() -> new RuntimeException("Quote not found"));

            Payment payment = new Payment();
            payment.setUserId(user.getId());
            payment.setShipmentId(quote.getShipmentId());
            payment.setQuoteId(quoteId);
            payment.setAmount(new BigDecimal(paymentIntent.getAmount()).divide(new BigDecimal("100")));
            payment.setStatus("completed");
            payment.setTransactionId(paymentIntentId);
            paymentRepository.save(payment);

            return ResponseEntity.ok(Map.of("message", "Payment recorded", "payment_id", payment.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
