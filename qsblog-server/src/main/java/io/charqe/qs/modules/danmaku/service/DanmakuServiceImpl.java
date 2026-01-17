package io.charqe.qs.modules.danmaku.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.charqe.qs.modules.danmaku.domain.dao.DanmakuMapper;
import io.charqe.qs.modules.danmaku.domain.pojo.Danmaku;
import io.charqe.qs.modules.danmaku.service.DanmakuService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 弹幕实现类
 *
 * @author sjq
 * @since 2025-03-03 15:00
 */
@Service
public class DanmakuServiceImpl extends ServiceImpl<DanmakuMapper, Danmaku> implements DanmakuService {


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
        List<Danmaku> latestDanmakuList = list(Wrappers.<Danmaku>lambdaQuery()
                // 创建时间倒序排序
                .orderByDesc(Danmaku::getCreateTime)
                // 只获取前20条数据
                .last("limit 20"));
        if (CollectionUtils.isEmpty(latestDanmakuList)) {
            latestDanmakuList = new ArrayList<>();
        }
        return latestDanmakuList;
    }
}


