package com.flyship.service;

import com.flyship.entity.DisputeReason;
import com.flyship.entity.Notification;
import com.flyship.entity.Quote;
import com.flyship.entity.Shipment;
import com.flyship.entity.ShipmentHistory;
import com.flyship.entity.User;
import com.flyship.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ShipmentServiceTest {

    private ShipmentRepository shipmentRepository;
    private UserRepository userRepository;
    private QuoteRepository quoteRepository;
    private ShipmentHistoryRepository shipmentHistoryRepository;
    private WalletLockRepository walletLockRepository;
    private TravelPlanRepository travelPlanRepository;
    private WalletService walletService;
    private EmailService emailService;
    private NotificationService notificationService;
    private ShipmentService shipmentService;

    private Shipment shipment;
    private Quote acceptedQuote;

    @BeforeEach
    void setUp() {
        shipmentRepository = Mockito.mock(ShipmentRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        quoteRepository = Mockito.mock(QuoteRepository.class);
        shipmentHistoryRepository = Mockito.mock(ShipmentHistoryRepository.class);
        walletLockRepository = Mockito.mock(WalletLockRepository.class);
        travelPlanRepository = Mockito.mock(TravelPlanRepository.class);
        walletService = Mockito.mock(WalletService.class);
        emailService = Mockito.mock(EmailService.class);
        notificationService = Mockito.mock(NotificationService.class);

        shipmentService = new ShipmentService();
        ReflectionTestUtils.setField(shipmentService, "shipmentRepository", shipmentRepository);
        ReflectionTestUtils.setField(shipmentService, "userRepository", userRepository);
        ReflectionTestUtils.setField(shipmentService, "quoteRepository", quoteRepository);
        ReflectionTestUtils.setField(shipmentService, "shipmentHistoryRepository", shipmentHistoryRepository);
        ReflectionTestUtils.setField(shipmentService, "walletLockRepository", walletLockRepository);
        ReflectionTestUtils.setField(shipmentService, "travelPlanRepository", travelPlanRepository);
        ReflectionTestUtils.setField(shipmentService, "walletService", walletService);
        ReflectionTestUtils.setField(shipmentService, "emailService", emailService);
        ReflectionTestUtils.setField(shipmentService, "notificationService", notificationService);

        shipment = new Shipment();
        shipment.setId(10L);
        shipment.setShipperId(1L);
        shipment.setOrigin("NYC");
        shipment.setDestination("LON");
        shipment.setStatus(Shipment.ShipmentStatus.accepted);

        acceptedQuote = new Quote();
        acceptedQuote.setId(200L);
        acceptedQuote.setShipmentId(10L);
        acceptedQuote.setTravelerId(3L);
        acceptedQuote.setStatus(Quote.QuoteStatus.accepted);

        User shipper = new User();
        shipper.setId(1L); shipper.setName("Sam Shipper"); shipper.setEmail("shipper@example.com");
        User traveler = new User();
        traveler.setId(3L); traveler.setName("Tom Traveler"); traveler.setEmail("traveler@example.com");

        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(shipper));
        when(userRepository.findById(3L)).thenReturn(Optional.of(traveler));
        when(quoteRepository.findByShipmentId(10L)).thenReturn(List.of(acceptedQuote));
        when(shipmentHistoryRepository.save(any(ShipmentHistory.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void updateStatus_toInTransit_notifiesShipperAndTraveler() {
        shipmentService.updateStatus(10L, "in_transit", "left origin", "NYC", 3L);

        verify(emailService).sendShipmentStatusChangeNotification(
                eq("shipper@example.com"), eq(10L), eq("NYC"), eq("LON"), eq("in_transit"));
        verify(emailService).sendShipmentStatusChangeNotification(
                eq("traveler@example.com"), eq(10L), eq("NYC"), eq("LON"), eq("in_transit"));
        verify(notificationService).create(eq(1L), eq(Notification.NotificationType.shipment_status_change), any(), any(), eq(10L));
        verify(notificationService).create(eq(3L), eq(Notification.NotificationType.shipment_status_change), any(), any(), eq(10L));
    }

    @Test
    void updateStatus_toDelivered_notifiesShipperAndTraveler() {
        shipment.setStatus(Shipment.ShipmentStatus.in_transit);

        shipmentService.updateStatus(10L, "delivered", "arrived", "LON", 3L);

        verify(emailService).sendShipmentStatusChangeNotification(
                eq("shipper@example.com"), eq(10L), eq("NYC"), eq("LON"), eq("delivered"));
        verify(emailService).sendShipmentStatusChangeNotification(
                eq("traveler@example.com"), eq(10L), eq("NYC"), eq("LON"), eq("delivered"));
    }

    @Test
    void updateStatus_forOtherStatuses_doesNotNotify() {
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        shipment.setStatus(Shipment.ShipmentStatus.pending);

        shipmentService.updateStatus(10L, "accepted", null, null, 1L);

        verify(emailService, never()).sendShipmentStatusChangeNotification(any(), any(), any(), any(), any());
        verify(notificationService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void deleteShipment_pending_storesCategorizedReasonAndOptionalDetail() {
        shipment.setStatus(Shipment.ShipmentStatus.pending);
        when(quoteRepository.findByShipmentIdAndStatusNot(eq(10L), any())).thenReturn(List.of());

        shipmentService.deleteShipment(10L, 1L, DisputeReason.item_lost, "Package went missing at the depot");

        assertEquals(DisputeReason.item_lost, shipment.getCancellationReasonCategory());
        assertEquals("Package went missing at the depot", shipment.getCancellationReason());
        assertEquals(Shipment.ShipmentStatus.deleted, shipment.getStatus());
    }

    @Test
    void deleteShipment_withoutOptionalDetail_storesCategoryOnly() {
        shipment.setStatus(Shipment.ShipmentStatus.pending);
        when(quoteRepository.findByShipmentIdAndStatusNot(eq(10L), any())).thenReturn(List.of());

        shipmentService.deleteShipment(10L, 1L, DisputeReason.schedule_change, null);

        assertEquals(DisputeReason.schedule_change, shipment.getCancellationReasonCategory());
        assertEquals(null, shipment.getCancellationReason());
    }
}
