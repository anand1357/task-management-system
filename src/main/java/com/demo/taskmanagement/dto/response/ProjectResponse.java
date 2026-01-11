package com.demo.taskmanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private String projectKey;
    private UserResponse owner;
    private Set<UserResponse> members = new HashSet<>();
    private Integer taskCount = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
}
