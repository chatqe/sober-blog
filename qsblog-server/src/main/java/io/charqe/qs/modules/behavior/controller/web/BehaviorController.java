package io.charqe.qs.modules.behavior.controller.web;

import io.charqe.qs.modules.behavior.service.BehaviorLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 行为控制
 *
 * @author Joy
 * @since 2026-01-27 21:42
 */
@Slf4j
@RestController
@RequestMapping("/behavior")
public class BehaviorController {


    @Resource
    private BehaviorLogService behaviorLogService;

    /**
     * 保存用户行为操作
     *
     * @param dto 行为请求参数
     * @return 点赞结果
     */
    // @PostMapping("/handleBeh")
    // public R handleBehavior(@RequestBody BehDTO dto) {
    //     return R.success(behaviorLogService.handleBeh(dto));
    // }
}
