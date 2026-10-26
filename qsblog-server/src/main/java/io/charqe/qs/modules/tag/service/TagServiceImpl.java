package io.charqe.qs.modules.tag.service;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.constants.CacheKeyConst;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.OptionScopeEnum;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.OptionVO;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.modules.category.service.CategoryService;
import io.charqe.qs.modules.tag.controller.admin.dto.TagSaveReqDTO;
import io.charqe.qs.modules.tag.controller.admin.vo.TagDetailVO;
import io.charqe.qs.modules.tag.controller.admin.vo.TagListVO;
import io.charqe.qs.modules.tag.controller.web.vo.TagDictVO;
import io.charqe.qs.modules.tag.controller.web.vo.TagWebVO;
import io.charqe.qs.modules.sysdict.controller.web.vo.DictVO;
import io.charqe.qs.modules.tag.convert.TagConvert;
import io.charqe.qs.modules.tag.domain.dao.TagMapper;
import io.charqe.qs.modules.tag.domain.pojo.Tag;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
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
        List<TagListVO> voList = mpPage.getRecords().stream().map(TagConvert.INSTANCE::toListVO).collect(Collectors.toList());
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
        TagDetailVO vo = TagConvert.INSTANCE.toDetailVO(tag);
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
        Tag entity = TagConvert.INSTANCE.toEntity(dto);
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

        Tag entity = TagConvert.INSTANCE.toEntity(dto);
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
        String cacheKey = CacheKeyConst.TAG_PUBLIC + categoryId;
        String cached = qingSiCache.get(cacheKey);
        if (cached != null) {
            return com.alibaba.fastjson2.JSON.parseArray(cached, TagWebVO.class);
        }

        List<TagWebVO> list = lambdaQuery()
                .eq(categoryId != null, Tag::getCategoryId, categoryId)
                .orderByAsc(Tag::getId)
                .list()
                .stream()
                .map(TagConvert.INSTANCE::toWebVO)
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

    private void clearCache() {
        qingSiCache.del(CommonConst.CATEGORY_INFO);
        qingSiCache.del(CommonConst.TAG_DICT);
        // 标签下拉选项（新结构版本 key）
        qingSiCache.del(CacheKeyConst.TAG_OPTIONS);
        // 历史缺陷：此处只删了固定 key，未处理按分类分片的 tag:public:cat_*，
        // 导致后台改标签后前台分类页长期脏读，现按前缀批量清理
        Set<String> publicKeys = qingSiCache.keys(CacheKeyConst.TAG_PUBLIC + "*");
        if (publicKeys != null && !publicKeys.isEmpty()) {
            qingSiCache.del(publicKeys);
        }
    }

    @Override
    public List<OptionVO> listOptions(OptionScopeEnum scope) {
        // tag 表无状态列，PUBLIC / ALL 口径共用一份缓存；scope 仅用于与分类端点契约对称
        String cached = qingSiCache.get(CacheKeyConst.TAG_OPTIONS);
        if (cached != null) {
            return JSON.parseArray(cached, OptionVO.class);
        }

        // isDel 由 BaseDO 的 @TableLogic 自动过滤；排序与分类保持一致采用 sort、id 升序
        List<OptionVO> options = lambdaQuery()
                .orderByAsc(Tag::getSort)
                .orderByAsc(Tag::getId)
                .list()
                .stream()
                .map(t -> {
                    OptionVO vo = OptionVO.of(t.getId(), t.getName(), t.getSort());
                    vo.setCategoryId(t.getCategoryId());
                    return vo;
                }).toList();

        if (!options.isEmpty()) {
            qingSiCache.put(CacheKeyConst.TAG_OPTIONS, options);
        }
        return options;
    }

    /**
     * @deprecated 请改用 {@link #listOptions(OptionScopeEnum)}
     */
    @Deprecated
    @Override
    public List<TagDictVO> getDictList() {
        String cached = qingSiCache.get(CommonConst.TAG_DICT);
        if (cached != null) {
            return JSON.parseArray(cached, TagDictVO.class);
        }
        List<Tag> list = lambdaQuery().orderByAsc(Tag::getId).list();
        List<TagDictVO> dictList = list.stream().map(t -> {
            TagDictVO vo = new TagDictVO();
            vo.setCateId(t.getCategoryId());
            vo.setValue(t.getTagDesc());
            vo.setLabel(t.getName());
            vo.setSort(t.getSort());
            return vo;
        }).toList();
        if (!dictList.isEmpty()) {
            qingSiCache.put(CommonConst.TAG_DICT, JSON.toJSONString(dictList));
        }
        return dictList;
    }

    @Override
    public List<String> getCloudNames() {
        // BaseDO.isDel 带 @TableLogic，MyBatis-Plus 会自动追加 is_del = 0 条件
        return lambdaQuery()
                .orderByAsc(Tag::getSort)
                .orderByAsc(Tag::getId)
                .list()
                .stream()
                .map(Tag::getName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }
}
