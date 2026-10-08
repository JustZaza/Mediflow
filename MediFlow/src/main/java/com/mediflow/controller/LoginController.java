package com.mediflow.controller;

import com.mediflow.model.User;
import com.mediflow.service.AuthService;

/**
 * Thin layer meant to be called from a JavaFX view (per section 10, the GUI never talks to
 * the database or writes SQL directly — it only calls into a Controller, which calls a Service).
 */
public class LoginController {
    private final AuthService authService;

    public LoginController(AuthService authService) { this.authService = authService; }

    public User login(String username, String password) {
        return authService.login(username, password);
    }
}
