package io.charqe.qs.modules.user.domain.dao;

import io.charqe.qs.modules.user.controller.web.dto.ArticleUserNameDTO;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.common.model.mp.CoreMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口，继承自CoreMapper，享受通用CRUD及扩展功能
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
public interface UserMapper extends CoreMapper<User> {

    List<ArticleUserNameDTO> selectBatchIdsWithUsername(@Param("ids") List<Long> ids);
}


