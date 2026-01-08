package com.demo.taskmanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private String key;
    private UserResponse owner;
    private Set<UserResponse> members;
    private Integer taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
}
