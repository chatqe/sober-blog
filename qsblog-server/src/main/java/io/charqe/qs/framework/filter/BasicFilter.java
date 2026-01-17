package io.charqe.qs.framework.filter;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.common.model.R;
import org.junit.jupiter.api.Order;
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

        // 1. 处理OPTIONS预检请求
        if (handlePreflightRequest(request, response)) {
            return;
        }

        // 2. 设置通用响应头（CORS、编码等）
        setCommonResponseHeaders(response);

        // 3. 继续执行过滤器链
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            // 4. 统一异常处理（捕获业务层未处理的异常）
            handleGlobalException(response, e);
        }
    }

    /**
     * 处理OPTIONS预检请求
     */
    private boolean handlePreflightRequest(HttpServletRequest request,
                                           HttpServletResponse response) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            setCorsHeaders(response);
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        return false;
    }

    /**
     * 设置通用响应头
     */
    private void setCommonResponseHeaders(HttpServletResponse response) {
        // CORS配置（可根据环境配置）
        setCorsHeaders(response);

        // 字符编码
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
    }

    /**
     * 设置CORS头
     */
    private void setCorsHeaders(HttpServletResponse response) {
        // 生产环境建议配置具体域名而非 "*"
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
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
