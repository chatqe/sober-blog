package io.charqe.qs.common.exception;

/**
 * 自定义异常
 *
 * @author sjq
 * @since 2025-12-16 14:55
 */
public class BizException extends RuntimeException {

    private String msg;

    public BizException() {
        super();
    }

    public BizException(String msg) {
        super(msg);
        this.msg = msg;
    }


    public BizException(Throwable cause) {
        super(cause);
        this.msg = cause.getMessage();
    }

    public BizException(String msg, Throwable cause) {
        super(msg, cause);
        this.msg = msg;
    }


    public String getMsg() {
        return msg;
    }
}


