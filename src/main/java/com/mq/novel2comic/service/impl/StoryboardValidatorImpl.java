package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.model.dto.novel.StoryboardPanel;
import com.mq.novel2comic.model.dto.novel.ValidationResult;
import com.mq.novel2comic.service.StoryboardValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 分镜验证器实现
 * 参考需求文档功能3.4：分镜逻辑验证
 * 
 * @author MQ
 */
@Slf4j
@Service
public class StoryboardValidatorImpl implements StoryboardValidator {

    @Override
    public ValidationResult validate(List<StoryboardPanel> panels) {
        if (panels == null || panels.isEmpty()) {
            return ValidationResult.error("分镜列表为空");
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 执行各项验证
        ValidationResult countResult = validatePanelCount(panels);
        warnings.addAll(countResult.getWarnings());
        errors.addAll(countResult.getErrors());

        ValidationResult dialogueResult = validateDialogueScenes(panels);
        warnings.addAll(dialogueResult.getWarnings());
        errors.addAll(dialogueResult.getErrors());

        ValidationResult shotResult = validateShotTypeVariety(panels);
        warnings.addAll(shotResult.getWarnings());
        errors.addAll(shotResult.getErrors());

        ValidationResult climaxResult = validateClimaxScenes(panels);
        warnings.addAll(climaxResult.getWarnings());
        errors.addAll(climaxResult.getErrors());

        // 汇总结果
        boolean isValid = errors.isEmpty();
        String message = isValid ? "验证通过" : "验证失败";

        if (!warnings.isEmpty()) {
            log.warn("分镜验证警告: {}", String.join("; ", warnings));
        }
        if (!errors.isEmpty()) {
            log.error("分镜验证错误: {}", String.join("; ", errors));
        }

        return ValidationResult.builder()
                .valid(isValid)
                .message(message)
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    @Override
    public ValidationResult validatePanelCount(List<StoryboardPanel> panels) {
        ValidationResult result = ValidationResult.success();
        int count = panels.size();

        // 规则1：分镜数量应在8-12个之间
        if (count < 8) {
            result.addWarning("分镜数量偏少（" + count + "个），建议增加到8个以上以充分表现故事情节");
        } else if (count > 12) {
            result.addWarning("分镜数量过多（" + count + "个），建议控制在12个以内以保持节奏紧凑");
        } else {
            log.debug("分镜数量合理: {} 个", count);
        }

        return result;
    }

    @Override
    public ValidationResult validateDialogueScenes(List<StoryboardPanel> panels) {
        ValidationResult result = ValidationResult.success();

        // 规则2：对话场景必须包含角色
        for (int i = 0; i < panels.size(); i++) {
            StoryboardPanel panel = panels.get(i);
            if ("dialogue".equals(panel.getSceneType())) {
                if (panel.getCharacters() == null || panel.getCharacters().isEmpty()) {
                    result.addError("分镜 " + (i + 1) + " 为对话场景但缺少角色信息");
                }
            }
        }

        return result;
    }

    @Override
    public ValidationResult validateShotTypeVariety(List<StoryboardPanel> panels) {
        ValidationResult result = ValidationResult.success();

        // 规则3：避免连续3个相同镜头类型
        for (int i = 0; i < panels.size() - 2; i++) {
            String shot1 = panels.get(i).getShotType();
            String shot2 = panels.get(i + 1).getShotType();
            String shot3 = panels.get(i + 2).getShotType();

            if (shot1 != null && shot1.equals(shot2) && shot1.equals(shot3)) {
                result.addWarning(
                    "分镜 " + (i + 1) + "-" + (i + 3) + " 镜头类型过于单一（" + shot1 + "），" +
                    "建议增加镜头变化以提升视觉效果"
                );
            }
        }

        return result;
    }

    @Override
    public ValidationResult validateClimaxScenes(List<StoryboardPanel> panels) {
        ValidationResult result = ValidationResult.success();

        // 规则4：情绪高潮应该用特写或特殊角度
        for (int i = 0; i < panels.size(); i++) {
            StoryboardPanel panel = panels.get(i);
            
            // 判断是否为高潮场景
            boolean isClimax = "climax".equals(panel.getSceneType()) || 
                              "emotion".equals(panel.getSceneType());
            
            if (isClimax && "full".equals(panel.getShotType())) {
                result.addWarning(
                    "分镜 " + (i + 1) + " 为高潮场景，建议使用特写（close-up）或特殊角度" +
                    "（仰视/俯视）以强化情绪表达"
                );
            }
        }

        return result;
    }
}

