package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class ChatUserInitializer implements CommandLineRunner {

    private final ChatUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String userAPassword;
    private final String userBPassword;

    public ChatUserInitializer(
            ChatUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.chat.user-a-password}") String userAPassword,
            @Value("${app.chat.user-b-password}") String userBPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAPassword = userAPassword;
        this.userBPassword = userBPassword;
    }

    @Override
    public void run(String... args) {
        saveConfiguredUser("userA", "User A", userAPassword);
        saveConfiguredUser("userB", "User B", userBPassword);
    }

    private void saveConfiguredUser(String username, String displayName, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Password configuration is required for " + username);
        }

        userRepository.save(new ChatUser(
                username,
                displayName,
                passwordEncoder.encode(password)
        ));
    }
}
