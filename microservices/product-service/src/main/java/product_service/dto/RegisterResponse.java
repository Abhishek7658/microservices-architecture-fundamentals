package product_service.dto;

import java.time.LocalDateTime;

public class RegisterResponse {
    private Long id;
    private String name;
    private String email;
    private final String role;
    private final String status;
    private final LocalDateTime createdAt;

    public RegisterResponse(Long id, String name, String email, String role, String status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public RegisterResponse(LocalDateTime createdAt, String role, String status) {
        this.createdAt = createdAt;
        this.role = role;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}