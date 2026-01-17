package io.charqe.qs.modules.weblinks.controller.web;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.weblinks.controller.web.vo.WeblinksVO;
import io.charqe.qs.modules.weblinks.domain.dao.WeblinksMapper;
import io.charqe.qs.modules.weblinks.domain.pojo.Weblinks;
import io.charqe.qs.utils.BlogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@RestController
@RequestMapping("/weblinks")
@Tag(name = "weblinks", description = "资源链接管理")
public class WeblinksController {

    @Resource
    private WeblinksMapper weblinksMapper;

    /**
     * 保存友链
     *
     * @param resourcePathVO 友链信息
     * @return 保存结果
     */
    @Operation(summary = "保存友链", description = "用于用户提交友链申请，需要登录后操作")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "保存成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如信息不全"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @LoginCheck
    @PostMapping("/save")
    @SaveCheck
    public R saveFriend(
        @Parameter(name = "resourcePathVO", description = "友链信息对象", required = true, example = "{\"title\":\"测试网站\",\"cover\":\"https://example.com/cover.jpg\",\"url\":\"https://example.com\",\"introduction\":\"测试网站介绍\"}")
        @RequestBody WeblinksVO resourcePathVO) {
        if (!StringUtils.hasText(resourcePathVO.getTitle()) || !StringUtils.hasText(resourcePathVO.getCover()) ||
                !StringUtils.hasText(resourcePathVO.getUrl()) || !StringUtils.hasText(resourcePathVO.getIntroduction())) {
            return R.fail("信息不全！");
        }
        Weblinks friend = new Weblinks();
        friend.setClassify(CommonConst.DEFAULT_FRIEND);
        friend.setTitle(resourcePathVO.getTitle());
        friend.setDesc(resourcePathVO.getIntroduction());
        friend.setCover(resourcePathVO.getCover());
        friend.setUrl(resourcePathVO.getUrl());
        friend.setRemark(BlogUtil.getUserId().toString());
        friend.setType(CommonConst.RESOURCE_PATH_TYPE_FRIEND);
        friend.setStatus(Boolean.FALSE);
        weblinksMapper.insert(friend);
        return R.success();
    }

    /**
     * 查询友链
     *
     * @return 友链列表，按分类分组
     */
    @Operation(summary = "查询友链", description = "用于查询所有已审核通过的友链，按分类分组返回")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功，返回友链列表"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/list")
    public R<Map<String, List<WeblinksVO>>> listFriend() {
        LambdaQueryChainWrapper<Weblinks> wrapper = new LambdaQueryChainWrapper<>(weblinksMapper);
        List<Weblinks> resourcePaths = wrapper.eq(Weblinks::getType, CommonConst.RESOURCE_PATH_TYPE_FRIEND)
                .eq(Weblinks::getStatus, Boolean.TRUE)
                .orderByAsc(Weblinks::getCreateTime)
                .list();
        Map<String, List<WeblinksVO>> collect = new HashMap<>();
        if (!CollectionUtils.isEmpty(resourcePaths)) {
            collect = resourcePaths.stream().map(rp -> {
                WeblinksVO resourcePathVO = new WeblinksVO();
                BeanUtils.copyProperties(rp, resourcePathVO);
                return resourcePathVO;
            }).collect(Collectors.groupingBy(WeblinksVO::getClassify));
        }
        return R.success(collect);
    }



    /**
     * 保存
     */
    @LoginCheck(0)
    @PostMapping("/saveWeblinks")
    public R saveWeblinks(@RequestBody WeblinksVO weblinksVO) {
        if (!StringUtils.hasText(weblinksVO.getTitle()) || !StringUtils.hasText(weblinksVO.getType())) {
            return R.fail("标题和资源类型不能为空！");
        }
        if (CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO.equals(weblinksVO.getType())) {
            weblinksVO.setRemark(BlogUtil.getAdminUser().getId().toString());
        }
        Weblinks Weblinks = new Weblinks();
        BeanUtils.copyProperties(weblinksVO, Weblinks);
        weblinksMapper.insert(Weblinks);
        return R.success();
    }

    /**
     * 删除
     */
    @GetMapping("/deleteWeblinks")
    @LoginCheck(0)
    public R deleteWeblinks(@RequestParam("id") Integer id) {
        weblinksMapper.deleteById(id);
        return R.success();
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    @LoginCheck(0)
    public R updateWeblinks(@RequestBody WeblinksVO weblinksVO) {
        if (!StringUtils.hasText(weblinksVO.getTitle()) || !StringUtils.hasText(weblinksVO.getType())) {
            return R.fail("标题和资源类型不能为空！");
        }
        if (weblinksVO.getId() == null) {
            return R.fail("Id不能为空！");
        }
        if (CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO.equals(weblinksVO.getType())) {
            weblinksVO.setRemark(BlogUtil.getAdminUser().getId().toString());
        }
        Weblinks Weblinks = new Weblinks();
        BeanUtils.copyProperties(weblinksVO, Weblinks);
        weblinksMapper.updateById(Weblinks);
        return R.success();
    }


    /**
     * 查询资源
     */
    @PostMapping("/listWeblinks")
    public R<Page> listWeblinks(@RequestBody BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<Weblinks> wrapper = new LambdaQueryChainWrapper<>(weblinksMapper);
        wrapper.eq(StringUtils.hasText(baseReqVO.getResourceType()), Weblinks::getType, baseReqVO.getResourceType());
        wrapper.eq(StringUtils.hasText(baseReqVO.getClassify()), Weblinks::getClassify, baseReqVO.getClassify());

        Integer userId = BlogUtil.getUserId();
        if (!BlogUtil.getAdminUser().getId().equals(userId)) {
            wrapper.eq(Weblinks::getStatus, Boolean.TRUE);
        } else {
            wrapper.eq(baseReqVO.getStatus() != null, Weblinks::getStatus, baseReqVO.getStatus());
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setColumn(StringUtils.hasText(baseReqVO.getOrder()) ? StrUtil.toUnderlineCase(baseReqVO.getOrder()) : "create_time");
        orderItem.setAsc(!baseReqVO.isDesc());
        List<OrderItem> orderItemList = new ArrayList<>();
        orderItemList.add(orderItem);
        baseReqVO.setOrders(orderItemList);

        wrapper.page(baseReqVO);

        List<Weblinks> weblinks = baseReqVO.getRecords();
        if (!CollectionUtils.isEmpty(weblinks)) {
            List<WeblinksVO> WeblinksVOs = weblinks.stream().map(rp -> {
                WeblinksVO WeblinksVO = new WeblinksVO();
                BeanUtils.copyProperties(rp, WeblinksVO);
                return WeblinksVO;
            }).collect(Collectors.toList());
            baseReqVO.setRecords(WeblinksVOs);
        }
        return R.success(baseReqVO);
    }
}


