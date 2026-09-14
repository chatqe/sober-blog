package io.charqe.qs.modules.behavior.handler;

import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.article.domain.dao.ArticleMapper;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorCnt;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.service.BehaviorCntService;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.common.model.mp.Lb;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 文章实体行为处理器
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Component
public class ArticleBehHandler implements BehEntityHandler {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private BehaviorCntService behCntService;

    @Override
    public boolean supports(Integer bizType) {
        return BizTypeEnum.ARTICLE.code().equals(bizType);
    }

    @Override
    public void validate(Long bizId) {
        if (bizId == null) {
            throw new BizException("文章ID不能为空");
        }
        Article article = articleMapper.selectById(bizId);
        if (article == null) {
            throw new BizException("文章不存在");
        }
    }

    @Override
    public void afterAction(Long bizId, Long userId) {
        // 同步更新 article.like_count，确保列表页显示正确
        BehaviorCnt likeCnt = behCntService.getOne(Lb.<BehaviorCnt>q()
                .eq(BehaviorCnt::getBizId, bizId)
                .eq(BehaviorCnt::getBizType, BizTypeEnum.ARTICLE.code())
                .eq(BehaviorCnt::getAction, BehActionType.LIKE.code()));
        if (likeCnt != null && likeCnt.getCount() != null) {
            articleMapper.updateLikeCount(bizId, likeCnt.getCount());
        }
        log.debug("文章 {} 行为后置处理完成", bizId);
    }

    @Override
    public void afterCancelAction(Long bizId, Long userId) {
        // 取消点赞后同步更新 article.like_count
        BehaviorCnt likeCnt = behCntService.getOne(Lb.<BehaviorCnt>q()
                .eq(BehaviorCnt::getBizId, bizId)
                .eq(BehaviorCnt::getBizType, BizTypeEnum.ARTICLE.code())
                .eq(BehaviorCnt::getAction, BehActionType.LIKE.code()));
        if (likeCnt != null && likeCnt.getCount() != null) {
            articleMapper.updateLikeCount(bizId, likeCnt.getCount());
        }
        log.debug("文章 {} 取消行为后置处理完成", bizId);
    }
}
