package com.mq.novel2comic.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从分镜关联原文中提取适合放到漫画上的台词字幕。 */
public final class SubtitleTextUtils {

    private static final Pattern QUOTED_TEXT = Pattern.compile(
            "[\\\"“”「」『』‘’'“”]([^\\\"“”「」『』‘’'“”]{1,160})[\\\"“”「」『』‘’'“”]");
    private static final int MAX_SUBTITLE_LENGTH = 160;
    private static final Pattern SPEECH_CONTEXT = Pattern.compile(
            "(说|道|问|答|喊|叫|吼|哼|嘀咕|自言自语|心想|心中)");

    private SubtitleTextUtils() {
    }

    /**
     * 优先提取引号内的对话；无引号时只接受短的对话场景文本，避免把叙述段落当字幕。
     */
    public static String extract(String source, String sceneType) {
        if (source == null || source.isBlank()) {
            return "";
        }
        String text = source.replaceAll("\\s+", " ").trim();
        Matcher matcher = QUOTED_TEXT.matcher(text);
        List<String> dialogues = new ArrayList<>();
        while (matcher.find()) {
            String dialogue = clean(matcher.group(1));
            if (!dialogue.isBlank()) {
                dialogues.add(dialogue);
            }
        }
        if (!dialogues.isEmpty()) {
            return truncate(String.join("\n", dialogues));
        }
        if ("dialogue".equalsIgnoreCase(sceneType) && text.length() <= 80) {
            return truncate(text);
        }
        return "";
    }

    /**
     * 用分镜原文作为时间线锚点，把小说中被场景切分遗漏的对白补回对应分镜。
     */
    public static Map<Integer, String> assign(String novelText, List<PanelSource> panelSources) {
        Map<Integer, List<String>> linesByPanel = new LinkedHashMap<>();
        if (panelSources == null || panelSources.isEmpty()) {
            return Map.of();
        }
        List<PanelSource> sortedSources = panelSources.stream()
                .sorted(Comparator.comparingInt(PanelSource::panelIndex))
                .toList();
        for (PanelSource source : sortedSources) {
            List<String> lines = new ArrayList<>();
            String dialogue = extract(source.sourceText(), source.sceneType());
            if (!dialogue.isBlank()) {
                addLines(lines, dialogue);
            }
            linesByPanel.put(source.panelIndex(), lines);
        }

        String novel = normalize(novelText);
        if (!novel.isBlank()) {
            List<LocatedPanel> locatedPanels = locatePanels(novel, sortedSources);
            if (locatedPanels.size() >= 2 || sortedSources.size() == 1) {
                Matcher matcher = QUOTED_TEXT.matcher(novel);
                while (matcher.find()) {
                    String dialogue = clean(matcher.group(1));
                    if (!isLikelyDialogue(novel, matcher.start(), dialogue)) {
                        continue;
                    }
                    LocatedPanel target = findPrecedingPanel(locatedPanels, matcher.start());
                    if (target != null) {
                        addLine(linesByPanel.get(target.source().panelIndex()), dialogue);
                    }
                }
            }
        }

        Map<Integer, String> result = new LinkedHashMap<>();
        for (PanelSource source : sortedSources) {
            List<String> lines = linesByPanel.get(source.panelIndex());
            if (lines.isEmpty()) {
                // 没有对白时保留一段简短场景注释，避免图片看起来像缺少文字。
                addLine(lines, normalize(source.sourceText()));
            }
            result.put(source.panelIndex(), truncate(String.join("\n", lines)));
        }
        return result;
    }

    private static List<LocatedPanel> locatePanels(String novel, List<PanelSource> sources) {
        List<LocatedPanel> result = new ArrayList<>();
        int searchFrom = 0;
        for (PanelSource source : sources) {
            String panelText = normalize(source.sourceText());
            int position = findAnchor(novel, panelText, searchFrom);
            if (position >= 0) {
                result.add(new LocatedPanel(source, position));
                searchFrom = position + 1;
            }
        }
        return result;
    }

    private static int findAnchor(String novel, String panelText, int searchFrom) {
        if (panelText.isBlank()) {
            return -1;
        }
        int position = novel.indexOf(panelText, searchFrom);
        if (position >= 0) {
            return position;
        }
        // LLM 有时会轻微缩写原文，使用最长句段做回退锚点。
        return Pattern.compile("[。！？!?；;，,]")
                .splitAsStream(panelText)
                .map(String::trim)
                .filter(part -> part.length() >= 8)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .mapToInt(part -> novel.indexOf(part, searchFrom))
                .filter(index -> index >= 0)
                .findFirst()
                .orElse(-1);
    }

    private static LocatedPanel findPrecedingPanel(List<LocatedPanel> panels, int dialoguePosition) {
        LocatedPanel target = panels.get(0);
        for (LocatedPanel panel : panels) {
            if (panel.position() > dialoguePosition) {
                break;
            }
            target = panel;
        }
        return target;
    }

    private static boolean isLikelyDialogue(String novel, int quoteStart, String dialogue) {
        if (dialogue.isBlank()) {
            return false;
        }
        if (dialogue.length() >= 7 || dialogue.matches(".*[。！？!?…]$")) {
            return true;
        }
        String context = novel.substring(Math.max(0, quoteStart - 12), quoteStart);
        return SPEECH_CONTEXT.matcher(context).find();
    }

    private static void addLines(List<String> lines, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String line : text.split("\\R")) {
            addLine(lines, line);
        }
    }

    private static void addLine(List<String> lines, String line) {
        String cleaned = clean(line);
        if (!cleaned.isBlank() && !lines.contains(cleaned)) {
            lines.add(cleaned);
        }
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private static String clean(String text) {
        return text.replaceAll("^[：:、，,\\s]+", "").trim();
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_SUBTITLE_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_SUBTITLE_LENGTH - 1).trim() + "…";
    }

    public record PanelSource(int panelIndex, String sourceText, String sceneType) {
    }

    private record LocatedPanel(PanelSource source, int position) {
    }
}
