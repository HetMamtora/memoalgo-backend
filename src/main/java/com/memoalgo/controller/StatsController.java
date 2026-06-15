package com.memoalgo.controller;

import com.memoalgo.dto.response.StatsResponse;
import com.memoalgo.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Dashboard analytics and progress tracking")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    @Operation(
            summary = "Get dashboard analytics",
            description = "Returns streak, retention rate, due count, problems by topic/difficulty"
    )
    public ResponseEntity<StatsResponse> getStats(){
        return ResponseEntity.ok(statsService.getStats());
    }
}
