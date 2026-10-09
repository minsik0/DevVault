package com.sparta.ai.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiMetrics {

    private final MeterRegistry meterRegistry;

    // RAG 질의응답 응답 시간 측정
    public <T> T recordAskLatency(Supplier<T> supplier) {
        return Timer.builder("devvault.ai.ask.latency")
                .description("RAG 질의응답 응답 시간")
                .tag("type", "rag")
                .register(meterRegistry)
                .record(supplier);
    }

    // 임베딩 처리 시간 측정
    public void recordEmbedLatency(Runnable runnable) {
        Timer.builder("devvault.ai.embed.latency")
                .description("임베딩 처리 시간")
                .tag("type", "embed")
                .register(meterRegistry)
                .record(runnable);
    }

    // 요약 처리 시간 측정
    public <T> T recordSummaryLatency(Supplier<T> supplier) {
        return Timer.builder("devvault.ai.summary.latency")
                .description("AI 요약 처리 시간")
                .tag("type", "summary")
                .register(meterRegistry)
                .record(supplier);
    }
}
