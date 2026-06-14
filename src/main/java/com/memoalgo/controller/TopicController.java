package com.memoalgo.controller;

import com.memoalgo.dto.response.TopicResponse;
import com.memoalgo.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "DSA topic listing")
@SecurityRequirement(name = "bearerAuth")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "Get all DSA topics")
    public ResponseEntity<List<TopicResponse>> getAllTopics(){
        return ResponseEntity.ok(topicService.getAllTopics());
    }

    @GetMapping("/{topicId}")
    @Operation(summary = "Get topic by ID")
    public ResponseEntity<TopicResponse> getTopicById(@PathVariable UUID topicId){
        return ResponseEntity.ok(topicService.getTopicById(topicId));
    }
}
