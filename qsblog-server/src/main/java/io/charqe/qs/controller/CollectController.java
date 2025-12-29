package io.charqe.qs.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.common.R;
import io.charqe.qs.dao.ResourcePathMapper;
import io.charqe.qs.model.resourcepath.vo.ResourcePathVO;
import io.charqe.qs.constants.CommonConst;
import io.charqe.qs.model.resourcepath.pojo.ResourcePath;
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
@RequestMapping("/webInfo")
public class CollectController {

    @Autowired
    private ResourcePathMapper resourcePathMapper;

    /**
     * 查询收藏
     */
    @GetMapping("/listCollect")
    public R<Map<String, List<ResourcePathVO>>> listCollect() {
        LambdaQueryChainWrapper<ResourcePath> wrapper = new LambdaQueryChainWrapper<>(resourcePathMapper);
        List<ResourcePath> resourcePaths = wrapper.eq(ResourcePath::getType, CommonConst.RESOURCE_PATH_TYPE_FAVORITES)
                .eq(ResourcePath::getStatus, Boolean.TRUE)
                .orderByAsc(ResourcePath::getTitle)
                .list();
        Map<String, List<ResourcePathVO>> collect = new HashMap<>();
        if (!CollectionUtils.isEmpty(resourcePaths)) {
            collect = resourcePaths.stream().map(rp -> {
                ResourcePathVO resourcePathVO = new ResourcePathVO();
                BeanUtils.copyProperties(rp, resourcePathVO);
                return resourcePathVO;
            }).collect(Collectors.groupingBy(ResourcePathVO::getClassify));
        }
        return R.success(collect);
    }
}


