package org.example.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String email;
    private String password;
    private String role; // student, supervisor, establishment, admin
    private String name;
    private String phone;
    private String skills;
    private boolean isActive;
    private LocalDateTime createdAt;

    public User() {}

    public User(int id, String email, String role, String name) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.name = name;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        if (email == null || email.isEmpty()) return name;
        return name + " (" + email + ")";
    }
}
