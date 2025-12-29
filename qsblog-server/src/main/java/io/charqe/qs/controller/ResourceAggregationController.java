package io.charqe.qs.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.R;
import io.charqe.qs.dao.ResourcePathMapper;
import io.charqe.qs.model.BaseReqVO;
import io.charqe.qs.model.resourcepath.vo.ResourcePathVO;
import io.charqe.qs.constants.CommonConst;
import io.charqe.qs.model.resourcepath.pojo.ResourcePath;
import io.charqe.qs.utils.BlogUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 资源聚合 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
public class ResourceAggregationController {

    @Autowired
    private ResourcePathMapper resourcePathMapper;

    /**
     * 保存
     */
    @LoginCheck(0)
    @PostMapping("/saveResourcePath")
    public R saveResourcePath(@RequestBody ResourcePathVO resourcePathVO) {
        if (!StringUtils.hasText(resourcePathVO.getTitle()) || !StringUtils.hasText(resourcePathVO.getType())) {
            return R.fail("标题和资源类型不能为空！");
        }
        if (CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO.equals(resourcePathVO.getType())) {
            resourcePathVO.setRemark(BlogUtil.getAdminUser().getId().toString());
        }
        ResourcePath resourcePath = new ResourcePath();
        BeanUtils.copyProperties(resourcePathVO, resourcePath);
        resourcePathMapper.insert(resourcePath);
        return R.success();
    }

    /**
     * 删除
     */
    @GetMapping("/deleteResourcePath")
    @LoginCheck(0)
    public R deleteResourcePath(@RequestParam("id") Integer id) {
        resourcePathMapper.deleteById(id);
        return R.success();
    }

    /**
     * 更新
     */
    @PostMapping("/updateResourcePath")
    @LoginCheck(0)
    public R updateResourcePath(@RequestBody ResourcePathVO resourcePathVO) {
        if (!StringUtils.hasText(resourcePathVO.getTitle()) || !StringUtils.hasText(resourcePathVO.getType())) {
            return R.fail("标题和资源类型不能为空！");
        }
        if (resourcePathVO.getId() == null) {
            return R.fail("Id不能为空！");
        }
        if (CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO.equals(resourcePathVO.getType())) {
            resourcePathVO.setRemark(BlogUtil.getAdminUser().getId().toString());
        }
        ResourcePath resourcePath = new ResourcePath();
        BeanUtils.copyProperties(resourcePathVO, resourcePath);
        resourcePathMapper.updateById(resourcePath);
        return R.success();
    }


    /**
     * 查询资源
     */
    @PostMapping("/listResourcePath")
    public R<Page> listResourcePath(@RequestBody BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<ResourcePath> wrapper = new LambdaQueryChainWrapper<>(resourcePathMapper);
        wrapper.eq(StringUtils.hasText(baseReqVO.getResourceType()), ResourcePath::getType, baseReqVO.getResourceType());
        wrapper.eq(StringUtils.hasText(baseReqVO.getClassify()), ResourcePath::getClassify, baseReqVO.getClassify());

        Integer userId = BlogUtil.getUserId();
        if (!BlogUtil.getAdminUser().getId().equals(userId)) {
            wrapper.eq(ResourcePath::getStatus, Boolean.TRUE);
        } else {
            wrapper.eq(baseReqVO.getStatus() != null, ResourcePath::getStatus, baseReqVO.getStatus());
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setColumn(StringUtils.hasText(baseReqVO.getOrder()) ? StrUtil.toUnderlineCase(baseReqVO.getOrder()) : "create_time");
        orderItem.setAsc(!baseReqVO.isDesc());
        List<OrderItem> orderItemList = new ArrayList<>();
        orderItemList.add(orderItem);
        baseReqVO.setOrders(orderItemList);

        wrapper.page(baseReqVO);

        List<ResourcePath> resourcePaths = baseReqVO.getRecords();
        if (!CollectionUtils.isEmpty(resourcePaths)) {
            List<ResourcePathVO> resourcePathVOs = resourcePaths.stream().map(rp -> {
                ResourcePathVO resourcePathVO = new ResourcePathVO();
                BeanUtils.copyProperties(rp, resourcePathVO);
                return resourcePathVO;
            }).collect(Collectors.toList());
            baseReqVO.setRecords(resourcePathVOs);
        }
        return R.success(baseReqVO);
    }
}


