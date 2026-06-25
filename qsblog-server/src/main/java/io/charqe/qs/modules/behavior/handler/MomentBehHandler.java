package io.charqe.qs.modules.behavior.handler;

import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.modules.moment.domain.dao.MomentMapper;
import io.charqe.qs.modules.moment.domain.pojo.Moment;
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

    @Override
    public boolean supports(Integer bizType) {
        return BizTypeEnum.MOMENT.code().equals(bizType);
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
}
