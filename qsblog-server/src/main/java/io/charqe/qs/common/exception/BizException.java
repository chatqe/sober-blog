package io.charqe.qs.common.exception;

import lombok.Getter;

/**
 * 自定义异常
 *
 * @author sjq
 * @since 2025-12-16 14:55
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 错误信息
     */
    private final String msg;
    /**
     * 错误码
     */
    private final String code;

    public BizException(IErrorCode IErrorCode) {
        this.code = IErrorCode.code();
        this.msg = IErrorCode.msg();
    }

    public BizException(String msg, String code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }


    public BizException(Throwable cause, String code) {
        super(cause);
        this.msg = cause.getMessage();
        this.code = code;
    }

    public BizException(String msg, Throwable cause, String code) {
        super(msg, cause);
        this.msg = msg;
        this.code = code;
    }


}


