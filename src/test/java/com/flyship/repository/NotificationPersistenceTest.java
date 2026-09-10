package com.flyship.repository;

import com.flyship.entity.Notification;
import com.flyship.entity.Notification.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the notifications.type native-ENUM-vs-Java-enum drift (FLY-26):
 * exercises the real JPA/Hibernate mapping (not a mocked repository) to confirm every
 * current NotificationType constant can actually be persisted and read back.
 */
@DataJpaTest
class NotificationPersistenceTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void save_persistsNotificationForEveryNotificationType() {
        for (NotificationType type : NotificationType.values()) {
            Notification notification = new Notification();
            notification.setUserId(1L);
            notification.setType(type);
            notification.setTitle("Title for " + type);
            notification.setMessage("Message for " + type);

            Notification saved = notificationRepository.save(notification);

            Optional<Notification> found = notificationRepository.findById(saved.getId());
            assertTrue(found.isPresent(), "Notification of type " + type + " should be persisted");
            assertEquals(type, found.get().getType());
        }
    }

    @Test
    void save_persistsDisputeFiledNotification() {
        Notification notification = new Notification();
        notification.setUserId(1L);
        notification.setType(NotificationType.dispute_filed);
        notification.setTitle("Dispute filed");
        notification.setMessage("A dispute was filed against your shipment.");
        notification.setShipmentId(10L);

        Notification saved = notificationRepository.save(notification);

        Notification found = notificationRepository.findById(saved.getId()).orElseThrow();
        assertEquals(NotificationType.dispute_filed, found.getType());
    }
}
