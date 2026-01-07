package config;

import entities.Role;
import entities.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import repositories.UserRepository;

@Component
public class BootstrapAdmin implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.bootstrap.username:admin}")
    private String username;

    @Value("${app.security.bootstrap.password:admin}")
    private String password;

    @Value("${app.security.bootstrap.role:ADMIN}")
    private String role;

    public BootstrapAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(username).isPresent()) return;

        Role parsedRole;
        try {
            parsedRole = Role.valueOf(role);
        } catch (Exception e) {
            parsedRole = Role.ADMIN;
        }

        UserEntity u = new UserEntity();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(parsedRole);

        userRepository.save(u);
        System.out.println("BOOTSTRAP: created user '" + username + "' role=" + parsedRole);
    }
}
