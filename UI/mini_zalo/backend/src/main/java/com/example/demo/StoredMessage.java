package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class StoredMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;

    private String recipient;

    private String message;

    private String attachmentName;

    private String attachmentType;

    @Lob
    private String attachmentData;

    private LocalDateTime readAt;

    private LocalDateTime sentAt;

    protected StoredMessage() {
    }

    public StoredMessage(String sender, String recipient, String message) {
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.sentAt = LocalDateTime.now();
    }

    public StoredMessage(
            String sender,
            String recipient,
            String message,
            String attachmentName,
            String attachmentType,
            String attachmentData
    ) {
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.attachmentName = attachmentName;
        this.attachmentType = attachmentType;
        this.attachmentData = attachmentData;
        this.sentAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    @JsonIgnore
    public String getAttachmentData() {
        return attachmentData;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void markRead(LocalDateTime readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }
}
