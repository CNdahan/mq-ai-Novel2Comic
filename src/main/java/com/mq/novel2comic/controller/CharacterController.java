package com.mq.novel2comic.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.entity.CharacterProfile;
import com.mq.novel2comic.model.entity.Novel;
import com.mq.novel2comic.model.vo.CharacterVO;
import com.mq.novel2comic.service.CharacterConsistencyService;
import com.mq.novel2comic.service.NovelService;
import com.mq.novel2comic.service.CharacterProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色控制器
 * @author MQ
 */
@RestController
@RequestMapping("/character")
@Slf4j
public class CharacterController {

    @Resource
    private CharacterProfileService characterProfileService;

    @Resource
    private NovelService novelService;

    @Resource
    private CharacterConsistencyService characterConsistencyService;

    /**
     * 获取小说的所有角色
     */
    @GetMapping("/list/{novelId}")
    public BaseResponse<List<CharacterVO>> getCharactersByNovelId(@PathVariable Long novelId) {
        log.info("获取小说角色列表，novelId: {}", novelId);
        QueryWrapper<CharacterProfile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("novelId", novelId);
        queryWrapper.orderByAsc("createTime");
        List<CharacterProfile> characters = characterProfileService.list(queryWrapper);
        log.info("查询到{}个角色", characters.size());

        if (characters.isEmpty()) {
            Novel novel = novelService.getById(novelId);
            if (novel == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "小说不存在");
            }
            if ("failed".equals(novel.getStatus())) {
                String errorMessage = novel.getErrorMessage();
                if (errorMessage != null && errorMessage.startsWith("角色提取失败")) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
                }
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "角色提取失败：" + (errorMessage == null || errorMessage.isBlank() ? "请检查AI配置后重试" : errorMessage));
            }
            if ("pending".equals(novel.getStatus()) || "processing".equals(novel.getStatus())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "角色提取处理中，请稍后重试");
            }
        }

        // 转换为VO对象返回给前端
        List<CharacterVO> characterVOList = characters.stream().map(character -> {
            CharacterVO vo = new CharacterVO();
            BeanUtils.copyProperties(character, vo);
            vo.setCharacterId(character.getId()); // 映射 id -> characterId
            // 记录详细信息用于调试
            log.info("角色详情 - 名称: {}, ID: {}, descriptionCn长度: {}, descriptionEn长度: {}, appearanceData类型: {}", 
                character.getCharacterName(),
                character.getId(),
                character.getDescriptionCn() != null ? character.getDescriptionCn().length() : 0,
                character.getDescriptionEn() != null ? character.getDescriptionEn().length() : 0,
                character.getAppearanceData() != null ? character.getAppearanceData().getClass().getSimpleName() : "null"
            );
            return vo;
        }).collect(Collectors.toList());
        log.info("准备返回响应 - code: 200, message: success, data.size: {}", characterVOList.size());
        return ResultUtils.success(characterVOList);
    }

    /**
     * 根据ID获取角色详情
     */
    @GetMapping("/{id}")
    public BaseResponse<CharacterProfile> getCharacterById(@PathVariable Long id) {
        CharacterProfile character = characterProfileService.getById(id);
        return ResultUtils.success(character);
    }

    /**
     * 更新角色描述
     */
    @PutMapping("/{id}")
    public BaseResponse<Boolean> updateCharacter(@PathVariable Long id, @RequestBody CharacterProfile character) {
        character.setId(id);
        boolean result = characterProfileService.updateById(character);
        return ResultUtils.success(result);
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteCharacter(@PathVariable Long id) {
        CharacterProfile character = characterProfileService.getById(id);
        if (character == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "角色不存在");
        }
        boolean result = characterProfileService.removeById(id);
        if (result) {
            characterConsistencyService.clearCache(character.getNovelId(), character.getCharacterName());
            log.info("删除角色成功，id={}, novelId={}, characterName={}", id, character.getNovelId(), character.getCharacterName());
        }
        return ResultUtils.success(result, "角色删除成功");
    }
    
    /**
     * 测试接口 - 返回简化的角色列表
     */
    @GetMapping("/test/{novelId}")
    public BaseResponse<String> testGetCharacters(@PathVariable Long novelId) {
        log.info("测试接口：获取角色列表，novelId: {}", novelId);
        QueryWrapper<CharacterProfile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("novelId", novelId);
        List<CharacterProfile> characters = characterProfileService.list(queryWrapper);
        
        String result = String.format("查询到%d个角色：%s", 
            characters.size(),
            characters.stream()
                .map(CharacterProfile::getCharacterName)
                .collect(Collectors.joining(", "))
        );
        
        log.info("测试接口返回: {}", result);
        return ResultUtils.success(result);
    }
}
