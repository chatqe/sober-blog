package io.charqe.qs.common.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应结果
 * 分页结果统一返回类
 * 注：兼容HTTP响应、RPC响应，支持空数据默认返回空列表
 *
 * @author sjq
 * @since 2025-11-20 20:52
 */
@Data
@Schema(description = "分页结果统一返回类")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据列表")
    private List<T> list;

    @Schema(description = "总条数")
    private Long total;

    @Schema(description = "总页数")
    private Integer pages;

    @Schema(description = "当前页码")
    private Integer pageNum;

    @Schema(description = "每页条数")
    private Integer pageSize;

    // ------------------- 静态工厂方法 -------------------

    /**
     * 空结果（避免返回null，减少前端空指针）
     */
    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setList(Collections.emptyList());
        result.setTotal(0L);
        result.setPages(0);
        result.setPageNum(1);
        result.setPageSize(10);
        return result;
    }

    /**
     * 从MyBatis-Plus的Page对象转换
     */
    public static <T> PageResult<T> fromMpPage(Page<T> mpPage) {
        PageResult<T> result = new PageResult<>();
        result.setList(mpPage.getRecords());
        result.setTotal(mpPage.getTotal());
        result.setPages(toInt(mpPage.getPages()));
        result.setPageNum(toInt(mpPage.getCurrent()));
        result.setPageSize(toInt(mpPage.getSize()));
        return result;
    }

    /**
     * 从MyBatis-Plus的Page对象转换，并应用VO映射函数
     * 适用于 Entity -> VO 的分页转换场景
     */
    public static <E, V> PageResult<V> fromMpPage(Page<E> mpPage, Function<E, V> mapper) {
        PageResult<V> result = new PageResult<>();
        result.setList(mpPage.getRecords().stream().map(mapper).collect(Collectors.toList()));
        result.setTotal(mpPage.getTotal());
        result.setPages(toInt(mpPage.getPages()));
        result.setPageNum(toInt(mpPage.getCurrent()));
        result.setPageSize(toInt(mpPage.getSize()));
        return result;
    }

    /**
     * 手动构建分页结果（适配无ORM框架场景）
     */
    public static <T> PageResult<T> build(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        int pages = total == 0 || pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPages(pages);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    private static Integer toInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (value < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) value);
    }
}
