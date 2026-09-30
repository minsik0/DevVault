package com.sparta.ai.dto;

import com.sparta.link.dto.LinkResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AskResponse {
    private String answer;
    private List<LinkResponse> references;  // 참조한 링크 목록
}
