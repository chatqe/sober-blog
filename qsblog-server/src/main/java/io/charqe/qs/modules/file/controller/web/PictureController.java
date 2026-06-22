package io.charqe.qs.modules.file.controller.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.weblinks.controller.web.vo.WeblinksVO;
import io.charqe.qs.modules.weblinks.domain.dao.WeblinksMapper;
import io.charqe.qs.modules.weblinks.domain.pojo.Weblinks;
import io.charqe.qs.utils.BlogUtil;
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
@RequestMapping("/webInfo")
public class PictureController {

    @Autowired
    private WeblinksMapper weblinksMapper;

    /**
     * 查询爱情
     */
    @GetMapping("/listAdminLovePhoto")
    public R<List<Map<String, Object>>> listAdminLovePhoto() {
        QueryWrapper<Weblinks> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("classify, count(*) as count")
                .eq("status", Boolean.TRUE)
                .eq("remark", BlogUtil.getAdminUser().getId().toString())
                .eq("type", CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO)
                .groupBy("classify");
        List<Map<String, Object>> maps = weblinksMapper.selectMaps(queryWrapper);

        return R.success(maps);
    }

    /**
     * 保存爱情
     */
    @LoginCheck
    @SaveCheck
    @PostMapping("/saveLovePhoto")
    public R saveLovePhoto(@RequestBody WeblinksVO weblinksVO) {
        if (!StringUtils.hasText(weblinksVO.getClassify()) || !StringUtils.hasText(weblinksVO.getCover()) ||
                !StringUtils.hasText(weblinksVO.getTitle())) {
            return R.fail("信息不全！");
        }
        Weblinks lovePhoto = new Weblinks();
        lovePhoto.setClassify(weblinksVO.getClassify());
        lovePhoto.setTitle(weblinksVO.getTitle());
        lovePhoto.setCover(weblinksVO.getCover());
        lovePhoto.setRemark(BlogUtil.getUserId().toString());
        lovePhoto.setType(CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO);
        lovePhoto.setStatus(Boolean.FALSE);
        weblinksMapper.insert(lovePhoto);
        return R.success();
    }
}


