package io.charqe.qs.utils;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;
import io.charqe.qs.modules.access.service.AccessLogService;
import io.charqe.qs.modules.article.domain.dao.ArticleMapper;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.category.domain.dao.CategoryMapper;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.modules.comment.domain.dao.CommentMapper;
import io.charqe.qs.modules.comment.domain.pojo.Comment;
import io.charqe.qs.modules.family.controller.web.vo.FamilyWebVO;
import io.charqe.qs.modules.family.service.FamilyService;
import io.charqe.qs.modules.comment.enums.CommentTypeEnum;
import io.charqe.qs.modules.tag.service.TagService;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.user.service.UserService;
import io.charqe.qs.utils.cache.QingSiCache;
import org.apache.commons.io.IOUtils;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;


@Component
public class CommonQuery {
    @Resource
    private CommentMapper commentMapper;


    @Resource
    private AccessLogService accessLogService;

    @Resource
    private UserService userService;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private FamilyService familyService;

    @Resource
    private TagService tagService;

    @Resource
    private QingSiCache qingSiCache;

    @Resource
    private UaParseUtil uaParseUtil;

    private Searcher searcher;

    @PostConstruct
    public void init() {
        try {
            searcher = Searcher.newWithBuffer(IOUtils.toByteArray(new ClassPathResource("ip2region.xdb").getInputStream()));
        } catch (Exception e) {
        }
    }


    /**
     * 从缓存加载 IP 访问历史去重集合。
     * <p>
     * 缓存中该值以 JSON 数组形式存储（如 {@code ["ip_userId", ...]}），
     * 必须使用 {@link JSON#parseArray(String, Class)} 解析；
     * fastjson2 的 {@code JSON.parseObject(str, TypeReference)} 期望对象格式 '{'，
     * 遇到数组首字符 '[' 会抛出 {@code JSONException: expect {, but [}。
     *
     * @return 去重集合；缓存无值或为空时返回空集合
     */
    private CopyOnWriteArraySet<String> loadIpHistory() {
        String cached = qingSiCache.get(CommonConst.IP_HISTORY);
        if (!StringUtils.hasText(cached)) {
            return new CopyOnWriteArraySet<>();
        }
        List<String> list = JSON.parseArray(cached, String.class);
        return list == null ? new CopyOnWriteArraySet<>() : new CopyOnWriteArraySet<>(list);
    }

    /**
     * 根据本地文件 查询ip对应省市区信息 保存到DB。
     * <p>
     * 调用方需在<b>请求线程</b>中构造好 {@link AccessLog} 并填充 ip/userId/ua/reqUrl/reqMethod/
     * respStatus/respTime/accessTime，再透传到异步线程（异步线程无法访问 RequestContextHolder）。
     * 本方法负责：去重（ip+userId）、UA 解析（browser/os/deviceType）、IP 归属地查询与字段长度截断。
     *
     * @param accessLog 访问日志（字段由请求线程收集）
     */
    public void saveHistory(AccessLog accessLog) {
        String ip = accessLog.getIp();
        Long userId = accessLog.getUserId();
        String ipUser = ip + (userId != null ? "_" + userId : "");

        CopyOnWriteArraySet<String> ipHistory = loadIpHistory();
        if (!ipHistory.contains(ipUser)) {
            synchronized (ipUser.intern()) {
                if (!ipHistory.contains(ipUser)) {
                    ipHistory.add(ipUser);

                    // 1. UA 解析（浏览器/操作系统/设备类型），原始 UA 按列长 varchar(1024) 截断
                    String ua = accessLog.getUa();
                    if (StringUtils.hasText(ua)) {
                        accessLog.setUa(truncate(ua, 1024));
                        UaParseUtil.UaInfo uaInfo = uaParseUtil.parse(ua);
                        accessLog.setBrowser(uaInfo.browser());
                        accessLog.setOs(uaInfo.os());
                        accessLog.setDeviceType(uaInfo.deviceType());
                    }

                    // 2. 请求URL按列长 varchar(512) 截断
                    accessLog.setReqUrl(truncate(accessLog.getReqUrl(), 512));

                    // 3. 本地 IP 库查询归属地（国家/省/市）
                    if (searcher != null && StringUtils.hasText(ip)) {
                        try {
                            String search = searcher.search(ip);
                            String[] region = search.split("\\|");
                            if (region.length > 0 && !"0".equals(region[0])) {
                                accessLog.setCountry(region[0]);
                            }
                            if (region.length > 2 && !"0".equals(region[2])) {
                                accessLog.setProvince(region[2]);
                            }
                            if (region.length > 3 && !"0".equals(region[3])) {
                                accessLog.setCity(region[3]);
                            }
                        } catch (Exception e) {
                            // 归属地查询失败不影响日志落库
                        }
                    }

                    accessLogService.save(accessLog);
                }
            }
        }
    }

