package io.charqe.qs.modules.behavior.handler;

import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.category.domain.dao.CategoryMapper;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.modules.tag.domain.dao.TagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 专栏/标签实体行为处理器
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Component
public class CategoryBehHandler implements BehEntityHandler {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private TagMapper tagMapper;

    private static final Set<Integer> SUPPORTED_TYPES = Set.of(
            BizTypeEnum.CATEGORY.code(),
            BizTypeEnum.TAG.code()
    );

    @Override
    public boolean supports(Integer bizType) {
        return SUPPORTED_TYPES.contains(bizType);
    }

    @Override
    public void validate(Long bizId) {
        if (bizId == null) {
            throw new BizException("专栏/标签ID不能为空");
        }
        // 先查 Category，查不到再查 Tag
        if (categoryMapper.selectById(bizId) == null && tagMapper.selectById(bizId) == null) {
            throw new BizException("专栏或标签不存在");
        }
    }
}
