package io.charqe.qs.modules.category.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 分类表
 * </p>
 *
 * @author joy
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("category")
public class Category extends BaseDO {

    /**
     * 状态
     */
    private Integer status;
    /**
     * 是否展示（0:否 1:是）
     */
    private Integer isShow;

    /**
     * 分类类型: 0-导航栏分类, 1-普通分类
     */
    private  Integer type;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类描述
     */
    private String cateDesc;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 分类图标
     */
    private String icon;


    /**
     * 最终修改人
     */
    private String updateBy;

    private String createBy;


}
