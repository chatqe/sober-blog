package io.charqe.qs.modules.user.auth;

/** 会话受众；EITHER 仅用于明确允许两端调用的共享接口。 */
public enum SessionScope {
    WEB, ADMIN, EITHER;

    public boolean accepts(SessionScope actual) {
        return actual != EITHER && (this == EITHER || this == actual);
    }
}
