package com.flyship.controller;

import com.flyship.entity.Payment;
import com.flyship.entity.Quote;
import com.flyship.entity.Shipment;
import com.flyship.entity.User;
import com.flyship.repository.PaymentRepository;
import com.flyship.repository.QuoteRepository;
import com.flyship.repository.ReviewRepository;
import com.flyship.repository.ShipmentRepository;
import com.flyship.repository.UserRepository;
import com.flyship.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private UserRepository userRepository;
    private UserController userController;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        userController = new UserController();
        ReflectionTestUtils.setField(userController, "userRepository", userRepository);

        user = new User();
        user.setId(1L);
        user.setName("Jane Traveler");
        user.setEmail("jane@example.com");
        user.setRole(User.UserRole.traveler);
        user.setCountryCode("+1");
        user.setMobileNumber("5550100");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updateProfile_updatesNameContactAndRole() {
        Map<String, String> body = new HashMap<>();
        body.put("name", "Jane Both");
        body.put("country_code", "+44");
        body.put("mobile_number", "5550199");
        body.put("role", "both");

        ResponseEntity<?> response = userController.updateProfile(body, new AuthenticatedUser(1L, "traveler"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(User.UserRole.both, user.getRole());
        assertEquals("Jane Both", user.getName());
        assertEquals("+44", user.getCountryCode());
        assertEquals("5550199", user.getMobileNumber());

        @SuppressWarnings("unchecked")
        Map<String, Object> resultBody = (Map<String, Object>) response.getBody();
        assertEquals("both", resultBody.get("role"));
    }

    @Test
    void updateProfile_leavesRoleUnchangedWhenOmitted() {
        Map<String, String> body = new HashMap<>();
        body.put("name", "Jane Traveler Updated");

        userController.updateProfile(body, new AuthenticatedUser(1L, "traveler"));

        assertEquals(User.UserRole.traveler, user.getRole());
    }

    @Test
    void updateProfile_rejectsInvalidRole() {
        Map<String, String> body = new HashMap<>();
        body.put("role", "astronaut");

        ResponseEntity<?> response = userController.updateProfile(body, new AuthenticatedUser(1L, "traveler"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(User.UserRole.traveler, user.getRole());
    }

    @Test
    @SuppressWarnings("unchecked")
    void stats_forBothRole_includesShipperAndTravelerFigures() {
        PaymentRepository payments = Mockito.mock(PaymentRepository.class);
        ShipmentRepository shipments = Mockito.mock(ShipmentRepository.class);
        QuoteRepository quotes = Mockito.mock(QuoteRepository.class);
        ReviewRepository reviews = Mockito.mock(ReviewRepository.class);
        ReflectionTestUtils.setField(userController, "paymentRepository", payments);
        ReflectionTestUtils.setField(userController, "shipmentRepository", shipments);
        ReflectionTestUtils.setField(userController, "quoteRepository", quotes);
        ReflectionTestUtils.setField(userController, "reviewRepository", reviews);

        // Payment 1: for user 1's own shipment (a spend). Payment 2: for a quote user 1 carried (an earning).
        Shipment own = new Shipment(); own.setId(10L); own.setShipperId(1L);
        Shipment carried = new Shipment(); carried.setId(20L); carried.setShipperId(2L);
        carried.setStatus(Shipment.ShipmentStatus.delivered);
        Quote quote = new Quote(); quote.setId(5L); quote.setTravelerId(1L); quote.setShipmentId(20L);
        Payment spend = new Payment(); spend.setShipmentId(10L); spend.setAmount(new BigDecimal("40"));
        Payment earning = new Payment(); earning.setShipmentId(20L); earning.setQuoteId(5L); earning.setAmount(new BigDecimal("75"));

        when(payments.findByStatus("completed")).thenReturn(List.of(spend, earning));
        when(shipments.findById(10L)).thenReturn(Optional.of(own));
        when(shipments.findById(20L)).thenReturn(Optional.of(carried));
        when(shipments.countByShipperIdAndStatus(1L, Shipment.ShipmentStatus.delivered)).thenReturn(3L);
        when(quotes.findById(5L)).thenReturn(Optional.of(quote));
        when(quotes.findByTravelerIdAndStatus(1L, Quote.QuoteStatus.accepted)).thenReturn(List.of(quote));
        when(reviews.getRatingSummary(1L)).thenReturn(List.<Object[]>of(new Object[] { 4.5, 2L }));

        ResponseEntity<?> response = userController.getStats(new AuthenticatedUser(1L, "both"));

        Map<String, Object> stats = (Map<String, Object>) response.getBody();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("40"), stats.get("totalSpends"));
        assertEquals(new BigDecimal("75"), stats.get("totalEarnings"));
        assertEquals(3L, stats.get("itemsShipped"));
        assertEquals(1L, stats.get("tripsDone"));
        assertEquals(4.5, stats.get("averageRating"));
        assertEquals(2L, stats.get("reviewCount"));
    }
}
