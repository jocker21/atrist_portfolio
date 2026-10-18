package com.example.artist;

import com.example.artist.entity.User;
import com.example.artist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            System.out.println("Admin user was not created: set app.admin.password or APP_ADMIN_PASSWORD first.");
            return;
        }
        createUserIfNotExists(adminUsername, adminPassword, "ROLE_ADMIN");
    }

    private void createUserIfNotExists(String username, String rawPassword, String role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setEnabled(true);
            userRepository.save(user);
            System.out.println("Створено користувача: " + username + " з роллю: " + role);
        }
    }
}
