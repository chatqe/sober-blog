package io.charqe.qs.modules.weblinks.controller.web;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.weblinks.controller.web.vo.WeblinksVO;
import io.charqe.qs.modules.weblinks.domain.dao.WeblinksMapper;
import io.charqe.qs.modules.weblinks.domain.pojo.Weblinks;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 资源聚合里的收藏夹，其他接口在ResourceAggregationController
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@RestController
@RequestMapping("/collect")
@Tag(name = "collect", description = "收藏夹")
public class CollectController {

    @Autowired
    private WeblinksMapper weblinksMapper;

    /**
     * 查询收藏
     */
    @GetMapping("/list")
    public R<Map<String, List<WeblinksVO>>> listCollect() {
        LambdaQueryChainWrapper<Weblinks> wrapper = new LambdaQueryChainWrapper<>(weblinksMapper);
        List<Weblinks> resourcePaths = wrapper.eq(Weblinks::getType, CommonConst.RESOURCE_PATH_TYPE_FAVORITES)
                .eq(Weblinks::getStatus, Boolean.TRUE)
                .orderByAsc(Weblinks::getTitle)
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


