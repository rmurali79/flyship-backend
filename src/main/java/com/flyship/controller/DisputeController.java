package com.flyship.controller;

import com.flyship.entity.Dispute.SubjectType;
import com.flyship.entity.DisputeReason;
import com.flyship.security.AuthenticatedUser;
import com.flyship.service.DisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    @Autowired
    private DisputeService disputeService;

    @PostMapping
    public ResponseEntity<?> fileDispute(@RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            SubjectType subjectType = parseSubjectType(body.get("subject_type"));
            Long subjectId = Long.valueOf(body.get("subject_id").toString());
            DisputeReason reasonCategory = parseReason(body.get("reason_category"));
            String description = body.get("description") != null ? body.get("description").toString() : null;
            List<String> evidencePhotoUrls = extractUrls(body.get("evidence_photo_urls"));

            Map<String, Object> dispute = disputeService.fileDispute(
                    user.getId(), subjectType, subjectId, reasonCategory, description, evidencePhotoUrls);
            return ResponseEntity.status(HttpStatus.CREATED).body(dispute);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyDisputes(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(disputeService.listForUser(user.getId()));
    }

    @GetMapping
    public ResponseEntity<?> getDisputesForSubject(@RequestParam("subject_type") String subjectType,
                                                    @RequestParam("subject_id") Long subjectId) {
        try {
            return ResponseEntity.ok(disputeService.listForSubject(parseSubjectType(subjectType), subjectId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDispute(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(disputeService.getDispute(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<?> addEvidence(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            List<String> urls = extractUrls(body.get("evidence_photo_urls"));
            if (urls == null || urls.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "evidence_photo_urls is required"));
            return ResponseEntity.ok(disputeService.addEvidence(id, user.getId(), urls));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> markUnderReview(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(disputeService.markUnderReview(id, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptDispute(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            String notes = body != null ? body.get("resolution_notes") : null;
            return ResponseEntity.ok(disputeService.acceptDispute(id, user.getId(), notes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectDispute(@PathVariable Long id, @RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(disputeService.rejectDispute(id, user.getId(), body.get("resolution_notes")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdrawDispute(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        try {
            return ResponseEntity.ok(disputeService.withdrawDispute(id, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private SubjectType parseSubjectType(Object raw) {
        if (raw == null) throw new RuntimeException("subject_type is required");
        try {
            return SubjectType.valueOf(raw.toString());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid subject_type: " + raw);
        }
    }

    private DisputeReason parseReason(Object raw) {
        if (raw == null) throw new RuntimeException("reason_category is required");
        try {
            return DisputeReason.valueOf(raw.toString());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid reason_category: " + raw);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractUrls(Object raw) {
        if (raw == null) return Collections.emptyList();
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        throw new RuntimeException("evidence_photo_urls must be an array");
    }
}
