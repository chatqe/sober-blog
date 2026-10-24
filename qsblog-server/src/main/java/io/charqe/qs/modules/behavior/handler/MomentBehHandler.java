package io.charqe.qs.modules.behavior.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.modules.moment.domain.dao.MomentMapper;
import io.charqe.qs.modules.moment.domain.pojo.Moment;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 微言/动态实体行为处理器
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Component
public class MomentBehHandler implements BehEntityHandler {

    @Resource
    private MomentMapper momentMapper;

    private static final String COLUMN_LIKES = "likes_count";
    private static final String COLUMN_VIEWS = "views_count";
    private static final String COLUMN_COMMENTS = "comments_count";

    @Override
    public boolean supports(Integer bizType) {
        return BizTypeEnum.MOMENT.getCode().equals(bizType);
    }

    @Override
    public void validate(Long bizId) {
        if (bizId == null) {
            throw new BizException("动态ID不能为空");
        }
        Moment moment = momentMapper.selectById(bizId);
        if (moment == null) {
            throw new BizException("动态不存在");
        }
    }

    /**
     * 更新动态冗余计数（正向行为）
     *
     * @param bizId  动态ID
     * @param userId 操作人ID
     * @param action 行为编码
     */
    @Override
    public void afterAction(Long bizId, Long userId, Integer action) {
        if (bizId == null || action == null) { return; }
        if (BehActionType.LIKE.getCode().equals(action)) {
            incrementMomentCount(bizId, COLUMN_LIKES);
        } else if (BehActionType.VIEW.getCode().equals(action)) {
            incrementMomentCount(bizId, COLUMN_VIEWS);
        } else if (BehActionType.COMMENT.getCode().equals(action)) {
            incrementMomentCount(bizId, COLUMN_COMMENTS);
        }
    }

    /**
     * 更新动态冗余计数（取消行为）
     *
     * @param bizId  动态ID
     * @param userId 操作人ID
     * @param action 正向行为编码（用于确定回退哪个计数字段）
     */
    @Override
    public void afterCancelAction(Long bizId, Long userId, Integer action) {
        if (bizId == null || action == null) { return; }
        if (BehActionType.LIKE.getCode().equals(action)) {
            decrementMomentCount(bizId, COLUMN_LIKES);
        } else if (BehActionType.VIEW.getCode().equals(action)) {
            decrementMomentCount(bizId, COLUMN_VIEWS);
        } else if (BehActionType.COMMENT.getCode().equals(action)) {
            decrementMomentCount(bizId, COLUMN_COMMENTS);
        }
    }

    private void incrementMomentCount(Long momentId, String column) {
        momentMapper.update(null, new LambdaUpdateWrapper<Moment>()
                .eq(Moment::getId, momentId)
                .setSql(column + " = " + column + " + 1"));
    }

    private void decrementMomentCount(Long momentId, String column) {
        momentMapper.update(null, new LambdaUpdateWrapper<Moment>()
                .eq(Moment::getId, momentId)
                .setSql(column + " = GREATEST(" + column + " - 1, 0)"));
    }
}
