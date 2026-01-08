package com.demo.taskmanagement.service;

import com.demo.taskmanagement.dto.request.CommentRequest;
import com.demo.taskmanagement.dto.request.TaskRequest;
import com.demo.taskmanagement.dto.response.CommentResponse;
import com.demo.taskmanagement.dto.response.TaskResponse;
import com.demo.taskmanagement.entity.Comment;
import com.demo.taskmanagement.entity.Project;
import com.demo.taskmanagement.entity.Task;
import com.demo.taskmanagement.entity.User;
import com.demo.taskmanagement.enums.TaskPriority;
import com.demo.taskmanagement.enums.TaskStatus;
import com.demo.taskmanagement.exception.ResourceNotFoundException;
import com.demo.taskmanagement.exception.UnauthorizedException;
import com.demo.taskmanagement.repository.CommentRepository;
import com.demo.taskmanagement.repository.ProjectRepository;
import com.demo.taskmanagement.repository.TaskRepository;
import com.demo.taskmanagement.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        User currentUser = userService.getCurrentUser();

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

        validateProjectMember(project, currentUser);

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setProject(project);
        task.setCreatedBy(currentUser);
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO);
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        task.setDueDate(request.getDueDate());
        task.setEstimatedHours(request.getEstimatedHours());
        task.setOrder(request.getOrder());

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getAssigneeId()));
            validateProjectMember(project, assignee);
            task.setAssignee(assignee);
        }

        Task savedTask = taskRepository.save(task);
        return convertToResponse(savedTask);
    }

    public List<TaskResponse> getAllTasksByProject(Long projectId) {
        User currentUser = userService.getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        validateProjectMember(project, currentUser);

        List<Task> tasks = taskRepository.findByProjectIdOrderByOrder(projectId);
        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getTasksByStatus(Long projectId, TaskStatus status) {
        User currentUser = userService.getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        validateProjectMember(project, currentUser);

        List<Task> tasks = taskRepository.findByProjectIdAndStatus(projectId, status);
        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        User currentUser = userService.getCurrentUser();
        validateProjectMember(task.getProject(), currentUser);

        return convertToResponse(task);
    }

    public List<TaskResponse> getMyTasks() {
        User currentUser = userService.getCurrentUser();
        List<Task> tasks = taskRepository.findByAssigneeId(currentUser.getId());
        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        User currentUser = userService.getCurrentUser();
        validateProjectMember(task.getProject(), currentUser);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setEstimatedHours(request.getEstimatedHours());
        task.setOrder(request.getOrder());

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getAssigneeId()));
            validateProjectMember(task.getProject(), assignee);
            task.setAssignee(assignee);
        } else {
            task.setAssignee(null);
        }

        Task updatedTask = taskRepository.save(task);
        return convertToResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long id, TaskStatus status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        User currentUser = userService.getCurrentUser();
        validateProjectMember(task.getProject(), currentUser);

        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);

        return convertToResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        User currentUser = userService.getCurrentUser();
        validateProjectMember(task.getProject(), currentUser);

        taskRepository.delete(task);
    }

    @Transactional
    public CommentResponse addComment(Long taskId, CommentRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User currentUser = userService.getCurrentUser();
        validateProjectMember(task.getProject(), currentUser);

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setTask(task);
        comment.setUser(currentUser);

        Comment savedComment = commentRepository.save(comment);
        return convertToCommentResponse(savedComment);
    }

    public List<CommentResponse> getTaskComments(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User currentUser = userService.getCurrentUser();
        validateProjectMember(task.getProject(), currentUser);

        List<Comment> comments = commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        return comments.stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        User currentUser = userService.getCurrentUser();
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    private void validateProjectMember(Project project, User user) {
        boolean isMember = project.getOwner().getId().equals(user.getId()) ||
                project.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));

        if (!isMember) {
            throw new UnauthorizedException("You are not a member of this project");
        }
    }

    private TaskResponse convertToResponse(Task task) {
        TaskResponse response = modelMapper.map(task, TaskResponse.class);
        response.setProjectId(task.getProject().getId());
        response.setProjectName(task.getProject().getName());
        return response;
    }

    private CommentResponse convertToCommentResponse(Comment comment) {
        return modelMapper.map(comment, CommentResponse.class);
    }
}
