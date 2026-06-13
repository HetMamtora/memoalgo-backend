package com.memoalgo.service;

import com.memoalgo.dto.request.LoginRequest;
import com.memoalgo.dto.request.RegisterRequest;
import com.memoalgo.dto.response.AuthResponse;
import com.memoalgo.entity.User;
import com.memoalgo.exception.ConflictException;
import com.memoalgo.repository.UserRepository;
import com.memoalgo.security.JwtTokenProvider;
import com.memoalgo.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request){
        String email = request.getEmail().toLowerCase().trim();

        if(userRepository.existsByEmail(email)){
            throw new ConflictException("An account with this email already exists");
        }

        if(userRepository.existsByUsername(request.getUsername())){
            throw new ConflictException("This username is already taken");
        }

        User user = User.builder()
                .email(email)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .lastActiveAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);

        return AuthResponse.builder()
                .accessToken(token)
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .build();
    }

    public AuthResponse login(LoginRequest request){
        String email = request.getEmail().toLowerCase().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastActiveAt(Instant.now());
            userRepository.save(user);
        });

        User user = userRepository.findByEmail(email).orElseThrow();
        log.info("User logged in: {}", email);

        return AuthResponse.builder()
                .accessToken(token)
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }
}
