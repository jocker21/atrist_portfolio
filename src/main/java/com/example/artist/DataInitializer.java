package com.example.artist;

import com.example.artist.entity.User;
import com.example.artist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUserIfNotExists("admin", "admin_admin", "ROLE_ADMIN");
        createUserIfNotExists("user", "user_user", "ROLE_USER");
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
