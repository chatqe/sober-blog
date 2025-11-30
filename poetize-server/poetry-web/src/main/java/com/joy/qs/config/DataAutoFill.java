package com.joy.qs.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.joy.qs.utils.BlogUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DataAutoFill implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createBy", String.class, !StringUtils.hasText(BlogUtil.getUsername()) ? "Sara" : BlogUtil.getUsername());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "updateBy", String.class, !StringUtils.hasText(BlogUtil.getUsername()) ? "Sara" : BlogUtil.getUsername());
    }
}
