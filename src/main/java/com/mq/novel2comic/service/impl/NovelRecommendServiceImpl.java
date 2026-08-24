package com.mq.novel2comic.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.mq.novel2comic.model.dto.novel.NovelRecommendDTO;
import com.mq.novel2comic.service.NovelRecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 小说推荐服务实现
 */
@Slf4j
@Service
public class NovelRecommendServiceImpl implements NovelRecommendService {

    // 测试用例文件路径
    private static final String TEST_CASE_FILE = "测试用例-小说上传.md";

    // 推荐语映射
    private static final Map<String, String> RECOMMENDATION_MAP = new HashMap<String, String>() {{
        put("修仙", "热血修仙，激情战斗场景");
        put("都市", "现代都市情感故事");
        put("科幻", "未来科技幻想冒险");
        put("武侠", "经典武侠恩怨情仇");
        put("悬疑", "烧脑推理悬疑剧情");
        put("历史", "穿越历史改变命运");
        put("校园", "青春校园甜蜜故事");
        put("游戏", "电竞竞技热血对决");
        put("末日", "末世求生惊险刺激");
        put("奇幻", "魔法冒险奇幻世界");
        put("商战", "商业谍战智慧博弈");
        put("灵异", "恐怖灵异惊悚体验");
        put("机甲", "机甲战争热血战斗");
        put("宫斗", "宫廷权谋步步惊心");
        put("推理", "侦探推理破案悬疑");
        put("军事", "军事特战动作场面");
        put("医疗", "医疗救援生死时速");
        put("音乐", "音乐梦想励志故事");
        put("美食", "美食竞技温情治愈");
        put("赛车", "极速赛车刺激体验");
        put("谍战", "谍战风云紧张刺激");
        put("职场", "职场奋斗励志成长");
        put("玄幻", "玄幻修真逆天改命");
        put("体育", "体育竞技热血青春");
        put("励志", "励志创业奋斗故事");
        put("犯罪", "卧底犯罪惊险刺激");
        put("冒险", "冒险寻宝刺激体验");
        put("古代", "古代传奇侠义故事");
        put("虚拟", "虚拟现实科幻悬疑");
        put("重生", "重生逆袭改变命运");
    }};

    @Override
    public List<NovelRecommendDTO> getDailyRecommendations() {
        List<NovelRecommendDTO> allNovels = parseTestCaseFile();
        if (allNovels.isEmpty()) {
            log.warn("未能解析到任何小说用例，返回空列表");
            return new ArrayList<>();
        }
        // 随机选择3篇小说
        List<NovelRecommendDTO> recommendations = new ArrayList<>();
        List<Integer> selectedIndices = new ArrayList<>();
        int count = Math.min(3, allNovels.size());
        while (selectedIndices.size() < count) {
            int index = RandomUtil.randomInt(allNovels.size());
            if (!selectedIndices.contains(index)) {
                selectedIndices.add(index);
                recommendations.add(allNovels.get(index));
            }
        }
        log.info("成功生成{}篇推荐小说", recommendations.size());
        return recommendations;
    }

    /**
     * 解析测试用例文件
     */
    private List<NovelRecommendDTO> parseTestCaseFile() {
        List<NovelRecommendDTO> novels = new ArrayList<>();
        try {
            File file = new File(TEST_CASE_FILE);
            if (!file.exists()) {
                log.error("测试用例文件不存在: {}", TEST_CASE_FILE);
                return novels;
            }
            String content = FileUtil.readUtf8String(file);
            // 使用正则表达式匹配JSON块
            Pattern pattern = Pattern.compile("```json\\s*\\{\\s*\"title\":\\s*\"([^\"]+)\"\\s*,\\s*\"content\":\\s*\"([^\"]+)\"\\s*,\\s*\"sourceType\":\\s*\"([^\"]+)\"\\s*}\\s*```", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String title = matcher.group(1);
                String novelContent = matcher.group(2);
                String sourceType = matcher.group(3);
                NovelRecommendDTO dto = new NovelRecommendDTO();
                dto.setTitle(title);
                dto.setContent(novelContent);
                dto.setSourceType(sourceType);
                dto.setCharacterCount(novelContent.length());
                // 设置推荐理由
                String recommendation = getRecommendation(sourceType);
                dto.setRecommendation(recommendation);
                novels.add(dto);
            }
            log.info("从测试用例文件中成功解析{}篇小说", novels.size());
        } catch (Exception e) {
            log.error("解析测试用例文件失败", e);
        }
        return novels;
    }

    /**
     * 获取推荐理由
     */
    private String getRecommendation(String sourceType) {
        if (sourceType == null) {
            return "精彩故事推荐";
        }
        // 提取第一个标签
        String[] tags = sourceType.split(",");
        String firstTag = tags[0].trim();
        return RECOMMENDATION_MAP.getOrDefault(firstTag, "精彩故事，值得一读");
    }
}

