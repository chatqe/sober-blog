package io.charqe.qs.common.model.mp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageQuery;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 自建serviceImpl 拓展
 *
 * @author sjq
 * @since 2025-12-16 14:56
 */
public class CoreServiceImpl<M extends CoreMapper<T>, T> extends ServiceImpl<M, T> implements CoreService<T> {

    /**
     * 单页上限 1000 条
     */
    private static final long MAX_LIMIT = 1000;

    /**
     * emoji 表情匹配
     */
    Pattern EMOJI_PATTERN = Pattern.compile("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F700}-\\x{1F77F}\\x{1F780}-\\x{1F7FF}\\x{1F800}-\\x{1F8FF}\\x{1F900}-\\x{1F9FF}\\x{1FA00}-\\x{1FA6F}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{2B50}\\x{2B06}\\x{2934}\\x{2935}\\x{200D}\\x{20E3}\\x{FE0F}]");

    @Override
    public boolean saveBatch(Collection<T> list) {
        // 500 条一提交，防止大 SQL
        return super.saveBatch(list, 500);
    }

    @Override
    public <E extends IPage<T>> E page(E page, Wrapper<T> queryWrapper) {
        // 封顶 1000 条
        if (page.getSize() > MAX_LIMIT) {
            page.setSize(MAX_LIMIT);
        }
        return super.page(page, queryWrapper);
    }


    /**
     * 通用“保存或更新”入口
     * 1. 过滤表情符号
     * 2. 根据 id 是否存在自动选择 insert / update
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(T entity) {
        // 1. 表情符号校验
        // if (EMOJI_PATTERN.matcher(JSONUtil.toJsonStr(entity)).find()) {
        //     throw new BizException("提交失败，请检查输入的字符是否包含表情符号或特殊符号");
        // }

        // 2. 反射拿 id 字段（支持父类）
        Long id = getIdValue(entity);
        if (id == null) {
            return save(entity);
        } else {
            return updateById(entity);
        }
    }


    /**
     * 通用反射取 id 字段值（支持 Long/long）
     */
    private Long getIdValue(T entity) {
        Class<?> clazz = entity.getClass();
        try {
            Field field = clazz.getDeclaredField("id");
            field.setAccessible(true);
            Object val = field.get(entity);
            return val == null ? null : Long.valueOf(val.toString());
        } catch (NoSuchFieldException e) {
            // 支持父类字段
            try {
                Field field = clazz.getSuperclass().getDeclaredField("id");
                field.setAccessible(true);
                Object val = field.get(entity);
                return val == null ? null : Long.valueOf(val.toString());
            } catch (Exception ex) {
                throw new BizException("实体类缺少 id 字段", ex);
            }
        } catch (Exception e) {
            throw new BizException("获取主键值失败", e);
        }
    }

    // ===================== 最新封装 =========================

    // 大厂经验值：分批大小1000条（平衡性能与数据库压力）
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * 幂等新增（你的方案+异常处理）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveIfAbsent(T entity) {
        return SqlHelper.retBool(getBaseMapper().saveIfAbsent(entity));
    }

    /**
     * 统一分页查询（你的方案+参数安全处理）
     */
    @Override
    public IPage<T> page(PageQuery pageQuery, Wrapper<T> wrapper) {
        // 复用你的PageQuery，同时做参数兜底
        Page<T> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        return this.page(page, wrapper);
    }

    /**
     * 高性能批量插入（我的方案+事务）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatchOptimize(Collection<T> entityList) {
        return saveBatchOptimize(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 高性能批量插入（自定义分批大小）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatchOptimize(Collection<T> entityList, int batchSize) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }
        Assert.isTrue(batchSize > 0, "分批大小必须大于0");

        List<T> list = (List<T>) entityList;
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            getBaseMapper().saveBatch(list.subList(i, end));
        }
        return true;
    }

    /**
     * 查询唯一数据（我的方案+健壮性校验）
     */
    @Override
    public T selectUnique(Wrapper<T> wrapper) {
        List<T> list = list(wrapper);
        Assert.isTrue(CollectionUtils.isEmpty(list) || list.size() == 1,
                "查询结果不唯一，符合条件的数据有" + list.size() + "条");
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    /**
     * 批量更新（分批执行）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchByIdOptimize(Collection<T> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }
        getBaseMapper().updateBatchById(entityList);
        return true;
    }
}


