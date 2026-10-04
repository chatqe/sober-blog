package io.charqe.qs.modules.danmaku.service;

import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.danmaku.controller.admin.dto.AdminDanmakuPageReqDTO;
import io.charqe.qs.modules.danmaku.controller.admin.vo.AdminDanmakuPageVO;
import io.charqe.qs.modules.danmaku.controller.web.vo.DanmakuVO;
import io.charqe.qs.modules.danmaku.domain.pojo.Danmaku;

import java.util.List;

/**
 * 弹幕实现
 *
 * @author sjq
 * @since 2025-03-03 14:59
 */
public interface DanmakuService extends CoreService<Danmaku> {
    String saveList(List<Danmaku> danmakus);

    List<DanmakuVO> latestDanmaku();

    /**
     * 随机获取弹幕留言
     *
     * @return
     */
    List<DanmakuVO> randomList();

    AdminDanmakuPageVO pageList(AdminDanmakuPageReqDTO dto);
}
