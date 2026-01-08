package com.demo.taskmanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatar;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private boolean active;
}
