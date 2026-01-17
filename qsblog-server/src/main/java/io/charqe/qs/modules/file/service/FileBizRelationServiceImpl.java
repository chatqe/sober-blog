package io.charqe.qs.modules.file.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.charqe.qs.modules.file.domain.dao.FileBizRelationMapper;
import io.charqe.qs.modules.file.domain.pojo.FileBizRelation;
import io.charqe.qs.modules.file.service.FileBizRelationService;
import org.springframework.stereotype.Service;

/**
 * 文件业务关系表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class FileBizRelationServiceImpl extends ServiceImpl<FileBizRelationMapper, FileBizRelation> implements FileBizRelationService {

}