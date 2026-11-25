
package com.officechat.config;

import com.officechat.entity.Department;
import com.officechat.entity.User;
import com.officechat.repository.DepartmentRepository;
import com.officechat.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedInitialData(
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            // 1. Seed departments
            String[][] departments = {
                {"IT", "Information Technology"},
                {"HRD", "Human Resources"},
                {"PRODUCTION", "Production Department"},
                {"FINANCE", "Finance Department"},
                {"GENERAL AFFAIR", "General Affairs"}
            };

            for (String[] data : departments) {
                if (!departmentRepository.existsByDepartmentName(data[0])) {
                    Department department = new Department();
                    department.setDepartmentName(data[0]);
                    department.setDescription(data[1]);
                    departmentRepository.save(department);
                }
            }

            // 2. Find IT department
            Department itDepartment = departmentRepository
                    .findAll()
                    .stream()
                    .filter(d -> "IT".equals(d.getDepartmentName()))
                    .findFirst()
                    .orElseThrow();

            // 3. Create initial administrator
            if (!userRepository.existsByEmail("admin@officechat.local")) {

                User admin = new User();

                admin.setEmployeeId("EMP001");
                admin.setFullName("Administrator OfficeChat");
                admin.setEmail("admin@officechat.local");
                admin.setPasswordHash(
                    passwordEncoder.encode("Admin12345")
                );
                admin.setPhone("081234567890");
                admin.setDepartment(itDepartment);
                admin.setRole("ADMIN");
                admin.setStatus("ACTIVE");
                admin.setIsOnline(0);

                userRepository.save(admin);
            }

            System.out.println("==================================");
            System.out.println("OFFICECHAT DATABASE SEED COMPLETED");
            System.out.println("Admin: admin@officechat.local");
            System.out.println("Password: Admin12345");
            System.out.println("==================================");
        };
    }
}
