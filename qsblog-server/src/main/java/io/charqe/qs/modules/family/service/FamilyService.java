package io.charqe.qs.modules.family.service;

import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.modules.family.controller.admin.dto.FamilyPageQueryDTO;
import io.charqe.qs.modules.family.controller.admin.vo.FamilyDetailVO;
import io.charqe.qs.modules.family.controller.admin.vo.FamilyListVO;
import io.charqe.qs.modules.family.controller.web.vo.FamilyWebVO;
import io.charqe.qs.modules.family.domain.pojo.Family;
import io.charqe.qs.common.model.mp.CoreService;

import java.util.List;

/**
 * 家庭卡片服务类
 *
 * @author Joy
 * @since 2026-06-22
 */
public interface FamilyService extends CoreService<Family> {

    /**
     * 分页查询家庭列表（Admin）
     */
    PageResult<FamilyListVO> pageList(FamilyPageQueryDTO query);

    /**
     * 获取家庭详情（Admin）
     */
    FamilyDetailVO getDetail(Long id);

    /**
     * 保存或更新家庭卡片
     */
    void saveOrUpdateFamily(Family family);

    /**
     * 软删除家庭卡片
     */
    void softDelete(Long id);

    /**
     * 切换启用状态
     */
    void toggleStatus(Long id);

    /**
     * 获取公开列表（Web）
     */
    List<FamilyWebVO> listPublic();

    /**
     * 获取公开详情（Web）
     */
    FamilyWebVO getPublicDetail(Long id);

    /**
     * 点赞（防重复）
     */
    boolean like(Long id, Long userId);
}
