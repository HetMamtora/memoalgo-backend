package com.memoalgo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoalgo.dto.request.LoginRequest;
import com.memoalgo.dto.request.RegisterRequest;
import com.memoalgo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthControllerTest — Integration tests for authentication endpoints.
 *
 * @SpringBootTest: loads the FULL application context (all beans, real DB)
 * @AutoConfigureMockMvc: creates a MockMvc that fires real HTTP requests
 *                        through the full Spring Security filter chain
 * @ActiveProfiles("test"): uses application-test.yml (memoalgo_test DB)
 *
 * Why integration tests for auth?
 * Auth involves Security filter chain → Controller → Service → Repository → DB.
 * Unit testing each layer separately wouldn't catch integration issues like
 * Spring Security blocking a public endpoint or JWT not being generated.
 *
 * Test isolation: each test uses a unique email (UUID) to avoid conflicts.
 * @AfterEach deletes test users by email prefix.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:postgresql://localhost:5432/memoalgo_test",
                "spring.datasource.username=postgres",
                "spring.datasource.password=${MEMOALGO_TEST_DB_PASSWORD:fallback}",
                "spring.flyway.locations=classpath:db/migration",
                "jwt.secret=test-secret-key-minimum-32-chars-long-for-algorithm",
                "jwt.expiration-ms=86400000",
                "jwt.refresh-expiration-ms=604800000"
        }
)
@AutoConfigureMockMvc
@DisplayName("Auth Controller Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    // Unique prefix per test run to avoid cross-test contamination
    private String testEmail;
    private String testUsername;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        testEmail = "test-" + unique + "@memoalgo.com";
        testUsername = "testuser-" + unique;
    }

    @AfterEach
    void tearDown() {
        // Clean up test users after each test
        userRepository.findByEmail(testEmail)
                .ifPresent(userRepository::delete);
    }

    // ─────────────────────────────────────────────────────
    // REGISTER TESTS
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register with valid data → 201 + JWT token returned")
    void register_withValidData_shouldReturn201AndToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setUsername(testUsername);
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.email", is(testEmail)))
                .andExpect(jsonPath("$.username", is(testUsername)));
    }

    @Test
    @DisplayName("POST /register with duplicate email → 409 Conflict")
    void register_withDuplicateEmail_shouldReturn409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setUsername(testUsername);
        request.setPassword("password123");

        // Register once
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to register again with same email
        request.setUsername("different-username");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    @DisplayName("POST /register with invalid email → 400 Bad Request")
    void register_withInvalidEmail_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-an-email");
        request.setUsername(testUsername);
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register with short password → 400 Bad Request")
    void register_withShortPassword_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setUsername(testUsername);
        request.setPassword("short"); // less than 8 chars

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register with blank username → 400 Bad Request")
    void register_withBlankUsername_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setUsername("");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────
    // LOGIN TESTS
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /login with valid credentials → 200 + JWT token returned")
    void login_withValidCredentials_shouldReturn200AndToken() throws Exception {
        // First register the user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.email", is(testEmail)));
    }

    @Test
    @DisplayName("POST /login with wrong password → 401 Unauthorized")
    void login_withWrongPassword_shouldReturn401() throws Exception {
        // Register first
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Try wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /login with non-existent email → 401 Unauthorized")
    void login_withNonExistentEmail_shouldReturn401() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nobody@memoalgo.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}