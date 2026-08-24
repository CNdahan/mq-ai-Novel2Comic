package com.mq.novel2comic;

import com.mq.novel2comic.model.dto.novel.CharacterInfo;
import com.mq.novel2comic.model.dto.novel.NovelStructure;
import com.mq.novel2comic.service.CharacterExtractService;
import com.mq.novel2comic.service.NovelParseService;
import com.mq.novel2comic.service.StoryboardService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
public class LLMIntegrationTest {

    @Resource
    private NovelParseService novelParseService;

    @Resource
    private CharacterExtractService characterExtractService;

    @Resource
    private StoryboardService storyboardService;

    @Test
    public void testNovelParse() {
        String testText = """
                李明站在窗前，看着外面的雨。他是一个25岁的年轻人，黑色短发，身材修长。
                突然，门铃响了。他打开门，看到了久违的朋友王芳。王芳今年23岁，长发飘飘，笑容甜美。
                """;

        try {
            NovelStructure structure = novelParseService.parse(testText);
            log.info("解析成功！角色数量：{}", structure.getCharacters().size());
            log.info("场景数量：{}", structure.getScenes().size());
            assert structure.getCharacters().size() > 0;
            assert structure.getScenes().size() > 0;
        } catch (Exception e) {
            log.error("测试失败", e);
            throw e;
        }
    }

    @Test
    public void testCharacterExtract() {
        String testText = """
                李明是一个25岁的年轻人，黑色短发，深邃的黑眸，身高180cm，修长身材。
                他常常穿白色衬衫和黑色西裤，给人一种冷峻高贵的感觉。
                """;

        try {
            List<CharacterInfo> characters = characterExtractService.extract(testText);
            log.info("提取成功！角色数量：{}", characters.size());
            assert characters.size() > 0;
        } catch (Exception e) {
            log.error("测试失败", e);
            throw e;
        }
    }
}