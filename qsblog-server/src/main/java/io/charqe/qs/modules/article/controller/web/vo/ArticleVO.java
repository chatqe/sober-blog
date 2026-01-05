package io.charqe.qs.modules.article.controller.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.charqe.qs.modules.label.domain.pojo.Label;
import io.charqe.qs.modules.sort.domain.pojo.Sort;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 文章 VO
 * 
 * 用于文章的前端展示和交互
 * 
 * @author sara
 * @since 2021-08-13
 */
@Schema(name = "ArticleVO", description = "文章视图对象，用于文章的前端展示和交互")
@Data
public class ArticleVO {

    @Schema(name = "id", description = "文章ID", example = "1")
    private Integer id;

    @Schema(name = "userId", description = "用户ID", example = "1001")
    private Integer userId;

    @Schema(name = "articleCover", description = "文章封面图片URL", example = "https://example.com/cover.jpg")
    //查询为空时，随机选择
    private String articleCover;

    @Schema(
        name = "articleTitle",
        description = "文章标题",
        required = true,
        example = "测试文章标题",
        minLength = 1,
        maxLength = 200
    )
    @NotBlank(message = "文章标题不能为空")
    private String articleTitle;

    @Schema(
        name = "articleContent",
        description = "文章内容",
        required = true,
        example = "这是一篇测试文章的内容..."
    )
    @NotBlank(message = "文章内容不能为空")
    private String articleContent;

    @Schema(name = "viewCount", description = "浏览次数", example = "100")
    private Integer viewCount;

    @Schema(name = "likeCount", description = "点赞次数", example = "20")
    private Integer likeCount;

    @Schema(name = "commentStatus", description = "评论状态，true-开启，false-关闭", example = "true")
    private Boolean commentStatus;

    @Schema(name = "recommendStatus", description = "推荐状态，true-推荐，false-不推荐", example = "false")
    private Boolean recommendStatus;

    @Schema(name = "videoUrl", description = "视频URL", example = "https://example.com/video.mp4")
    private String videoUrl;

    @Schema(name = "password", description = "文章密码，为空则不加密", example = "123456")
    private String password;

    @Schema(name = "tips", description = "文章提示信息", example = "这是一篇加密文章")
    private String tips;

    @Schema(name = "viewStatus", description = "可见状态，true-公开，false-私有", example = "true")
    private Boolean viewStatus;

    @Schema(
        name = "createTime",
        description = "创建时间",
        example = "2023-01-01 12:00:00",
        format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(
        name = "updateTime",
        description = "更新时间",
        example = "2023-01-02 14:30:00",
        format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(name = "updateBy", description = "更新人", example = "admin")
    private String updateBy;

    @Schema(
        name = "sortId",
        description = "文章分类ID",
        required = true,
        example = "1"
    )
    @NotNull(message = "文章分类不能为空")
    private Integer sortId;

    @Schema(
        name = "labelId",
        description = "文章标签ID",
        required = true,
        example = "1"
    )
    @NotNull(message = "文章标签不能为空")
    private Integer labelId;

    @Schema(name = "commentCount", description = "评论数量", example = "5")
    // 需要查询封装
    private Integer commentCount;
    
    @Schema(name = "username", description = "用户名", example = "testuser")
    private String username;
    
    @Schema(name = "sort", description = "文章分类信息")
    private Sort sort;
    
    @Schema(name = "label", description = "文章标签信息")
    private Label label;
    
    @Schema(name = "hasVideo", description = "是否包含视频，true-是，false-否", example = "false")
    private Boolean hasVideo = false;
}


