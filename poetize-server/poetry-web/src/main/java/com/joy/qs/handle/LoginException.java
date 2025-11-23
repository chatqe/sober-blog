package com.joy.qs.handle;

public class LoginException extends RuntimeException {

    private String msg;

    public LoginException() {
        super();
    }

    public LoginException(String msg) {
        super(msg);
        this.msg = msg;
    }


    public LoginException(Throwable cause) {
        super(cause);
        this.msg = cause.getMessage();
    }

    public LoginException(String msg, Throwable cause) {
        super(msg, cause);
        this.msg = msg;
    }


    public String getMsg() {
        return msg;
    }
}
