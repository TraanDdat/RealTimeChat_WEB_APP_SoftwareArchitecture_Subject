package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;

import java.util.Base64;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
public class UserController {

    private final StoredMessageRepository messageRepository;

    public UserController(StoredMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return List.of(
                new User(1L, "userA", "User A"),
                new User(2L, "userB", "User B")
        );
    }

    @GetMapping("/messages")
    public List<StoredMessage> getMessages(HttpSession session) {
        String currentUser = requireAuthenticatedUser(session);
        return messageRepository.findConversation(currentUser, otherUser(currentUser));
    }

    @PostMapping("/messages")
    public StoredMessage sendMessage(@RequestBody ChatMessage message, HttpSession session) {
        String currentUser = requireAuthenticatedUser(session);
        String recipient = otherUser(currentUser);
        if (!recipient.equals(message.recipient())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người nhận không hợp lệ");
        }
        String text = message.message() == null ? "" : message.message().trim();
        String attachmentData = message.attachmentData();
        if (text.isEmpty() && (attachmentData == null || attachmentData.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tin nhắn hoặc tệp đính kèm là bắt buộc");
        }
        if (attachmentData != null && attachmentData.length() > 7_000_000) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Tệp đính kèm tối đa 5 MB");
        }
        if (attachmentData != null) {
            if (message.attachmentName() == null || message.attachmentName().isBlank()
                    || message.attachmentName().length() > 255
                    || message.attachmentType() == null || message.attachmentType().isBlank()
                    || message.attachmentType().length() > 128) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông tin tệp đính kèm không hợp lệ");
            }
            try {
                if (Base64.getDecoder().decode(attachmentData).length > 5 * 1024 * 1024) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Tệp đính kèm tối đa 5 MB");
                }
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu tệp đính kèm không hợp lệ");
            }
        } else if (message.attachmentName() != null || message.attachmentType() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông tin tệp đính kèm không hợp lệ");
        }

        return messageRepository.save(new StoredMessage(
                currentUser,
                recipient,
                text,
                message.attachmentName(),
                message.attachmentData() == null ? null : safeAttachmentType(message.attachmentType()),
                attachmentData
        ));
    }

    @GetMapping("/messages/unread-count")
    public Map<String, Long> getUnreadCount(HttpSession session) {
        String currentUser = requireAuthenticatedUser(session);
        return Map.of("count", messageRepository.countByRecipientAndReadAtIsNull(currentUser));
    }

    @PostMapping("/messages/read")
    public Map<String, Integer> markMessagesRead(HttpSession session) {
        String currentUser = requireAuthenticatedUser(session);
        int updated = messageRepository.markIncomingMessagesRead(currentUser, LocalDateTime.now());
        return Map.of("updated", updated);
    }

    @GetMapping("/messages/{id}/attachment")
    public ResponseEntity<byte[]> getAttachment(@PathVariable Long id, HttpSession session) {
        String currentUser = requireAuthenticatedUser(session);
        StoredMessage message = messageRepository.findById(id)
                .filter(item -> currentUser.equals(item.getSender()) || currentUser.equals(item.getRecipient()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp đính kèm"));
        if (message.getAttachmentData() == null || message.getAttachmentName() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tin nhắn không có tệp đính kèm");
        }

        byte[] content;
        try {
            content = Base64.getDecoder().decode(message.getAttachmentData());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dữ liệu tệp đính kèm bị lỗi");
        }

        String type = safeAttachmentType(message.getAttachmentType());
        ContentDisposition disposition = type.startsWith("image/") || type.startsWith("audio/")
                ? ContentDisposition.inline().filename(message.getAttachmentName()).build()
                : ContentDisposition.attachment().filename(message.getAttachmentName()).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(type))
                .body(content);
    }

    private String requireAuthenticatedUser(HttpSession session) {
        String username = (String) session.getAttribute(AuthController.SESSION_USER);
        if (!isChatUser(username)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        return username;
    }

    private String otherUser(String username) {
        return "userA".equals(username) ? "userB" : "userA";
    }

    private boolean isChatUser(String user) {
        return "userA".equals(user) || "userB".equals(user);
    }

    private String safeAttachmentType(String type) {
        if (type == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String normalizedType = type.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalizedType)) {
            normalizedType = "image/jpeg";
        }
        if (List.of("image/png", "image/jpeg", "image/gif", "image/webp",
                "audio/webm", "audio/ogg", "audio/wav", "audio/mpeg", "audio/mp4").contains(normalizedType)) {
            return normalizedType;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}