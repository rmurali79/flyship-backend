package com.flyship.controller;

import com.flyship.entity.Notification;
import com.flyship.security.AuthenticatedUser;
import com.flyship.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    private NotificationService notificationService;
    private NotificationController notificationController;
    private final AuthenticatedUser user = new AuthenticatedUser(5L, "shipper");

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        notificationController = new NotificationController();
        ReflectionTestUtils.setField(notificationController, "notificationService", notificationService);
    }

    @Test
    void getNotifications_returnsNotificationsForCurrentUser() {
        Notification notification = new Notification();
        notification.setId(1L);
        when(notificationService.getForUser(5L)).thenReturn(List.of(notification));

        ResponseEntity<?> response = notificationController.getNotifications(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(notification), response.getBody());
    }

    @Test
    void getUnreadCount_returnsCountForCurrentUser() {
        when(notificationService.getUnreadCount(5L)).thenReturn(2L);

        ResponseEntity<?> response = notificationController.getUnreadCount(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("unread_count", 2L), response.getBody());
    }

    @Test
    void markAsRead_returnsUpdatedNotification() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setRead(true);
        when(notificationService.markAsRead(1L, 5L)).thenReturn(notification);

        ResponseEntity<?> response = notificationController.markAsRead(1L, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(notification, response.getBody());
    }

    @Test
    void markAsRead_returnsNotFoundWhenMissing() {
        when(notificationService.markAsRead(1L, 5L)).thenThrow(new RuntimeException("Notification not found"));

        ResponseEntity<?> response = notificationController.markAsRead(1L, user);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void markAllAsRead_delegatesToServiceForCurrentUser() {
        ResponseEntity<?> response = notificationController.markAllAsRead(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Mockito.verify(notificationService).markAllAsRead(eq(5L));
    }
}
