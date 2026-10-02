package com.sparta.link.service;

import com.sparta.ai.service.RagService;
import com.sparta.ai.service.SummaryService;
import com.sparta.link.crawler.MetadataCrawler;
import com.sparta.link.dto.LinkPageResponse;
import com.sparta.link.dto.LinkResponse;
import com.sparta.link.dto.LinkSaveRequest;
import com.sparta.link.entity.Link;
import com.sparta.link.entity.LinkTag;
import com.sparta.link.repository.LinkRepository;
import com.sparta.link.repository.LinkTagRepository;
import com.sparta.tag.Tag;
import com.sparta.tag.TagRepository;
import com.sparta.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
public class LinkService {

    private final LinkRepository linkRepository;
    private final TagRepository tagRepository;
    private final LinkTagRepository linkTagRepository;
    private final MetadataCrawler metadataCrawler;
    private final RagService ragService;
    private final SummaryService summaryService;

    private static final int PAGE_SIZE = 20;

    // 링크 저장
    public LinkResponse save(User user, LinkSaveRequest request) {
        // 메타데이터 크롤링
        MetadataCrawler.LinkMetadata metadata = metadataCrawler.crawl(request.getUrl());

        Link link = Link.of(user, request.getUrl());
        link.updateMetadata(metadata.title(), metadata.description(), metadata.favicon());
        linkRepository.save(link);

        // 태그 처리
        attachTags(user, link, request.getTags());

        // AI 처리 — 비동기로 실행 (응답 지연 방지)
        CompletableFuture.runAsync(() -> {
            summaryService.summarize(link);   // 요약 + 키워드 추출
            ragService.embedLink(link);        // 벡터 임베딩
        });

        return LinkResponse.from(link);
    }

    // 링크 목록 조회 (커서 기반 페이지네이션)
    @Transactional(readOnly = true)
    public LinkPageResponse getLinks(User user, Long cursorId) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE + 1);

        List<Link> links = (cursorId == null)
                ? linkRepository.findByUserIdOrderByIdDesc(user.getId(), pageable)
                : linkRepository.findByUserIdAndIdLessThanOrderByIdDesc(user.getId(), cursorId, pageable);

        boolean hasNext = links.size() > PAGE_SIZE;
        if (hasNext) links = links.subList(0, PAGE_SIZE);

        Long nextCursor = hasNext ? links.get(links.size() - 1).getId() : null;

        return new LinkPageResponse(
                links.stream().map(LinkResponse::from).toList(),
                nextCursor,
                hasNext
        );
    }

    // 링크 상세 조회
    @Transactional(readOnly = true)
    public LinkResponse getLink(User user, Long linkId) {
        Link link = getLinkOrThrow(user, linkId);
        return LinkResponse.from(link);
    }

    // 태그 수정
    public LinkResponse updateTags(User user, Long linkId, List<String> tagNames) {
        Link link = getLinkOrThrow(user, linkId);

        // 기존 태그 전체 삭제 후 재등록
        link.getLinkTags().clear();
        attachTags(user, link, tagNames);

        return LinkResponse.from(link);
    }

    // 링크 삭제
    public void delete(User user, Long linkId) {
        Link link = getLinkOrThrow(user, linkId);
        linkRepository.delete(link);
    }

    // 공통 — 태그 처리 (없으면 생성, 있으면 재사용)
    private void attachTags(User user, Link link, List<String> tagNames) {
        for (String name : tagNames) {
            Tag tag = tagRepository.findByUserIdAndName(user.getId(), name)
                    .orElseGet(() -> tagRepository.save(Tag.of(user, name)));
            link.getLinkTags().add(LinkTag.of(link, tag));
        }
    }

    private Link getLinkOrThrow(User user, Long linkId) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("링크를 찾을 수 없습니다."));
        if (!link.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return link;
    }
}