package com.mq.novel2comic.utils;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * 文本清洗工具类
 */
public class TextCleanUtils {

    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\\n{3,}");

    /**
     * 清洗文本
     */
    public static String clean(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        // 去除控制字符
        text = SPECIAL_CHARS.matcher(text).replaceAll("");
        // 统一换行符
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        // 去除多余空格（保留单个空格）
        text = MULTIPLE_SPACES.matcher(text).replaceAll(" ");
        // 去除多余换行（最多保留2个换行）
        text = MULTIPLE_NEWLINES.matcher(text).replaceAll("\n\n");
        // 去除首尾空白
        return text.trim();
    }

    /**
     * 简化清洗（仅去除首尾空白和多余空格）
     */
    public static String cleanSimple(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return MULTIPLE_SPACES.matcher(text.trim()).replaceAll(" ");
    }
}
