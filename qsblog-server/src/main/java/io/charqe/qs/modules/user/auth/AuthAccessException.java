package io.charqe.qs.modules.user.auth;

/** 认证失败与权限不足单独处理，不改变普通业务异常语义。 */
public class AuthAccessException extends RuntimeException {
    private final int status;
    public AuthAccessException(int status, String message) {
        super(message);
        this.status = status;
    }
    public int getStatus() { return status; }
}
