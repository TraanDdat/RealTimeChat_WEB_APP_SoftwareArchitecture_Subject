package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface StoredMessageRepository extends JpaRepository<StoredMessage, Long> {
    List<StoredMessage> findAllByOrderByIdAsc();

    @Query("""
            select message from StoredMessage message
            where (message.sender = :userA and message.recipient = :userB)
               or (message.sender = :userB and message.recipient = :userA)
            order by message.id asc
            """)
    List<StoredMessage> findConversation(
            @Param("userA") String userA,
            @Param("userB") String userB
    );

    @Modifying
    @Transactional
    @Query("""
            update StoredMessage message
            set message.sender = 'userA', message.recipient = 'userB'
            where message.sender is null and message.recipient is null
            """)
    int assignLegacyMessagesToUserA();

    long countByRecipientAndReadAtIsNull(String recipient);

    @Modifying
    @Transactional
    @Query("""
            update StoredMessage message
            set message.readAt = :readAt
            where message.recipient = :recipient
              and message.readAt is null
            """)
    int markIncomingMessagesRead(
            @Param("recipient") String recipient,
            @Param("readAt") LocalDateTime readAt
    );
}
