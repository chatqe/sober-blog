package com.joy.qs.common.mp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.qs.handle.BizException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 自建serviceImpl 拓展
 *
 * @author sjq
 * @since 2025-12-16 14:56
 */
public class BaseServiceImpl<M extends BaseDao<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {

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
}
