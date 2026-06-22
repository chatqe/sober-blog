package io.charqe.qs.modules.sysdict.controller.web;

import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.sysdict.controller.web.vo.SysDictDataVO;
import io.charqe.qs.modules.sysdict.service.SysDictDataService;
import io.charqe.qs.modules.sysdict.service.SysDictTypeService;
import io.charqe.qs.modules.sysdict.domain.pojo.SysDictType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 字典数据控制器（Web端）
 *
 * @author Joy
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/sysDict")
@Tag(name = "字典查询", description = "面向前端的字典数据查询接口，含Redis缓存")
public class SysDictController {

    @Resource
    private SysDictTypeService sysDictTypeService;

    @Resource
    private SysDictDataService sysDictDataService;

    @Operation(summary = "根据类型编码查询字典数据", description = "获取指定字典类型下的所有启用数据，返回VO格式，带缓存")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @GetMapping("/listByTypeCode")
    public R<List<SysDictDataVO>> listByTypeCode(
            @Parameter(name = "typeCode", description = "字典类型编码", required = true, example = "gender")
            @RequestParam("typeCode") String typeCode) {
        return R.success(sysDictDataService.listByTypeCode(typeCode));
    }

    @Operation(summary = "批量查询多个字典类型数据", description = "一次性获取多个字典类型的数据，减少请求次数，用于页面初始化")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/batchList")
    public R<Map<String, List<SysDictDataVO>>> batchList(
            @Parameter(name = "typeCodes", description = "字典类型编码列表，逗号分隔", required = true, example = "gender,orderStatus")
            @RequestParam("typeCodes") String typeCodes) {
        List<String> codeList = java.util.Arrays.asList(typeCodes.split(","));
        return R.success(sysDictDataService.batchListByTypeCodes(codeList));
    }

    @Operation(summary = "获取所有启用的字典类型", description = "返回所有启用的字典类型列表，供前端选择字典类型时使用")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/type/list")
    public R<List<SysDictType>> listEnabledTypes() {
        return R.success(sysDictTypeService.listEnabled());
    }

    @Operation(summary = "代码值转显示标签", description = "将数据库存储的代码值转换为前端展示的文本标签，如将0转换为男")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "转换成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @GetMapping("/itemLabel")
    public R<String> getItemLabel(
            @Parameter(name = "typeCode", description = "字典类型编码", required = true, example = "gender")
            @RequestParam("typeCode") String typeCode,
            @Parameter(name = "itemValue", description = "字典项值", required = true, example = "0")
            @RequestParam("itemValue") String itemValue) {
        return R.success(sysDictDataService.getItemLabel(typeCode, itemValue));
    }
}
