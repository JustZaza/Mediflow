package com.mediflow.controller;

import com.mediflow.model.User;
import com.mediflow.service.AuthService;
import org.springframework.web.bind.annotation.*;

/**
 * Thin layer meant to be called from a JavaFX view (per section 10, the GUI never talks to
 * the database or writes SQL directly — it only calls into a Controller, which calls a Service).
 */

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        User user = authService.login(
                request.username(),
                request.password()
        );

        return new LoginResponse(
                user.getUserId(),
                user.getUsername(),
                user.getRole().name(),
                user.getFullName()
        );
    }

    public User login(String username, String password) {
        return authService.login(username, password);
    }

    public record LoginRequest(
            String username,
            String password
    ) {}

    public record LoginResponse(
            int userId,
            String username,
            String role,
            String fullName
    ) {}
}
