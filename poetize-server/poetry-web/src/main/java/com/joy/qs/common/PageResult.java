package com.joy.qs.common;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应
 * 分页结果统一返回类
 * 注：兼容HTTP响应、RPC响应，支持空数据默认返回空列表
 *
 * @author sjq
 * @since 2025-11-20 20:52
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总条数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页条数
     */
    private Integer pageSize;

    // ------------------- 静态工厂方法：简化创建 -------------------

    /**
     * 空结果（避免返回null，减少前端空指针）
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0L, 0, 1, 10);
    }

    /**
     * 从MyBatis-Plus的Page对象转换（最常用）
     */
    public static <T> PageResult<T> fromMpPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> mpPage) {
        PageResult<T> result = new PageResult<>();
        result.setList(mpPage.getRecords()); // 数据列表
        result.setTotal(mpPage.getTotal()); // 总条数
        result.setPages((int) mpPage.getPages()); // 总页数（MP返回long，转int需确保不超范围）
        result.setPageNum((int) mpPage.getCurrent()); // 当前页码
        result.setPageSize((int) mpPage.getSize()); // 每页条数
        return result;
    }

    /**
     * 从JPA的Page对象转换
     */
    public static <T> PageResult<T> fromJpaPage(org.springframework.data.domain.Page<T> jpaPage) {
        PageResult<T> result = new PageResult<>();
        result.setList(jpaPage.getContent()); // 数据列表
        result.setTotal(jpaPage.getTotalElements()); // 总条数
        result.setPages(jpaPage.getTotalPages()); // 总页数
        result.setPageNum(jpaPage.getNumber() + 1); // JPA页码从0开始，转成1开始
        result.setPageSize(jpaPage.getSize()); // 每页条数
        return result;
    }

    /**
     * 手动构建分页结果（适配无ORM框架场景）
     */
    public static <T> PageResult<T> build(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        int pages = total == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize); // 总页数计算：向上取整
        return new PageResult<>(list, total, pages, pageNum, pageSize);
    }
}
