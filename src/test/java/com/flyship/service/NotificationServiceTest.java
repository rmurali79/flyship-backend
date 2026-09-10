package com.flyship.service;

import com.flyship.entity.Notification;
import com.flyship.entity.Notification.NotificationType;
import com.flyship.repository.NotificationRepository;
import jakarta.persistence.Column;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = Mockito.mock(NotificationRepository.class);
        notificationService = new NotificationService();
        ReflectionTestUtils.setField(notificationService, "notificationRepository", notificationRepository);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_savesNotificationWithGivenFields() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        Notification result = notificationService.create(5L, NotificationType.new_quote, "Title", "Message", 10L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(5L, saved.getUserId());
        assertEquals(NotificationType.new_quote, saved.getType());
        assertEquals("Title", saved.getTitle());
        assertEquals("Message", saved.getMessage());
        assertEquals(10L, saved.getShipmentId());
        assertEquals(1L, result.getId());
    }

    @Test
    void markAsRead_marksMatchingNotificationRead() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUserId(5L);
        notification.setRead(false);
        when(notificationRepository.findByIdAndUserId(1L, 5L)).thenReturn(Optional.of(notification));

        Notification result = notificationService.markAsRead(1L, 5L);

        assertTrue(result.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_throwsWhenNotFoundForUser() {
        when(notificationRepository.findByIdAndUserId(1L, 5L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(1L, 5L));
    }

    @Test
    void markAllAsRead_marksEveryUnreadNotification() {
        Notification a = new Notification(); a.setId(1L); a.setRead(false);
        Notification b = new Notification(); b.setId(2L); b.setRead(false);
        when(notificationRepository.findByUserIdAndReadFalse(5L)).thenReturn(List.of(a, b));

        notificationService.markAllAsRead(5L);

        assertTrue(a.isRead());
        assertTrue(b.isRead());
        verify(notificationRepository).saveAll(List.of(a, b));
    }

    @Test
    void getUnreadCount_delegatesToRepository() {
        when(notificationRepository.countByUserIdAndReadFalse(5L)).thenReturn(3L);

        assertEquals(3L, notificationService.getUnreadCount(5L));
    }

    @Test
    void readField_mapsToIsReadColumn_notReservedMariaDbWord() throws NoSuchFieldException {
        Field readField = Notification.class.getDeclaredField("read");
        Column column = readField.getAnnotation(Column.class);

        assertEquals("is_read", column.name());
    }
}
