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
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryService {

    private final ChatClient chatClient;
    private final LinkRepository linkRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SUMMARY_CACHE_PREFIX = "summary:";

    // 링크 본문 자동 요약 + 키워드 추출
    public void summarize(Link link) {
        String cacheKey = SUMMARY_CACHE_PREFIX + link.getId();

        // 이미 요약된 경우 스킵
        if (link.getSummary() != null) {
            log.info("이미 요약된 링크: {}", link.getId());
            return;
        }

        try {
            // 1. 본문 크롤링
            String content = crawlContent(link.getUrl());
            if (content.isBlank()) return;

            // 2. 요약 + 키워드 동시 추출 (API 호출 1번으로 처리)
            String result = chatClient.prompt()
                    .system("""
                            다음 형식으로만 응답하세요. 다른 말은 하지 마세요.
                            
                            SUMMARY: (3문장 이내 핵심 요약)
                            KEYWORDS: (핵심 키워드 5개, 쉼표 구분)
                            """)
                    .user("다음 글을 요약해주세요:\n\n" + truncate(content, 3000))
                    .call()
                    .content();

            // 3. 파싱
            String summary = parseSection(result, "SUMMARY");
            List<String> keywords = parseKeywords(result);

            // 4. DB 저장
            link.updateSummary(summary);
            linkRepository.save(link);

            // 5. 키워드를 태그로 자동 등록 (선택적)
            log.info("요약 완료 - linkId: {}, keywords: {}", link.getId(), keywords);

        } catch (Exception e) {
            log.error("요약 실패 - linkId: {}", link.getId(), e);
        }
    }

    private String parseSection(String result, String section) {
        return Arrays.stream(result.split("\n"))
                .filter(line -> line.startsWith(section + ":"))
                .map(line -> line.substring(section.length() + 1).trim())
                .findFirst()
                .orElse("");
    }

    private List<String> parseKeywords(String result) {
        return Arrays.stream(result.split("\n"))
                .filter(line -> line.startsWith("KEYWORDS:"))
                .map(line -> line.substring("KEYWORDS:".length()).trim())
                .flatMap(k -> Arrays.stream(k.split(",")))
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .toList();
    }

    // 3000자 이상은 잘라서 전달 (토큰 비용 절감)
    private String truncate(String text, int limit) {
        return text.length() > limit ? text.substring(0, limit) + "..." : text;
    }

    private String crawlContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(8000)
                    .get();
            doc.select("script, style, nav, footer, header").remove();
            return doc.body().text();
        } catch (IOException e) {
            log.warn("본문 크롤링 실패: {}", url);
            return "";
        }
    }
}
