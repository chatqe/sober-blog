package io.charqe.qs.modules.message.controller.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理后台留言视图对象")
public class AdminMessageVO {

    @Schema(description = "留言ID", example = "1")
    private Long id;

    @Schema(description = "用户ID，0=游客", example = "0")
    private Long userId;

    @Schema(description = "昵称", example = "访客")
    private String nickname;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "个人网址", example = "https://example.com")
    private String website;

    @Schema(description = "留言内容", example = "这是一条测试留言")
    private String content;

    @Schema(description = "IP地址", example = "192.168.1.1")
    private String ip;

    @Schema(description = "IP地理位置", example = "香港")
    private String ipLocation;

    @Schema(description = "国家", example = "中国")
    private String country;

    @Schema(description = "省份", example = "")
    private String province;

    @Schema(description = "城市", example = "香港")
    private String city;

    @Schema(description = "运营商", example = "HK Telecom")
    private String isp;

    @Schema(description = "操作系统", example = "Windows 11")
    private String os;

    @Schema(description = "浏览器", example = "Chrome 154.0.0.0")
    private String browser;

    @Schema(description = "设备类型", example = "PC")
    private String deviceType;

    @Schema(description = "点赞数", example = "5")
    private Integer likeCount;

    @Schema(description = "点踩数", example = "0")
    private Integer dislikeCount;

    @Schema(description = "回复数", example = "3")
    private Integer replyCount;

    @Schema(description = "状态: 1=正常 2=审核中 3=拒绝 4=隐藏", example = "1")
    private Integer status;

    @Schema(description = "是否置顶", example = "false")
    private Boolean isTop;

    @Schema(description = "审核人ID", example = "100")
    private Long moderatorId;

    @Schema(description = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime moderateTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
