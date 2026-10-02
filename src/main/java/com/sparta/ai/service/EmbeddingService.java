package com.sparta.ai.service;

import com.sparta.link.entity.Link;
import com.sparta.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final LinkRepository linkRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String EMBED_CACHE_PREFIX = "embed:";
    private static final int CHUNK_SIZE = 500;

    // 링크 본문 크롤링 → 청킹 → 벡터 저장
    public void embed(Link link) {
        // Redis 캐시 확인 (같은 URL 재요청 방지)
        String cacheKey = EMBED_CACHE_PREFIX + link.getId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            log.info("이미 임베딩된 링크: {}", link.getId());
            return;
        }

        try {
            // 1. 본문 크롤링
            String content = crawlContent(link.getUrl());
            if (content.isBlank()) return;

            // 2. 청킹
            List<String> chunks = chunk(content);

            // 3. Document 생성 (메타데이터 포함)
            List<Document> documents = chunks.stream()
                    .map(chunk -> Document.builder()
                            .text(chunk)
                            .metadata(Map.of(
                                    "linkId", String.valueOf(link.getId()),
                                    "url", link.getUrl(),
                                    "title", link.getTitle() != null ? link.getTitle() : ""
                            ))
                            .build()
                    )
                    .toList();

            // 4. 벡터 DB 저장
            vectorStore.add(documents);

            // 5. 임베딩 완료 표시
            link.markEmbedded();
            linkRepository.save(link);

            // 6. Redis 캐시 저장 (30일)
            redisTemplate.opsForValue().set(cacheKey, "1", 30, TimeUnit.DAYS);

            log.info("임베딩 완료 - linkId: {}, chunks: {}", link.getId(), chunks.size());

        } catch (Exception e) {
            log.error("임베딩 실패 - linkId: {}", link.getId(), e);
        }
    }

    private List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        int overlap = 50;
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end));
            start += CHUNK_SIZE - overlap;
        }

        return chunks;
    }

    // 본문 크롤링 (메타데이터 제외 순수 텍스트)
    private String crawlContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(8000)
                    .get();

            // script, style 제거 후 텍스트 추출
            doc.select("script, style, nav, footer, header").remove();
            return doc.body().text();

        } catch (IOException e) {
            log.warn("본문 크롤링 실패: {}", url);
            return "";
        }
    }
}
