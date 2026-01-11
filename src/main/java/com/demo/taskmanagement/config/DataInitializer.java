package com.demo.taskmanagement.config;

import com.demo.taskmanagement.entity.Role;
import com.demo.taskmanagement.enums.ERole;
import com.demo.taskmanagement.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialize roles if they don't exist
        if (roleRepository.count() == 0) {
            Role userRole = new Role();
            userRole.setName(ERole.ROLE_USER);
            roleRepository.save(userRole);

            Role adminRole = new Role();
            adminRole.setName(ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);

            Role managerRole = new Role();
            managerRole.setName(ERole.ROLE_MANAGER);
            roleRepository.save(managerRole);

            Role productOwnerRole = new Role();
            productOwnerRole.setName(ERole.ROLE_PRODUCT_OWNER);
            roleRepository.save(productOwnerRole);

            System.out.println("✅ Roles initialized successfully!");
        } else {
            System.out.println("✅ Roles already exist in database");
        }
    }
}