package com.flyship.controller;

import com.flyship.entity.User;
import com.flyship.repository.UserRepository;
import com.flyship.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
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
}
