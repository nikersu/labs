package services;

import entities.UserEntity;
import entities.Role;
import repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserEntity createUser(String username, String rawPassword, Role role) {
        UserEntity user = new UserEntity(username, passwordEncoder.encode(rawPassword), role);
        UserEntity saved = userRepository.save(user);
        logger.info("Created user with id={}, username={}, role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return saved;
    }

    public UserEntity updateUser(UserEntity user, String username, String rawPassword, Role role) {
        user.setUsername(username);
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        if (role != null) {
            user.setRole(role);
        }
        UserEntity saved = userRepository.save(user);
        logger.info("Updated user with id={}, username={}, role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return saved;
    }

    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
        logger.info("Deleted user with id={}", id);
    }

    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }
}
