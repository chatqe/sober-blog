package com.joy.qs.handle;

import com.joy.qs.enums.CodeMsg;

/**
 * 自定义后台异常
 *
 * @author sjq
 * @since 2025-03-21 18:29
 */
public class ServiceException extends RuntimeException{

    private final Integer code; // 错误码
    private final String msg; // 错误信息

    public ServiceException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public ServiceException(String msg) {
        super(msg);
        this.code = CodeMsg.FAIL.getCode();
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
