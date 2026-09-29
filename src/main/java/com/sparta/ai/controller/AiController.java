package com.sparta.ai.controller;

import com.sparta.ai.dto.AskRequest;
import com.sparta.ai.dto.AskResponse;
import com.sparta.ai.service.RagService;
import com.sparta.common.response.ApiResponse;
import com.sparta.link.dto.LinkResponse;
import com.sparta.link.entity.Link;
import com.sparta.link.repository.LinkRepository;
import com.sparta.user.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final RagService ragService;
    private final LinkRepository linkRepository;

    // 링크 수동 임베딩 요청
    @PostMapping("/embed/{linkId}")
    public ResponseEntity<ApiResponse<Void>> embed(@AuthenticationPrincipal User user,
                                                   @PathVariable Long linkId) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("링크를 찾을 수 없습니다."));
        ragService.embedLink(link);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 의미 기반 유사 링크 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LinkResponse>>> search(@AuthenticationPrincipal User user,
                                                                  @RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.ok(ragService.searchSimilar(user, query)));
    }

    // RAG 질의응답
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<AskResponse>> ask(@AuthenticationPrincipal User user,
                                                        @Valid @RequestBody AskRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(ragService.ask(user, request.getQuestion())));
    }
}
