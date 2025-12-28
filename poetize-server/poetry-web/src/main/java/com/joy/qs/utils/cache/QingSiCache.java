package com.joy.qs.utils.cache;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis工具类
 *
 * @author sjq
 * @since 2025-02-28 18:58
 */
@Slf4j
@Component
public class QingSiCache {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    /** -------------------key相关操作--------------------- */

    /**
     * 删除key
     *
     * @param key
     */
    public void del(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 批量删除key
     *
     * @param keys
     */
    public void del(Collection<String> keys) {
        stringRedisTemplate.delete(keys);
    }


    /**
     * 是否存在key
     *
     * @param key
     * @return
     */
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     *
     * @param key
     * @param timeout
     * @param unit
     * @return
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.expire(key, timeout, unit);
    }

    /**
     * 设置过期时间
     *
     * @param key
     * @param date
     * @return
     */
    public Boolean expireAt(String key, Date date) {
        return stringRedisTemplate.expireAt(key, date);
    }

    /**
     * 查找匹配的key
     *
     * @param pattern
     * @return
     */
    public Set<String> keys(String pattern) {
        return stringRedisTemplate.keys(pattern);
    }


    /**
     * -------------------String相关操作---------------------
     */
    public String get(String key) {
        if (key == null) {
            log.error("key is null");
            return null; // 或抛出自定义异常，或返回默认值
        }
        return stringRedisTemplate.opsForValue().get(key);
    }


    /**
     * 持久缓存
     *
     * @param key
     * @param value
     */
    public void put(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public void put(String key, Object value) {
        String jsonStr = JSON.toJSONString(value);
        stringRedisTemplate.opsForValue().set(key, jsonStr);
    }

    /**
     * 默认 时间单位 s
     *
     * @param key
     * @param value
     * @param timeout
     */
    public void put(String key, String value, long timeout) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 默认 时间单位 s
     *
     * @param key
     * @param value
     * @param timeout
     */
    public void put(String key, Object value, long timeout) {
        String jsonStr = JSON.toJSONString(value);
        stringRedisTemplate.opsForValue().set(key, jsonStr, timeout, TimeUnit.SECONDS);
    }

    /**
     * 缓存带过期时间单位
     *
     * @param key
     * @param value
     * @param timeout
     * @param unit
     */
    public void put(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void put(String key, Object value, long timeout, TimeUnit unit) {

        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(value), timeout, unit);
    }


    // public void set(String key, String value, Date date){
    //     stringRedisTemplate.opsForValue().set(key, value, date);
    // }


    /************************Hash************************/
    public void putHash(String key, String hashKey, String value) {
        stringRedisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 获取hashKey对应的value
     */
    public String getHash(String key, String hashKey) {
        return (String) stringRedisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 是否含有hashKey
     */
    public Boolean hasHashKey(String key, String hashKey) {
        return stringRedisTemplate.opsForHash().hasKey(key, hashKey);
    }

    /**
     * 删除hashKey
     */
    public void delHashKey(String key, String hashKey) {
        stringRedisTemplate.opsForHash().delete(key, hashKey);
    }


    public Boolean putIfAbsent(String key, long timeout, TimeUnit unit) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            return false;
        }
        stringRedisTemplate.opsForValue().set(key, "1", timeout, unit);
        return true;
    }
}
