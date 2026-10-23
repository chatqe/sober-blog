package io.charqe.qs.modules.message.controller.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "留言列表查询参数")
public class MessageQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "排序方式: latest=最新, hot=热门", example = "latest")
    private String sortBy = "latest";

    @Schema(description = "关键词搜索（昵称或内容）", example = "测试")
    private String searchKey;
}
