package io.charqe.qs.modules.like.domain.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.charqe.qs.modules.like.domain.pojo.Like;


/**
 * 点赞记录表，用于存储用户对文章、评论、视频等的点赞信息(Like)表数据库访问层
 *
 * @author shenjiaqi
 * @since 2025-03-21 13:53:10
 */
public interface LikeMapper extends BaseMapper<Like> {

}



