package io.charqe.qs.common.exception.handle;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.exception.LoginException;
import io.charqe.qs.common.exception.ServiceException;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.user.auth.AuthAccessException;
import org.springframework.http.ResponseEntity;
import javax.servlet.http.HttpServletResponse;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.IPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthAccessException.class)
    public ResponseEntity<R<Void>> handleAuth(AuthAccessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(R.fail(ex.getStatus(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R handlerException(Exception ex, HttpServletResponse response) {
        log.error("请求URL-----------------" + IPUtil.getRequest().getRequestURL());
        log.error("出错啦------------------", ex);
        if (ex instanceof BizException) {
            BizException e = (BizException) ex;
            return R.fail(e.getMessage());
        }

        if (ex instanceof LoginException) {
            LoginException e = (LoginException) ex;
            response.setStatus(401);
            return R.fail(401, e.getMessage());
        }

        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException e = (MethodArgumentNotValidException) ex;
            Map<String, String> collect = e.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            response.setStatus(400);
            return R.fail(400, JSON.toJSONString(collect));
        }

        if (ex instanceof MissingServletRequestParameterException) {
            response.setStatus(400);
            return R.fail(400, CodeMsg.PARAMETER_ERROR.getMsg());
        }

        if (ex instanceof ServiceException) {
            ServiceException e = (ServiceException) ex;
            Integer code = e.getCode() == null ? CodeMsg.FAIL.getCode() : e.getCode();
            String msg = e.getMsg() == null ? CodeMsg.FAIL.getMsg() : e.getMsg();
            return R.fail(code, msg);
        }

        return R.fail(CodeMsg.FAIL);
    }



}


