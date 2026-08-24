package com.mq.novel2comic.mapper;

import com.mq.novel2comic.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author MQ
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2025-10-20 21:11:45
* @Entity com.mq.novel2comic.model.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




