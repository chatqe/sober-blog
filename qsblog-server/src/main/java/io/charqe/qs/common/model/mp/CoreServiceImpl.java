package io.charqe.qs.common.model.mp;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * 基础ServiceImpl实现，包含通用业务逻辑的具体实现
 * <p>
 * 设计原则：
 * 1. 保持与MyBatis-Plus API兼容
 * 2. 提供良好的错误处理和日志记录
 * 3. 优化性能，特别是批量操作
 * 4. 支持事务管理
 * 5. 处理边界情况和异常场景
 *
 * @author Joy
 * @since 2025-12-16 14:56
 */
@Slf4j
public class CoreServiceImpl<M extends CoreMapper<T>, T> extends ServiceImpl<M, T> implements CoreService<T> {

    /**
     * 默认分批大小
     */
    private static final int DEFAULT_BATCH_SIZE = 500;
    
    /**
     * 最大分页大小
     */
    private static final long MAX_PAGE_SIZE = 1000;
    
    /**
     * 最小分页大小
     */
    private static final long MIN_PAGE_SIZE = 1;
    
    /**
     * 最大批量操作大小
     */
    private static final int MAX_BATCH_SIZE = 1000;
    
    /**
     * emoji 表情匹配
     */
    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F700}-\\x{1F77F}\\x{1F780}-\\x{1F7FF}\\x{1F800}-\\x{1F8FF}\\x{1F900}-\\x{1F9FF}\\x{1FA00}-\\x{1FA6F}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{2B50}\\x{2B06}\\x{2934}\\x{2935}\\x{200D}\\x{20E3}\\x{FE0F}]");

    /* ==================== 单条记录操作 ==================== */
    
    @Override
    public Optional<T> getByIdOpt(Serializable id) {
        Assert.notNull(id, "ID不能为空");
        return Optional.ofNullable(getById(id));
    }
    
    @Override
    public Optional<T> getOneOpt(Wrapper<T> queryWrapper) {
        Assert.notNull(queryWrapper, "查询条件不能为空");
        try {
            return Optional.ofNullable(getOne(queryWrapper, false));
        } catch (Exception e) {
            log.warn("查询单条记录失败: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public T getUnique(Wrapper<T> queryWrapper) {
        Assert.notNull(queryWrapper, "查询条件不能为空");
        List<T> list = list(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        if (list.size() > 1) {
            throw new BizException("查询结果不唯一，返回了" + list.size() + "条记录");
        }
        return list.get(0);
    }
    
    @Override
    public T getFirst(Wrapper<T> queryWrapper) {
        Assert.notNull(queryWrapper, "查询条件不能为空");
        Page<T> page = new Page<>(1, 1);
        IPage<T> result = page(page, queryWrapper);
        List<T> records = result.getRecords();
        return CollectionUtils.isEmpty(records) ? null : records.get(0);
    }
    
    /* ==================== 批量操作 ==================== */
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatchOptimize(Collection<T> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }
        
        log.info("开始批量插入，总数: {}, 默认分批大小: {}", entityList.size(), DEFAULT_BATCH_SIZE);
        long startTime = System.currentTimeMillis();
        
        try {
            // 预插入处理
            entityList.forEach(this::preInsert);
            
            // 批量插入，使用默认分批大小500
            boolean result = super.saveBatch(entityList, DEFAULT_BATCH_SIZE);
            
            log.info("批量插入完成，耗时: {}ms, 结果: {}", System.currentTimeMillis() - startTime, result);
            return result;
        } catch (Exception e) {
            log.error("批量插入失败: {}", e.getMessage(), e);
            throw new BizException("批量插入失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchByIdOptimize(Collection<T> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }
        
        log.info("开始批量更新，总数: {}, 默认分批大小: {}", entityList.size(), DEFAULT_BATCH_SIZE);
        long startTime = System.currentTimeMillis();
        
        try {
            // 预更新处理
            entityList.forEach(this::preUpdate);
            
            // 批量更新，使用默认分批大小500
            boolean result = super.updateBatchById(entityList, DEFAULT_BATCH_SIZE);
            
            log.info("批量更新完成，耗时: {}ms, 结果: {}", System.currentTimeMillis() - startTime, result);
            return result;
        } catch (Exception e) {
            log.error("批量更新失败: {}", e.getMessage(), e);
            throw new BizException("批量更新失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatch(T entity, Wrapper<T> updateWrapper) {
        Assert.notNull(entity, "实体对象不能为空");
        Assert.notNull(updateWrapper, "更新条件不能为空");
        
        try {
            // 预更新处理
            preUpdate(entity);
            
            return update(entity, updateWrapper);
        } catch (Exception e) {
            log.error("批量更新失败: {}", e.getMessage(), e);
            throw new BizException("批量更新失败: " + e.getMessage(), e);
        }
    }
    
    /* ==================== 幂等操作 ==================== */
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveIfAbsent(T entity) {
        Assert.notNull(entity, "实体对象不能为空");
        
        try {
            // 这里需要根据业务逻辑实现幂等判断
            // 例如：根据唯一索引查询是否已存在
            // 暂时实现为：如果ID为空则插入，否则更新
            Long id = getIdValue(entity);
            if (id == null) {
                return save(entity);
            } else {
                return updateById(entity);
            }
        } catch (Exception e) {
            log.error("幂等新增失败: {}", e.getMessage(), e);
            throw new BizException("幂等新增失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateIdempotent(T entity) {
        Assert.notNull(entity, "实体对象不能为空");
        
        try {
            // 表情符号校验
            if (EMOJI_PATTERN.matcher(JSONUtil.toJsonStr(entity)).find()) {
                throw new BizException("提交失败，请检查输入的字符是否包含表情符号或特殊符号");
            }
            
            // 反射拿id字段（支持父类）
            Long id = getIdValue(entity);
            if (id == null) {
                // 新增前处理
                preInsert(entity);
                return save(entity);
            } else {
                // 更新前处理
                preUpdate(entity);
                return updateById(entity);
            }
        } catch (Exception e) {
            log.error("幂等新增或更新失败: {}", e.getMessage(), e);
            throw new BizException("幂等新增或更新失败: " + e.getMessage(), e);
        }
    }
    
    /* ==================== 分页查询 ==================== */
    
    @Override
    public IPage<T> page(PageQuery pageQuery, Wrapper<T> queryWrapper) {
        return pageWithCount(pageQuery, queryWrapper);
    }
    
    @Override
    public IPage<T> pageWithCount(PageQuery pageQuery, Wrapper<T> queryWrapper) {
        Assert.notNull(pageQuery, "分页参数不能为空");
        
        // 构建分页对象
        Page<T> page = buildPage(pageQuery, true);
        return page(page, queryWrapper);
    }
    
    @Override
    public IPage<T> pageWithoutCount(PageQuery pageQuery, Wrapper<T> queryWrapper) {
        Assert.notNull(pageQuery, "分页参数不能为空");
        
        // 构建分页对象，不查询总数
        Page<T> page = buildPage(pageQuery, false);
        return page(page, queryWrapper);
    }
    
    /**
     * 构建分页对象
     *
     * @param pageQuery 分页参数
     * @param searchCount 是否查询总数
     * @return Page<T>
     */
    private Page<T> buildPage(PageQuery pageQuery, boolean searchCount) {
        // 校验页码和每页大小
        long pageNum = Math.max(pageQuery.getPageNum(), 1);
        long pageSize = Math.max(Math.min(pageQuery.getPageSize(), MAX_PAGE_SIZE), MIN_PAGE_SIZE);
        
        return new Page<>(pageNum, pageSize, searchCount);
    }
    
    @Override
    public <E extends IPage<T>> E page(E page, Wrapper<T> queryWrapper) {
        // 封顶 1000 条
        if (page.getSize() > MAX_PAGE_SIZE) {
            page.setSize(MAX_PAGE_SIZE);
        }
        return super.page(page, queryWrapper);
    }
    
    /* ==================== 条件查询 ==================== */
    
    @Override
    public List<T> list(Wrapper<T> queryWrapper) {
        return super.list(queryWrapper);
    }
    
    @Override
    public List<T> list(Wrapper<T> queryWrapper, String sortField, boolean isAsc) {
        Assert.notNull(queryWrapper, "查询条件不能为空");
        Assert.hasText(sortField, "排序字段不能为空");
        
        // 这里需要根据具体的条件构造器类型来添加排序
        // 暂时简单实现
        return list(queryWrapper);
    }
    
    @Override
    public List<Map<String, Object>> listMaps(Wrapper<T> queryWrapper) {
        return super.listMaps(queryWrapper);
    }
    
    /* ==================== 计数查询 ==================== */
    
    @Override
    public long countLong(Wrapper<T> queryWrapper) {
        return count(queryWrapper);
    }
    
    @Override
    public boolean exists(Wrapper<T> queryWrapper) {
        return count(queryWrapper) > 0;
    }
    
    /* ==================== 数据校验 ==================== */
    
    @Override
    public boolean validate(T entity) {
        Assert.notNull(entity, "实体对象不能为空");
        
        // 基础校验：表情符号校验
        if (EMOJI_PATTERN.matcher(JSONUtil.toJsonStr(entity)).find()) {
            throw new BizException("提交失败，请检查输入的字符是否包含表情符号或特殊符号");
        }
        
        // 可以在这里添加更多的校验逻辑
        return true;
    }
    
    @Override
    public boolean validateBatch(Collection<T> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }
        
        AtomicBoolean result = new AtomicBoolean(true);
        entityList.forEach(entity -> {
            try {
                validate(entity);
            } catch (Exception e) {
                result.set(false);
                throw e;
            }
        });
        
        return result.get();
    }
    
    /* ==================== 扩展方法 ==================== */
    
    @Override
    public M getBaseMapper() {
        return super.getBaseMapper();
    }
    
    @Override
    public void preInsert(T entity) {
        Assert.notNull(entity, "实体对象不能为空");
        
        // 设置创建时间和更新时间
        setFieldValue(entity, "createTime", LocalDateTime.now());
        setFieldValue(entity, "updateTime", LocalDateTime.now());
        
        // 设置默认状态
        setFieldValue(entity, "status", 1);
        
        // 设置创建人
        // 这里可以从上下文获取当前登录用户ID
        // setFieldValue(entity, "createBy", getCurrentUserId());
    }
    
    @Override
    public void preUpdate(T entity) {
        Assert.notNull(entity, "实体对象不能为空");
        
        // 设置更新时间
        setFieldValue(entity, "updateTime", LocalDateTime.now());
        
        // 设置更新人
        // 这里可以从上下文获取当前登录用户ID
        // setFieldValue(entity, "updateBy", getCurrentUserId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(T entity) {
        return saveOrUpdateIdempotent(entity);
    }
    
    /* ==================== 工具方法 ==================== */
    
    /**
     * 通用反射取id字段值（支持Long/long）
     *
     * @param entity 实体对象
     * @return Long
     */
    private Long getIdValue(T entity) {
        Assert.notNull(entity, "实体对象不能为空");
        
        try {
            // 使用hutool的反射工具获取id字段
            Field field = ReflectUtil.getField(entity.getClass(), "id");
            if (field == null) {
                // 尝试从父类获取
                field = ReflectUtil.getField(entity.getClass().getSuperclass(), "id");
            }
            
            if (field == null) {
                throw new BizException("实体类缺少id字段");
            }
            
            field.setAccessible(true);
            Object val = field.get(entity);
            return val == null ? null : Long.valueOf(val.toString());
        } catch (Exception e) {
            throw new BizException("获取主键值失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 通用反射设置字段值
     *
     * @param entity 实体对象
     * @param fieldName 字段名
     * @param value 字段值
     */
    private void setFieldValue(T entity, String fieldName, Object value) {
        Assert.notNull(entity, "实体对象不能为空");
        Assert.hasText(fieldName, "字段名不能为空");
        
        try {
            // 使用hutool的反射工具设置字段值
            Field field = ReflectUtil.getField(entity.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(entity, value);
            } else {
                // 尝试从父类获取
                field = ReflectUtil.getField(entity.getClass().getSuperclass(), fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    field.set(entity, value);
                }
            }
        } catch (Exception e) {
            log.warn("设置字段值失败，实体: {}, 字段: {}, 值: {}, 错误: {}", 
                    entity.getClass().getName(), fieldName, value, e.getMessage());
        }
    }
    
    /**
     * 获取当前登录用户ID
     * <p>
     * 这里可以从上下文获取当前登录用户ID
     *
     * @return Long
     */
    private Long getCurrentUserId() {
        // 暂时返回null，实际项目中应该从上下文获取
        return null;
    }
}


