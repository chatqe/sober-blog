package io.charqe.qs.modules.moment.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 光标分页响应VO
 */
@Data
@Schema(description = "光标分页响应")
public class CursorPageVO<T> {

    @Schema(description = "数据列表")
    private List<T> list;

    @Schema(description = "下一页光标（null表示无更多数据）")
    private String nextCursor;

    @Schema(description = "是否有更多数据")
    private boolean hasNext;

    public static <T> CursorPageVO<T> of(List<T> list, String nextCursor) {
        CursorPageVO<T> vo = new CursorPageVO<>();
        vo.setList(list);
        vo.setNextCursor(nextCursor);
        vo.setHasNext(nextCursor != null);
        return vo;
    }
}
