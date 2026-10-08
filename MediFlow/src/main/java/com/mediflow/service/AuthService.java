package com.mediflow.service;

import com.mediflow.dao.UserDAO;
import com.mediflow.exception.AuthenticationException;
import com.mediflow.model.User;
import com.mediflow.util.PasswordUtil;
import com.mediflow.util.ValidationUtil;

import java.util.Optional;

/** FR-01: login, role identification, secure password handling. */
public class AuthService {
    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) { this.userDAO = userDAO; }

    public User login(String username, String plainPassword) {
        ValidationUtil.requireNonBlank(username, "Username");
        ValidationUtil.requireNonBlank(plainPassword, "Password");

        Optional<User> found = userDAO.findByUsername(username);
        if (found.isEmpty()) {
            throw new AuthenticationException("Invalid username or password");
        }
        User user = found.get();
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new AuthenticationException("This account has been disabled");
        }
        if (!PasswordUtil.verify(plainPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }
        return user;
    }

    public User register(String username, String plainPassword, User.Role role, String fullName, String email) {
        ValidationUtil.requireNonBlank(username, "Username");
        ValidationUtil.requireNonBlank(plainPassword, "Password");
        if (userDAO.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        User user = new User(username, PasswordUtil.hash(plainPassword), role, fullName, email);
        return userDAO.insert(user);
    }
}
