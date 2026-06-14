package com.memoalgo.util;

import com.memoalgo.entity.User;
import com.memoalgo.exception.UnauthorizedException;
import com.memoalgo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SecurityUtils — helper for getting the authenticated user in services.
 *
 * Pattern:
 *   Every protected service method starts by calling getCurrentUser()
 *   to get the User entity of the caller. This ensures:
 *   - All data operations are scoped to the authenticated user
 *   - No user can access another user's data
 *
 * Why a @Component and not a static utility?
 *   We need to inject UserRepository (a Spring bean), which requires
 *   this class itself to be a Spring bean. Static classes cannot
 *   use @Autowired / @RequiredArgsConstructor.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Get the User entity of the currently authenticated caller.
     *
     * Reads the email from the SecurityContext (set by JwtAuthenticationFilter),
     * then loads the full User entity from the database.
     *
     * @throws UnauthorizedException if no authentication exists in context
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database"));
    }
}