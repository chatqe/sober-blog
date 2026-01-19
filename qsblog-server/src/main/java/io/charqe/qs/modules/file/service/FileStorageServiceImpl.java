package io.charqe.qs.modules.file.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.file.domain.dao.FileStorageMapper;
import io.charqe.qs.modules.file.domain.pojo.FileStorage;
import io.charqe.qs.modules.file.service.FileStorageService;
import org.springframework.stereotype.Service;

/**
 * 文件存储表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class FileStorageServiceImpl extends CoreServiceImpl<FileStorageMapper, FileStorage> implements FileStorageService {

}