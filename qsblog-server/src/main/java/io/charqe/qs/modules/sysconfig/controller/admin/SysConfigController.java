package io.charqe.qs.modules.sysconfig.controller.admin;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.sysconfig.domain.pojo.SysConfig;
import io.charqe.qs.common.enums.SysConfigVisibilityEnum;
import io.charqe.qs.modules.sysconfig.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Slf4j
@RestController
@RequestMapping("/sysConfig")
@Tag(name = "sysConfig", description = "系统配置管理")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 查询系统参数
     *
     * @param request HTTP请求对象
     * @return 系统参数映射
     */
    @Operation(summary = "查询系统参数", description = "用于查询系统的公共配置参数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功，返回系统参数映射"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    // 公开接口：C 端首屏需匿名读取公共配置，服务层已限定 PUBLIC，故不加 @LoginCheck
    @GetMapping("/listSysConfig")
    public R<Map<String, String>> listSysConfig(
        @Parameter(hidden = true) HttpServletRequest request) {
        log.info("{} 访问listSysConfig()",request.getRequestURI());
        LambdaQueryChainWrapper<SysConfig> wrapper = new LambdaQueryChainWrapper<>(sysConfigService.getBaseMapper());
        List<SysConfig> sysConfigs = wrapper.eq(SysConfig::getConfigType, Integer.toString(SysConfigVisibilityEnum.PUBLIC.getCode()))
                .list();
        Map<String, String> collect = sysConfigs.stream().collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));
        return R.success(collect);
    }

    /**
     * 保存或更新系统配置
     *
     * @param sysConfig 系统配置信息
     * @return 操作结果
     */
    @Operation(summary = "保存或更新系统配置", description = "用于保存或更新系统配置，支持公共配置和私有配置两种类型")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如配置信息不完整或配置类型不正确"),
        @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/saveOrUpdateConfig")
    @LoginCheck(0)
    public R saveConfig(
        @Parameter(name = "sysConfig", description = "系统配置信息", required = true, example = "{\"configName\":\"网站名称\",\"configKey\":\"site_name\",\"configValue\":\"清思博客\",\"configType\":\"1\"}")
        @RequestBody SysConfig sysConfig) {
        if (!StringUtils.hasText(sysConfig.getConfigName()) ||
                !StringUtils.hasText(sysConfig.getConfigKey()) ||
                !StringUtils.hasText(sysConfig.getConfigType())) {
            return R.fail("请完善所有配置信息！");
        }
        String configType = sysConfig.getConfigType();
        if (!Integer.toString(SysConfigVisibilityEnum.PUBLIC.getCode()).equals(configType) &&
                !Integer.toString(SysConfigVisibilityEnum.PRIVATE.getCode()).equals(configType)) {
            return R.fail("配置类型不正确！");
        }
        sysConfigService.saveOrUpdate(sysConfig);
        return R.success();
    }

    /**
     * 删除系统配置
     *
     * @param id 配置ID
     * @return 操作结果
     */
    @Operation(summary = "删除系统配置", description = "用于删除指定ID的系统配置")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如配置ID为空"),
        @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/deleteConfig")
    @LoginCheck(0)
    public R deleteConfig(
        @Parameter(name = "id", description = "配置ID", required = true, example = "1")
        @RequestParam("id") Integer id) {
        sysConfigService.removeById(id);
        return R.success();
    }

    /**
     * 查询所有系统配置
     *
     * @return 系统配置列表
     */
    @Operation(summary = "查询所有系统配置", description = "用于查询所有系统配置，包括公共配置和私有配置")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功，返回系统配置列表"),
        @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/listConfig")
    @LoginCheck(0)
    public R<List<SysConfig>> listConfig() {
        return R.success(new LambdaQueryChainWrapper<>(sysConfigService.getBaseMapper()).list());
    }
}


