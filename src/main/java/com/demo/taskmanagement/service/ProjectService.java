package com.demo.taskmanagement.service;

import com.demo.taskmanagement.dto.request.ProjectRequest;
import com.demo.taskmanagement.dto.response.ProjectResponse;
import com.demo.taskmanagement.entity.Project;
import com.demo.taskmanagement.entity.User;
import com.demo.taskmanagement.exception.ResourceNotFoundException;
import com.demo.taskmanagement.exception.UnauthorizedException;
import com.demo.taskmanagement.repository.ProjectRepository;
import com.demo.taskmanagement.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        User currentUser = userService.getCurrentUser();

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setProjectKey(request.getProjectKey());
        project.setOwner(currentUser);

        Set<User> members = new HashSet<>();
        members.add(currentUser); // Owner is also a member

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            for (Long memberId : request.getMemberIds()) {
                User member = userRepository.findById(memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + memberId));
                members.add(member);
            }
        }

        project.setMembers(members);
        Project savedProject = projectRepository.save(project);

        return convertToResponse(savedProject);
    }

    public List<ProjectResponse> getAllProjects() {
        User currentUser = userService.getCurrentUser();
        List<Project> projects = projectRepository.findAllByUserIdInvolvement(currentUser.getId());
        return projects.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        User currentUser = userService.getCurrentUser();
        validateProjectAccess(project, currentUser);

        return convertToResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        User currentUser = userService.getCurrentUser();
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Only project owner can update the project");
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setProjectKey(request.getProjectKey());

        if (request.getMemberIds() != null) {
            Set<User> members = new HashSet<>();
            members.add(project.getOwner()); // Keep owner as member

            for (Long memberId : request.getMemberIds()) {
                User member = userRepository.findById(memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + memberId));
                members.add(member);
            }
            project.setMembers(members);
        }

        Project updatedProject = projectRepository.save(project);
        return convertToResponse(updatedProject);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        User currentUser = userService.getCurrentUser();
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Only project owner can delete the project");
        }

        projectRepository.delete(project);
    }

    @Transactional
    public ProjectResponse addMemberToProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User currentUser = userService.getCurrentUser();
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Only project owner can add members");
        }

        User newMember = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        project.getMembers().add(newMember);
        Project updatedProject = projectRepository.save(project);

        return convertToResponse(updatedProject);
    }

    @Transactional
    public ProjectResponse removeMemberFromProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User currentUser = userService.getCurrentUser();
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Only project owner can remove members");
        }

        if (project.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("Cannot remove project owner from members");
        }

        project.getMembers().removeIf(member -> member.getId().equals(userId));
        Project updatedProject = projectRepository.save(project);

        return convertToResponse(updatedProject);
    }

    private void validateProjectAccess(Project project, User user) {
        boolean hasAccess = project.getOwner().getId().equals(user.getId()) ||
                project.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));

        if (!hasAccess) {
            throw new UnauthorizedException("You don't have access to this project");
        }
    }

    private ProjectResponse convertToResponse(Project project) {
        ProjectResponse response = modelMapper.map(project, ProjectResponse.class);
        response.setTaskCount(project.getTasks() != null ? project.getTasks().size() : 0);
        return response;
    }
}
