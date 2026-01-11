package com.demo.taskmanagement.service;

import com.demo.taskmanagement.dto.request.LoginRequest;
import com.demo.taskmanagement.dto.request.RegisterRequest;
import com.demo.taskmanagement.dto.response.JwtResponse;
import com.demo.taskmanagement.dto.response.MessageResponse;
import com.demo.taskmanagement.entity.Role;
import com.demo.taskmanagement.entity.User;
import com.demo.taskmanagement.enums.ERole;
import com.demo.taskmanagement.exception.BadRequestException;
import com.demo.taskmanagement.repository.RoleRepository;
import com.demo.taskmanagement.repository.UserRepository;
import com.demo.taskmanagement.security.JwtUtils;
import com.demo.taskmanagement.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EmailService emailService;

    public JwtResponse login(LoginRequest loginRequest) {
        // Check if input is email or username
        User user;
        if (loginRequest.getUsername().contains("@")) {
            user = userRepository.findByEmail(loginRequest.getUsername())
                    .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        } else {
            user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new BadRequestException("Invalid username or password"));
        }

        // Check if user is active (not soft deleted)
        if (!user.isActive()) {
            throw new BadRequestException("User account is deactivated");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );
    }

    @Transactional
    public MessageResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BadRequestException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getFullName());

        Set<String> strRoles = registerRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        // Check if this is the first user - make them PRODUCT_OWNER
        long userCount = userRepository.count();

        if (userCount == 0) {
            // First user becomes Product Owner with all privileges
            Role productOwnerRole = roleRepository.findByName(ERole.ROLE_PRODUCT_OWNER)
                    .orElseThrow(() -> new RuntimeException("Error: Product Owner role is not found."));
            roles.add(productOwnerRole);

            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Admin role is not found."));
            roles.add(adminRole);
        } else if (strRoles == null || strRoles.isEmpty()) {
            // Default role
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            // Assign requested roles
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "manager":
                        Role managerRole = roleRepository.findByName(ERole.ROLE_MANAGER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(managerRole);
                        break;
                    case "product_owner":
                        Role productOwnerRole = roleRepository.findByName(ERole.ROLE_PRODUCT_OWNER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(productOwnerRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        if (userCount == 0) {
            return new MessageResponse("First user (Product Owner) registered successfully!");
        }

        return new MessageResponse("User registered successfully!");
    }

    public MessageResponse forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Email not found"));

        if (!user.isActive()) {
            throw new BadRequestException("User account is deactivated");
        }

        String resetToken = jwtUtils.generatePasswordResetToken(email);

        // Send email with reset link
        emailService.sendPasswordResetEmail(email, resetToken);

        return new MessageResponse("Password reset instructions sent to your email");
    }

    @Transactional
    public MessageResponse resetPassword(String token, String newPassword) {
        String email = jwtUtils.getEmailFromResetToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid token"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return new MessageResponse("Password reset successfully");
    }
}
