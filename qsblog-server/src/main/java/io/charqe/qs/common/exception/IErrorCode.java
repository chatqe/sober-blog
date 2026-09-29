package io.charqe.qs.common.exception;

/**
 * 错误码接口（仅用于异常处理和错误响应）
 *
 * @author Joy
 * @since 2026-01-27 18:59
 */
public interface IErrorCode {
    String code();      // 错误码（如 A100101）
    String msg();   // 错误描述
}
