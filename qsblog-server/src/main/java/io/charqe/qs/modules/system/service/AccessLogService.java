package io.charqe.qs.modules.system.service;

import io.charqe.qs.utils.CommonQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 访问日志服务类
 * 用于异步处理访问日志的存储，避免影响主流程性能
 */
@Slf4j
@Service
public class AccessLogService {

    @Resource
    private CommonQuery commonQuery;

    /**
     * 异步保存访问日志
     * @param ip 访问IP
     */
    @Async("accessLogExecutor")
    public void saveAccessLogAsync(String ip) {
        try {
            commonQuery.saveHistory(ip);
            log.debug("异步保存访问日志成功，IP: {}", ip);
        } catch (Exception e) {
            log.error("异步保存访问日志失败: {}", e.getMessage(), e);
            // 可以加入监控指标
            // Metrics.counter("access_log_async_failure").increment();
        }
    }
}