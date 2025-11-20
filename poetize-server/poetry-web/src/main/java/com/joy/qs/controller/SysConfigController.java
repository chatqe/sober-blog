package com.joy.qs.controller;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.joy.qs.aop.LoginCheck;
import com.joy.qs.common.R;
import com.joy.qs.entity.SysConfig;
import com.joy.qs.enums.YouthEnum;
import com.joy.qs.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 参数配置表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2024-03-23
 */
@Slf4j
@RestController
@RequestMapping("/sysConfig")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 查询系统参数
     */
    @GetMapping("/listSysConfig")
    public R<Map<String, String>> listSysConfig(HttpServletRequest request) {
        log.info("{} 访问listSysConfig()",request.getRequestURI());
        LambdaQueryChainWrapper<SysConfig> wrapper = new LambdaQueryChainWrapper<>(sysConfigService.getBaseMapper());
        List<SysConfig> sysConfigs = wrapper.eq(SysConfig::getConfigType, Integer.toString(YouthEnum.SYS_CONFIG_PUBLIC.getCode()))
                .list();
        Map<String, String> collect = sysConfigs.stream().collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));
        return R.success(collect);
    }

    /**
     * 保存或更新
     */
    @PostMapping("/saveOrUpdateConfig")
    @LoginCheck(0)
    public R saveConfig(@RequestBody SysConfig sysConfig) {
        if (!StringUtils.hasText(sysConfig.getConfigName()) ||
                !StringUtils.hasText(sysConfig.getConfigKey()) ||
                !StringUtils.hasText(sysConfig.getConfigType())) {
            return R.fail("请完善所有配置信息！");
        }
        String configType = sysConfig.getConfigType();
        if (!Integer.toString(YouthEnum.SYS_CONFIG_PUBLIC.getCode()).equals(configType) &&
                !Integer.toString(YouthEnum.SYS_CONFIG_PRIVATE.getCode()).equals(configType)) {
            return R.fail("配置类型不正确！");
        }
        sysConfigService.saveOrUpdate(sysConfig);
        return R.success();
    }

    /**
     * 删除
     */
    @GetMapping("/deleteConfig")
    @LoginCheck(0)
    public R deleteConfig(@RequestParam("id") Integer id) {
        sysConfigService.removeById(id);
        return R.success();
    }

    /**
     * 查询
     */
    @GetMapping("/listConfig")
    @LoginCheck(0)
    public R<List<SysConfig>> listConfig() {
        return R.success(new LambdaQueryChainWrapper<>(sysConfigService.getBaseMapper()).list());
    }
}
