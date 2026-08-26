package com.mq.novel2comic.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubtitleTextUtilsTest {

    @Test
    void extractsQuotedDialogueAndKeepsMultipleLines() {
        assertEquals("早上好！\n快走吧。", SubtitleTextUtils.extract(
                "林夕说：“早上好！”顾言回答：\"快走吧。\"", "dialogue"));
    }

    @Test
    void doesNotUseLongNarrationAsSubtitle() {
        String narration = "一段很长的场景叙述，没有任何角色说话，应该保留为空，避免把整段小说内容覆盖到漫画图片上。";
        assertEquals("", SubtitleTextUtils.extract(narration, "climax"));
    }

    @Test
    void acceptsShortUnquotedDialogueScene() {
        assertEquals("快躲开！", SubtitleTextUtils.extract("快躲开！", "dialogue"));
    }

    @Test
    void usesStoryboardNarrationAsCaptionWhenThereIsNoDialogue() {
        List<SubtitleTextUtils.PanelSource> panels = List.of(
                new SubtitleTextUtils.PanelSource(1, "钻探机车内，易水寒意识逐渐模糊，左半身和后背都是伤。", "climax"));

        Map<Integer, String> captions = SubtitleTextUtils.assign("", panels);

        assertEquals("钻探机车内，易水寒意识逐渐模糊，左半身和后背都是伤。", captions.get(1));
    }

    @Test
    void assignsNovelDialoguesToThePrecedingStoryboardAnchor() {
        String novel = "“人类是宇宙的强力蛀虫”。风暴吞噬着整个天地。"
                + "易水寒喊道：“预测说今天是晴天！”六十五米的距离仿佛天堑。"
                + "“娘的，要死在沙子里吗？”他终于进入钻探机，身受重伤。"
                + "醒来后看见仪表异常。两个月前母亲离世，只得到很“和平”的处理和"
                + "一场“不必要”的官司。他自言自语道：“老妈，这回儿子也能去陪您了。”"
                + "钻探机突然剧烈震动，他惊讶叫道：“什么，怎么可能？”";
        List<SubtitleTextUtils.PanelSource> panels = List.of(
                new SubtitleTextUtils.PanelSource(1, "风暴吞噬着整个天地。", "climax"),
                new SubtitleTextUtils.PanelSource(2, "六十五米的距离仿佛天堑。", "climax"),
                new SubtitleTextUtils.PanelSource(3, "他终于进入钻探机，身受重伤。", "climax"),
                new SubtitleTextUtils.PanelSource(4, "醒来后看见仪表异常。", "climax"),
                new SubtitleTextUtils.PanelSource(5, "两个月前母亲离世", "climax"),
                new SubtitleTextUtils.PanelSource(6, "钻探机突然剧烈震动", "climax"));

        Map<Integer, String> subtitles = SubtitleTextUtils.assign(novel, panels);

        assertEquals("人类是宇宙的强力蛀虫\n预测说今天是晴天！", subtitles.get(1));
        assertEquals("娘的，要死在沙子里吗？", subtitles.get(2));
        assertEquals("他终于进入钻探机，身受重伤。", subtitles.get(3));
        assertEquals("醒来后看见仪表异常。", subtitles.get(4));
        assertEquals("老妈，这回儿子也能去陪您了。", subtitles.get(5));
        assertEquals("什么，怎么可能？", subtitles.get(6));
    }
}
