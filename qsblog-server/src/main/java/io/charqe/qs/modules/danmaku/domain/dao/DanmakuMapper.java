package io.charqe.qs.modules.danmaku.domain.dao;

import io.charqe.qs.modules.danmaku.domain.pojo.Danmaku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
public interface DanmakuMapper extends BaseMapper<Danmaku> {

    List<Danmaku> queryAllByLimit(@Param("offset") int offset, @Param("limit") int limit);

}


