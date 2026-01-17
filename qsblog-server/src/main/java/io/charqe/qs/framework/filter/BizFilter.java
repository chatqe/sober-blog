package io.charqe.qs.framework.filter;

import io.charqe.qs.infra.storage.FileFilter;
import io.charqe.qs.modules.system.service.AccessLogService;
import io.charqe.qs.utils.IPUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
    private AccessLogService accessLogService;

    @Resource
    private FileFilter fileFilter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1. 记录请求开始时间（用于计算响应耗时）
        long startTime = System.currentTimeMillis();

        try {
            // 2. 执行文件安全检查
            if (!checkFileSecurity(request, response)) {
                // 安全检查失败，已返回响应，直接结束
                return;
            }

            // 3. 可扩展：其他业务检查
            // checkBusinessRules(request, response);

            // 4. 继续执行
            chain.doFilter(request, response);
        } finally {
            // 5. 请求完成后收集日志（finally保证无论是否异常都会执行）
            try {
                recordAccessLog(request, response, startTime);
            } catch (Exception e) {
                // 日志收集失败不影响主流程
                log.error("收集访问日志失败", e);
            }
        }
    }

    /**
     * 记录访问日志
     */
    private void recordAccessLog(HttpServletRequest request, HttpServletResponse response, long startTime) {
        try {
            String ip = IPUtil.getRealIp(request);
            String uri = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();
            long responseTime = System.currentTimeMillis() - startTime;

            // 异步记录到数据库
            accessLogService.saveAccessLogAsync(ip);

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
     */
    private boolean checkFileSecurity(HttpServletRequest request,
                                      HttpServletResponse response)
            throws IOException {

        // 仅对文件相关请求进行检查
        if (isFileRelatedRequest(request)) {
            boolean pass = fileFilter.doFilterFile(request, response);
            if (!pass) {
                // 安全检查失败，返回错误
                // 注意：这里不设置CORS头，因为TechnicalFilter已经设置了
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
