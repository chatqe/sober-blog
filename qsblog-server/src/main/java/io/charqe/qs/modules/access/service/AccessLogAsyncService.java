package io.charqe.qs.modules.access.service;

import io.charqe.qs.utils.CommonQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 访问日志服务类
 * 用于异步处理访问日志的存储，避免影响主流程性能
 *
 * @author Joy
 * @since 2026-01-17 19:02
 */
@Slf4j
@Service
public class AccessLogAsyncService {

    @Resource
    private CommonQuery commonQuery;

    /**
     * 异步保存访问日志
     *
     * @param ip     访问IP
     * @param userId 用户ID。必须由调用方在请求线程中提前解析后传入；
     *               异步线程无法访问 RequestContextHolder（ThreadLocal），
     *               若在本方法内调用 BlogUtil.getUserId() 会抛 NullPointerException。
     */
    @Async("accessLogExecutor")
    public void saveAccessLogAsync(String ip, Long userId) {
        try {
            commonQuery.saveHistory(ip, userId);
            commonQuery.saveAccessLogByBaidu(ip);
            log.debug("异步保存访问日志成功，IP: {}", ip);
        } catch (Exception e) {
            log.error("异步保存访问日志失败: {}", e.getMessage(), e);
            // 可以加入监控指标
            // Metrics.counter("access_log_async_failure").increment();
        }
    }
}