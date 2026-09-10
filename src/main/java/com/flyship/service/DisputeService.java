package com.flyship.service;

import com.flyship.entity.*;
import com.flyship.entity.Dispute.DisputeStatus;
import com.flyship.entity.Dispute.SubjectType;
import com.flyship.entity.WalletLock.LockStatus;
import com.flyship.entity.WalletLock.LockType;
import com.flyship.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DisputeService {

    @Autowired private DisputeRepository disputeRepository;
    @Autowired private DisputeEvidenceRepository disputeEvidenceRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private WalletLockRepository walletLockRepository;
    @Autowired private WalletService walletService;
    @Autowired private NotificationService notificationService;

    @Transactional
    public Map<String, Object> fileDispute(Long userId, SubjectType subjectType, Long subjectId,
                                            DisputeReason reasonCategory, String description, List<String> evidencePhotoUrls) {
        Long respondentUserId = resolveRespondent(subjectType, subjectId, userId);

        Dispute dispute = new Dispute();
        dispute.setSubjectType(subjectType);
        dispute.setSubjectId(subjectId);
        dispute.setFiledByUserId(userId);
        dispute.setRespondentUserId(respondentUserId);
        dispute.setReasonCategory(reasonCategory);
        dispute.setDescription(description);
        dispute.setStatus(DisputeStatus.open);
        dispute = disputeRepository.save(dispute);

        addEvidenceInternal(dispute.getId(), userId, evidencePhotoUrls);

        notificationService.create(respondentUserId, Notification.NotificationType.dispute_filed,
                "A dispute was filed against " + subjectType + " #" + subjectId,
                String.format("A dispute (%s) was filed regarding %s #%d. Reason: %s",
                        "#" + dispute.getId(), subjectType, subjectId, reasonCategory),
                subjectType == SubjectType.shipment ? subjectId : null);

        return disputeToMap(dispute);
    }

    @Transactional
    public Map<String, Object> addEvidence(Long disputeId, Long userId, List<String> photoUrls) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        requireParty(dispute, userId);
        if (dispute.getStatus() != DisputeStatus.open && dispute.getStatus() != DisputeStatus.under_review)
            throw new RuntimeException("Cannot add evidence to a resolved dispute");
        addEvidenceInternal(disputeId, userId, photoUrls);
        return disputeToMap(dispute);
    }

    private void addEvidenceInternal(Long disputeId, Long userId, List<String> photoUrls) {
        if (photoUrls == null) return;
        for (String url : photoUrls) {
            if (url == null || url.isBlank()) continue;
            DisputeEvidence evidence = new DisputeEvidence();
            evidence.setDisputeId(disputeId);
            evidence.setPhotoUrl(url);
            evidence.setUploadedByUserId(userId);
            disputeEvidenceRepository.save(evidence);
        }
    }

    public Map<String, Object> getDispute(Long disputeId) {
        return disputeToMap(getDisputeOrThrow(disputeId));
    }

    public List<Map<String, Object>> listForUser(Long userId) {
        return disputeRepository.findByFiledByUserIdOrRespondentUserIdOrderByCreatedAtDesc(userId, userId)
                .stream().map(this::disputeToMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> listForSubject(SubjectType subjectType, Long subjectId) {
        return disputeRepository.findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(subjectType, subjectId)
                .stream().map(this::disputeToMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> markUnderReview(Long disputeId, Long userId) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getRespondentUserId().equals(userId))
            throw new RuntimeException("Only the respondent can mark a dispute under review");
        if (dispute.getStatus() != DisputeStatus.open)
            throw new RuntimeException("Dispute is not open");
        dispute.setStatus(DisputeStatus.under_review);
        disputeRepository.save(dispute);
        return disputeToMap(dispute);
    }

    @Transactional
    public Map<String, Object> acceptDispute(Long disputeId, Long userId, String resolutionNotes) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getRespondentUserId().equals(userId))
            throw new RuntimeException("Only the respondent can accept a dispute");
        requireResolvable(dispute);

        String walletOutcome = applyWalletOutcomeIfPossible(dispute);

        dispute.setStatus(DisputeStatus.accepted);
        dispute.setResolutionNotes(joinNotes(resolutionNotes, walletOutcome));
        dispute.setResolvedByUserId(userId);
        dispute.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        notifyResolution(dispute, "accepted");
        return disputeToMap(dispute);
    }

    @Transactional
    public Map<String, Object> rejectDispute(Long disputeId, Long userId, String resolutionNotes) {
        if (resolutionNotes == null || resolutionNotes.isBlank())
            throw new RuntimeException("Resolution notes are required to reject a dispute");
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getRespondentUserId().equals(userId))
            throw new RuntimeException("Only the respondent can reject a dispute");
        requireResolvable(dispute);

        dispute.setStatus(DisputeStatus.rejected);
        dispute.setResolutionNotes(resolutionNotes);
        dispute.setResolvedByUserId(userId);
        dispute.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        notifyResolution(dispute, "rejected");
        return disputeToMap(dispute);
    }

    @Transactional
    public Map<String, Object> withdrawDispute(Long disputeId, Long userId) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        if (!dispute.getFiledByUserId().equals(userId))
            throw new RuntimeException("Only the filer can withdraw a dispute");
        requireResolvable(dispute);

        dispute.setStatus(DisputeStatus.withdrawn);
        dispute.setResolvedByUserId(userId);
        dispute.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(dispute);
        return disputeToMap(dispute);
    }

    // Reuses the existing WalletService penalty split (the same one cancellation flows use) so an
    // accepted shipment-level dispute compensates the filer out of any still-active budget lock,
    // instead of this feature inventing its own payout/pricing rules.
    private String applyWalletOutcomeIfPossible(Dispute dispute) {
        if (dispute.getSubjectType() != SubjectType.shipment) {
            return "No automated wallet action: collateral for quote-level disputes must be settled manually.";
        }
        WalletLock lock = walletLockRepository.findByReferenceIdAndTypeAndStatus(
                dispute.getSubjectId(), LockType.shipment_budget, LockStatus.active).orElse(null);
        if (lock == null) {
            return "No active wallet lock found for this shipment; resolved without an automatic payout.";
        }
        Map<String, java.math.BigDecimal> result = walletService.applyCancellationPenalty(lock, dispute.getFiledByUserId());
        return String.format("Wallet lock #%d settled: %s compensated to filer, %s refunded, %s fee.",
                lock.getId(), result.get("penaltyAmount"), result.get("refundAmount"), result.get("feeAmount"));
    }

    private void notifyResolution(Dispute dispute, String outcome) {
        notificationService.create(dispute.getFiledByUserId(), Notification.NotificationType.dispute_resolved,
                "Your dispute #" + dispute.getId() + " was " + outcome,
                "The respondent " + outcome + " your dispute regarding " + dispute.getSubjectType() + " #" + dispute.getSubjectId() + ".",
                dispute.getSubjectType() == SubjectType.shipment ? dispute.getSubjectId() : null);
    }

    private void requireResolvable(Dispute dispute) {
        if (dispute.getStatus() != DisputeStatus.open && dispute.getStatus() != DisputeStatus.under_review)
            throw new RuntimeException("Dispute is already resolved");
    }

    private void requireParty(Dispute dispute, Long userId) {
        if (!dispute.getFiledByUserId().equals(userId) && !dispute.getRespondentUserId().equals(userId))
            throw new RuntimeException("Unauthorized");
    }

    private Dispute getDisputeOrThrow(Long disputeId) {
        return disputeRepository.findById(disputeId).orElseThrow(() -> new RuntimeException("Dispute not found"));
    }

    private String joinNotes(String resolutionNotes, String walletOutcome) {
        if (resolutionNotes == null || resolutionNotes.isBlank()) return walletOutcome;
        return resolutionNotes + "\n\n" + walletOutcome;
    }

    private Long resolveRespondent(SubjectType subjectType, Long subjectId, Long userId) {
        if (subjectType == SubjectType.shipment) {
            Shipment shipment = shipmentRepository.findById(subjectId)
                    .orElseThrow(() -> new RuntimeException("Shipment not found"));
            Quote acceptedQuote = quoteRepository.findByShipmentId(subjectId).stream()
                    .filter(q -> q.getStatus() == Quote.QuoteStatus.accepted).findFirst()
                    .orElseThrow(() -> new RuntimeException("This shipment has no accepted traveler to dispute with yet"));

            if (userId.equals(shipment.getShipperId())) return acceptedQuote.getTravelerId();
            if (userId.equals(acceptedQuote.getTravelerId())) return shipment.getShipperId();
            throw new RuntimeException("Only the shipper or the accepted traveler can file a dispute on this shipment");
        } else {
            Quote quote = quoteRepository.findById(subjectId)
                    .orElseThrow(() -> new RuntimeException("Quote not found"));
            Shipment shipment = shipmentRepository.findById(quote.getShipmentId())
                    .orElseThrow(() -> new RuntimeException("Shipment not found"));

            if (userId.equals(quote.getTravelerId())) return shipment.getShipperId();
            if (userId.equals(shipment.getShipperId())) return quote.getTravelerId();
            throw new RuntimeException("Only the traveler or the shipper can file a dispute on this quote");
        }
    }

    private Map<String, Object> disputeToMap(Dispute d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", d.getId());
        map.put("subject_type", d.getSubjectType());
        map.put("subject_id", d.getSubjectId());
        map.put("filed_by_user_id", d.getFiledByUserId());
        map.put("respondent_user_id", d.getRespondentUserId());
        map.put("reason_category", d.getReasonCategory());
        map.put("description", d.getDescription());
        map.put("status", d.getStatus());
        map.put("resolution_notes", d.getResolutionNotes());
        map.put("resolved_by_user_id", d.getResolvedByUserId());
        map.put("resolved_at", d.getResolvedAt());
        map.put("created_at", d.getCreatedAt());
        map.put("updated_at", d.getUpdatedAt());
        map.put("evidence", disputeEvidenceRepository.findByDisputeIdOrderByCreatedAtAsc(d.getId()));
        return map;
    }
}
