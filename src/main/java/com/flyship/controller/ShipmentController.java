package com.flyship.controller;

import com.flyship.entity.Shipment;
import com.flyship.entity.ShipmentHistory;
import com.flyship.security.AuthenticatedUser;
import com.flyship.service.ShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    @Autowired
    private ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<?> createShipment(@RequestBody Shipment shipment,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            Shipment created = shipmentService.createShipment(shipment, user.getId(), user.getRole());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllShipments(@AuthenticationPrincipal AuthenticatedUser user,
                                             @RequestParam(name = "matched", required = false, defaultValue = "false") boolean matched) {
        try {
            List<Map<String, Object>> shipments = shipmentService.getAllShipments(user.getId(), user.getRole(), matched);
            return ResponseEntity.ok(shipments);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-shipments")
    public ResponseEntity<?> getMyShipments(@AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(shipmentService.getMyShipments(user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-deliveries")
    public ResponseEntity<?> getMyDeliveries(@AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(shipmentService.getMyDeliveries(user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getShipmentById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(shipmentService.getShipmentById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                           @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            ShipmentHistory history = shipmentService.updateStatus(id,
                    body.get("status"), body.get("description"), body.get("location"), user.getId());
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelShipment(@PathVariable Long id,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(shipmentService.cancelShipment(id, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<?> deleteShipment(@PathVariable Long id,
                                             @RequestBody Map<String, String> body,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            String reason = body.get("reason");
            if (reason == null || reason.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
            return ResponseEntity.ok(shipmentService.deleteShipment(id, user.getId(), reason));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
