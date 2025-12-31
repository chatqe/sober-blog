package io.charqe.qs.modules.treehole.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.charqe.qs.modules.treehole.domain.pojo.TreeHole;

import java.util.List;

/**
 * 树洞实现
 *
 * @author sjq
 * @since 2025-03-03 14:59
 */
public interface TreeHoleService extends IService<TreeHole> {
    String saveList(List<TreeHole> treeHoles);

    List<TreeHole> latestTreeHole();

}