    /**
     * 查询 IP 归属地（国家|省|市），返回格式如 "中国|广东省|深圳市"
     *
     * @param ip IP 地址
     * @return 地理位置字符串，查询失败返回 null
     */
    public String getIpRegion(String ip) {
        if (!StringUtils.hasText(ip) || searcher == null) {
            return null;
        }
        try {
            String search = searcher.search(ip);
            String[] region = search.split("\\|");
            if (region.length < 4 || "0".equals(region[0])) {
                return null;
            }
            return region[0] + "|" + region[2] + "|" + region[3];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 按最大长度截断字符串，避免超出数据库列长度导致插入失败
     */
    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }


    /**
     * baidu 查询ip对应省市区信息 保存到DB。
     *
     * @param ip 访问IP
     * @deprecated 归属地查询已由本地 ip2region 在 {@link #saveHistory(AccessLog)} 中统一完成，
     * 此方法当前无调用方；若未来启用，需由调用方补齐 reqUrl/reqMethod/respStatus/respTime/accessTime
     * 等 NOT NULL 字段，否则落库会失败。
     */
    @Deprecated
    public void saveAccessLogByBaidu(String ip) {
        AccessLog accessLog = new AccessLog();
        accessLog.setIp(ip);
        accessLog.setUserId(BlogUtil.getUserId());
        saveHistory(accessLog);
    }


    public User getUser(Long userId) {
        User user = JSON.parseObject(qingSiCache.get(CommonConst.USER_CACHE + userId), User.class);
        if (user != null) {
            return user;
        }
        user = userService.getById(userId);
        if (user != null) {
            qingSiCache.put(CommonConst.USER_CACHE + userId.toString(), JSON.toJSONString(user), CommonConst.EXPIRE);
            return user;
        }
        return null;
    }

    public List<User> getAdmire() {
        List<User> admire = JSON.parseObject(qingSiCache.get(CommonConst.ADMIRE), new TypeReference<List<User>>() {
        });
        if (admire != null) {
            return admire;
        }

        synchronized (CommonConst.ADMIRE.intern()) {
            admire = JSON.parseObject(qingSiCache.get(CommonConst.ADMIRE), new TypeReference<List<User>>() {
            });
            if (admire != null) {
                return admire;
            } else {
                List<User> users = userService.lambdaQuery().select(User::getId, User::getUsername, User::getAdmire, User::getAvatar).isNotNull(User::getAdmire).list();

                qingSiCache.put(CommonConst.ADMIRE, users, CommonConst.EXPIRE);

                return users;
            }
        }
    }

    public List<FamilyWebVO> getFamilyList() {
        List<FamilyWebVO> familyVOList = JSON.parseObject(qingSiCache.get(CommonConst.FAMILY_LIST), new TypeReference<List<FamilyWebVO>>() {
        });
        if (familyVOList != null) {
            return familyVOList;
        }

        synchronized (CommonConst.FAMILY_LIST.intern()) {
            familyVOList = JSON.parseObject(qingSiCache.get(CommonConst.FAMILY_LIST), new TypeReference<List<FamilyWebVO>>() {
            });
            if (familyVOList != null) {
                return familyVOList;
            } else {
                familyVOList = familyService.listPublic();
                qingSiCache.put(CommonConst.FAMILY_LIST, familyVOList);
                return familyVOList;
            }
        }
    }


    /**
     * 获取内容实体评论数（Integer类型编码，推荐使用）
     *
     * @param bizId   业务主键ID
     * @param bizType 评论类型编码（CommentTypeEnum）
     * @return 评论数
     */
    public Integer getCommentCount(Long bizId, Integer bizType) {
        return getCommentCount(bizId, String.valueOf(bizType));
    }

    /**
     * 获取内容实体评论数（String类型编码）
     *
     * @param bizId 业务主键ID
     * @param type  评论类型（可以是编码数字字符串或描述）
     * @return 评论数
     */
    public Integer getCommentCount(Long bizId, String type) {
        Integer count = JSON.parseObject(qingSiCache.get(CommonConst.COMMENT_COUNT_CACHE + bizId + "_" + type), Integer.class);
        if (count != null) {
            return count;
        }
        // 尝试解析为Integer编码，或通过描述查找
        Integer bizType;
        try {
            bizType = Integer.parseInt(type);
        } catch (NumberFormatException e) {
            CommentTypeEnum cte = CommentTypeEnum.getByDesc(type);
            bizType = cte != null ? cte.getCode() : null;
        }
        if (bizType == null) {
            return 0;
        }
        LambdaQueryChainWrapper<Comment> wrapper = new LambdaQueryChainWrapper<>(commentMapper);
        Integer c = Math.toIntExact(wrapper.eq(Comment::getBizId, bizId)
                .eq(Comment::getBizType, bizType)
                .eq(Comment::getIsDel, Boolean.FALSE).count());
        qingSiCache.put(CommonConst.COMMENT_COUNT_CACHE + bizId + "_" + type, c, CommonConst.EXPIRE);
        return c;
    }

    public List<Long> getUserArticleIds(Long userId) {
        List<Long> ids = JSON.parseObject(qingSiCache.get(CommonConst.USER_ARTICLE_LIST + userId.toString()), new TypeReference<List<Long>>() {
        });
        if (ids != null) {
            return ids;
        }

        synchronized ((CommonConst.USER_ARTICLE_LIST + userId.toString()).intern()) {
            ids = JSON.parseObject(qingSiCache.get(CommonConst.USER_ARTICLE_LIST + userId.toString()), new TypeReference<List<Long>>() {
            });
            if (ids != null) {
                return ids;
            } else {
                LambdaQueryChainWrapper<Article> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
                List<Article> articles = wrapper.eq(Article::getUserId, userId).select(Article::getId).list();
                // List<Integer> collect = articles.stream().map(Article::getId).collect(Collectors.toList());
                List<Long> collect = articles.stream().map(Article::getId).collect(Collectors.toList());
                qingSiCache.put(CommonConst.USER_ARTICLE_LIST + userId.toString(), collect, CommonConst.EXPIRE);
                return collect;
            }
        }
    }

    public List<List<Long>> getArticleIds(String searchText) {
        List<Article> articles = qingSiCache.get(CommonConst.ARTICLE_LIST) == null ?
                null : JSON.parseObject(qingSiCache.get(CommonConst.ARTICLE_LIST), new TypeReference<List<Article>>() {
        });
        if (articles == null) {
            synchronized (CommonConst.ARTICLE_LIST.intern()) {
                articles = JSON.parseObject(qingSiCache.get(CommonConst.ARTICLE_LIST), new TypeReference<List<Article>>() {
                });
                if (articles == null) {
                    // LambdaQueryChainWrapper<Article> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
                    // articles = wrapper.select(Article::getId, Article::getArticleTitle, Article::getArticleContent)
                    //         .orderByDesc(Article::getCreateTime)
                    //         .list();
                    articles = articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                            .orderByDesc(Article::getCreateTime));
                    if (CollectionUtils.isEmpty(articles)) {
                        // 为空直接返回
                        return new ArrayList<>();
                    }
                    qingSiCache.put(CommonConst.ARTICLE_LIST, articles);
                }
            }
        }

        List<List<Long>> ids = new ArrayList<>();
        List<Long> titleIds = new ArrayList<>();
        // List<Integer> contentIds = new ArrayList<>(); 暂时不用内容匹配

        for (Article article : articles) {
            if (StrUtil.matchString(article.getArticleTitle(), searchText)) {
                titleIds.add(article.getId());
            }
            // else if (StrUtil.matchString(article.getArticleContent(), searchText)) {
            //     contentIds.add(article.getId());
            // }
        }

        ids.add(titleIds);
        // ids.add(contentIds);
        return ids;
    }

