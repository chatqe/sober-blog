package io.charqe.qs.modules.tag.service;

import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.modules.tag.controller.admin.dto.TagSaveReqDTO;
import io.charqe.qs.modules.tag.controller.admin.vo.TagDetailVO;
import io.charqe.qs.modules.tag.controller.admin.vo.TagListVO;
import io.charqe.qs.modules.tag.controller.web.vo.TagDictVO;
import io.charqe.qs.modules.tag.controller.web.vo.TagWebVO;
import io.charqe.qs.modules.sysdict.controller.web.vo.DictVO;
import io.charqe.qs.modules.tag.domain.pojo.Tag;
import io.charqe.qs.common.model.mp.CoreService;

import java.util.List;

/**
 * 标签服务类
 *
 * @author Joy
 * @since 2026-06-22
 */
public interface TagService extends CoreService<Tag> {

    /**
     * 分页查询标签列表（Admin）
     */
    PageResult<TagListVO> pageList(Long categoryId, String keyword, int pageNum, int pageSize);

    /**
     * 获取标签详情（Admin）
     */
    TagDetailVO getDetail(Long id);

    /**
     * 新增标签
     */
    void save(TagSaveReqDTO dto);

    /**
     * 更新标签
     */
    void update(TagSaveReqDTO dto);

    /**
     * 删除标签
     */
    void delete(Long id);

    /**
     * 获取公开标签列表（Web）
     */
    List<TagWebVO> listPublic(Long categoryId);

    /**
     * 获取标签下拉字典列表
     */
    List<TagDictVO> getDictList();

    /**
     * 获取标签云名称列表（Web）
     */
    List<String> getCloudNames();
}
