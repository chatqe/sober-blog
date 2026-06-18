package io.charqe.qs.utils;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.exception.LoginException;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
public class BlogUtil {

    private static final QingSiCache qingSiCache =
            SpringContextHolder.getBean(QingSiCache.class);

    public static void checkEmail() {
        String token = BlogUtil.getToken();
        if (!StringUtils.hasText(token)) {
            throw new LoginException(CodeMsg.NOT_LOGIN.getMsg());
        }
        User user = JSON.parseObject(qingSiCache.get(token), User.class);
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BizException("请先绑定邮箱！");
        }
    }

    public static String getToken() {
        String token = IPUtil.getRequest().getHeader(CommonConst.TOKEN_HEADER);
        return "null".equals(token) ? null : token;
    }

    public static User getCurrentUser() {
        String token = BlogUtil.getToken();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return JSON.parseObject(qingSiCache.get(token), User.class);
    }

    public static User getAdminUser() {
        return JSON.parseObject(qingSiCache.get(CommonConst.ADMIN), User.class);
    }

    public static Long getUserId() {
        String token = BlogUtil.getToken();
        if (!StringUtils.hasText(token)) {
            return null;
        }

        User user = JSON.parseObject(qingSiCache.get(token), User.class);
        return user == null ? null : user.getId();
    }

    public static String getUsername() {
        String token = BlogUtil.getToken();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        if (qingSiCache.get(token) == null) {
            return null;
        }
        User user = JSON.parseObject(qingSiCache.get(token), User.class);
        return user == null ? null : user.getUsername();
    }

    public static String getRandomAvatar(String key) {
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        if (webInfo != null) {
            String randomAvatar = webInfo.getRandomAvatar();
            List<String> randomAvatars = JSON.parseArray(randomAvatar, String.class);
            if (!CollectionUtils.isEmpty(randomAvatars)) {
                if (StringUtils.hasText(key)) {
                    return randomAvatars.get(BlogUtil.hashLocation(key, randomAvatars.size()));
                } else {
                    String ipAddr = IPUtil.getRealIp(IPUtil.getRequest());
                    if (StringUtils.hasText(ipAddr)) {
                        return randomAvatars.get(BlogUtil.hashLocation(ipAddr, randomAvatars.size()));
                    } else {
                        return randomAvatars.get(0);
                    }
                }
            }
        }
        return null;
    }


    public static String getRandomName(String key) {
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        if (webInfo != null) {
            String randomName = webInfo.getRandomName();
            List<String> randomNames = JSON.parseArray(randomName, String.class);
            if (!CollectionUtils.isEmpty(randomNames)) {
                if (StringUtils.hasText(key)) {
                    return randomNames.get(BlogUtil.hashLocation(key, randomNames.size()));
                } else {
                    String ipAddr = IPUtil.getRealIp(IPUtil.getRequest());
                    if (StringUtils.hasText(ipAddr)) {
                        return randomNames.get(BlogUtil.hashLocation(ipAddr, randomNames.size()));
                    } else {
                        return randomNames.get(0);
                    }
                }
            }
        }
        return null;
    }

    public static String getRandomCover(String key) {
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        if (webInfo != null) {
            String randomCover = webInfo.getRandomCover();
            List<String> randomCovers = JSON.parseArray(randomCover, String.class);
            if (!CollectionUtils.isEmpty(randomCovers)) {
                if (StringUtils.hasText(key)) {
                    return randomCovers.get(BlogUtil.hashLocation(key, randomCovers.size()));
                } else {
                    String ipAddr = IPUtil.getRealIp(IPUtil.getRequest());
                    if (StringUtils.hasText(ipAddr)) {
                        return randomCovers.get(BlogUtil.hashLocation(ipAddr, randomCovers.size()));
                    } else {
                        return randomCovers.get(0);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 任意 key 尽量均匀地映射到 0 … length-1 的槽位里，同时保证快（只有位运算，无取模）。
     *
     * @param key
     * @param length
     * @return
     */
    public static int hashLocation(String key, int length) {
        // 调用 Java 内置 hashCode()，得到一个 32 位 int（可能分布很糟，比如连续整数只相差低位几位）
        int h = key.hashCode();
        // h ^ (h >>> 16)  （扰动函数） 让 高 16 位的特征扩散到低 16 位，后续做“与”运算时，低位也能带上高位的随机性，减少冲突。
        // & (length - 1)  （位运算取槽）length 必须是 2 的幂（例如 16、32、64…），那么 length-1 就是低位全 1 的掩码。
        // 结果：等价于 h % length，但 CPU 只需一次 按位与，比取模指令快 3~5 倍。
        return (h ^ (h >>> 16)) & (length - 1);
    }
}


