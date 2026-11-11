package com.mediflow.controller;

import com.mediflow.exception.AuthenticationException;
import com.mediflow.exception.InvalidPrescriptionException;
import com.mediflow.model.User;
import com.mediflow.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotificationRestController {

    private static final String SESSION_KEY = "user";   // <-- same value as in LoginController / ClinicalRestController

    private final NotificationService notificationService;

    public NotificationRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/medical-records/{id}/email")
    public Map<String, String> emailRecord(@PathVariable int id, HttpSession session) {
        Object u = session.getAttribute(SESSION_KEY);
        if (!(u instanceof User user)) throw new AuthenticationException("Please sign in.");
        if (user.getRole() != User.Role.DOCTOR) {
            throw new InvalidPrescriptionException("Only doctors can email consultation notes");
        }
        return Map.of("sentTo", notificationService.emailConsultation(id, user));
    }
}