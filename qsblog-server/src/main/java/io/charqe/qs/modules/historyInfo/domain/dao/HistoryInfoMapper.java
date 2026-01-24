package io.charqe.qs.modules.historyInfo.domain.dao;

import io.charqe.qs.common.model.mp.CoreMapper;
import io.charqe.qs.modules.historyInfo.domain.pojo.HistoryInfo;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
public interface HistoryInfoMapper extends CoreMapper<HistoryInfo> {

    /**
     * 访问IP最多的10个省 各省份独立 IP 数
     */
    @Select("select nation, province, count(distinct ip) as num" +
            " from history_info" +
            " where nation is not null and province is not null" +
            " group by nation, province" +
            " order by num desc" +
            " limit 10")
    List<Map<String, Object>> getHistoryByProvince();

    /**
     * 访问次数最多的10个IP
     */
    @Select("select ip, count(*) as num" +
            " from history_info" +
            " group by ip" +
            " order by num desc" +
            " limit 10")
    List<Map<String, Object>> getHistoryByIp();

    /**
     * 访问24小时内的数据
     */
    @Select("select ip, user_id, nation, province" +
            " from history_info" +
            " where create_time >= (now() - interval 24 hour)")
    List<Map<String, Object>> getHistoryBy24Hour();

    /**
     * 总访问量
     */
    @Select("select count(*) from history_info")
    Long getHistoryCount();
}


