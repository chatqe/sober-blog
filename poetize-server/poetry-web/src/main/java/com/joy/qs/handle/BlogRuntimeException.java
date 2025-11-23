package com.joy.qs.handle;

public class BlogRuntimeException extends RuntimeException {

    private String msg;

    public BlogRuntimeException() {
        super();
    }

    public BlogRuntimeException(String msg) {
        super(msg);
        this.msg = msg;
    }


    public BlogRuntimeException(Throwable cause) {
        super(cause);
        this.msg = cause.getMessage();
    }

    public BlogRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
        this.msg = msg;
    }


    public String getMsg() {
        return msg;
    }
}
