package com.sparta.link.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LinkPageResponse {
    private List<LinkResponse> links;
    private Long nextCursor;
    private boolean hasNext;
}
