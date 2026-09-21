package io.charqe.qs.modules.tag.controller.web.vo;

import io.charqe.qs.modules.sysdict.controller.web.vo.DictVO;
import lombok.Data;

/**
 * 标签字典VO
 *
 * @author Joy
 * @since 2026-09-19 21:26
 */
@Data
public class TagDictVO extends DictVO {

    /**
     * 分类id
     */
    private Long cateId;
}
