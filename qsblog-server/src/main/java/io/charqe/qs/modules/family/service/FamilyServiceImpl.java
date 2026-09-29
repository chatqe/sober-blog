package io.charqe.qs.modules.family.service;

import io.charqe.qs.common.constants.CacheKeyConst;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.family.controller.admin.dto.FamilyPageQueryDTO;
import io.charqe.qs.modules.family.controller.admin.vo.FamilyDetailVO;
import io.charqe.qs.modules.family.controller.admin.vo.FamilyListVO;
import io.charqe.qs.modules.family.controller.web.vo.FamilyWebVO;
import io.charqe.qs.modules.family.domain.dao.FamilyMapper;
import io.charqe.qs.modules.family.domain.pojo.Family;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 家庭卡片服务实现
 *
 * @author Joy
 * @since 2026-06-22
 */
@Slf4j
@Service
public class FamilyServiceImpl extends CoreServiceImpl<FamilyMapper, Family> implements FamilyService {

    @Resource
    private QingSiCache qingSiCache;

    private static final String CACHE_PREFIX = "family:";

    @Override
    public PageResult<FamilyListVO> pageList(FamilyPageQueryDTO query) {
        var wrapper = Lb.<Family>q()
                .eq(query.getStatus() != null, Family::getStatus, query.getStatus())
                .orderByDesc(Family::getCreateTime);

        var mpPage = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        PageResult<FamilyListVO> result = new PageResult<>();
        List<FamilyListVO> voList = mpPage.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        result.setList(voList);
        result.setTotal(mpPage.getTotal());
        result.setPages((int) mpPage.getPages());
        result.setPageNum((int) mpPage.getCurrent());
        result.setPageSize((int) mpPage.getSize());
        return result;
    }

    @Override
    public FamilyDetailVO getDetail(Long id) {
        Family family = getById(id);
        if (family == null) {
            throw new BizException("家庭卡片不存在");
        }
        return toDetailVO(family);
    }

    @Override
    public void saveOrUpdateFamily(Family family) {
        Family existing = lambdaQuery().eq(Family::getUserId, family.getUserId()).oneOpt().orElse(null);
        if (existing != null) {
            // 更新
            family.setId(existing.getId());
            updateById(family);
        } else {
            // 新增
            save(family);
        }
        clearFamilyCache();
    }

    @Override
    public void softDelete(Long id) {
        removeById(id);
        clearFamilyCache();
    }

    @Override
    public void toggleStatus(Long id) {
        Family family = getById(id);
        if (family == null) {
            throw new BizException("家庭卡片不存在");
        }
        family.setStatus(!family.getStatus());
        updateById(family);
        clearFamilyCache();
    }

    @Override
    public List<FamilyWebVO> listPublic() {
        String cacheKey = CACHE_PREFIX + "public:list";
        String cached = qingSiCache.get(cacheKey);
        if (cached != null) {
            return com.alibaba.fastjson2.JSON.parseArray(cached, FamilyWebVO.class);
        }

        List<FamilyWebVO> list = lambdaQuery()
                .eq(Family::getStatus, true)
                .orderByAsc(Family::getCreateTime)
                .list()
                .stream()
                .map(this::toWebVO)
                .collect(Collectors.toList());

        if (!list.isEmpty()) {
            qingSiCache.put(cacheKey, com.alibaba.fastjson2.JSON.toJSONString(list));
        }
        return list;
    }

    @Override
    public FamilyWebVO getPublicDetail(Long id) {
        Family family = getById(id);
        if (family == null || family.getStatus() == null || !family.getStatus()) {
            throw new BizException("家庭卡片不存在或未启用");
        }
        return toWebVO(family);
    }

    @Override
    public boolean like(Long id, Long userId) {
        Family family = getById(id);
        if (family == null) {
            throw new BizException("家庭卡片不存在");
        }

        // 防重复点赞：Redis key
        String likeKey = CacheKeyConst.USER_LIKES + "family:" + userId + ":family:" + id;
        if (qingSiCache.hasKey(likeKey)) {
            throw new BizException("您已经点过赞了");
        }

        // 点赞计数 +1
        family.setLikeCount((family.getLikeCount() == null ? 0 : family.getLikeCount()) + 1);
        updateById(family);

        // 记录点赞（1小时过期，用户刷新页面后可再次点赞）
        qingSiCache.put(likeKey, "1", 3600);

        clearFamilyCache();
        return true;
    }

    private FamilyListVO toListVO(Family entity) {
        FamilyListVO vo = new FamilyListVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setManName(entity.getManName());
        vo.setWomanName(entity.getWomanName());
        vo.setTiming(entity.getTiming());
        vo.setStatus(entity.getStatus());
        vo.setLikeCount(entity.getLikeCount());
        vo.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        return vo;
    }

    private FamilyDetailVO toDetailVO(Family entity) {
        FamilyDetailVO vo = new FamilyDetailVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setBgCover(entity.getBgCover());
        vo.setManCover(entity.getManCover());
        vo.setWomanCover(entity.getWomanCover());
        vo.setManName(entity.getManName());
        vo.setWomanName(entity.getWomanName());
        vo.setTiming(entity.getTiming());
        vo.setCountdownTitle(entity.getCountdownTitle());
        vo.setCountdownTime(entity.getCountdownTime());
        vo.setFamilyInfo(entity.getFamilyInfo());
        vo.setStatus(entity.getStatus());
        vo.setLikeCount(entity.getLikeCount());
        vo.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        vo.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().toString() : null);
        return vo;
    }

    private FamilyWebVO toWebVO(Family entity) {
        FamilyWebVO vo = new FamilyWebVO();
        vo.setId(entity.getId());
        vo.setBgCover(entity.getBgCover());
        vo.setManName(entity.getManName());
        vo.setWomanName(entity.getWomanName());
        vo.setTiming(entity.getTiming());
        vo.setLikeCount(entity.getLikeCount());
        return vo;
    }

    private void clearFamilyCache() {
        qingSiCache.del(CACHE_PREFIX + "public:list");
    }
}
