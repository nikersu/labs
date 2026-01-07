package controllers;

import dto.UserDto;
import entities.Role;
import entities.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import services.UserService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessService.canAccessUser(#id, authentication)")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("@accessService.canAccessUsername(#username, authentication)")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        Role role = parseRole(userDto.getRole());
        UserEntity saved = userService.createUser(userDto.getUsername(), userDto.getPasswordHash(), role);
        logger.info("User created via API. id={}, username={}, role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessService.canAccessUser(#id, authentication)")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @RequestBody UserDto userDto,
            Authentication authentication) {
        return userService.findById(id)
                .map(user -> {
                    Role role = parseRoleForUpdate(userDto.getRole(), authentication);
                    UserEntity updated = userService.updateUser(user, userDto.getUsername(), userDto.getPasswordHash(), role);
                    logger.info("User updated via API. id={}, username={}, role={}", updated.getId(), updated.getUsername(), updated.getRole());
                    return ResponseEntity.ok(toDto(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.existsById(id)) {
            userService.deleteById(id);
            logger.info("User deleted via API. id={}", id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private UserDto toDto(UserEntity user) {
        return new UserDto(user.getId(), user.getUsername(), user.getPasswordHash(), user.getRole().name());
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return Role.USER;
        }
        return Role.valueOf(role.trim().toUpperCase());
    }

    private Role parseRoleForUpdate(String role, Authentication authentication) {
        if (role == null || role.isBlank()) {
            return null;
        }
        if (!isAdmin(authentication)) {
            logger.warn("Role update ignored for non-admin user={}", authentication.getName());
            return null;
        }
        return parseRole(role);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
