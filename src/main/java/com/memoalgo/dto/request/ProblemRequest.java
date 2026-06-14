package com.memoalgo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProblemRequest {

    @NotBlank(message = "Problem title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 500, message = "URL must not exceed 500 characters")
    private String url;

    @NotBlank(message = "Difficulty is required")
    @Pattern(
            regexp = "EASY|MEDIUM|HARD",
            message = "Difficulty must be EAST, MEDIUM, or HARD"
    )
    private String difficulty;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;

    private UUID topicId;

    private Set<String> tags = new HashSet<>();
}
