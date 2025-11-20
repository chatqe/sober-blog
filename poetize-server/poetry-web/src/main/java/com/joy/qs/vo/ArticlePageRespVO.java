package com.joy.qs.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章分页响应
 *
 * @author sjq
 * @since 2025-11-20 19:39
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticlePageRespVO extends Page {
    private boolean desc = true;
    private String searchKey;
    private String sortId;
}
