package io.charqe.qs.modules.like.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.charqe.qs.modules.like.domain.dao.LikeMapper;
import io.charqe.qs.modules.like.domain.pojo.Like;
import org.springframework.stereotype.Service;

/**
 * 点赞记录表，用于存储用户对文章、评论、视频等的点赞信息(Like)表服务实现类
 *
 * @author shenjiaqi
 * @since 2025-03-21 14:37:09
 */
@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

}



