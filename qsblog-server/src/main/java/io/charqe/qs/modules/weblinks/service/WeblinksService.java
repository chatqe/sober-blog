package io.charqe.qs.modules.weblinks.service;

import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.modules.weblinks.domain.pojo.Weblinks;
import io.charqe.qs.modules.weblinks.controller.web.vo.WeblinksVO;
import io.charqe.qs.common.model.mp.CoreService;

import java.util.List;
import java.util.Map;

/**
 * 资源链接服务接口
 *
 * @author Joy
 * @since 2026-06-21
 */
public interface WeblinksService extends CoreService<Weblinks> {

    /**
     * 保存友链申请
     *
     * @param vo 友链信息
     */
    void saveFriend(WeblinksVO vo);

    /**
     * 查询已审核通过的友链列表，按分类分组
     *
     * @return 分类 -> 友链列表
     */
    Map<String, List<WeblinksVO>> listFriend();

    /**
     * 保存通用资源
     *
     * @param vo 资源信息
     */
    void saveWeblinks(WeblinksVO vo);

    /**
     * 删除资源
     *
     * @param id 资源ID
     */
    void deleteWeblinks(Long id);

    /**
     * 更新资源
     *
     * @param vo 资源信息
     */
    void updateWeblinks(WeblinksVO vo);

    /**
     * 分页查询资源列表
     *
     * @param resourceType 资源类型
     * @param classify 分类
     * @param order 排序字段
     * @param desc 是否降序
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<WeblinksVO> listWeblinks(String resourceType, String classify,
                                         String order, boolean desc,
                                         int pageNum, int pageSize);
}
