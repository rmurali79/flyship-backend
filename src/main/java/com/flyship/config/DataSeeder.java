package com.flyship.config;

import com.flyship.entity.City;
import com.flyship.entity.Quote;
import com.flyship.entity.Shipment;
import com.flyship.entity.ShipmentHistory;
import com.flyship.entity.TravelPlan;
import com.flyship.entity.User;
import com.flyship.entity.Wallet;
import com.flyship.repository.CityRepository;
import com.flyship.repository.QuoteRepository;
import com.flyship.repository.ShipmentHistoryRepository;
import com.flyship.repository.ShipmentRepository;
import com.flyship.repository.TravelPlanRepository;
import com.flyship.repository.UserRepository;
import com.flyship.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final int DEMO_USER_COUNT = 10;

    private static final String[] CITIES = {
            "New York", "London", "Singapore", "Tokyo", "Paris",
            "Dubai", "Hong Kong", "Los Angeles", "Sydney", "Mumbai"
    };

    // {item description, packaging/condition details}
    private static final String[][] ITEMS = {
            {"Laptop", "Sealed box, fragile electronics."},
            {"Camera Gear", "DSLR body and two lenses in a padded case."},
            {"Documents & Gifts", "Envelope of documents plus a small wrapped gift."},
            {"Electronics", "Two boxed smartphones, unopened."},
            {"Handicrafts", "Hand-carved wooden decor items, wrapped."},
            {"Books", "Box of six hardcover novels."},
            {"Spices & Textiles", "Assorted spices and a folded textile bundle."},
            {"Fashion Items", "Two folded garments in a garment bag."},
            {"Watch", "Luxury wristwatch in its original case."},
            {"Medicines", "Prescription medicines, temperature-stable, sealed."},
            {"Sports Equipment", "Folded badminton racket set with shuttlecocks."},
            {"Toys", "Two boxed children's toys, unopened."},
            {"Artwork", "Small framed painting, bubble-wrapped."},
            {"Jewellery", "Costume jewellery set in a velvet pouch."},
            {"Perfume", "Two sealed perfume bottles, boxed."},
            {"Musical Instrument Parts", "Guitar strings and spare parts kit."},
            {"Laptop Accessories", "Charger, mouse, and sleeve bundle."},
            {"Snacks & Confectionery", "Vacuum-sealed regional snacks."},
            {"Stationery", "Assorted premium pens and a notebook."},
            {"Baby Items", "Two boxed baby-care products, sealed."},
            {"Tablet", "10-inch tablet in a protective case."},
            {"Wedding Invitation Cards", "Bundle of printed invitation cards."},
            {"Headphones", "Noise-cancelling headphones, boxed."},
            {"Shoes", "One pair of leather shoes in original box."},
            {"Auto Parts", "Small car accessory, boxed."},
            {"Currency/Documents", "Sealed envelope, legal documents."},
            {"Home Decor", "Ceramic vase, bubble-wrapped."},
            {"Skincare Products", "Assorted skincare items, sealed."},
            {"Board Game", "Boxed board game, shrink-wrapped."},
            {"Camera Drone", "Compact drone in a hard case."}
    };

    private static final String[] QUOTE_MESSAGES = {
            "I'm flying that route next week, happy to carry this.",
            "Direct flight, can hand-deliver at arrival.",
            "Frequent flyer on this route, no problem carrying it.",
            "I have extra baggage allowance for this trip.",
            "Traveling for work, glad to help out.",
            "This fits easily in my check-in bag.",
            "I can pick it up on my way to the airport.",
            "Non-stop flight, should arrive quickly.",
            "Happy to take extra care with this item.",
            "I do this route often, smooth handover guaranteed."
    };

    private static final String[] CANCELLATION_REASONS = {
            "Shipper found a faster local courier option.",
            "Item was no longer needed at the destination.",
            "Traveler's flight got cancelled, no replacement found in time.",
            "Shipper decided to postpone shipping the item.",
            "Duplicate shipment request, cancelled by mistake."
    };

    private static final String[] WITHDRAWAL_REASONS = {
            "Traveler's travel plans changed.",
            "Traveler's baggage allowance is now insufficient.",
            "Shipper accepted a different traveler's quote."
    };

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private ShipmentHistoryRepository shipmentHistoryRepository;

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Random rnd = new Random(20260728L);
    private int itemCursor = 0;
    private final java.util.Map<Long, List<TravelPlan>> travelPlansByTraveler = new java.util.LinkedHashMap<>();
    private final java.util.Map<Long, List<TravelPlan>> futurePlansByTraveler = new java.util.LinkedHashMap<>();
    private int matchedPlanCursor = 0;

    @Override
    public void run(String... args) {
        if (cityRepository.count() == 0) {
            log.info("Seeding cities...");
            cityRepository.saveAll(List.of(
                    new City("New York", "NYC", "USA", "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=400&h=300&fit=crop"),
                    new City("London", "LHR", "UK", "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=400&h=300&fit=crop"),
                    new City("Singapore", "SIN", "Singapore", "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?w=400&h=300&fit=crop"),
                    new City("Tokyo", "HND", "Japan", "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=400&h=300&fit=crop"),
                    new City("Paris", "CDG", "France", "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=400&h=300&fit=crop"),
                    new City("Dubai", "DXB", "UAE", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=400&h=300&fit=crop"),
                    new City("Hong Kong", "HKG", "Hong Kong", "https://images.unsplash.com/photo-1536599018102-9f803c140fc1?w=400&h=300&fit=crop"),
                    new City("Los Angeles", "LAX", "USA", "https://images.unsplash.com/photo-1534190760961-74e8c1c5c3da?w=400&h=300&fit=crop"),
                    new City("Sydney", "SYD", "Australia", "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?w=400&h=300&fit=crop"),
                    new City("Mumbai", "BOM", "India", "https://images.unsplash.com/photo-1570168007204-dfb528c6958f?w=400&h=300&fit=crop")
            ));
            log.info("Cities seeded.");
        }

        seedTestUsers();

        if (travelPlanRepository.count() == 0) {
            seedTravelPlans();
        }

        if (shipmentRepository.count() == 0) {
            seedDemoShipments();
        }
    }

    private void seedTestUsers() {
        String hashed = passwordEncoder.encode("123456");

        for (int i = 1; i <= DEMO_USER_COUNT; i++) {
            createUserIfMissing("shipper" + i + "@flyship.test", "Shipper " + i, User.UserRole.shipper, hashed);
            createUserIfMissing("traveler" + i + "@flyship.test", "Traveler " + i, User.UserRole.traveler, hashed);
        }
    }

    private void createUserIfMissing(String email, String name, User.UserRole role, String hashedPassword) {
        if (userRepository.existsByEmail(email)) return;

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(hashedPassword);
        user.setRole(role);
        user.setIsActive(true);
        userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setCurrency("USD");
        wallet.setBalance(new BigDecimal("1000.00"));
        walletRepository.save(wallet);

        log.info("Seeded {} user: {}", role, email);
    }

    // ── Itineraries (travel plans) ──────────────────────────────────────────

    private void seedTravelPlans() {
        log.info("Seeding travel plan itineraries...");
        List<Long> travelerIds = userIds("traveler");
        int count = 0;

        for (Long travelerId : travelerIds) {
            // Recent-past trips (trip history) — 1-2 per traveler, ended within the last ~6 weeks.
            int pastPlans = 1 + rnd.nextInt(2);
            for (int p = 0; p < pastPlans; p++) {
                LocalDate start = LocalDate.now().minusDays(10 + rnd.nextInt(35));
                saveTravelPlan(travelerId, start);
                count++;
            }

            // Upcoming trips — 2-3 per traveler, guaranteed to still be in the future so
            // "matched" shipment listings built from these always carry a future deadline.
            int futurePlans = 2 + rnd.nextInt(2);
            for (int p = 0; p < futurePlans; p++) {
                LocalDate start = LocalDate.now().plusDays(3 + rnd.nextInt(56));
                TravelPlan plan = saveTravelPlan(travelerId, start);
                futurePlansByTraveler.computeIfAbsent(travelerId, k -> new ArrayList<>()).add(plan);
                count++;
            }
        }
        log.info("Seeded {} travel plan itineraries.", count);
    }

    private TravelPlan saveTravelPlan(Long travelerId, LocalDate start) {
        String[] pair = pickCityPair();
        LocalDate end = start.plusDays(1 + rnd.nextInt(9));

        TravelPlan plan = new TravelPlan();
        plan.setTravelerId(travelerId);
        plan.setOrigin(pair[0]);
        plan.setDestination(pair[1]);
        plan.setStartDate(start);
        plan.setEndDate(end);
        plan.setAvailableBaggageKg(randomDecimal(3, 20));
        travelPlanRepository.save(plan);
        travelPlansByTraveler.computeIfAbsent(travelerId, k -> new ArrayList<>()).add(plan);
        return plan;
    }

    /**
     * Round-robins across travelers (not raw plans) so every traveler gets a fair share of
     * "matched" listings, then picks a random one of that traveler's *upcoming* itineraries —
     * guaranteeing the shipment's reach-latest-by deadline lands in the future.
     */
    private TravelPlan nextMatchedPlan() {
        List<Long> travelers = new ArrayList<>(futurePlansByTraveler.keySet());
        Long travelerId = travelers.get(matchedPlanCursor % travelers.size());
        matchedPlanCursor++;
        List<TravelPlan> plans = futurePlansByTraveler.get(travelerId);
        return plans.get(rnd.nextInt(plans.size()));
    }

    // ── Shipments, quotations & history across every lifecycle stage ───────

    private void seedDemoShipments() {
        log.info("Seeding demo shipments...");

        List<Long> shipperIds = userIds("shipper");
        List<Long> travelerIds = userIds("traveler");

        int submitted = seedSubmitted(shipperIds, 15);
        int quoted = seedQuoted(shipperIds, travelerIds, 20);
        int accepted = seedAccepted(shipperIds, travelerIds, 20);
        int inTransit = seedInTransit(shipperIds, travelerIds, 20);
        int delivered = seedDelivered(shipperIds, travelerIds, 20);
        int cancelled = seedCancelled(shipperIds, travelerIds, 5);

        int total = submitted + quoted + accepted + inTransit + delivered + cancelled;
        log.info("Demo shipments seeded ({} total: {} submitted, {} quotation received, {} quotation accepted, "
                        + "{} itinerary in progress, {} item delivered, {} cancelled).",
                total, submitted, quoted, accepted, inTransit, delivered, cancelled);
    }

    /**
     * Stage 1: shipment submitted — posted by the shipper, no quotes yet.
     * Aligned to a real itinerary (city pair, dates, baggage capacity) so it shows up in that
     * traveler's "matched" browse view — otherwise pending shipments and travel plans have
     * independently-random routes and almost never line up.
     */
    private int seedSubmitted(List<Long> shipperIds, int count) {
        for (int i = 0; i < count; i++) {
            Long shipper = shipperIds.get(rnd.nextInt(shipperIds.size()));
            newShipment(shipper, Shipment.ShipmentStatus.pending, nextMatchedPlan());
        }
        return count;
    }

    /** Stage 2: quotation received — one or two travelers have quoted, shipper hasn't decided yet. */
    private int seedQuoted(List<Long> shipperIds, List<Long> travelerIds, int count) {
        for (int i = 0; i < count; i++) {
            Long shipper = shipperIds.get(rnd.nextInt(shipperIds.size()));
            Shipment shipment = newShipment(shipper, Shipment.ShipmentStatus.pending, nextMatchedPlan());

            int quotesForShipment = 1 + rnd.nextInt(2); // 1-2 competing quotes
            List<Long> candidates = distinctTravelers(travelerIds, quotesForShipment);
            for (Long traveler : candidates) {
                newQuote(shipment.getId(), traveler, quoteAmount(shipment.getMaxBudget()),
                        Quote.QuoteStatus.pending, randomQuoteMessage(), 3 + rnd.nextInt(10));
            }
        }
        return count;
    }

    /** Stage 3: quotation accepted — shipper accepted a quote, escrow funded, pickup pending. */
    private int seedAccepted(List<Long> shipperIds, List<Long> travelerIds, int count) {
        for (int i = 0; i < count; i++) {
            Long shipper = shipperIds.get(rnd.nextInt(shipperIds.size()));
            Shipment shipment = newShipment(shipper, Shipment.ShipmentStatus.accepted);
            Long acceptedTraveler = travelerIds.get(rnd.nextInt(travelerIds.size()));
            addLosingQuotes(shipment, travelerIds, acceptedTraveler);
            acceptQuote(shipment, acceptedTraveler);

            LocalDateTime now = LocalDateTime.now();
            recordHistory(shipment.getId(), Shipment.ShipmentStatus.pending, "Shipment posted by shipper.",
                    shipment.getOrigin(), now.minusDays(3));
            recordHistory(shipment.getId(), Shipment.ShipmentStatus.accepted, "Quote accepted, escrow funded.",
                    shipment.getOrigin(), now.minusDays(1));
        }
        return count;
    }

    /** Stage 4: itinerary in progress — traveler has picked up the package en route. */
    private int seedInTransit(List<Long> shipperIds, List<Long> travelerIds, int count) {
        for (int i = 0; i < count; i++) {
            Long shipper = shipperIds.get(rnd.nextInt(shipperIds.size()));
            Shipment shipment = newShipment(shipper, Shipment.ShipmentStatus.in_transit);
            Long acceptedTraveler = travelerIds.get(rnd.nextInt(travelerIds.size()));
            addLosingQuotes(shipment, travelerIds, acceptedTraveler);
            acceptQuote(shipment, acceptedTraveler);

            LocalDateTime now = LocalDateTime.now();
            recordHistory(shipment.getId(), Shipment.ShipmentStatus.pending, "Shipment posted by shipper.",
                    shipment.getOrigin(), now.minusDays(5));
            recordHistory(shipment.getId(), Shipment.ShipmentStatus.accepted, "Quote accepted, escrow funded.",
                    shipment.getOrigin(), now.minusDays(4));
            recordHistory(shipment.getId(), Shipment.ShipmentStatus.in_transit, "Traveler picked up the package.",
                    shipment.getOrigin(), now.minusHours(6 + rnd.nextInt(48)));
        }
        return count;
    }

    /** Stage 5: item delivered — full lifecycle with a complete history trail. */
    private int seedDelivered(List<Long> shipperIds, List<Long> travelerIds, int count) {
        for (int i = 0; i < count; i++) {
            Long shipper = shipperIds.get(rnd.nextInt(shipperIds.size()));
            Shipment shipment = newShipment(shipper, Shipment.ShipmentStatus.delivered);
            Long acceptedTraveler = travelerIds.get(rnd.nextInt(travelerIds.size()));
            addLosingQuotes(shipment, travelerIds, acceptedTraveler);
            acceptQuote(shipment, acceptedTraveler);
            seedHistoryTrail(shipment.getId(), shipment.getOrigin(), shipment.getDestination());
        }
        return count;
    }

    /** Stage 6: cancelled — shipper called it off, either before or after a quote existed. */
    private int seedCancelled(List<Long> shipperIds, List<Long> travelerIds, int count) {
        for (int i = 0; i < count; i++) {
            Long shipper = shipperIds.get(rnd.nextInt(shipperIds.size()));
            Shipment shipment = newShipment(shipper, Shipment.ShipmentStatus.cancelled);
            shipment.setCancellationReason(CANCELLATION_REASONS[rnd.nextInt(CANCELLATION_REASONS.length)]);
            shipmentRepository.save(shipment);

            if (rnd.nextBoolean()) {
                Long traveler = travelerIds.get(rnd.nextInt(travelerIds.size()));
                Quote quote = newQuote(shipment.getId(), traveler, quoteAmount(shipment.getMaxBudget()),
                        Quote.QuoteStatus.withdrawn, randomQuoteMessage(), 3 + rnd.nextInt(10));
                quote.setWithdrawalReason(WITHDRAWAL_REASONS[rnd.nextInt(WITHDRAWAL_REASONS.length)]);
                quoteRepository.save(quote);
            }

            recordHistory(shipment.getId(), Shipment.ShipmentStatus.cancelled, shipment.getCancellationReason(),
                    shipment.getOrigin(), LocalDateTime.now().minusHours(rnd.nextInt(72)));
        }
        return count;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private List<Long> userIds(String rolePrefix) {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= DEMO_USER_COUNT; i++) {
            ids.add(userRepository.findByEmail(rolePrefix + i + "@flyship.test").orElseThrow().getId());
        }
        return ids;
    }

    /** Adds 0-2 rejected competing quotes from other travelers, for realism. */
    private void addLosingQuotes(Shipment shipment, List<Long> travelerIds, Long acceptedTraveler) {
        int losers = rnd.nextInt(3); // 0, 1 or 2
        for (int i = 0; i < losers; i++) {
            Long traveler = travelerIds.get(rnd.nextInt(travelerIds.size()));
            if (traveler.equals(acceptedTraveler)) continue;
            newQuote(shipment.getId(), traveler, quoteAmount(shipment.getMaxBudget()),
                    Quote.QuoteStatus.rejected, randomQuoteMessage(), 3 + rnd.nextInt(10));
        }
    }

    private List<Long> distinctTravelers(List<Long> travelerIds, int count) {
        List<Long> pool = new ArrayList<>(travelerIds);
        java.util.Collections.shuffle(pool, rnd);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    /** Random origin/destination and dates — used for stages the "matched" traveler browse view never shows. */
    private Shipment newShipment(Long shipperId, Shipment.ShipmentStatus status) {
        String[] pair = pickCityPair();
        return newShipment(shipperId, status, pair[0], pair[1],
                LocalDate.now().plusDays(5 + rnd.nextInt(30)), randomDecimal(0.5, 10.0));
    }

    /**
     * Origin/destination/reach-by aligned to a traveler's itinerary and weight capped to their declared
     * baggage capacity, so the shipment actually surfaces in that traveler's "matched" browse view
     * (see ShipmentService.getAllShipments — exact origin/destination match, reachLatestBy >= plan.endDate,
     * weight <= plan.availableBaggageKg).
     */
    private Shipment newShipment(Long shipperId, Shipment.ShipmentStatus status, TravelPlan matchedPlan) {
        double maxWeight = matchedPlan.getAvailableBaggageKg() != null
                ? Math.max(1.0, matchedPlan.getAvailableBaggageKg().doubleValue() - 0.5)
                : 10.0;
        BigDecimal weight = randomDecimal(0.5, Math.min(maxWeight, 10.0));
        LocalDate reachLatestBy = matchedPlan.getEndDate().plusDays(2 + rnd.nextInt(10));
        return newShipment(shipperId, status, matchedPlan.getOrigin(), matchedPlan.getDestination(),
                reachLatestBy, weight);
    }

    private Shipment newShipment(Long shipperId, Shipment.ShipmentStatus status, String origin, String destination,
                                  LocalDate reachLatestBy, BigDecimal weight) {
        String[] item = ITEMS[itemCursor % ITEMS.length];
        itemCursor++;

        BigDecimal maxBudget = randomDecimal(50, 300);

        Shipment shipment = new Shipment();
        shipment.setShipperId(shipperId);
        shipment.setOrigin(origin);
        shipment.setDestination(destination);
        shipment.setItemDescription(item[0]);
        shipment.setDetails(item[1]);
        shipment.setWeight(weight);
        shipment.setMaxBudget(maxBudget);
        shipment.setPhotoUrl("https://picsum.photos/seed/flyship-" + itemCursor + "/400/300");

        boolean needsCollection = rnd.nextBoolean();
        shipment.setShipmentArrangement(needsCollection
                ? Shipment.ShipmentArrangement.need_collection
                : Shipment.ShipmentArrangement.self_handover);
        if (needsCollection) {
            shipment.setCollectionPoint("Front desk, " + origin + " Downtown Hotel");
        }

        shipment.setDeliveryArrangement(rnd.nextBoolean()
                ? Shipment.DeliveryArrangement.deliver
                : Shipment.DeliveryArrangement.self_collect);
        shipment.setDeliveryRecipientName("Recipient at " + destination);
        shipment.setDeliveryAddress("123 Demo Street, " + destination);
        shipment.setReachLatestBy(reachLatestBy);
        shipment.setStatus(status);
        return shipmentRepository.save(shipment);
    }

    private Quote newQuote(Long shipmentId, Long travelerId, BigDecimal amount, Quote.QuoteStatus status,
                            String message, int deliveryInDays) {
        Quote quote = new Quote();
        quote.setShipmentId(shipmentId);
        quote.setTravelerId(travelerId);
        quote.setAmount(amount);
        quote.setCurrency("USD");
        quote.setMessage(message);
        quote.setDeliveryDate(LocalDate.now().plusDays(deliveryInDays));
        quote.setStatus(status);
        return quoteRepository.save(quote);
    }

    private void acceptQuote(Shipment shipment, Long travelerId) {
        Quote quote = newQuote(shipment.getId(), travelerId, quoteAmount(shipment.getMaxBudget()),
                Quote.QuoteStatus.accepted, "Accepted — looking forward to it.", 2 + rnd.nextInt(8));
        shipment.setEscrowAmount(quote.getAmount());
        shipment.setEscrowCurrency("USD");
        shipmentRepository.save(shipment);
    }

    private void seedHistoryTrail(Long shipmentId, String origin, String destination) {
        LocalDateTime now = LocalDateTime.now();
        int daysAgo = 4 + rnd.nextInt(10);
        recordHistory(shipmentId, Shipment.ShipmentStatus.pending, "Shipment posted by shipper.",
                origin, now.minusDays(daysAgo));
        recordHistory(shipmentId, Shipment.ShipmentStatus.accepted, "Quote accepted, escrow funded.",
                origin, now.minusDays(daysAgo - 1));
        recordHistory(shipmentId, Shipment.ShipmentStatus.in_transit, "Traveler picked up the package.",
                origin, now.minusDays(daysAgo - 2));
        recordHistory(shipmentId, Shipment.ShipmentStatus.delivered, "Package delivered to recipient.",
                destination, now.minusHours(rnd.nextInt(72)));
    }

    private void recordHistory(Long shipmentId, Shipment.ShipmentStatus status, String description,
                                String location, LocalDateTime timestamp) {
        ShipmentHistory history = new ShipmentHistory();
        history.setShipmentId(shipmentId);
        history.setStatus(status);
        history.setDescription(description);
        history.setLocation(location);
        history.setTimestamp(timestamp);
        shipmentHistoryRepository.save(history);
    }

    private String[] pickCityPair() {
        String origin = CITIES[rnd.nextInt(CITIES.length)];
        String destination;
        do {
            destination = CITIES[rnd.nextInt(CITIES.length)];
        } while (destination.equals(origin));
        return new String[]{origin, destination};
    }

    private String randomQuoteMessage() {
        return QUOTE_MESSAGES[rnd.nextInt(QUOTE_MESSAGES.length)];
    }

    private BigDecimal quoteAmount(BigDecimal maxBudget) {
        double factor = 0.6 + rnd.nextDouble() * 0.35; // 60%-95% of the shipper's budget
        return maxBudget.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal randomDecimal(double min, double max) {
        double value = min + rnd.nextDouble() * (max - min);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
