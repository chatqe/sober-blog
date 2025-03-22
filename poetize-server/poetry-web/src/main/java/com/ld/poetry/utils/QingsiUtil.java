package com.ld.poetry.utils;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.entity.WebInfo;
import com.ld.poetry.utils.cache.PoetryCache;
import com.ld.poetry.utils.cache.QingSiCache;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 项目工具类
 *
 * @author sjq
 * @since 2025-03-05 21:22
 */
@Component
public class QingsiUtil {

    @Resource
    private QingSiCache qingSiCache;

    public  String getRandomAvatar() {
        List<String> randomAvatars = JSON.parseObject(qingSiCache.get(CommonConst.PATH_TYPE_RANDOM_AVATAR), new TypeReference<List<String>>() {
        });
        if (CollectionUtils.isEmpty(randomAvatars)) {
            return "https://cdn.youngwanton.top/qingsi/static/avatar/XHS_15938521174586a979a11-1705-3f46-8be9-e2cff0c1c564.jpg";
        }
        return randomAvatars.get(RandomUtil.randomInt(0, randomAvatars.size()));
    }
}
