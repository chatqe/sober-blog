package io.charqe.qs.modules.weblinks.controller.web;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.weblinks.controller.web.vo.WeblinksVO;
import io.charqe.qs.modules.weblinks.domain.dao.WeblinksMapper;
import io.charqe.qs.modules.weblinks.domain.pojo.Weblinks;
import io.charqe.qs.utils.BlogUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 资源聚合里的友链，其他接口在ResourceAggregationController
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/weblinks")
@Tag(name = "资源链接管理", description = "友链/网络资源")
public class WebLinksController {

    @Resource
    private WeblinksMapper weblinksMapper;

    /**
     * 保存友链
     */
    @LoginCheck
    @PostMapping("/save")
    @SaveCheck
    public R saveFriend(@RequestBody WeblinksVO resourcePathVO) {
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
     */
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
}


