package io.charqe.qs.common.exception.handle;

import com.alibaba.fastjson.JSON;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.exception.LoginException;
import io.charqe.qs.common.exception.ServiceException;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.utils.BlogUtil;
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

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R handlerException(Exception ex) {
        log.error("请求URL-----------------" + BlogUtil.getRequest().getRequestURL());
        log.error("出错啦------------------", ex);
        if (ex instanceof BizException) {
            BizException e = (BizException) ex;
            return R.fail(e.getMessage());
        }

        if (ex instanceof LoginException) {
            LoginException e = (LoginException) ex;
            return R.fail(300, e.getMessage());
        }

        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException e = (MethodArgumentNotValidException) ex;
            Map<String, String> collect = e.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            return R.fail(JSON.toJSONString(collect));
        }

        if (ex instanceof MissingServletRequestParameterException) {
            return R.fail(CodeMsg.PARAMETER_ERROR);
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


