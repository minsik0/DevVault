package com.sparta.link.dto;

import com.sparta.link.entity.Link;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class LinkResponse {
    private Long id;
    private String url;
    private String title;
    private String description;
    private String faviconUrl;
    private String summary;
    private List<String> tags;
    private LocalDateTime createdAt;

    public static LinkResponse from(Link link) {
        return LinkResponse.builder()
                .id(link.getId())
                .url(link.getUrl())
                .title(link.getTitle())
                .description(link.getDescription())
                .faviconUrl(link.getFaviconUrl())
                .summary(link.getSummary())
                .tags(link.getLinkTags().stream()
                        .map(lt -> lt.getTag().getName())
                        .toList())
                .createdAt(link.getCreatedAt())
                .build();
    }
}
