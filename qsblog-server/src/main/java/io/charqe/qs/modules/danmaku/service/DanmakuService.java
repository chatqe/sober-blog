package io.charqe.qs.modules.danmaku.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.charqe.qs.modules.danmaku.domain.pojo.Danmaku;

import java.util.List;

/**
 * 弹幕实现
 *
 * @author sjq
 * @since 2025-03-03 14:59
 */
public interface DanmakuService extends IService<Danmaku> {
    String saveList(List<Danmaku> danmakus);

    List<Danmaku> latestDanmaku();

}


