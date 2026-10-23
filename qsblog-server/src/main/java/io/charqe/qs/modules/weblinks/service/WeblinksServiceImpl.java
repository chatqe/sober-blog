package io.charqe.qs.modules.weblinks.service;

import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.weblinks.controller.web.vo.WeblinksVO;
import io.charqe.qs.modules.weblinks.convert.WeblinksConvert;
import io.charqe.qs.modules.weblinks.domain.dao.WeblinksMapper;
import io.charqe.qs.modules.weblinks.domain.pojo.Weblinks;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源链接服务实现
 *
 * @author Joy
 * @since 2026-06-21
 */
@Slf4j
@Service
public class WeblinksServiceImpl extends CoreServiceImpl<WeblinksMapper, Weblinks> implements WeblinksService {


    @Override
    public void saveFriend(WeblinksVO vo) {
        // 参数校验
        if (!StringUtils.hasText(vo.getTitle())) {
            throw new BizException("友链标题不能为空");
        }
        if (!StringUtils.hasText(vo.getCover())) {
            throw new BizException("友链封面不能为空");
        }
        if (!StringUtils.hasText(vo.getUrl())) {
            throw new BizException("友链地址不能为空");
        }
        if (!StringUtils.hasText(vo.getIntroduction())) {
            throw new BizException("友链介绍不能为空");
        }

        // 构建实体
        Weblinks friend = new Weblinks();
        friend.setClassify(CommonConst.DEFAULT_FRIEND);
        friend.setTitle(vo.getTitle());
        friend.setDesc(vo.getIntroduction());
        friend.setCover(vo.getCover());
        friend.setUrl(vo.getUrl());
        friend.setRemark(BlogUtil.getUserId().toString());
        friend.setType(CommonConst.RESOURCE_PATH_TYPE_FRIEND);
        friend.setStatus(Boolean.FALSE);

        save(friend);
    }

    @Override
    public Map<String, List<WeblinksVO>> listFriend() {
        List<Weblinks> list = lambdaQuery()
                .eq(Weblinks::getType, CommonConst.RESOURCE_PATH_TYPE_FRIEND)
                .eq(Weblinks::getStatus, Boolean.TRUE)
                .orderByAsc(Weblinks::getCreateTime)
                .list();

        if (CollectionUtils.isEmpty(list)) {
            return new HashMap<>();
        }

        // 使用 Convert 转换（自动处理 getDesc -> introduction 映射）
        return WeblinksConvert.INSTANCE.toVOList(list).stream()
                .collect(java.util.stream.Collectors.groupingBy(WeblinksVO::getClassify));
    }

    @Override
    public void saveWeblinks(WeblinksVO vo) {
        if (!StringUtils.hasText(vo.getTitle())) {
            throw new BizException("标题不能为空");
        }
        if (!StringUtils.hasText(vo.getType())) {
            throw new BizException("资源类型不能为空");
        }

        Weblinks entity = WeblinksConvert.INSTANCE.toEntity(vo);

        // lovePhoto 类型自动设置 remark 为管理员 ID
        if (CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO.equals(vo.getType())) {
            entity.setRemark(BlogUtil.getAdminUser().getId().toString());
        }

        save(entity);
    }

    @Override
    public void deleteWeblinks(Long id) {
        if (id == null) {
            throw new BizException("资源ID不能为空");
        }
        removeById(id);
    }

    @Override
    public void updateWeblinks(WeblinksVO vo) {
        if (!StringUtils.hasText(vo.getTitle())) {
            throw new BizException("标题不能为空");
        }
        if (!StringUtils.hasText(vo.getType())) {
            throw new BizException("资源类型不能为空");
        }
        if (vo.getId() == null) {
            throw new BizException("ID不能为空");
        }

        Weblinks entity = WeblinksConvert.INSTANCE.toEntity(vo);

        // lovePhoto 类型自动设置 remark 为管理员 ID
        if (CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO.equals(vo.getType())) {
            entity.setRemark(BlogUtil.getAdminUser().getId().toString());
        }

        updateById(entity);
    }

    @Override
    public PageResult<WeblinksVO> listWeblinks(String resourceType, String classify,
                                                String order, boolean desc,
                                                int pageNum, int pageSize) {
        // 使用 Lb.q() 构建查询条件
        var wrapper = Lb.<Weblinks>q()
                .eq(StringUtils.hasText(resourceType), Weblinks::getType, resourceType)
                .eq(StringUtils.hasText(classify), Weblinks::getClassify, classify);

        // 权限控制：非管理员只查看已审核通过的资源
        Long userId = BlogUtil.getUserId();
        if (userId == null || !userId.equals(BlogUtil.getAdminUser().getId())) {
            wrapper.eq(Weblinks::getStatus, Boolean.TRUE);
        }

        // 排序：默认按创建时间降序
        wrapper.orderByDesc(Weblinks::getCreateTime);

        // 分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Weblinks> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        page(mpPage, wrapper);

        // 使用 PageResult.fromMpPage 的 mapper 重载，一步完成分页+VO转换
        return PageResult.fromMpPage(mpPage, WeblinksConvert.INSTANCE::toVO);
    }
}
