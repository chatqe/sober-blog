package io.charqe.qs.modules.tag.service;

import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.modules.category.service.CategoryService;
import io.charqe.qs.modules.tag.controller.admin.dto.TagSaveReqDTO;
import io.charqe.qs.modules.tag.controller.admin.vo.TagDetailVO;
import io.charqe.qs.modules.tag.controller.admin.vo.TagListVO;
import io.charqe.qs.modules.tag.controller.web.vo.TagWebVO;
import io.charqe.qs.modules.tag.domain.dao.TagMapper;
import io.charqe.qs.modules.tag.domain.pojo.Tag;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务实现
 *
 * @author Joy
 * @since 2026-06-22
 */
@Slf4j
@Service
public class TagServiceImpl extends CoreServiceImpl<TagMapper, Tag> implements TagService {

    @Resource
    private CategoryService categoryService;

    @Resource
    private QingSiCache qingSiCache;

    @Override
    public PageResult<TagListVO> pageList(Long categoryId, String keyword, int pageNum, int pageSize) {
        var wrapper = Lb.<Tag>q()
                .eq(categoryId != null, Tag::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Tag::getName, keyword)
                .orderByAsc(Tag::getId);

        var mpPage = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), wrapper);
        PageResult<TagListVO> result = new PageResult<>();
        List<TagListVO> voList = mpPage.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        result.setList(voList);
        result.setTotal(mpPage.getTotal());
        result.setPages((int) mpPage.getPages());
        result.setPageNum((int) mpPage.getCurrent());
        result.setPageSize((int) mpPage.getSize());
        return result;
    }

    @Override
    public TagDetailVO getDetail(Long id) {
        Tag tag = getById(id);
        if (tag == null) {
            throw new BizException("标签不存在");
        }
        TagDetailVO vo = toDetailVO(tag);
        // 填充分类名称
        Category category = categoryService.getById(tag.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        return vo;
    }

    @Override
    public void save(TagSaveReqDTO dto) {
        validate(dto);
        Tag entity = toEntity(dto);
        save(entity);
        clearCache();
    }

    @Override
    public void update(TagSaveReqDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("标签ID不能为空");
        }
        validate(dto);

        Tag existing = getById(dto.getId());
        if (existing == null) {
            throw new BizException("标签不存在");
        }

        Tag entity = toEntity(dto);
        entity.setId(dto.getId());
        updateById(entity);
        clearCache();
    }

    @Override
    public void delete(Long id) {
        Tag tag = getById(id);
        if (tag == null) {
            throw new BizException("标签不存在");
        }
        removeById(id);
        clearCache();
    }

    @Override
    public List<TagWebVO> listPublic(Long categoryId) {
        String cacheKey = "tag:public:cat_" + categoryId;
        String cached = qingSiCache.get(cacheKey);
        if (cached != null) {
            return com.alibaba.fastjson2.JSON.parseArray(cached, TagWebVO.class);
        }

        List<TagWebVO> list = lambdaQuery()
                .eq(categoryId != null, Tag::getCategoryId, categoryId)
                .orderByAsc(Tag::getId)
                .list()
                .stream()
                .map(this::toWebVO)
                .collect(Collectors.toList());

        if (!list.isEmpty()) {
            qingSiCache.put(cacheKey, com.alibaba.fastjson2.JSON.toJSONString(list));
        }
        return list;
    }

    private void validate(TagSaveReqDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new BizException("标签名称不能为空");
        }
        if (dto.getCategoryId() == null) {
            throw new BizException("分类ID不能为空");
        }
        // 验证分类是否存在
        if (categoryService.getById(dto.getCategoryId()) == null) {
            throw new BizException("分类不存在");
        }
    }

    private Tag toEntity(TagSaveReqDTO dto) {
        Tag entity = new Tag();
        entity.setName(dto.getName());
        entity.setDesc(dto.getDesc());
        entity.setCategoryId(dto.getCategoryId());
        return entity;
    }

    private TagListVO toListVO(Tag entity) {
        TagListVO vo = new TagListVO();
        vo.setId(entity.getId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setName(entity.getName());
        vo.setDesc(entity.getDesc());
        vo.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        return vo;
    }

    private TagDetailVO toDetailVO(Tag entity) {
        TagDetailVO vo = new TagDetailVO();
        vo.setId(entity.getId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setName(entity.getName());
        vo.setDesc(entity.getDesc());
        vo.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        vo.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().toString() : null);
        return vo;
    }

    private TagWebVO toWebVO(Tag entity) {
        TagWebVO vo = new TagWebVO();
        vo.setId(entity.getId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setName(entity.getName());
        vo.setDesc(entity.getDesc());
        return vo;
    }

    private void clearCache() {
        qingSiCache.del(CommonConst.SORT_INFO);
    }
}
