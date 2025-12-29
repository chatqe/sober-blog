package io.charqe.qs.common;


import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.domain.Sort;

import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分页入参
 * 分页查询基础类（所有分页入参需继承此类）
 * 注：兼容HTTP参数（page/pageNum 均可）、RPC参数透传
 * Serializable 是 Java 的「标记接口」（无任何抽象方法），作用是告诉 JVM：这个类的对象可以被「序列化」（将对象转换为字节流）和「反序列化」（将字节流恢复为对象），用于 跨进程 / 跨介质的数据传输（如网络传输、文件存储、缓存写入）。
 * @author sjq
 * @since 2025-11-20 20:50
 */
@Data
public class PageQuery implements Serializable {

    // 显式声明序列化版本号（建议用 IDE 自动生成，如 Alt+Insert → SerialVersionUID）
    private static final long serialVersionUID = 1L;
    /**
     * 当前页码：默认1，支持前端传 page 或 pageNum（通过@AliasFor兼容）
     * 校验：最小为1，防止负数/0页码
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    /**
     * 每页条数：默认10，最大500（防止超大页查询压垮数据库）
     * 校验：1~500之间，可通过配置动态调整上限
     */
    @Range(min = 1, max = 500, message = "页大小需在1~500之间")
    private Integer pageSize = 10;

    /**
     * 排序条件：格式如 "createTime desc, id asc"
     * 用于兼容多字段排序，避免前端传多个排序参数
     */
    private String orderBy;

    // ------------------- 扩展方法：适配ORM框架 -------------------

    /**
     * 转换为MyBatis-Plus的Page对象（最常用）
     * MyBatis-Plus的Page默认页码从1开始，与当前设计一致
     */
    public <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> toMpPage() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        // 处理排序：如果orderBy不为空，设置排序字段
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            page.setOrders(
                    Arrays.stream(orderBy.split(","))
                            .map(order -> {
                                String[] parts = order.trim().split(" ");
                                String column = parts[0];
                                boolean isAsc = parts.length <= 1 || "asc".equalsIgnoreCase(parts[1]);
                                return isAsc ? com.baomidou.mybatisplus.core.metadata.OrderItem.asc(column) : com.baomidou.mybatisplus.core.metadata.OrderItem.desc(column);
                            })
                            .collect(Collectors.toList())
            );
        }
        return page;
    }

    /**
     * 转换为JPA的Pageable对象（适配Spring Data JPA）
     */
    public org.springframework.data.domain.Pageable toJpaPageable() {
        // 页码转换：JPA页码从0开始，当前pageNum从1开始，需减1
        int jpaPageNum = pageNum - 1;
        // 排序处理
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.unsorted();
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            List<Sort.Order> orders = Arrays.stream(orderBy.split(","))
                    .map(order -> {
                        String[] parts = order.trim().split(" ");
                        String property = parts[0];
                        org.springframework.data.domain.Sort.Direction direction = parts.length <= 1 || "asc".equalsIgnoreCase(parts[1])
                                ? org.springframework.data.domain.Sort.Direction.ASC
                                : org.springframework.data.domain.Sort.Direction.DESC;
                        return new org.springframework.data.domain.Sort.Order(direction, property);
                    })
                    .collect(Collectors.toList());
            sort = org.springframework.data.domain.Sort.by(orders);
        }
        return org.springframework.data.domain.PageRequest.of(jpaPageNum, pageSize, sort);
    }
}


