package io.charqe.qs.modules.treehole.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.charqe.qs.modules.treehole.domain.dao.TreeHoleMapper;
import io.charqe.qs.modules.treehole.domain.pojo.TreeHole;
import io.charqe.qs.modules.treehole.service.TreeHoleService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 树洞实现类
 *
 * @author sjq
 * @since 2025-03-03 15:00
 */
@Service
public class TreeHoleServiceImpl extends ServiceImpl<TreeHoleMapper, TreeHole> implements TreeHoleService {


    /**
     * 批量保存
     *
     * @param treeHoles
     * @return
     */
    @Override
    public String saveList(List<TreeHole> treeHoles) {

        // for (TreeHole treeHole : treeHoles) {
        //     if (!StringUtils.hasText(treeHole.getAvatar())) {
        //         treeHole.setAvatar(BlogUtil.getRandomAvatar(null));
        //     }
        //
        // }

        return null;
    }

    /**
     * 最新的20条树洞留言
     *
     * @return
     */
    @Override
    public List<TreeHole> latestTreeHole() {
        List<TreeHole> latestTreeHoleList = list(Wrappers.<TreeHole>lambdaQuery()
                // 创建时间倒序排序
                .orderByDesc(TreeHole::getCreateTime)
                // 只获取前20条数据
                .last("limit 20"));
        if (CollectionUtils.isEmpty(latestTreeHoleList)) {
            latestTreeHoleList = new ArrayList<>();
        }
        return latestTreeHoleList;
    }
}


