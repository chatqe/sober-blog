package io.charqe.qs.modules.weblinks.controller.web;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.model.BaseReqVO;

import io.charqe.qs.common.constants.CommonConst;

import io.charqe.qs.modules.weblinks.controller.web.vo.WeblinksVO;
import io.charqe.qs.modules.weblinks.domain.dao.WeblinksMapper;
import io.charqe.qs.modules.weblinks.domain.pojo.Weblinks;
import io.charqe.qs.utils.BlogUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 资源管理
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/webInfo")
@Tag(name = "资源管理", description = "友链/网络资源等")
public class ResourceAggregationController {

    @Autowired
    private WeblinksMapper weblinksMapper;

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
    @PostMapping("/updateWeblinks")
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


