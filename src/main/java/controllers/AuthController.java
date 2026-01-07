package controllers;

import dto.UserDto;
import entities.Role;
import entities.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Проверка на существующего пользователя
            if (userService.findByUsername(request.getUsername()).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Пользователь с таким именем уже существует");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Валидация
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Имя пользователя не может быть пустым");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Пароль не может быть пустым");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Создание пользователя с ролью USER по умолчанию
            UserEntity user = userService.createUser(
                    request.getUsername().trim(),
                    request.getPassword(),
                    Role.USER
            );

            logger.info("User registered via API. id={}, username={}", user.getId(), user.getUsername());
            
            UserDto userDto = new UserDto(
                    user.getId(),
                    user.getUsername(),
                    null, // Не возвращаем пароль
                    user.getRole().name()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
        } catch (Exception e) {
            logger.error("Error during registration", e);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Ошибка при регистрации: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Внутренний класс для запроса регистрации
    public static class RegisterRequest {
        private String username;
        private String password;

        public RegisterRequest() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}





