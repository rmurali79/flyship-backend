package com.flyship.controller;

import com.flyship.entity.TravelPlan;
import com.flyship.security.AuthenticatedUser;
import com.flyship.service.TravelPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/travel-plans")
public class TravelPlanController {

    @Autowired
    private TravelPlanService travelPlanService;

    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody TravelPlan plan,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            TravelPlan created = travelPlanService.createPlan(plan, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-plans")
    public ResponseEntity<?> getMyPlans(@AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(travelPlanService.getMyPlans(user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelPlan(@PathVariable Long id,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(travelPlanService.cancelPlan(id, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
