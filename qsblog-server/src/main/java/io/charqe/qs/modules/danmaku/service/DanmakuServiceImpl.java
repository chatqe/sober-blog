package io.charqe.qs.modules.danmaku.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.PageQuery;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.danmaku.controller.admin.dto.AdminDanmakuPageReqDTO;
import io.charqe.qs.modules.danmaku.controller.admin.vo.AdminDanmakuVO;
import io.charqe.qs.modules.danmaku.controller.web.vo.DanmakuVO;
import io.charqe.qs.modules.danmaku.convert.DanmakuConvert;
import io.charqe.qs.modules.danmaku.domain.dao.DanmakuMapper;
import io.charqe.qs.modules.danmaku.domain.pojo.Danmaku;
import io.charqe.qs.utils.BlogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 弹幕实现类
 *
 * @author sjq
 * @since 2025-03-03 15:00
 */
@Service
public class DanmakuServiceImpl extends CoreServiceImpl<DanmakuMapper, Danmaku> implements DanmakuService {

    @Autowired
    private DanmakuConvert danmakuConvert;

    /**
     * 批量保存
     *
     * @param danmakus
     * @return
     */
    @Override
    public String saveList(List<Danmaku> danmakus) {
        return null;
    }

    /**
     * 最新的20条弹幕留言
     *
     * @return
     */
    @Override
    public List<DanmakuVO> latestDanmaku() {
        List<Danmaku> latestDanmakuList = list(Lb.<Danmaku>q()
                .orderByDesc(Danmaku::getCreateTime)
                .last("limit 20"));
        if (CollectionUtils.isEmpty(latestDanmakuList)) {
            return new ArrayList<>();
        }
        return danmakuConvert.toVOList(latestDanmakuList);
    }

    @Override
    public List<DanmakuVO> randomList() {
        List<Danmaku> danmakus;
        int count = Math.toIntExact(this.count());
        if (count > CommonConst.DANMAKU_COUNT) {
            int i = new Random().nextInt(count + 1 - CommonConst.DANMAKU_COUNT);
            PageQuery pageQuery = new PageQuery();
            pageQuery.setPageNum(i);
            pageQuery.setPageSize(CommonConst.DANMAKU_COUNT);
            IPage<Danmaku> danmakuIPage = page(new Page<>(i, CommonConst.DANMAKU_COUNT), Lb.<Danmaku>q()
                    .select(Danmaku::getId, Danmaku::getAvatar, Danmaku::getMessage, Danmaku::getCreateTime)
                    .orderByDesc(Danmaku::getCreateTime));
            danmakus = danmakuIPage.getRecords();
        } else {
            danmakus = this.list();
        }

        danmakus.forEach(danmaku -> {
            if (!StringUtils.hasText(danmaku.getAvatar())) {
                danmaku.setAvatar(BlogUtil.getRandomAvatar(danmaku.getId().toString()));
            }
        });
        return danmakuConvert.toVOList(danmakus);
    }

    @Override
    public PageResult<AdminDanmakuVO> pageList(AdminDanmakuPageReqDTO dto) {
        LambdaQueryWrapper<Danmaku> wrapper = Lb.<Danmaku>q()
                .like(StringUtils.hasText(dto.getKeyword()), Danmaku::getMessage, dto.getKeyword())
                .ge(dto.getStartTime() != null, Danmaku::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, Danmaku::getCreateTime, dto.getEndTime())
                .orderByDesc(Danmaku::getCreateTime);
        Page<Danmaku> mpPage = page(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return PageResult.fromMpPage(mpPage, danmakuConvert::toAdminVO);
    }
}
