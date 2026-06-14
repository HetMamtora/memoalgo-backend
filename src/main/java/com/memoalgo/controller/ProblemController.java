package com.memoalgo.controller;

import com.memoalgo.dto.request.ProblemRequest;
import com.memoalgo.dto.response.ProblemResponse;
import com.memoalgo.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * ProblemController — full CRUD REST endpoints for user problems.
 *
 * All endpoints require a valid JWT (enforced by SecurityConfig).
 * Ownership is validated inside ProblemService, not here.
 * Controllers stay thin — just routing, validation, and HTTP status codes.
 *
 * Routes:
 *   GET    /api/v1/problems           → list all problems (with optional filters)
 *   GET    /api/v1/problems/{id}      → get problem by ID
 *   POST   /api/v1/problems           → create problem
 *   PUT    /api/v1/problems/{id}      → update problem
 *   DELETE /api/v1/problems/{id}      → soft delete problem
 */
@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
@Tag(name = "Problems", description = "DSA problem management")
@SecurityRequirement(name = "bearerAuth")
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    @Operation(summary = "Get all problems for the authenticated user",
            description = "Optional query params: difficulty (EASY/MEDIUM/HARD), topicId (UUID)")
    public ResponseEntity<List<ProblemResponse>> getAllProblems(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) UUID topicId) {

        return ResponseEntity.ok(problemService.getAllProblems(difficulty, topicId));
    }

    @GetMapping("/{problemId}")
    @Operation(summary = "Get a problem by ID")
    public ResponseEntity<ProblemResponse> getProblemById(@PathVariable UUID problemId) {
        return ResponseEntity.ok(problemService.getProblemById(problemId));
    }

    @PostMapping
    @Operation(summary = "Create a new problem")
    public ResponseEntity<ProblemResponse> createProblem(
            @Valid @RequestBody ProblemRequest request) {

        ProblemResponse response = problemService.createProblem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{problemId}")
    @Operation(summary = "Update an existing problem")
    public ResponseEntity<ProblemResponse> updateProblem(
            @PathVariable UUID problemId,
            @Valid @RequestBody ProblemRequest request) {

        return ResponseEntity.ok(problemService.updateProblem(problemId, request));
    }

    @DeleteMapping("/{problemId}")
    @Operation(summary = "Soft delete a problem")
    public ResponseEntity<Void> deleteProblem(@PathVariable UUID problemId) {
        problemService.deleteProblem(problemId);
        return ResponseEntity.noContent().build();
    }
}