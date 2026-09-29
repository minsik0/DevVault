package com.sparta.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AskRequest {
    @NotBlank
    private String question;
}

