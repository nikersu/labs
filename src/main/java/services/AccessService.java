package services;

import config.SecurityUserDetails;
import entities.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccessService {

    private static final Logger logger = LoggerFactory.getLogger(AccessService.class);

    private final UserService userService;
    private final FunctionService functionService;

    public AccessService(UserService userService, FunctionService functionService) {
        this.userService = userService;
        this.functionService = functionService;
    }

    public boolean canAccessUser(Long userId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        Long currentUserId = currentUserId(authentication);
        boolean allowed = currentUserId != null && currentUserId.equals(userId);
        if (!allowed) {
            logger.warn("User access denied. userId={}, principal={}", userId, authentication.getName());
        }
        return allowed;
    }

    public boolean canAccessUsername(String username, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        Optional<UserEntity> user = userService.findByUsername(username);
        boolean allowed = user.map(u -> currentUserId(authentication) != null
                        && currentUserId(authentication).equals(u.getId()))
                .orElse(false);
        if (!allowed) {
            logger.warn("Username access denied. username={}, principal={}", username, authentication.getName());
        }
        return allowed;
    }

    public boolean canAccessFunction(Long functionId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        Long currentUserId = currentUserId(authentication);
        boolean allowed = functionService.findById(functionId)
                .map(function -> currentUserId != null && currentUserId.equals(function.getUser().getId()))
                .orElse(false);
        if (!allowed) {
            logger.warn("Function access denied. functionId={}, principal={}", functionId, authentication.getName());
        }
        return allowed;
    }

    public boolean canAccessUserFunctions(Long userId, Authentication authentication) {
        return canAccessUser(userId, authentication);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof SecurityUserDetails details) {
            return details.getId();
        }
        return null;
    }
}
