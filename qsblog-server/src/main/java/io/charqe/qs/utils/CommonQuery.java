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
import io.charqe.qs.modules.tag.controller.web.vo.TagWebVO;
import io.charqe.qs.modules.tag.service.TagService;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.user.service.UserService;
import io.charqe.qs.utils.cache.QingSiCache;
import org.apache.commons.io.IOUtils;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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

    private Searcher searcher;

    @PostConstruct
    public void init() {
        try {
            searcher = Searcher.newWithBuffer(IOUtils.toByteArray(new ClassPathResource("ip2region.xdb").getInputStream()));
        } catch (Exception e) {
        }
    }


    /**
     * 根据本地文件 查询ip对应省市区信息 保存到DB
     *
     * @param ip
     */
    public void saveHistory(String ip) {
        Long userId = BlogUtil.getUserId();
        String ipUser = ip + (userId != null ? "_" + userId : "");

        CopyOnWriteArraySet<String> ipHistory =
                qingSiCache.get(CommonConst.IP_HISTORY) == null ? new CopyOnWriteArraySet<>()
                        : JSON.parseObject(qingSiCache.get(CommonConst.IP_HISTORY), new TypeReference<CopyOnWriteArraySet<String>>() {
                });
        if (!ipHistory.contains(ipUser)) {
            synchronized (ipUser.intern()) {
                if (!ipHistory.contains(ipUser)) {
                    ipHistory.add(ipUser);
                    AccessLog accessLog = new AccessLog();
                    accessLog.setIp(ip);
                    accessLog.setUserId(userId);
                    if (searcher != null) {
                        try {
                            String search = searcher.search(ip);
                            String[] region = search.split("\\|");
                            if (!"0".equals(region[0])) {
                                accessLog.setCountry(region[0]);
                            }
                            if (!"0".equals(region[2])) {
                                accessLog.setProvince(region[2]);
                            }
                            if (!"0".equals(region[3])) {
                                accessLog.setCity(region[3]);
                            }
                        } catch (Exception e) {
                        }
                    }
                    accessLogService.save(accessLog);
                }
            }
        }
    }


    /**
     * baidu 查询ip对应省市区信息 保存到DB
     *
     * @param ip
     */
    public void saveAccessLogByBaidu(String ip) {
        Long userId = BlogUtil.getUserId();
        String ipUser = ip + (userId != null ? "_" + userId : "");

        CopyOnWriteArraySet<String> ipHistory =
                qingSiCache.get(CommonConst.IP_HISTORY) == null ? new CopyOnWriteArraySet<>()
                        : JSON.parseObject(qingSiCache.get(CommonConst.IP_HISTORY), new TypeReference<CopyOnWriteArraySet<String>>() {
                });
        if (!ipHistory.contains(ipUser)) {
            synchronized (ipUser.intern()) {
                if (!ipHistory.contains(ipUser)) {
                    ipHistory.add(ipUser);
                    AccessLog accessLog = new AccessLog();
                    accessLog.setIp(ip);
                    accessLog.setUserId(userId);
                    if (searcher != null) {
                        try {
                            String search = searcher.search(ip);
                            String[] region = search.split("\\|");
                            if (!"0".equals(region[0])) {
                                accessLog.setCountry(region[0]);
                            }
                            if (!"0".equals(region[2])) {
                                accessLog.setProvince(region[2]);
                            }
                            if (!"0".equals(region[3])) {
                                accessLog.setCity(region[3]);
                            }
                        } catch (Exception e) {
                        }
                    }
                    accessLogService.save(accessLog);
                }
            }
        }
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
            bizType = cte != null ? cte.code() : null;
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
        List<Category> categoryInfo = JSON.parseObject(qingSiCache.get(CommonConst.SORT_INFO), new TypeReference<List<Category>>() {
        });
        if (categoryInfo != null) {
            return categoryInfo;
        }

        synchronized (CommonConst.SORT_INFO.intern()) {
            categoryInfo = JSON.parseObject(qingSiCache.get(CommonConst.SORT_INFO), new TypeReference<List<Category>>() {
            });
            if (categoryInfo == null) {
                List<Category> categories = categoryMapper.selectList(Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSort));
                if (!CollectionUtils.isEmpty(categories)) {
                    categories.forEach(category -> {
                        LambdaQueryChainWrapper<Article> categoryWrapper = new LambdaQueryChainWrapper<>(articleMapper);
                        Integer countOfCategory = Math.toIntExact(categoryWrapper.eq(Article::getSortId, category.getId()).count());
                        // category.setCountOfCategory(countOfCategory);

                        List<TagWebVO> tags = tagService.listPublic(category.getId());
                        // category.setTags(tags);
                    });
                }
                qingSiCache.put(CommonConst.SORT_INFO, categories);
                return categories;
            } else {
                return categoryInfo;
            }
        }
    }
}



