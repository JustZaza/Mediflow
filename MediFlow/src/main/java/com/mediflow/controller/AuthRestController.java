//package com.mediflow.controller;
//
//import com.mediflow.model.User;
//import com.mediflow.service.AuthService;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/auth")
//public class AuthRestController {
//
//    private final AuthService authService;
//
//    public AuthRestController(AuthService authService) {
//        this.authService = authService;
//    }
//
//    @PostMapping("/login")
//    public LoginResponse login(@RequestBody LoginRequest request) {
//        User user = authService.login(
//                request.username(),
//                request.password()
//        );
//
//        return new LoginResponse(
//                user.getUserId(),
//                user.getUsername(),
//                user.getRole().name(),
//                user.getFullName()
//        );
//    }
//
//    public record LoginRequest(
//            String username,
//            String password
//    ) {
//    }
//
//    public record LoginResponse(
//            int userId,
//            String username,
//            String role,
//            String fullName
//    ) {
//    }
//}
