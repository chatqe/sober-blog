package io.charqe.qs.modules.access.domain.dao;

import io.charqe.qs.common.model.mp.CoreMapper;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 访问日志Mapper
 *
 * @author Joy
 * @since 2026-01-19 15:06
 */
@Mapper
public interface AccessLogMapper extends CoreMapper<AccessLog> {

}
