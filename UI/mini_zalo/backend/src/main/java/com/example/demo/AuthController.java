package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {

    public static final String SESSION_USER = "authenticatedUser";

    private final ChatUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(ChatUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (request.username() == null || request.password() == null) {
            throw invalidCredentials();
        }

        ChatUser user = userRepository.findById(request.username().trim())
                .filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
                .orElseThrow(AuthController::invalidCredentials);

        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute(SESSION_USER, user.getUsername());
        return new LoginResponse(user.getUsername(), user.getDisplayName());
    }

    @GetMapping("/auth/me")
    public LoginResponse currentUser(HttpSession session) {
        String username = (String) session.getAttribute(SESSION_USER);
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }

        ChatUser user = userRepository.findById(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));
        return new LoginResponse(user.getUsername(), user.getDisplayName());
    }

    @PostMapping("/auth/logout")
    public void logout(HttpSession session) {
        session.invalidate();
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tên tài khoản hoặc mật khẩu không đúng");
    }
}
