package com.ld.poetry.controller;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.SysConfig;
import com.ld.poetry.enums.PoetryEnum;
import com.ld.poetry.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

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
@RestController
@RequestMapping("/sysConfig")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 查询系统参数
     */
    @GetMapping("/listSysConfig")
    public PoetryResult<Map<String, String>> listSysConfig() {
        LambdaQueryChainWrapper<SysConfig> wrapper = new LambdaQueryChainWrapper<>(sysConfigService.getBaseMapper());
        List<SysConfig> sysConfigs = wrapper.eq(SysConfig::getConfigType, Integer.toString(PoetryEnum.SYS_CONFIG_PUBLIC.getCode()))
                .list();
        Map<String, String> collect = sysConfigs.stream().collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));
        return PoetryResult.success(collect);
    }
}
