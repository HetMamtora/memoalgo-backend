package com.memoalgo.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReviewRequest {

    @NotNull(message = "Quality rating is required")
    @Min(value = 0, message = "Quality must be at least 0")
    @Max(value = 5, message = "Quality must be at most 5")
    private Integer quality;
}
