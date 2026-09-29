package io.charqe.qs.modules.file.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.file.domain.dao.FileBizRelationMapper;
import io.charqe.qs.modules.file.domain.pojo.FileBizRef;
import org.springframework.stereotype.Service;

/**
 * 文件业务关系表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class FileBizRefServiceImpl extends CoreServiceImpl<FileBizRelationMapper, FileBizRef> implements FileBizRefService {

}