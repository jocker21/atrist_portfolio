package com.example.artist;

import com.example.artist.entity.User;
import com.example.artist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// CommandLineRunner — Spring запускает метод run() сразу после старта приложения.
// Используем чтобы создать пользователей если их ещё нет в БД.
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        createUserIfNotExists("artist", "user_root");
        createUserIfNotExists("admin", "admin_admin");
        createUserIfNotExists("user", "user_user");
    }

    private void createUserIfNotExists(String username, String rawPassword) {
        // Проверяем что пользователь ещё не существует
        // чтобы не создавать дубликаты при каждом перезапуске
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole("ROLE_ADMIN");
            user.setEnabled(true);
            userRepository.save(user);
            System.out.println("Создан пользователь: " + username);
        }
    }
}
