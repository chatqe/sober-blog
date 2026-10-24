package io.charqe.qs.modules.behavior.handler;

import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.comment.domain.dao.CommentMapper;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 评论实体行为处理器
 * 仅支持 BizTypeEnum.COMMENT
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Component
public class CommentBehHandler implements BehEntityHandler {

    @Resource
    private CommentMapper commentMapper;

    // 只接管「评论实体」：BizTypeEnum.ARTICLE(1)/MOMENT(2) 已分别由 Article/Moment 处理器负责，
    // 若在此重复声明会因工厂按 bean 名顺序取首个命中者，导致给文章/动态点赞时误用评论表校验。
    private static final Set<Integer> SUPPORTED_TYPES = Set.of(BizTypeEnum.COMMENT.getCode());

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
