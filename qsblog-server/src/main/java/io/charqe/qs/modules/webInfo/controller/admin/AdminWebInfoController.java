package io.charqe.qs.modules.webInfo.controller.admin;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.webInfo.domain.dao.WebInfoMapper;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理后台-网站信息控制
 *
 * @author sjq
 * @since 2025-12-31 20:04
 */
@Slf4j
@RestController
@RequestMapping("admin/webInfo")
@Tag(name = "adminWebInfo", description = "管理后台-网站信息控制")
public class AdminWebInfoController {

    @Resource
    private WebInfoMapper webInfoMapper;


    /**
     * 获取网站信息
     */
    @GetMapping("/get")
    @LoginCheck(0)
    public R<WebInfo> getWebInfo() {
        LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
        List<WebInfo> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            return R.success(list.get(0));
        } else {
            return R.success();
        }
    }
}
