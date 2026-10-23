package io.charqe.qs.modules.behavior.event;

import io.charqe.qs.modules.behavior.domain.pojo.BehaviorLog;
import io.charqe.qs.modules.behavior.service.BehaviorLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 行为日志异步监听器
 * 监听 BehActionEvent，异步写入 BehaviorLog 表
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Component
public class BehLogEventListener {

    @Resource
    private BehaviorLogService behaviorLogService;


    @Async
    @EventListener
    public void handleBehAction(BehActionEvent event) {
        try {
            BehaviorLog logEntry = new BehaviorLog();
            logEntry.setUserId(event.getUserId());
            logEntry.setBizId(event.getBizId());
            logEntry.setBizType(event.getBizType());
            logEntry.setAction(event.getActionCode());

            behaviorLogService.save(logEntry);
            log.debug("行为日志已异步写入: {}", event);
        } catch (Exception e) {
            log.error("异步写入行为日志失败: {}", event, e);
        }
    }
}
