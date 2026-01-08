package com.demo.taskmanagement.dto.request;

import com.demo.taskmanagement.enums.TaskPriority;
import com.demo.taskmanagement.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {
    @NotBlank(message = "Task title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private TaskStatus status;

    private TaskPriority priority;

    private Long assigneeId;

    private LocalDate dueDate;

    private Integer estimatedHours;

    private Integer order;
}
