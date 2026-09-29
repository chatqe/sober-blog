package io.charqe.qs.modules.behavior.service;

/**
 * 统一行为动作服务标记接口
 * 所有正向/反向行为服务（点赞、点踩、订阅、转发、评论）均实现此接口，
 * 便于 Controller 中统一注入和类型判断。
 * 具体 doAction/cancelAction 逻辑由 AbstractBehService 模板方法实现。
 *
 * @author Joy
 * @since 2026-06-15
 */
public interface IActionService {
}
