package com.flyship.repository;

import com.flyship.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByShipmentIdOrderByCreatedAtAsc(Long shipmentId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.shipmentId = ?1 AND m.receiverId = ?2 AND m.isRead = false")
    long countUnread(Long shipmentId, Long receiverId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.shipmentId = ?1 AND m.receiverId = ?2 AND m.isRead = false")
    void markAsRead(Long shipmentId, Long receiverId);

    @Query("SELECT m FROM Message m WHERE (m.senderId = ?1 OR m.receiverId = ?1) AND m.id IN " +
           "(SELECT MAX(m2.id) FROM Message m2 WHERE m2.senderId = ?1 OR m2.receiverId = ?1 GROUP BY m2.shipmentId) ORDER BY m.createdAt DESC")
    List<Message> findLatestPerShipment(Long userId);
}
