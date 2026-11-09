package com.mediflow.controller;

import com.mediflow.dao.DoctorDAO;
import com.mediflow.model.Doctor;
import com.mediflow.model.User;
import com.mediflow.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthService authService;
    private final DoctorDAO doctorDAO;

    public LoginController(AuthService authService, DoctorDAO doctorDAO) {
        this.authService = authService;
        this.doctorDAO = doctorDAO;
    }

    record LoginRequest(String username, String password) {}
    record LoginResponse(int userId, String username, String fullName, String role, Integer doctorId) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req, HttpSession session) {
        User user = authService.login(req.username(), req.password());
        session.setAttribute("user", user);

        Integer doctorId = user.getRole() == User.Role.DOCTOR
                ? doctorDAO.findByUserId(user.getUserId()).map(Doctor::getDoctorId).orElse(null)
                : null;

        return new LoginResponse(
                user.getUserId(),
                user.getUsername(),
                user.getFullName(),      // order matches the record above
                user.getRole().name(),
                doctorId);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }
}