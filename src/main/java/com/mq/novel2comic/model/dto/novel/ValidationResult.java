package com.mq.novel2comic.model.dto.novel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证结果DTO
 * 
 * @author MQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    /**
     * 是否验证通过
     */
    private boolean valid;
    
    /**
     * 验证消息
     */
    private String message;
    
    /**
     * 错误列表（阻塞性）
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    /**
     * 警告列表（非阻塞性）
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    /**
     * 创建成功结果
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .message("验证通过")
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static ValidationResult error(String message) {
        ValidationResult result = ValidationResult.builder()
                .valid(false)
                .message(message)
                .build();
        result.addError(message);
        return result;
    }
    
    /**
     * 添加错误
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.valid = false;
    }
    
    /**
     * 添加警告
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
    
    /**
     * 是否有错误
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    /**
     * 是否有警告
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}

