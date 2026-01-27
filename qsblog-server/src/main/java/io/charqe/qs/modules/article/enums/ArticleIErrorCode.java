package io.charqe.qs.modules.article.enums;

import io.charqe.qs.common.exception.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文章相关错误响应枚举
 *
 * @author Joy
 * @since 2026-01-27 19:29
 */
@Getter
@AllArgsConstructor
public enum ArticleIErrorCode implements IErrorCode {
    NOT_FOUND("A100101", "文章不存在"),

    UPDATE_FAILED("A100102", "文章更新失败"),
    ;

    private final String code;
    private final String msg;

    @Override
    public String code() {
        return null;
    }

    @Override
    public String msg() {
        return null;
    }
}
