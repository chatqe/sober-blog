package io.charqe.qs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.charqe.qs.dao.LikeMapper;
import io.charqe.qs.model.likerecord.pojo.LikeRecord;
import io.charqe.qs.service.LikeRecordService;
import org.springframework.stereotype.Service;

/**
 * 点赞记录表，用于存储用户对文章、评论、视频等的点赞信息(Like)表服务实现类
 *
 * @author shenjiaqi
 * @since 2025-03-21 14:37:09
 */
@Service
public class LikeRecordServiceImpl extends ServiceImpl<LikeMapper, LikeRecord> implements LikeRecordService {

}



