package io.charqe.qs.modules.weblinks.controller.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class WeblinksVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "分类")
    private String classify;

    @Schema(description = "封面")
    private String cover;

    @Schema(description = "友链地址")
    private String url;

    @Schema(description = "资源类型")
    private String type;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否启用[0:否，1:是]")
    private Boolean status;

    @Schema(description = "简介")
    private String introduction;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
