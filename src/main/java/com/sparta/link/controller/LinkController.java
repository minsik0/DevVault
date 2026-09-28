package com.sparta.link.controller;

import com.sparta.common.response.ApiResponse;
import com.sparta.link.dto.LinkPageResponse;
import com.sparta.link.dto.LinkResponse;
import com.sparta.link.service.LinkService;
import com.sparta.user.User;
import jakarta.validation.Valid;
import jdk.dynalink.linker.LinkRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    public ResponseEntity<ApiResponse<LinkResponse>> save(@AuthenticationPrincipal User user,
                                                          @Valid @RequestBody LinkRequest linkRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(linkService.save(user, linkRequest)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LinkPageResponse>> getLinks(@AuthenticationPrincipal User user,
                                                                  @RequestParam(required = false) Long cursor) {
        return ResponseEntity.ok(ApiResponse.ok(linkService.getLinks(user, cursor)));
    }

    @GetMapping("/{linkId}")
    public ResponseEntity<ApiResponse<LinkResponse>> getLink(@AuthenticationPrincipal User user,
                                                             @PathVariable Long linkId) {
        return ResponseEntity.ok(ApiResponse.ok(linkService.getLink(user, linkId)));
    }

    @PatchMapping("/{linkId}")
    public ResponseEntity<ApiResponse<LinkResponse>> updateTags(@AuthenticationPrincipal User user,
                                                                @PathVariable Long linkId,
                                                                @RequestBody List<String> tags) {
        return ResponseEntity.ok(ApiResponse.ok(linkService.updateTags(user, linkId, tags)));
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal User user,
                                                    @PathVariable Long linkId) {
        linkService.delete(user, linkId);
        return ResponseEntity.noContent().build();
    }
}