    /**
     * 获取分类标签信息
     */
    public List<Category> getCategoryInfo() {
        List<Category> categoryInfo = JSON.parseObject(qingSiCache.get(CommonConst.CATEGORY_INFO), new TypeReference<List<Category>>() {
        });
        if (categoryInfo != null) {
            return categoryInfo;
        }

        synchronized (CommonConst.CATEGORY_INFO.intern()) {
            categoryInfo = JSON.parseObject(qingSiCache.get(CommonConst.CATEGORY_INFO), new TypeReference<List<Category>>() {
            });
            if (categoryInfo == null) {
                List<Category> categories = categoryMapper.selectList(Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSort));
                if (!CollectionUtils.isEmpty(categories)) {
                    categories.forEach(category -> {
                        // LambdaQueryChainWrapper<Article> categoryWrapper = new LambdaQueryChainWrapper<>(articleMapper);
                        // Integer countOfCategory = Math.toIntExact(categoryWrapper.eq(Article::getCategoryId, category.getId()).count());
                        // category.setCountOfCategory(countOfCategory);

                        // List<TagWebVO> tags = tagService.listNavBar(category.getId());
                        // category.setTags(tags);
                    });
                }
                qingSiCache.put(CommonConst.CATEGORY_INFO, categories);
                return categories;
            } else {
                return categoryInfo;
            }
        }
    }
}



