package io.charqe.qs.framework.filter;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.common.model.R;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 技术层过滤器
 *
 * @author Joy
 * @since 2026-01-17 19:13
 */
@Component
@Order(1) // 最先执行
public class BasicFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1. 设置通用响应头（编码等）
        // 注意：CORS 由 CorsConfig 中的 Spring CorsFilter 统一处理，此处不再手动设置
        setCommonResponseHeaders(response);

        // 2. 继续执行过滤器链
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            // 3. 统一异常处理（捕获业务层未处理的异常）
            handleGlobalException(response, e);
        }
    }

    /**
     * 设置通用响应头
     */
    private void setCommonResponseHeaders(HttpServletResponse response) {
        // 字符编码
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
    }

    /**
     * 全局异常处理
     */
    private void handleGlobalException(HttpServletResponse response, Exception e)
            throws IOException {
        // 记录日志
        logger.error("全局过滤器捕获异常", e);

        // 返回统一错误格式
        R<Object> error = R.fail(
                CodeMsg.FAIL.getCode(),
                "系统繁忙，请稍后重试"
        );

        response.getWriter().write(JSON.toJSONString(error));
    }
}
