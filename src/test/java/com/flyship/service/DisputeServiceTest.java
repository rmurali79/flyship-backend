package com.flyship.service;

import com.flyship.entity.*;
import com.flyship.entity.Dispute.DisputeStatus;
import com.flyship.entity.Dispute.SubjectType;
import com.flyship.entity.WalletLock.LockStatus;
import com.flyship.entity.WalletLock.LockType;
import com.flyship.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DisputeServiceTest {

    private DisputeRepository disputeRepository;
    private DisputeEvidenceRepository disputeEvidenceRepository;
    private ShipmentRepository shipmentRepository;
    private QuoteRepository quoteRepository;
    private WalletLockRepository walletLockRepository;
    private WalletService walletService;
    private NotificationService notificationService;
    private DisputeService disputeService;

    private Shipment shipment;
    private Quote acceptedQuote;

    @BeforeEach
    void setUp() {
        disputeRepository = Mockito.mock(DisputeRepository.class);
        disputeEvidenceRepository = Mockito.mock(DisputeEvidenceRepository.class);
        shipmentRepository = Mockito.mock(ShipmentRepository.class);
        quoteRepository = Mockito.mock(QuoteRepository.class);
        walletLockRepository = Mockito.mock(WalletLockRepository.class);
        walletService = Mockito.mock(WalletService.class);
        notificationService = Mockito.mock(NotificationService.class);

        disputeService = new DisputeService();
        ReflectionTestUtils.setField(disputeService, "disputeRepository", disputeRepository);
        ReflectionTestUtils.setField(disputeService, "disputeEvidenceRepository", disputeEvidenceRepository);
        ReflectionTestUtils.setField(disputeService, "shipmentRepository", shipmentRepository);
        ReflectionTestUtils.setField(disputeService, "quoteRepository", quoteRepository);
        ReflectionTestUtils.setField(disputeService, "walletLockRepository", walletLockRepository);
        ReflectionTestUtils.setField(disputeService, "walletService", walletService);
        ReflectionTestUtils.setField(disputeService, "notificationService", notificationService);

        shipment = new Shipment();
        shipment.setId(10L);
        shipment.setShipperId(1L);
        shipment.setOrigin("NYC");
        shipment.setDestination("LON");
        shipment.setStatus(Shipment.ShipmentStatus.delivered);

        acceptedQuote = new Quote();
        acceptedQuote.setId(200L);
        acceptedQuote.setShipmentId(10L);
        acceptedQuote.setTravelerId(3L);
        acceptedQuote.setStatus(Quote.QuoteStatus.accepted);

        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(quoteRepository.findByShipmentId(10L)).thenReturn(List.of(acceptedQuote));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
            Dispute d = inv.getArgument(0);
            if (d.getId() == null) d.setId(500L);
            return d;
        });
        when(disputeEvidenceRepository.findByDisputeIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
    }

    @Test
    void fileDispute_byShipper_setsRespondentToAcceptedTraveler() {
        Map<String, Object> result = disputeService.fileDispute(
                1L, SubjectType.shipment, 10L, DisputeReason.item_damaged, "Box arrived crushed", List.of());

        assertEquals(3L, result.get("respondent_user_id"));
        assertEquals(1L, result.get("filed_by_user_id"));
        assertEquals(DisputeStatus.open, result.get("status"));
        verify(notificationService).create(eq(3L), eq(Notification.NotificationType.dispute_filed), any(), any(), eq(10L));
    }

    @Test
    void fileDispute_byAcceptedTraveler_setsRespondentToShipper() {
        Map<String, Object> result = disputeService.fileDispute(
                3L, SubjectType.shipment, 10L, DisputeReason.communication_issue, null, List.of());

        assertEquals(1L, result.get("respondent_user_id"));
    }

    @Test
    void fileDispute_byUninvolvedUser_throws() {
        assertThrows(RuntimeException.class, () ->
                disputeService.fileDispute(99L, SubjectType.shipment, 10L, DisputeReason.other, null, List.of()));
    }

    @Test
    void fileDispute_savesEachEvidencePhoto() {
        disputeService.fileDispute(1L, SubjectType.shipment, 10L, DisputeReason.item_damaged, "damaged",
                List.of("http://example.com/a.jpg", "http://example.com/b.jpg"));

        verify(disputeEvidenceRepository, times(2)).save(any(DisputeEvidence.class));
    }

    @Test
    void addEvidence_byNonParty_throws() {
        Dispute dispute = openDispute();
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));

        assertThrows(RuntimeException.class, () ->
                disputeService.addEvidence(500L, 99L, List.of("http://example.com/c.jpg")));
    }

    @Test
    void addEvidence_onResolvedDispute_throws() {
        Dispute dispute = openDispute();
        dispute.setStatus(DisputeStatus.accepted);
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));

        assertThrows(RuntimeException.class, () ->
                disputeService.addEvidence(500L, 1L, List.of("http://example.com/c.jpg")));
    }

    @Test
    void acceptDispute_byNonRespondent_throws() {
        Dispute dispute = openDispute();
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));

        assertThrows(RuntimeException.class, () -> disputeService.acceptDispute(500L, 1L, "ok"));
    }

    @Test
    void acceptDispute_withActiveLock_appliesWalletPenaltyAndResolves() {
        Dispute dispute = openDispute();
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));

        WalletLock lock = new WalletLock();
        lock.setId(900L);
        when(walletLockRepository.findByReferenceIdAndTypeAndStatus(10L, LockType.shipment_budget, LockStatus.active))
                .thenReturn(Optional.of(lock));
        when(walletService.applyCancellationPenalty(eq(lock), eq(1L))).thenReturn(Map.of(
                "penaltyAmount", new BigDecimal("25.00"),
                "refundAmount", new BigDecimal("50.00"),
                "feeAmount", new BigDecimal("25.00")));

        Map<String, Object> result = disputeService.acceptDispute(500L, 3L, "Confirmed, my fault");

        assertEquals(DisputeStatus.accepted, result.get("status"));
        verify(walletService).applyCancellationPenalty(lock, 1L);
        assertNotNull(dispute.getResolutionNotes());
        assertTrue(dispute.getResolutionNotes().contains("Confirmed, my fault"));
        verify(notificationService).create(eq(1L), eq(Notification.NotificationType.dispute_resolved), any(), any(), eq(10L));
    }

    @Test
    void acceptDispute_withoutActiveLock_resolvesWithoutWalletAction() {
        Dispute dispute = openDispute();
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));
        when(walletLockRepository.findByReferenceIdAndTypeAndStatus(10L, LockType.shipment_budget, LockStatus.active))
                .thenReturn(Optional.empty());

        Map<String, Object> result = disputeService.acceptDispute(500L, 3L, null);

        assertEquals(DisputeStatus.accepted, result.get("status"));
        verify(walletService, never()).applyCancellationPenalty(any(), any());
    }

    @Test
    void rejectDispute_withoutNotes_throws() {
        Dispute dispute = openDispute();
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));

        assertThrows(RuntimeException.class, () -> disputeService.rejectDispute(500L, 3L, "  "));
    }

    @Test
    void rejectDispute_byRespondentWithNotes_resolves() {
        Dispute dispute = openDispute();
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));

        Map<String, Object> result = disputeService.rejectDispute(500L, 3L, "No evidence of damage");

        assertEquals(DisputeStatus.rejected, result.get("status"));
        verify(walletService, never()).applyCancellationPenalty(any(), any());
    }

    @Test
    void withdrawDispute_byNonFiler_throws() {
        Dispute dispute = openDispute();
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));

        assertThrows(RuntimeException.class, () -> disputeService.withdrawDispute(500L, 3L));
    }

    @Test
    void withdrawDispute_byFiler_resolves() {
        Dispute dispute = openDispute();
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));

        Map<String, Object> result = disputeService.withdrawDispute(500L, 1L);

        assertEquals(DisputeStatus.withdrawn, result.get("status"));
    }

    @Test
    void markUnderReview_byNonRespondent_throws() {
        Dispute dispute = openDispute();
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));

        assertThrows(RuntimeException.class, () -> disputeService.markUnderReview(500L, 1L));
    }

    @Test
    void markUnderReview_byRespondent_succeeds() {
        Dispute dispute = openDispute();
        when(disputeRepository.findById(500L)).thenReturn(Optional.of(dispute));

        Map<String, Object> result = disputeService.markUnderReview(500L, 3L);

        assertEquals(DisputeStatus.under_review, result.get("status"));
    }

    private Dispute openDispute() {
        Dispute dispute = new Dispute();
        dispute.setId(500L);
        dispute.setSubjectType(SubjectType.shipment);
        dispute.setSubjectId(10L);
        dispute.setFiledByUserId(1L);
        dispute.setRespondentUserId(3L);
        dispute.setReasonCategory(DisputeReason.item_damaged);
        dispute.setStatus(DisputeStatus.open);
        return dispute;
    }
}
