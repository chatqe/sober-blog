package io.charqe.qs.modules.weblinks.controller.web;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.infra.storage.QiniuUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "qiniu",description = "对象存储(七牛云)")
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


