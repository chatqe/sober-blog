package io.charqe.qs.modules.sort.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.charqe.qs.modules.label.domain.pojo.Label;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 分类
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sort")
public class Sort implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @NotNull       // ② 更新校验
    @JsonFormat(shape = JsonFormat.Shape.STRING) // ③ 序列化防精度丢失
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 分类名称
     */
    @TableField("sort_name")
    private String sortName;

    /**
     * 分类描述
     */
    @TableField("sort_description")
    private String sortDescription;

    /**
     * 分类类型[0:导航栏分类，1:普通分类]
     */
    @TableField("sort_type")
    private Integer sortType;

    /**
     * 分类优先级：数字小的在前面
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 是否展示 0否1真
     */
    @TableField("status")
    private Integer status;

    @TableField(exist = false)
    private Integer countOfSort;

    @TableField(exist = false)
    private List<Label> labels;
}

