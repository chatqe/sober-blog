package io.charqe.qs.aop;

import io.charqe.qs.modules.user.auth.SessionScope;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginCheck {
    int value() default 2;
    SessionScope scope() default SessionScope.WEB;
    boolean activity() default true;
}


