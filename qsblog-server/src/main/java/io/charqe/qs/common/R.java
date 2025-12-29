package io.charqe.qs.common;

import io.charqe.qs.enums.CodeMsg;
import lombok.Data;

import java.io.Serializable;

/**
 * 公共响应类
 *
 * @author Upjqs
 * @since 2025-11-20 21:08
 */

@Data
public class R<T> implements Serializable {

    // 显式声明序列化版本号（建议用 IDE 自动生成，如 Alt+Insert → SerialVersionUID）
    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;
    private long currentTimeMillis = System.currentTimeMillis();

    public R() {
        this.code = 200;
    }

    public R(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public R(T data) {
        this.code = 200;
        this.data = data;
    }

    public R(String msg) {
        this.code = 500;
        this.msg = msg;
    }

    public static <T> R<T> fail(String message) {
        return new R(message);
    }

    public static <T> R<T> fail(CodeMsg codeMsg) {
        return new R(codeMsg.getCode(), codeMsg.getMsg());
    }

    public static <T> R<T> fail(Integer code, String message) {
        return new R(code, message);
    }

    public static <T> R<T> success(T data) {
        return new R(data);
    }

    public static <T> R<T> success() {
        return new R();
    }
}


