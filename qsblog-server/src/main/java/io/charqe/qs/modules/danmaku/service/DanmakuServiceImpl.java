package io.charqe.qs.modules.danmaku.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.model.PageQuery;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
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

        // for (Danmaku danmaku : danmakus) {
        //     if (!StringUtils.hasText(danmaku.getAvatar())) {
        //         danmaku.setAvatar(BlogUtil.getRandomAvatar(null));
        //     }
        //
        // }

        return null;
    }

    /**
     * 最新的20条弹幕留言
     *
     * @return
     */
    @Override
    public List<Danmaku> latestDanmaku() {
        List<Danmaku> latestDanmakuList = list(Lb.<Danmaku>q()
                // 创建时间倒序排序
                .orderByDesc(Danmaku::getCreateTime)
                // 只获取前20条数据
                .last("limit 20"));
        if (CollectionUtils.isEmpty(latestDanmakuList)) {
            latestDanmakuList = new ArrayList<>();
        }
        return latestDanmakuList;
    }


    @Override
    public List<DanmakuVO> randomList() {
        List<Danmaku> danmakus;
        int count = Math.toIntExact(this.count());
        if (count > CommonConst.DANMAKU_COUNT) {
            int i = new Random().nextInt(count + 1 - CommonConst.DANMAKU_COUNT);
            int treeHoleCount = CommonConst.DANMAKU_COUNT;
            PageQuery pageQuery = new PageQuery();
            pageQuery.setPageNum(i);
            pageQuery.setPageSize(treeHoleCount);
            IPage<Danmaku> danmakuIPage = page(pageQuery, Lb.<Danmaku>q().select(Danmaku::getId, Danmaku::getAvatar, Danmaku::getMessage, Danmaku::getCreateTime)
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
}


