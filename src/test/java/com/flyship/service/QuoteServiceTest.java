package com.flyship.service;

import com.flyship.entity.Notification;
import com.flyship.entity.Quote;
import com.flyship.entity.Shipment;
import com.flyship.entity.User;
import com.flyship.entity.WalletLock;
import com.flyship.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QuoteServiceTest {

    private QuoteRepository quoteRepository;
    private ShipmentRepository shipmentRepository;
    private UserRepository userRepository;
    private WalletLockRepository walletLockRepository;
    private WalletService walletService;
    private ReviewRepository reviewRepository;
    private EmailService emailService;
    private NotificationService notificationService;
    private QuoteService quoteService;

    private Shipment shipment;
    private User shipper;

    @BeforeEach
    void setUp() {
        quoteRepository = Mockito.mock(QuoteRepository.class);
        shipmentRepository = Mockito.mock(ShipmentRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        walletLockRepository = Mockito.mock(WalletLockRepository.class);
        walletService = Mockito.mock(WalletService.class);
        reviewRepository = Mockito.mock(ReviewRepository.class);
        emailService = Mockito.mock(EmailService.class);
        notificationService = Mockito.mock(NotificationService.class);

        quoteService = new QuoteService();
        ReflectionTestUtils.setField(quoteService, "quoteRepository", quoteRepository);
        ReflectionTestUtils.setField(quoteService, "shipmentRepository", shipmentRepository);
        ReflectionTestUtils.setField(quoteService, "userRepository", userRepository);
        ReflectionTestUtils.setField(quoteService, "walletLockRepository", walletLockRepository);
        ReflectionTestUtils.setField(quoteService, "walletService", walletService);
        ReflectionTestUtils.setField(quoteService, "reviewRepository", reviewRepository);
        ReflectionTestUtils.setField(quoteService, "emailService", emailService);
        ReflectionTestUtils.setField(quoteService, "notificationService", notificationService);

        shipment = new Shipment();
        shipment.setId(10L);
        shipment.setShipperId(1L);
        shipment.setOrigin("NYC");
        shipment.setDestination("LON");
        shipment.setStatus(Shipment.ShipmentStatus.pending);

        shipper = new User();
        shipper.setId(1L);
        shipper.setName("Sam Shipper");
        shipper.setEmail("shipper@example.com");

        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(shipper));
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> {
            Quote q = invocation.getArgument(0);
            if (q.getId() == null) q.setId(100L);
            return q;
        });
    }

    @Test
    void createQuote_notifiesShipperOfNewQuote() {
        quoteService.createQuote(10L, new BigDecimal("50.00"), LocalDate.now().plusDays(5), "USD", "hi", 2L);

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(emailService).sendNewQuoteNotification(eq("shipper@example.com"), eq(10L), eq("NYC"), eq("LON"),
                amountCaptor.capture(), eq("USD"));
        assertEquals(0, new BigDecimal("50.00").compareTo(amountCaptor.getValue()));
        verify(notificationService).create(eq(1L), eq(Notification.NotificationType.new_quote), any(), any(), eq(10L));
    }

    @Test
    void createQuote_doesNotNotifyWhenShipperMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        quoteService.createQuote(10L, new BigDecimal("50.00"), LocalDate.now().plusDays(5), "USD", "hi", 2L);

        verify(emailService, never()).sendNewQuoteNotification(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void acceptQuote_notifiesTravelerThatQuoteWasAccepted() {
        Quote quote = new Quote();
        quote.setId(200L);
        quote.setShipmentId(10L);
        quote.setTravelerId(3L);
        quote.setAmount(new BigDecimal("75.00"));
        quote.setCurrency("USD");
        quote.setStatus(Quote.QuoteStatus.pending);

        User traveler = new User();
        traveler.setId(3L);
        traveler.setName("Tom Traveler");
        traveler.setEmail("traveler@example.com");

        shipment.setStatus(Shipment.ShipmentStatus.pending);

        when(quoteRepository.findById(200L)).thenReturn(Optional.of(quote));
        when(userRepository.findById(3L)).thenReturn(Optional.of(traveler));
        when(walletLockRepository.findByReferenceIdAndTypeAndStatus(any(), any(), any())).thenReturn(Optional.empty());
        when(quoteRepository.findByShipmentIdAndIdNotAndStatus(any(), any(), any())).thenReturn(List.of());

        quoteService.acceptQuote(200L, 1L);

        verify(emailService).sendQuoteAcceptedNotification(eq("traveler@example.com"), eq(10L), eq("NYC"), eq("LON"),
                eq(new BigDecimal("75.00")), eq("USD"));
        verify(emailService, never()).sendNewQuoteNotification(any(), any(), any(), any(), any(), any());
        verify(notificationService).create(eq(3L), eq(Notification.NotificationType.quote_accepted), any(), any(), eq(10L));
    }
}
