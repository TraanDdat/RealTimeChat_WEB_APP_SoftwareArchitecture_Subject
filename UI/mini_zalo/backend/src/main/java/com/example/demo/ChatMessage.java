package com.example.demo;

public record ChatMessage(
        String recipient,
        String message,
        String attachmentName,
        String attachmentType,
        String attachmentData
) {
}
