package io.charqe.qs.modules.access.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统访问记录实体
 *
 * @author Joy
 * @since 2026-01-18 21:03
 */
@Data
@TableName("access_log")
public class AccessLog extends BaseDO {
    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 访问时间
     */
    private LocalDateTime accessTime;

    /**
     * 访问IP地址
     */
    private String ip;

    /**
     * IP完整地理位置（省/市/区）
     */
    private String ipLocation;

    /**
     * 国家
     */
    private String country;

    /**
     * 省份
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区县
     */
    private String district;

    /**
     * 运营商（如：中国移动、中国电信）
     */
    private String isp;

    /**
     * 来源页URL（从哪个页面跳转过来）
     */
    private String referer;

    /**
     * 用户代理（浏览器/设备信息）
     */
    private String ua;

    /**
     * 浏览器类型（Chrome/Firefox/微信内置等）
     */
    private String browser;

    /**
     * 操作系统（Windows/macOS/Android/iOS等）
     */
    private String os;

    /**
     * 设备类型：PC/Mobile/Pad等
     */
    private String deviceType;

    /**
     * 访问的URL
     */
    private String reqUrl;

    /**
     * 请求方式
     */
    private String reqMethod;

    /**
     * 响应状态码
     */
    private Integer respStatus;

    /**
     * 响应耗时（毫秒）
     */
    private Integer respTime;

    /**
     * 扩展字段
     * 说明：若需要解析JSON内容，可使用 com.alibaba.fastjson.JSONObject 或自定义VO类
     */
    private String extInfo;

}
