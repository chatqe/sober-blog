package io.charqe.qs.modules.weblinks.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 友链表实体类
 *
 * @author Joy
 * @since 2026-01-16 21:55
 */

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("weblinks")
public class Weblinks extends BaseDO {

    /**
     * 标题
     */
    private String title;
    /**
     * 分类
     */
    private String classify;

    /**
     * 友链地址
     */
    private String url;

    /**
     * 友链头像
     */
    private String cover;

    /**
     * 友链描述
     */
    private String desc;
    /**
     * 备注
     */
    private String remark;

    /**
     * 资源类型
     */
    private String type;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态[0:未审核，1:已审核]
     */
    private Boolean status;

    /**
     * 最终修改人
     */
    private String updateBy;

}
