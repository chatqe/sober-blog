package com.joy.qs.controller;

import com.joy.qs.aop.LoginCheck;
import com.joy.qs.aop.SaveCheck;
import com.joy.qs.common.R;
import com.joy.qs.utils.storage.QiniuUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 七牛云
 */
@RestController
@RequestMapping("/qiniu")
@ConditionalOnBean(QiniuUtil.class)
public class QiniuController {

    @Autowired
    private QiniuUtil qiniuUtil;

    /**
     * 获取覆盖凭证，用于七牛云
     */
    @GetMapping("/getUpToken")
    @LoginCheck
    @SaveCheck
    public R<String> getUpToken(@RequestParam("key") String key) {
        return R.success(qiniuUtil.getToken(key));
    }
}
