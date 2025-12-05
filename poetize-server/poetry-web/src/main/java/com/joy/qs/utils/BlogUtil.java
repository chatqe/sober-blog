package com.joy.qs.utils;

import com.alibaba.fastjson.JSON;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.handle.BlogRuntimeException;
import com.joy.qs.model.user.pojo.User;
import com.joy.qs.model.webinfo.pojo.WebInfo;
import com.joy.qs.utils.cache.QingSiCache;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.List;

public class BlogUtil {

    private static final QingSiCache qingSiCache =
            SpringContextHolder.getBean(QingSiCache.class);

    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    public static void checkEmail() {
        // User user = (User) PoetryCache.get(BlogUtil.getToken());
        User user = JSON.parseObject(qingSiCache.get(BlogUtil.getToken()), User.class);
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BlogRuntimeException("请先绑定邮箱！");
        }
    }

    public static String getToken() {
        String token = BlogUtil.getRequest().getHeader(CommonConst.TOKEN_HEADER);
        return "null".equals(token) ? null : token;
    }

    public static User getCurrentUser() {
        // User user = (User) PoetryCache.get(BlogUtil.getToken());
        User user = JSON.parseObject(qingSiCache.get(BlogUtil.getToken()), User.class);
        return user;
    }

    public static User getAdminUser() {
        // User admin = (User) PoetryCache.get(CommonConst.ADMIN);
        User admin = JSON.parseObject(qingSiCache.get(CommonConst.ADMIN), User.class);
        return admin;
    }

    public static Integer getUserId() {
        String token = BlogUtil.getToken();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        // User user = (User) PoetryCache.get(token);
        User user = JSON.parseObject(token, User.class);
        return user == null ? null : user.getId();
    }

    public static String getUsername() {
        // User user = (User) PoetryCache.get(BlogUtil.getToken());
        User user = JSON.parseObject(qingSiCache.get(BlogUtil.getToken()), User.class);
        return user == null ? null : user.getUsername();
    }

    public static String getRandomAvatar(String key) {
        // WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        if (webInfo != null) {
            String randomAvatar = webInfo.getRandomAvatar();
            List<String> randomAvatars = JSON.parseArray(randomAvatar, String.class);
            if (!CollectionUtils.isEmpty(randomAvatars)) {
                if (StringUtils.hasText(key)) {
                    return randomAvatars.get(BlogUtil.hashLocation(key, randomAvatars.size()));
                } else {
                    String ipAddr = BlogUtil.getIpAddr(BlogUtil.getRequest());
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
        // WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        if (webInfo != null) {
            String randomName = webInfo.getRandomName();
            List<String> randomNames = JSON.parseArray(randomName, String.class);
            if (!CollectionUtils.isEmpty(randomNames)) {
                if (StringUtils.hasText(key)) {
                    return randomNames.get(BlogUtil.hashLocation(key, randomNames.size()));
                } else {
                    String ipAddr = BlogUtil.getIpAddr(BlogUtil.getRequest());
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
        // WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        if (webInfo != null) {
            String randomCover = webInfo.getRandomCover();
            List<String> randomCovers = JSON.parseArray(randomCover, String.class);
            if (!CollectionUtils.isEmpty(randomCovers)) {
                if (StringUtils.hasText(key)) {
                    return randomCovers.get(BlogUtil.hashLocation(key, randomCovers.size()));
                } else {
                    String ipAddr = BlogUtil.getIpAddr(BlogUtil.getRequest());
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

    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
                if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                    // 根据网卡取本机配置的IP
                    ipAddress = InetAddress.getLocalHost().getHostAddress();
                }
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            if (ipAddress != null && ipAddress.length() > 15) {
                if (ipAddress.indexOf(",") > 0) {
                    ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
                }
            }
        } catch (Exception e) {
            ipAddress = null;
        }
        return ipAddress;
    }


    public static int hashLocation(String key, int length) {
        int h = key.hashCode();
        return (h ^ (h >>> 16)) & (length - 1);
    }
}
