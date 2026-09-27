package com.sparta.link.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class LinkSaveRequest {
    @NotBlank
    @org.hibernate.validator.constraints.URL
    private String url;

    private List<String> tags = new ArrayList<>();
}
