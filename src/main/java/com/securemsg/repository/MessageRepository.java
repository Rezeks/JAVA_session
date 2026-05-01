package com.securemsg.repository;

import com.securemsg.domain.DeliveryStatus;
import com.securemsg.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findBySenderIdOrRecipientId(UUID senderId, UUID recipientId);
    List<Message> findByRecipientIdAndStatus(UUID recipientId, DeliveryStatus status);
}
