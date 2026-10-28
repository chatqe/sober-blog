package io.charqe.qs.common.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class BaseReqVO extends Page {

    private String order;

    private boolean desc = true;

    private Long bizId;

    private String commentType;

    private Long rootId;

    private String searchKey;

    private String articleSearch;

    // 是否推荐[0:否，1:是]
    private Boolean recommendStatus;

    // ID 一律 Long：全局约定下 Long 会被同时发为字符串与 string schema，
    // 若保持 Integer 则这些筛选参数与其它 id 类型不一致
    private Long categoryId;

    private Long tagId;

    private Boolean userStatus;

    private Integer userType;

    private Long userId;

    private String resourceType;

    private Boolean status;

    private String classify;
}


