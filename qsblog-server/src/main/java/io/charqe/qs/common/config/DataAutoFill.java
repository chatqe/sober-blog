package io.charqe.qs.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.charqe.qs.utils.BlogUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 公共字段自动填充。
 * <p>
 * 负责填充 {@code BaseDO} 中声明了 {@code @TableField(fill=...)} 的公共字段：
 * isDel / createTime / updateTime（createBy/updateBy 仅在实体存在该字段时填充）。
 * <p>
 * 注意：本回调在所有 insert/update 时触发，<b>包括异步线程落库（如访问日志）</b>。
 * 异步线程没有 RequestContextHolder，获取当前用户必须做降级处理，
 * 否则会因 NullPointerException 中断整条数据写入。
 */
@Component
public class DataAutoFill implements MetaObjectHandler {

    /** 无登录态 / 非 Web 线程时的默认操作人 */
    private static final String DEFAULT_OPERATOR = "Joy";
    /** 逻辑删除：未删除 */
    private static final Integer NOT_DELETED = 0;

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 以下字段对应 BaseDO 中 fill=INSERT 的公共字段，必须显式填充，
        // 否则 MyBatis-Plus 会以 null 显式写入，覆盖数据库默认值，触发 NOT NULL 约束
        this.strictInsertFill(metaObject, "isDel", Integer.class, NOT_DELETED);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        // createBy 仅当实体存在该属性时填充（BaseDO 无此字段时 strict 模式自动跳过）
        this.strictInsertFill(metaObject, "createBy", String.class, currentOperator());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", String.class, currentOperator());
    }

    /**
     * 获取当前操作人。
     * 异步线程 / 定时任务等无请求上下文的场景降级为系统默认，
     * 保证自动填充不会因获取用户信息失败而中断数据落库。
     */
    private String currentOperator() {
        try {
            String username = BlogUtil.getUsername();
            return StringUtils.hasText(username) ? username : DEFAULT_OPERATOR;
        } catch (Exception e) {
            return DEFAULT_OPERATOR;
        }
    }
}
