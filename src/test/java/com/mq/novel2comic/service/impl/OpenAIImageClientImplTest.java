package com.mq.novel2comic.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mq.novel2comic.model.dto.ai.AigcConfig;
import com.mq.novel2comic.service.AigcConfigService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAIImageClientImplTest {

    @Test
    void generateImageRetriesOneTransientUpstreamFailureAndUsesConfiguredTwoKSize() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        AtomicReference<String> lastRequestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/images/generations", exchange -> {
            int attempt = requestCount.incrementAndGet();
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int status = attempt == 1 ? 500 : 200;
            String body = attempt == 1
                    ? "{\"error\":{\"message\":\"Upstream gateway error\"}}"
                    : "{\"data\":[{\"url\":\"https://example.com/image.png\"}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            OpenAIImageClientImpl client = new OpenAIImageClientImpl();
            ReflectionTestUtils.setField(client, "objectMapper", new ObjectMapper());
            ReflectionTestUtils.setField(client, "aigcConfigService", configService(
                    "http://localhost:" + server.getAddress().getPort() + "/v1/images/generations"
            ));

            assertEquals("https://example.com/image.png", client.generateImage("prompt", "", "2048x2048"));
            assertEquals(2, requestCount.get());
            assertEquals(1, countOccurrences(lastRequestBody.get(), "\"size\":\"2048x2048\""));
        } finally {
            server.stop(0);
        }
    }

    private int countOccurrences(String value, String expected) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(expected, index)) >= 0) {
            count++;
            index += expected.length();
        }
        return count;
    }

    private AigcConfigService configService(String baseUrl) {
        AigcConfig config = AigcConfig.builder()
                .provider("openai")
                .apiKey("test-key")
                .model("gpt-image-2")
                .baseUrl(baseUrl)
                .resolution("2k")
                .build();
        return new AigcConfigService() {
            public AigcConfig getConfig() { return config; }
            public AigcConfig saveConfig(AigcConfig ignored) { return config; }
            public boolean clearConfig() { return true; }
            public boolean hasStoredConfig() { return true; }
        };
    }
}
