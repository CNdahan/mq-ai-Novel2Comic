package com.mq.novel2comic.utils;

/**
 * Prompt 模板工具类
 */
public class PromptTemplateUtils {

    /**
     * 小说解析 Prompt
     */
    public static String buildParsePrompt(String text) {
        return String.format("""
                你是一个专业的小说分析助手。请仔细分析以下小说文本，并提取关键信息。
                
                小说文本：
                %s
                
                请按照以下 JSON 格式返回分析结果：
                {
                  "title": "小说标题（如果文本中没有明确标题，请根据内容生成一个简短标题）",
                  "summary": "故事概要（100-200字）",
                  "theme": "故事主题（一句话概括）",
                  "characters": [
                    {
                      "name": "角色名称",
                      "description": "角色描述",
                      "appearance": ["外貌特征1", "外貌特征2"],
                      "personality": ["性格特点1", "性格特点2"],
                      "role": "角色类型（protagonist/supporting/minor）"
                    }
                  ],
                  "scenes": [
                    {
                      "sceneNumber": 1,
                      "content": "场景原文内容",
                      "location": "场景地点",
                      "time": "场景时间",
                      "characters": ["参与角色1", "参与角色2"],
                      "description": "场景描述（适合转换为漫画的场景描述）"
                    }
                  ]
                }
                
                注意：
                1. 确保返回的是标准 JSON 格式
                2. 角色的外貌和性格特征要具体明确
                3. 场景描述要适合转换为漫画画面
                4. 场景划分要合理，每个场景是一个相对独立的情节单元
                """, text);
    }

    /**
     * 角色提取 Prompt
     */
    public static String buildCharacterExtractPrompt(String text) {
        return String.format("""
                你是一个角色分析专家。请从以下小说文本中识别并提取所有角色信息。
                
                小说文本：
                %s
                
                请按照以下 JSON 格式返回角色列表：
                [
                  {
                    "name": "角色名称",
                    "description": "角色描述（包括身份、背景等）",
                    "appearance": ["外貌特征1", "外貌特征2", "外貌特征3"],
                    "personality": ["性格特点1", "性格特点2", "性格特点3"],
                    "role": "角色类型（protagonist主角/supporting配角/minor次要角色）"
                  }
                ]
                
                要求：
                1. 识别所有有名字或明确身份的角色
                2. 外貌特征要详细具体（发型、发色、身高、体型、着装等）
                3. 性格特点要准确（从行为和对话中推断）
                4. 按照角色重要性排序
                5. 确保返回标准 JSON 格式
                """, text);
    }

    /**
     * 场景分析 Prompt
     */
    public static String buildSceneAnalysisPrompt(String text) {
        return String.format("""
                你是一个场景分析专家。请将以下小说文本拆分为多个独立的场景。
                
                小说文本：
                %s
                
                请按照以下 JSON 格式返回场景列表：
                [
                  {
                    "sceneNumber": 1,
                    "content": "场景原文内容",
                    "location": "场景地点（具体的地点描述）",
                    "time": "场景时间（上午/下午/晚上/夜晚等）",
                    "characters": ["参与角色1", "参与角色2"],
                    "description": "场景的视觉化描述（适合转换为漫画画面，包括环境、人物动作、表情等）"
                  }
                ]
                
                场景划分原则：
                1. 时间或地点发生明显变化时，划分为新场景
                2. 人物关系或情节有重大转折时，划分为新场景
                3. 每个场景应该是一个相对完整的叙事单元
                4. 场景描述要详细，包含足够的视觉细节
                5. 确保返回标准 JSON 格式
                """, text);
    }
}
