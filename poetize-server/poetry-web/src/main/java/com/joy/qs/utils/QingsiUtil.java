package com.joy.qs.utils;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.utils.cache.QingSiCache;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
            return "https://cdn.youngwanton.top/qingsi/static/avatar/11747635931168666.jpg";
        }
        return randomAvatars.get(RandomUtil.randomInt(0, randomAvatars.size()));
    }
}
