package com.mediflow.model;

/** A login account for any staff member. Role drives which dashboard/actions are available. */
public class User {
    public enum Role { ADMIN, DOCTOR, RECEPTIONIST, PHARMACIST }
    public enum Status { ACTIVE, DISABLED }

    private int userId;
    private String username;
    private String passwordHash;
    private Role role;
    private String fullName;
    private String email;
    private Status status = Status.ACTIVE;

    public User() {}

    public User(String username, String passwordHash, Role role, String fullName, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
