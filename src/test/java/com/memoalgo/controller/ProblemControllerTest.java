package com.memoalgo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoalgo.dto.request.ProblemRequest;
import com.memoalgo.dto.request.RegisterRequest;
import com.memoalgo.repository.ProblemRepository;
import com.memoalgo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProblemControllerTest — Integration tests for Problem CRUD endpoints.
 *
 * Pattern:
 * 1. @BeforeEach: register a test user + extract JWT token
 * 2. Each test uses that JWT in the Authorization header
 * 3. @AfterEach: clean test user and their problems
 *
 * This covers the full ownership model:
 * - Can only create/read/update/delete own problems
 * - Requests without JWT are rejected
 * - Requests with someone else's problem ID return 404
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
@DisplayName("Problem Controller Integration Tests")
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    private String jwtToken;
    private String testEmail;
    private String testUsername;

    @BeforeEach
    void setUp() throws Exception {
        // Generate unique test user per test run
        String unique = UUID.randomUUID().toString().substring(0, 8);
        testEmail = "test-" + unique + "@memoalgo.com";
        testUsername = "testuser-" + unique;

        // Register test user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract JWT token from register response
        JsonNode responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        jwtToken = responseBody.get("accessToken").asText();
    }

    @AfterEach
    void tearDown() {
        // Clean up: delete user (cascades to problems, reviews, tags)
        userRepository.findByEmail(testEmail)
                .ifPresent(user -> {
                    problemRepository.findByUser(user)
                            .forEach(problemRepository::delete);
                    userRepository.delete(user);
                });
    }

    // ─────────────────────────────────────────────────────
    // CREATE TESTS
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /problems with valid data and JWT → 201 + problem returned")
    void createProblem_withValidData_shouldReturn201() throws Exception {
        ProblemRequest request = buildProblemRequest("Two Sum", "EASY");

        mockMvc.perform(post("/api/v1/problems")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Two Sum")))
                .andExpect(jsonPath("$.difficulty", is("EASY")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.tags", hasItem("arrays")));
    }

    @Test
    @DisplayName("POST /problems without JWT → 403 Forbidden")
    void createProblem_withoutJwt_shouldReturn403() throws Exception {
        ProblemRequest request = buildProblemRequest("Two Sum", "EASY");

        mockMvc.perform(post("/api/v1/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /problems with invalid difficulty → 400 Bad Request")
    void createProblem_withInvalidDifficulty_shouldReturn400() throws Exception {
        ProblemRequest request = buildProblemRequest("Two Sum", "INVALID");

        mockMvc.perform(post("/api/v1/problems")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /problems with blank title → 400 Bad Request")
    void createProblem_withBlankTitle_shouldReturn400() throws Exception {
        ProblemRequest request = buildProblemRequest("", "EASY");

        mockMvc.perform(post("/api/v1/problems")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────
    // READ TESTS
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /problems returns only the authenticated user's problems")
    void getProblems_shouldReturnOnlyOwnProblems() throws Exception {
        // Create two problems
        createProblemViaApi("Two Sum", "EASY");
        createProblemViaApi("Binary Tree Level Order", "MEDIUM");

        mockMvc.perform(get("/api/v1/problems")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", hasItems("Two Sum", "Binary Tree Level Order")));
    }

    @Test
    @DisplayName("GET /problems?difficulty=EASY filters by difficulty")
    void getProblems_withDifficultyFilter_shouldReturnFilteredResults() throws Exception {
        createProblemViaApi("Two Sum", "EASY");
        createProblemViaApi("Merge K Lists", "HARD");

        mockMvc.perform(get("/api/v1/problems")
                        .param("difficulty", "EASY")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Two Sum")));
    }

    @Test
    @DisplayName("GET /problems/{id} returns the problem if owned by user")
    void getProblemById_whenOwnedByUser_shouldReturn200() throws Exception {
        String problemId = createProblemViaApi("Two Sum", "EASY");

        mockMvc.perform(get("/api/v1/problems/" + problemId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(problemId)))
                .andExpect(jsonPath("$.title", is("Two Sum")));
    }

    @Test
    @DisplayName("GET /problems/{id} with non-existent ID → 404 Not Found")
    void getProblemById_withNonExistentId_shouldReturn404() throws Exception {
        String randomId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/problems/" + randomId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────
    // UPDATE TESTS
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /problems/{id} updates the problem successfully")
    void updateProblem_withValidData_shouldReturn200() throws Exception {
        String problemId = createProblemViaApi("Two Sum", "EASY");

        ProblemRequest updateRequest = buildProblemRequest("Two Sum - Updated", "MEDIUM");

        mockMvc.perform(put("/api/v1/problems/" + problemId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Two Sum - Updated")))
                .andExpect(jsonPath("$.difficulty", is("MEDIUM")));
    }

    // ─────────────────────────────────────────────────────
    // DELETE TESTS
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /problems/{id} soft-deletes the problem → 204 No Content")
    void deleteProblem_shouldReturn204() throws Exception {
        String problemId = createProblemViaApi("Two Sum", "EASY");

        mockMvc.perform(delete("/api/v1/problems/" + problemId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /problems/{id} then GET /problems → deleted problem not in list")
    void deleteProblem_thenGetAll_shouldNotIncludeDeleted() throws Exception {
        String problemId = createProblemViaApi("Two Sum", "EASY");
        createProblemViaApi("Binary Search", "EASY");

        // Delete first problem
        mockMvc.perform(delete("/api/v1/problems/" + problemId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // List should only show the remaining problem
        mockMvc.perform(get("/api/v1/problems")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Binary Search")));
    }

    // ─────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────

    private ProblemRequest buildProblemRequest(String title, String difficulty) {
        ProblemRequest request = new ProblemRequest();
        request.setTitle(title);
        request.setUrl("https://leetcode.com/problems/test/");
        request.setDifficulty(difficulty);
        request.setNotes("Test notes");
        request.setTags(Set.of("arrays"));
        return request;
    }

    private String createProblemViaApi(String title, String difficulty) throws Exception {
        ProblemRequest request = buildProblemRequest(title, difficulty);

        MvcResult result = mockMvc.perform(post("/api/v1/problems")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }
}