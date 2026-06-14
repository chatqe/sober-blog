package io.charqe.qs.modules.behavior.service.handler;

import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.comment.domain.dao.CommentMapper;
import io.charqe.qs.modules.comment.enums.CommentTypeEnum;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 评论实体行为处理器
 * 支持 BizTypeEnum.COMMENT 和 CommentTypeEnum 各类型
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Component
public class CommentBehHandler implements BehEntityHandler {

    @Resource
    private CommentMapper commentMapper;

    // 注意：BizTypeEnum.COMMENT(2) 和 CommentTypeEnum.MESSAGE(2) 值相同，用 HashSet 避免 Set.of() 去重报错
    private static final Set<Integer> SUPPORTED_TYPES = new HashSet<>(Arrays.asList(
            BizTypeEnum.COMMENT.code(),
            CommentTypeEnum.ARTICLE.code(),
            CommentTypeEnum.MESSAGE.code(),
            CommentTypeEnum.LOVE.code()
    ));

    @Override
    public boolean supports(Integer bizType) {
        return SUPPORTED_TYPES.contains(bizType);
    }

    @Override
    public void validate(Long bizId) {
        if (bizId == null) {
            throw new BizException("评论ID不能为空");
        }
        if (commentMapper.selectById(bizId) == null) {
            throw new BizException("评论不存在");
        }
    }
}
