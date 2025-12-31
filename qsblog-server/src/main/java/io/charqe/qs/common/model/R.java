package io.charqe.qs.common.model;

import io.charqe.qs.common.enums.CodeMsg;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 公共响应类
 *
 * @author Upjqs
 * @since 2025-11-20 21:08
 */

@Data
@Schema(description = "公共响应类")
public class R<T> implements Serializable {

    // 显式声明序列化版本号（建议用 IDE 自动生成，如 Alt+Insert → SerialVersionUID）
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码：200-成功，其他-失败", example = "200")
    private int code;
    @Schema(description = "业务消息", example = "操作成功")
    private String msg;
    @Schema(description = "响应数据")
    private T data;
    @Schema(description = "服务器时间戳", example = "1672502400000")
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


