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

    private Integer categoryId;

    private Integer tagId;

    private Boolean userStatus;

    private Integer userType;

    private Integer userId;

    private String resourceType;

    private Boolean status;

    private String classify;
}


