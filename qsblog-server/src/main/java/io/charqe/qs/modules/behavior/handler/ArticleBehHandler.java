package io.charqe.qs.modules.behavior.handler;

import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.article.domain.dao.ArticleMapper;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
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
        // 文章点赞后可清除缓存，让文章列表刷新点赞数
        log.debug("文章 {} 行为后置处理完成", bizId);
    }
}
