package com.flyship.config;

import com.flyship.entity.City;
import com.flyship.entity.User;
import com.flyship.entity.Wallet;
import com.flyship.repository.CityRepository;
import com.flyship.repository.UserRepository;
import com.flyship.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    }

    private void seedTestUsers() {
        String hashed = passwordEncoder.encode("123456");

        for (int i = 1; i <= 5; i++) {
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
}
