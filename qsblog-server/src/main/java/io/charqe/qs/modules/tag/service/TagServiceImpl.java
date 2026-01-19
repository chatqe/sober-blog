package io.charqe.qs.modules.tag.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.tag.domain.dao.TagMapper;
import io.charqe.qs.modules.tag.domain.pojo.Tag;
import io.charqe.qs.modules.tag.service.TagService;
import org.springframework.stereotype.Service;

/**
 * 标签表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class TagServiceImpl extends CoreServiceImpl<TagMapper, Tag> implements TagService {

}