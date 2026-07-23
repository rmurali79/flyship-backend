package com.flyship.controller;

import com.flyship.entity.Message;
import com.flyship.entity.Quote;
import com.flyship.entity.Shipment;
import com.flyship.entity.User;
import com.flyship.repository.MessageRepository;
import com.flyship.repository.QuoteRepository;
import com.flyship.repository.ShipmentRepository;
import com.flyship.repository.UserRepository;
import com.flyship.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired private MessageRepository messageRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping("/shipment/{shipmentId}")
    @Transactional
    public ResponseEntity<?> getMessages(@PathVariable Long shipmentId,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            if (!isAuthorized(shipmentId, user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Chat not available"));
            }

            messageRepository.markAsRead(shipmentId, user.getId());

            List<Message> messages = messageRepository.findByShipmentIdOrderByCreatedAtAsc(shipmentId);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/shipment/{shipmentId}")
    public ResponseEntity<?> sendMessage(@PathVariable Long shipmentId,
                                          @RequestBody Map<String, String> body,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            String content = body.get("content");
            if (content == null || content.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
            }

            Long receiverId = getOtherParty(shipmentId, user.getId());
            if (receiverId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "Chat not available"));
            }

            Message msg = new Message();
            msg.setShipmentId(shipmentId);
            msg.setSenderId(user.getId());
            msg.setReceiverId(receiverId);
            msg.setContent(content);
            msg.setMessageType(body.getOrDefault("type", "text"));
            messageRepository.save(msg);

            return ResponseEntity.ok(msg);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shipment/{shipmentId}/info")
    public ResponseEntity<?> getChatInfo(@PathVariable Long shipmentId,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            Long otherId = getOtherParty(shipmentId, user.getId());
            if (otherId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "Chat not available"));
            }

            User other = userRepository.findById(otherId).orElse(null);
            Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
            long unread = messageRepository.countUnread(shipmentId, user.getId());

            Map<String, Object> info = new HashMap<>();
            info.put("shipment_id", shipmentId);
            info.put("shipment_route", shipment != null ? shipment.getOrigin() + " → " + shipment.getDestination() : "");
            info.put("other_user_id", otherId);
            info.put("other_user_name", other != null ? other.getName() : "Unknown");
            info.put("other_user_phone", other != null ? other.getMobileNumber() : null);
            info.put("other_user_country_code", other != null ? other.getCountryCode() : null);
            info.put("unread_count", unread);

            return ResponseEntity.ok(info);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@AuthenticationPrincipal AuthenticatedUser user) {
        try {
            List<Message> latest = messageRepository.findLatestPerShipment(user.getId());
            List<Map<String, Object>> result = new ArrayList<>();

            for (Message msg : latest) {
                Long otherId = msg.getSenderId().equals(user.getId()) ? msg.getReceiverId() : msg.getSenderId();
                User other = userRepository.findById(otherId).orElse(null);
                Shipment shipment = shipmentRepository.findById(msg.getShipmentId()).orElse(null);
                long unread = messageRepository.countUnread(msg.getShipmentId(), user.getId());

                Map<String, Object> conv = new HashMap<>();
                conv.put("shipment_id", msg.getShipmentId());
                conv.put("route", shipment != null ? shipment.getOrigin() + " → " + shipment.getDestination() : "");
                conv.put("other_user_name", other != null ? other.getName() : "Unknown");
                conv.put("last_message", msg.getContent());
                conv.put("last_message_time", msg.getCreatedAt());
                conv.put("unread_count", unread);
                conv.put("is_mine", msg.getSenderId().equals(user.getId()));
                result.add(conv);
            }

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isAuthorized(Long shipmentId, Long userId) {
        return getOtherParty(shipmentId, userId) != null;
    }

    private Long getOtherParty(Long shipmentId, Long userId) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
        if (shipment == null) return null;
        if (shipment.getStatus() != Shipment.ShipmentStatus.accepted
                && shipment.getStatus() != Shipment.ShipmentStatus.in_transit
                && shipment.getStatus() != Shipment.ShipmentStatus.delivered) return null;

        Quote acceptedQuote = quoteRepository.findByShipmentId(shipmentId).stream()
                .filter(q -> q.getStatus() == Quote.QuoteStatus.accepted).findFirst().orElse(null);
        if (acceptedQuote == null) return null;

        if (userId.equals(shipment.getShipperId())) return acceptedQuote.getTravelerId();
        if (userId.equals(acceptedQuote.getTravelerId())) return shipment.getShipperId();
        return null;
    }
}
