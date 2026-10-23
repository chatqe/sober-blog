package io.charqe.qs.modules.family.controller.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
public class FamilyVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 背景封面
     */
    @NotBlank(message = "背景封面不能为空")
    private String bgCover;

    /**
     * 男生头像
     */
    @NotBlank(message = "男生头像不能为空")
    private String manCover;

    /**
     * 女生头像
     */
    @NotBlank(message = "女生头像不能为空")
    private String womanCover;

    /**
     * 男生昵称
     */
    @NotBlank(message = "男生昵称不能为空")
    private String manName;

    /**
     * 女生昵称
     */
    @NotBlank(message = "女生昵称不能为空")
    private String womanName;

    /**
     * 计时
     */
    @NotBlank(message = "计时不能为空")
    private String timing;

    @Schema(description = "倒计时标题")
    private String countdownTitle;

    @Schema(description = "是否启用[0:否，1:是]")
    private Boolean status;

    @Schema(description = "倒计时时间")
    private String countdownTime;

    @Schema(description = "额外信息")
    private String familyInfo;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

