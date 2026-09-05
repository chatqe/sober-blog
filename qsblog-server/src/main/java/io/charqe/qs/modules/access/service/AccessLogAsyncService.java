package io.charqe.qs.modules.access.service;

import io.charqe.qs.modules.access.domain.pojo.AccessLog;
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
     * 异步保存访问日志。
     * <p>
     * 入参 {@link AccessLog} 必须由调用方在<b>请求线程</b>构造并填充好字段
     * （ip/userId/ua/reqUrl/reqMethod/respStatus/respTime/accessTime）；
     * 异步线程无法访问 RequestContextHolder（ThreadLocal），不能在本方法内读取请求信息。
     *
     * @param accessLog 访问日志（字段已在请求线程收集完毕）
     */
    @Async("accessLogExecutor")
    public void saveAccessLogAsync(AccessLog accessLog) {
        try {
            commonQuery.saveHistory(accessLog);
            log.debug("异步保存访问日志成功，IP: {}", accessLog.getIp());
        } catch (Exception e) {
            log.error("异步保存访问日志失败: {}", e.getMessage(), e);
            // 可以加入监控指标
            // Metrics.counter("access_log_async_failure").increment();
        }
    }
}
