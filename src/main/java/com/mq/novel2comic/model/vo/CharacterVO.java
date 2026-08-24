package com.mq.novel2comic.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 角色信息VO（用于前端展示）
 * @author MQ
 */
@Data
public class CharacterVO {
    
    /**
     * 角色ID
     */
    private Long characterId;
    
    /**
     * 小说ID
     */
    private Long novelId;
    
    /**
     * 角色名称
     */
    private String characterName;
    
    /**
     * 中文描述
     */
    private String descriptionCn;
    
    /**
     * 英文描述
     */
    private String descriptionEn;
    
    /**
     * 外貌特征数据
     */
    private Object appearanceData;
    
    /**
     * 参考图URL
     */
    private String referenceImageUrl;
    
    /**
     * 使用次数
     */
    private Integer useCount;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}

