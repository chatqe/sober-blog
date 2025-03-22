package com.ld.poetry.entity;

import java.util.Date;
import java.io.Serializable;

import lombok.Data;

/**
 * 点赞记录表，用于存储用户对文章、评论、视频等的点赞信息(Like)表实体类
 *
 * @author shenjiaqi
 * @since 2025-03-21 13:53:10
 */
@Data
@SuppressWarnings("serial")
public class Like {
    /**
     * 主键
     */
    private Long id;
    /**
     * 点赞用户ID
     */
    private Long userId;
    /**
     * 点赞目标ID，表示被点赞的对象（如文章、评论等）
     */
    private Long targetId;
    /**
     * 点赞目标类型，1-文章，2-评论，3-视频,4-音乐等
     */
    private Integer targetType;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 修改时间
     */
    private Date updateTime;

}

