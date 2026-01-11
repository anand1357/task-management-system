package com.demo.taskmanagement.service;

import com.demo.taskmanagement.dto.response.UserResponse;
import com.demo.taskmanagement.entity.Role;
import com.demo.taskmanagement.entity.User;
import com.demo.taskmanagement.enums.ERole;
import com.demo.taskmanagement.exception.ResourceNotFoundException;
import com.demo.taskmanagement.repository.RoleRepository;
import com.demo.taskmanagement.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return convertToResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToResponse(user);
    }

    public List<UserResponse> searchUsers(String keyword) {
        List<User> users = userRepository.findAll();
        return users.stream()
                .filter(user -> user.isActive()) // Only show active users
                .filter(user -> user.getUsername().toLowerCase().contains(keyword.toLowerCase()) ||
                        (user.getFullName() != null && user.getFullName().toLowerCase().contains(keyword.toLowerCase())) ||
                        user.getEmail().toLowerCase().contains(keyword.toLowerCase()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateUser(Long userId, String fullName, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (fullName != null && !fullName.isEmpty()) {
            user.setFullName(fullName);
        }

        if (roleNames != null && !roleNames.isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : roleNames) {
                ERole eRole;
                switch (roleName.toLowerCase()) {
                    case "admin":
                        eRole = ERole.ROLE_ADMIN;
                        break;
                    case "manager":
                        eRole = ERole.ROLE_MANAGER;
                        break;
                    case "product_owner":
                        eRole = ERole.ROLE_PRODUCT_OWNER;
                        break;
                    default:
                        eRole = ERole.ROLE_USER;
                }
                Role role = roleRepository.findByName(eRole)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = modelMapper.map(user, UserResponse.class);
        response.setRoles(user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet()));
        return response;
    }
}
