package com.ld.poetry.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ld.poetry.entity.TreeHole;

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
