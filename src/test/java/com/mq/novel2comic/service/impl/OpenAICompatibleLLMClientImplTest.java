package com.mq.novel2comic.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mq.novel2comic.model.dto.ai.AiConfig;
import com.mq.novel2comic.service.AiConfigService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAICompatibleLLMClientImplTest {

    @Test
    void chatRetriesTransient502AndReturnsTheSuccessfulResponse() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = startServer(requestCount, attempt -> attempt == 1 ? 502 : 200);
        try {
            OpenAICompatibleLLMClientImpl client = clientFor(server);

            assertEquals("[]", client.chat("prompt", "system"));
            assertEquals(2, requestCount.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void chatStopsAfterThreeTransientFailures() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        HttpServer server = startServer(requestCount, attempt -> 502);
        try {
            OpenAICompatibleLLMClientImpl client = clientFor(server);

            assertThrows(RuntimeException.class, () -> client.chat("prompt", "system"));
            assertEquals(3, requestCount.get());
        } finally {
            server.stop(0);
        }
    }

    private OpenAICompatibleLLMClientImpl clientFor(HttpServer server) {
        OpenAICompatibleLLMClientImpl client = new OpenAICompatibleLLMClientImpl();
        ReflectionTestUtils.setField(client, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(client, "aiConfigService", configService(
                "http://localhost:" + server.getAddress().getPort() + "/v1"));
        return client;
    }

    private HttpServer startServer(AtomicInteger requestCount, StatusProvider statusProvider) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            int attempt = requestCount.incrementAndGet();
            int status = statusProvider.status(attempt);
            String body = status == 200
                    ? "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"[]\"}}]}"
                    : "{\"error\":{\"message\":\"Upstream gateway error\"}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    private AiConfigService configService(String baseUrl) {
        AiConfig config = AiConfig.builder()
                .provider("openai")
                .apiKey("test-key")
                .model("gpt-test")
                .baseUrl(baseUrl)
                .build();
        return new AiConfigService() {
            public AiConfig getConfig() { return config; }
            public AiConfig saveConfig(AiConfig ignored) { return config; }
            public boolean clearConfig() { return true; }
            public boolean hasStoredConfig() { return true; }
        };
    }

    @FunctionalInterface
    private interface StatusProvider {
        int status(int attempt);
    }
}
