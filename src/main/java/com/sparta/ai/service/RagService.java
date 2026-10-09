package com.sparta.ai.service;

import com.sparta.ai.config.AiMetrics;
import com.sparta.ai.dto.AskResponse;
import com.sparta.link.dto.LinkResponse;
import com.sparta.link.entity.Link;
import com.sparta.link.repository.LinkRepository;
import com.sparta.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final EmbeddingService embeddingService;
    private final LinkRepository linkRepository;
    private final AiMetrics aiMetrics;

    private static final int SEARCH_TOP_K = 5;

    // 링크 임베딩 요청 (LinkService에서 저장 후 호출)
    public void embedLink(Link link) {
        embeddingService.embed(link);
    }

    // 유사 링크 검색
    public List<LinkResponse> searchSimilar(User user, String query) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(SEARCH_TOP_K)
                        .filterExpression("userId == '" + user.getId() + "'")
                        .build()
        );

        return docs.stream()
                .map(doc -> {
                    Long linkId = Long.valueOf((String) doc.getMetadata().get("linkId"));
                    return linkRepository.findById(linkId)
                            .map(LinkResponse::from)
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    // RAG 질의응답 — 내 저장글 기반으로 질문
    public AskResponse ask(User user, String question) {
        return aiMetrics.recordAskLatency(() -> {
        // 1. 유사 문서 검색
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(SEARCH_TOP_K)
                        .build()
        );

        if (docs.isEmpty()) {
            return new AskResponse("저장된 링크에서 관련 내용을 찾지 못했습니다.", List.of());
        }

        // 2. 컨텍스트 구성
        String context = docs.stream()
                .map(doc -> "[" + doc.getMetadata().get("title") + "]\n" + doc.getText())
                .collect(Collectors.joining("\n\n---\n\n"));

        // 3. GPT 호출
        String answer = chatClient.prompt()
                .system("""
                        당신은 개발자의 저장된 글을 기반으로 질문에 답변하는 어시스턴트입니다
                        반드시 제공된 컨텍스트 내에서만 답변하세요
                        컨텍스트에 없는 내용은 "저장된 글에서 찾을 수 없습니다"라고 답하세요
                        """)
                .user("컨텍스트:\n" + context + "\n\n질문: " + question)
                .call()
                .content();

        // 4. 참조 링크 추출
        List<LinkResponse> references = docs.stream()
                .map(doc -> {
                    Long linkId = Long.valueOf((String) doc.getMetadata().get("linkId"));
                    return linkRepository.findById(linkId)
                            .map(LinkResponse::from)
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return new AskResponse(answer, references);
        });
    }
}
