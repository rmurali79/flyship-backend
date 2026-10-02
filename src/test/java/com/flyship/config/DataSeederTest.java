package com.flyship.config;

import com.flyship.entity.User;
import com.flyship.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSeederTest {

    @Test
    void renamesDemoUsersStillOnTheirDefaultName_only() {
        Map<String, User> users = new HashMap<>();
        users.put("shipper1@flyship.test", user("Shipper 1"));
        users.put("traveler3@flyship.test", user("Traveler 3"));
        users.put("shipper2@flyship.test", user("Someone Renamed"));

        UserRepository repo = Mockito.mock(UserRepository.class);
        when(repo.findByEmail(anyString())).thenAnswer(inv -> Optional.ofNullable(users.get(inv.<String>getArgument(0))));
        DataSeeder seeder = new DataSeeder();
        ReflectionTestUtils.setField(seeder, "userRepository", repo);

        seeder.renameDefaultDemoUsers();

        assertEquals("Shiloh Vance", users.get("shipper1@flyship.test").getName());
        assertEquals("Trenton Vance", users.get("traveler3@flyship.test").getName());
        assertEquals("Someone Renamed", users.get("shipper2@flyship.test").getName());
        verify(repo, Mockito.times(2)).save(any(User.class));
    }

    @Test
    void doesNothingOnceRenamed() {
        UserRepository repo = Mockito.mock(UserRepository.class);
        when(repo.findByEmail(anyString())).thenReturn(Optional.of(user("Shiloh Vance")));
        DataSeeder seeder = new DataSeeder();
        ReflectionTestUtils.setField(seeder, "userRepository", repo);

        seeder.renameDefaultDemoUsers();

        verify(repo, never()).save(any(User.class));
    }

    private static User user(String name) {
        User u = new User();
        u.setName(name);
        return u;
    }
}
