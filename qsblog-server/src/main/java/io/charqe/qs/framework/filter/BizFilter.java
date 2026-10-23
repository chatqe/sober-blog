package io.charqe.qs.framework.filter;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.common.model.R;
import io.charqe.qs.infra.storage.FileFilter;
import io.charqe.qs.modules.user.auth.AuthAccessException;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;
import io.charqe.qs.modules.access.service.AccessLogAsyncService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.IPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 业务层过滤器
 * 职责：业务相关的安全检查、日志记录、权限验证等
 * 顺序：在技术层之后执行
 *
 * @author Joy
 * @since 2026-01-17 19:02
 */
@Slf4j
@Component
@Order(2) // 第二执行
public class BizFilter extends OncePerRequestFilter {
    
    @Resource
    private AccessLogAsyncService accessLogAsyncService;

    @Resource
    private FileFilter fileFilter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 0. CORS 预检请求（OPTIONS）直接放行：预检非业务访问，
        //    不做文件安全检查、不记录访问日志（否则每个跨域请求会产生预检+实际两条记录）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // 1. 记录请求开始时间（用于计算响应耗时）
        long startTime = System.currentTimeMillis();

        // 2. 在请求线程中提前解析用户ID。
        //    访问日志通过 @Async 异步落库，而异步线程无法访问 RequestContextHolder
        //    （ThreadLocal，请求结束即清理），若在异步方法内调用 BlogUtil.getUserId()
        //    会抛 NullPointerException。因此必须在提交异步任务前取好并透传。
        //    解析失败不影响主请求，降级为不关联用户（仅记录IP）。
        Long userId;
        try {
            userId = BlogUtil.getUserId();
        } catch (Exception e) {
            log.warn("解析用户ID失败，访问日志将不记录用户: {}", e.getMessage());
            userId = null;
        }

        try {
            // 3. 执行文件安全检查
            try {
                if (!checkFileSecurity(request, response)) {
                    // 安全检查失败，已返回响应，直接结束
                    return;
                }
            } catch (AuthAccessException e) {
                response.setStatus(e.getStatus());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSON.toJSONString(R.fail(e.getStatus(), e.getMessage())));
                return;
            }

            // 4. 可扩展：其他业务检查
            // checkBusinessRules(request, response);

            // 5. 继续执行
            chain.doFilter(request, response);
        } finally {
            // 6. 请求完成后收集日志（finally保证无论是否异常都会执行）
            try {
                recordAccessLog(request, response, startTime, userId);
            } catch (Exception e) {
                // 日志收集失败不影响主流程
                log.error("收集访问日志失败", e);
            }
        }
    }

    /**
     * 记录访问日志
     *
     * @param request   当前请求
     * @param response  当前响应
     * @param startTime 请求开始时间戳
     * @param userId    用户ID（在请求线程中提前解析，透传给异步方法）
     */
    private void recordAccessLog(HttpServletRequest request, HttpServletResponse response,
                                 long startTime, Long userId) {
        try {
            String ip = IPUtil.getRealIp(request);
            String uri = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();
            long responseTime = System.currentTimeMillis() - startTime;

            // 在请求线程构造 AccessLog 并收集全部字段，再透传给异步线程落库。
            // 原因：异步线程无法访问 RequestContextHolder（ThreadLocal），
            // 故 UA、URL、userId 等请求相关信息必须在此处取好。
            AccessLog accessLog = new AccessLog();
            accessLog.setIp(ip);
            accessLog.setUserId(userId);
            accessLog.setUa(request.getHeader("User-Agent"));
            accessLog.setReqUrl(uri);
            accessLog.setReqMethod(method);
            accessLog.setRespStatus(status);
            accessLog.setRespTime((int) responseTime);
            accessLog.setAccessTime(LocalDateTime.now());

            // 异步落库（UA 解析、IP 归属地查询在异步线程完成，不阻塞主请求）
            accessLogAsyncService.saveAccessLogAsync(accessLog);

            // 同时记录到日志文件（可选）
            log.info("访问记录 - IP: {}, Method: {}, URI: {}, Status: {}, ResponseTime: {}ms",
                    ip, method, uri, status, responseTime);

        } catch (Exception e) {
            // 日志记录失败不应该影响主流程
            log.warn("访问日志记录失败: {}", e.getMessage());
            // 可以加入监控指标
            // Metrics.counter("access_log_failure").increment();
        }
    }

    /**
     * 文件安全检查
     * FileFilter.doFilterFile 返回值语义：
     *   - true  表示需要拦截（限流触发、未登录、无缓存用户信息等）
     *   - false 表示放行（管理员、非 /resource/upload 路径等）
     *
     * 注意：doFilterFile 自身不会向客户端写响应，因此拦截场景需由本方法主动写错误响应。
     */
    private boolean checkFileSecurity(HttpServletRequest request,
                                      HttpServletResponse response)
            throws IOException {

        // 仅对文件相关请求进行检查
        if (isFileRelatedRequest(request)) {
            // 返回 true 表示需要拦截
            if (fileFilter.doFilterFile(request, response)) {
                // 安全检查未通过，主动写错误响应给客户端
                // 注意：CORS 由 CorsConfig 中的 Spring CorsFilter 统一处理，此处不再手动设置
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSON.toJSONString(R.fail(CodeMsg.PARAMETER_ERROR.getCode(), CodeMsg.PARAMETER_ERROR.getMsg())));
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否为文件相关请求
     */
    private boolean isFileRelatedRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 上传请求
        if (uri.contains("/upload") && "POST".equals(method)) {
            return true;
        }

        // 文件下载/查看（可根据需要调整）
        if (uri.contains("/file/") || uri.contains("/image/")) {
            return true;
        }

        return false;
    }

    /**
     * 扩展：其他业务规则检查
     */
    private boolean checkBusinessRules(HttpServletRequest request,
                                       HttpServletResponse response) {
        // 示例：访问频率限制
        // 示例：API签名验证
        // 示例：黑名单检查
        return true;
    }
}
