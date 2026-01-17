package io.charqe.qs.modules.weblinks.controller.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.R;

import io.charqe.qs.common.constants.CommonConst;

import io.charqe.qs.modules.weblinks.controller.web.vo.WeblinksVO;
import io.charqe.qs.modules.weblinks.domain.dao.WeblinksMapper;
import io.charqe.qs.modules.weblinks.domain.pojo.Weblinks;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@RestController
@RequestMapping("/music")
@Tag(name = "music",description = "音乐")
public class MusicController {

    @Autowired
    private WeblinksMapper weblinksMapper;

    /**
     * 查询音乐
     */
    @GetMapping("/listFunny")
    public R<List<Map<String, Object>>> listFunny() {
        QueryWrapper<Weblinks> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("classify, count(*) as count")
                .eq("status", Boolean.TRUE)
                .eq("type", CommonConst.RESOURCE_PATH_TYPE_FUNNY)
                .groupBy("classify");
        List<Map<String, Object>> maps = weblinksMapper.selectMaps(queryWrapper);
        return R.success(maps);
    }

    /**
     * 保存音乐
     */
    @LoginCheck
    @SaveCheck
    @PostMapping("/saveFunny")
    public R saveFunny(@RequestBody WeblinksVO weblinksVO) {
        if (!StringUtils.hasText(weblinksVO.getClassify()) || !StringUtils.hasText(weblinksVO.getCover()) ||
                !StringUtils.hasText(weblinksVO.getUrl()) || !StringUtils.hasText(weblinksVO.getTitle())) {
            return R.fail("信息不全！");
        }
        Weblinks funny = new Weblinks();
        funny.setClassify(weblinksVO.getClassify());
        funny.setTitle(weblinksVO.getTitle());
        funny.setCover(weblinksVO.getCover());
        funny.setUrl(weblinksVO.getUrl());
        funny.setType(CommonConst.RESOURCE_PATH_TYPE_FUNNY);
        funny.setStatus(Boolean.FALSE);
        weblinksMapper.insert(funny);
        return R.success();
    }
}


