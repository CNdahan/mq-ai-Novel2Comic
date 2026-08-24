package com.mq.novel2comic.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring AI 配置类
 * 配置ChatClient对象池、EmbeddingModel和VectorStore
 */
@Configuration
public class SpringAIConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /**
     * 自定义 RestClient.Builder，配置超时时间
     * 解决 LLM API 调用超时问题
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(30));   // 连接超时：30秒
        requestFactory.setReadTimeout(Duration.ofMinutes(5));       // 读取超时：5分钟（小说解析等长任务需要更长时间）
        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    /**
     * ChatClient对象池
     * 为不同任务创建专用的ChatClient实例
     * 借鉴需求文档3.1.1节的设计
     */
    @Bean
    public ConcurrentHashMap<String, ChatClient> chatClientPool(ChatClient.Builder chatClientBuilder) {
        ConcurrentHashMap<String, ChatClient> pool = new ConcurrentHashMap<>();

        // 小说解析专用ChatClient
        pool.put("novel_parser", createChatClient(chatClientBuilder,
                "你是一个专业的小说分析专家，擅长提取小说的结构、角色和场景信息。" +
                        "你能准确识别小说中的主要角色、场景变化、情节发展，并以结构化的JSON格式输出分析结果。"));

        // 角色提取专用ChatClient
        pool.put("character_extractor", createChatClient(chatClientBuilder,
                "你是一个角色特征提取专家，擅长从文本中提取角色的详细外貌特征、性格特点和背景信息。" +
                        "你能准确识别角色的发型、发色、身高、体型、着装风格等视觉特征，并以适合AI绘画的英文描述输出。"));

        // 场景分析专用ChatClient
        pool.put("scene_designer", createChatClient(chatClientBuilder,
                "你是一个漫画分镜设计师，擅长将文字场景转换为漫画分镜脚本。" +
                        "你能准确判断场景类型、选择合适的镜头类型（特写/中景/全景等），并生成适合漫画表现的场景描述。"));

        // Prompt工程专用ChatClient
        pool.put("prompt_engineer", createChatClient(chatClientBuilder,
                "你是一个AI绘画提示词工程师，擅长编写高质量的文生图Prompt。" +
                        "你能将场景描述转换为详细、准确的英文Prompt，确保生成的图片符合预期效果。"));

        return pool;
    }

    /**
     * 创建专用ChatClient
     * 设置系统提示词和默认参数
     */
    private ChatClient createChatClient(ChatClient.Builder builder, String systemPrompt) {
        return builder
                .defaultSystem(systemPrompt)
                .build();
    }

    /**
     * 注意：EmbeddingModel 和 VectorStore 配置
     * 
     * Spring AI Alibaba 会自动装配 EmbeddingModel，无需手动配置。
     * 
     * VectorStore（Milvus）集成说明：
     * 1. 如需使用向量数据库进行角色一致性RAG检索，需要额外添加依赖：
     *    <dependency>
     *        <groupId>org.springframework.ai</groupId>
     *        <artifactId>spring-ai-milvus-store</artifactId>
     *    </dependency>
     * 
     * 2. 添加配置后，创建 VectorStore Bean：
     *    @Bean
     *    @ConditionalOnClass(name = "org.springframework.ai.vectorstore.milvus.MilvusVectorStore")
     *    public VectorStore milvusVectorStore(EmbeddingModel embeddingModel) {
     *        return MilvusVectorStore.builder()
     *            .embeddingModel(embeddingModel)
     *            .host("localhost")
     *            .port(19530)
     *            .databaseName("novel2comic")
     *            .collectionName("character_profiles")
     *            .build();
     *    }
     * 
     * 3. 暂时不使用向量数据库，角色一致性功能可以通过数据库查询实现。
     */
}
