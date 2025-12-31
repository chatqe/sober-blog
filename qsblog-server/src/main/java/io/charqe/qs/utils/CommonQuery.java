package io.charqe.qs.utils;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.article.domain.dao.ArticleMapper;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.comment.domain.dao.CommentMapper;
import io.charqe.qs.modules.comment.domain.pojo.Comment;
import io.charqe.qs.modules.family.domain.dao.FamilyMapper;
import io.charqe.qs.modules.family.domain.pojo.Family;
import io.charqe.qs.modules.family.controller.web.vo.FamilyVO;
import io.charqe.qs.modules.historyInfo.domain.pojo.HistoryInfo;
import io.charqe.qs.modules.historyInfo.domain.dao.HistoryInfoMapper;
import io.charqe.qs.modules.label.domain.dao.LabelMapper;
import io.charqe.qs.modules.label.domain.pojo.Label;
import io.charqe.qs.modules.sort.domain.dao.SortMapper;
import io.charqe.qs.modules.sort.domain.pojo.Sort;
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
    private HistoryInfoMapper historyInfoMapper;

    @Resource
    private UserService userService;

    @Resource
    private SortMapper sortMapper;

    @Resource
    private LabelMapper labelMapper;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private FamilyMapper familyMapper;

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
        Integer userId = BlogUtil.getUserId();
        String ipUser = ip + (userId != null ? "_" + userId : "");

        // CopyOnWriteArraySet<String> ipHistory = (CopyOnWriteArraySet<String>) PoetryCache.get(CommonConst.IP_HISTORY);
        CopyOnWriteArraySet<String> ipHistory =
                qingSiCache.get(CommonConst.IP_HISTORY) == null ? new CopyOnWriteArraySet<>()
                        : JSON.parseObject(qingSiCache.get(CommonConst.IP_HISTORY), new TypeReference<CopyOnWriteArraySet<String>>() {
                });
        if (!ipHistory.contains(ipUser)) {
            synchronized (ipUser.intern()) {
                if (!ipHistory.contains(ipUser)) {
                    ipHistory.add(ipUser);
                    HistoryInfo historyInfo = new HistoryInfo();
                    historyInfo.setIp(ip);
                    historyInfo.setUserId(userId);
                    if (searcher != null) {
                        try {
                            String search = searcher.search(ip);
                            String[] region = search.split("\\|");
                            if (!"0".equals(region[0])) {
                                historyInfo.setNation(region[0]);
                            }
                            if (!"0".equals(region[2])) {
                                historyInfo.setProvince(region[2]);
                            }
                            if (!"0".equals(region[3])) {
                                historyInfo.setCity(region[3]);
                            }
                        } catch (Exception e) {
                        }
                    }
                    historyInfoMapper.insert(historyInfo);
                }
            }
        }
    }


    /**
     * baidu 查询ip对应省市区信息 保存到DB
     *
     * @param ip
     */
    public void saveHistoryByBaidu(String ip) {
        Integer userId = BlogUtil.getUserId();
        String ipUser = ip + (userId != null ? "_" + userId : "");

        // CopyOnWriteArraySet<String> ipHistory = (CopyOnWriteArraySet<String>) PoetryCache.get(CommonConst.IP_HISTORY);
        CopyOnWriteArraySet<String> ipHistory =
                qingSiCache.get(CommonConst.IP_HISTORY) == null ? new CopyOnWriteArraySet<>()
                        : JSON.parseObject(qingSiCache.get(CommonConst.IP_HISTORY), new TypeReference<CopyOnWriteArraySet<String>>() {
                });
        if (!ipHistory.contains(ipUser)) {
            synchronized (ipUser.intern()) {
                if (!ipHistory.contains(ipUser)) {
                    ipHistory.add(ipUser);
                    HistoryInfo historyInfo = new HistoryInfo();
                    historyInfo.setIp(ip);
                    historyInfo.setUserId(userId);
                    if (searcher != null) {
                        try {
                            String search = searcher.search(ip);
                            String[] region = search.split("\\|");
                            if (!"0".equals(region[0])) {
                                historyInfo.setNation(region[0]);
                            }
                            if (!"0".equals(region[2])) {
                                historyInfo.setProvince(region[2]);
                            }
                            if (!"0".equals(region[3])) {
                                historyInfo.setCity(region[3]);
                            }
                        } catch (Exception e) {
                        }
                    }
                    historyInfoMapper.insert(historyInfo);
                }
            }
        }
    }


    public User getUser(Integer userId) {
        // User user = (User) PoetryCache.get(CommonConst.USER_CACHE + userId.toString());
        User user = JSON.parseObject(qingSiCache.get(CommonConst.USER_CACHE + userId), User.class);
        if (user != null) {
            return user;
        }
        user = userService.getById(userId);
        if (user != null) {
            // PoetryCache.put(CommonConst.USER_CACHE + userId.toString(), u, CommonConst.EXPIRE);
            qingSiCache.put(CommonConst.USER_CACHE + userId.toString(), JSON.toJSONString(user), CommonConst.EXPIRE);
            return user;
        }
        return null;
    }

    public List<User> getAdmire() {
        // List<User> admire = (List<User>) PoetryCache.get(CommonConst.ADMIRE);
        List<User> admire = JSON.parseObject(qingSiCache.get(CommonConst.ADMIRE), new TypeReference<List<User>>() {
        });
        if (admire != null) {
            return admire;
        }

        synchronized (CommonConst.ADMIRE.intern()) {
            // admire = (List<User>) PoetryCache.get(CommonConst.ADMIRE);
            admire = JSON.parseObject(qingSiCache.get(CommonConst.ADMIRE), new TypeReference<List<User>>() {
            });
            if (admire != null) {
                return admire;
            } else {
                List<User> users = userService.lambdaQuery().select(User::getId, User::getUsername, User::getAdmire, User::getAvatar).isNotNull(User::getAdmire).list();

                // PoetryCache.put(CommonConst.ADMIRE, users, CommonConst.EXPIRE);
                qingSiCache.put(CommonConst.ADMIRE, users, CommonConst.EXPIRE);

                return users;
            }
        }
    }

    public List<FamilyVO> getFamilyList() {
        // List<FamilyVO> familyVOList = (List<FamilyVO>) PoetryCache.get(CommonConst.FAMILY_LIST);
        List<FamilyVO> familyVOList = JSON.parseObject(qingSiCache.get(CommonConst.FAMILY_LIST), new TypeReference<List<FamilyVO>>() {
        });
        if (familyVOList != null) {
            return familyVOList;
        }

        synchronized (CommonConst.FAMILY_LIST.intern()) {
            // familyVOList = (List<FamilyVO>) PoetryCache.get(CommonConst.FAMILY_LIST);
            familyVOList = JSON.parseObject(qingSiCache.get(CommonConst.FAMILY_LIST), new TypeReference<List<FamilyVO>>() {
            });
            if (familyVOList != null) {
                return familyVOList;
            } else {
                LambdaQueryChainWrapper<Family> queryChainWrapper = new LambdaQueryChainWrapper<>(familyMapper);
                List<Family> familyList = queryChainWrapper.eq(Family::getStatus, Boolean.TRUE).list();
                if (!CollectionUtils.isEmpty(familyList)) {
                    familyVOList = familyList.stream().map(family -> {
                        FamilyVO familyVO = new FamilyVO();
                        BeanUtils.copyProperties(family, familyVO);
                        return familyVO;
                    }).collect(Collectors.toList());
                } else {
                    familyVOList = new ArrayList<>();
                }

                // PoetryCache.put(CommonConst.FAMILY_LIST, familyVOList);
                qingSiCache.put(CommonConst.FAMILY_LIST, familyVOList);
                return familyVOList;
            }
        }
    }

    public Integer getCommentCount(Integer source, String type) {
        // Integer count = (Integer) PoetryCache.get(CommonConst.COMMENT_COUNT_CACHE + source.toString() + "_" + type);
        Integer count = JSON.parseObject(qingSiCache.get(CommonConst.COMMENT_COUNT_CACHE + source.toString() + "_" + type), Integer.class);
        if (count != null) {
            return count;
        }
        LambdaQueryChainWrapper<Comment> wrapper = new LambdaQueryChainWrapper<>(commentMapper);
        Integer c = Math.toIntExact(wrapper.eq(Comment::getSource, source).eq(Comment::getType, type).count());
        // PoetryCache.put(CommonConst.COMMENT_COUNT_CACHE + source.toString() + "_" + type, c, CommonConst.EXPIRE);
        qingSiCache.put(CommonConst.COMMENT_COUNT_CACHE + source.toString() + "_" + type, c, CommonConst.EXPIRE);
        return c;
    }

    public List<Integer> getUserArticleIds(Integer userId) {
        // List<Integer> ids = (List<Integer>) PoetryCache.get(CommonConst.USER_ARTICLE_LIST + userId.toString());
        List<Integer> ids = JSON.parseObject(qingSiCache.get(CommonConst.USER_ARTICLE_LIST + userId.toString()), new TypeReference<List<Integer>>() {
        });
        if (ids != null) {
            return ids;
        }

        synchronized ((CommonConst.USER_ARTICLE_LIST + userId.toString()).intern()) {
            // ids = (List<Integer>) PoetryCache.get(CommonConst.USER_ARTICLE_LIST + userId.toString());
            ids = JSON.parseObject(qingSiCache.get(CommonConst.USER_ARTICLE_LIST + userId.toString()), new TypeReference<List<Integer>>() {
            });
            if (ids != null) {
                return ids;
            } else {
                LambdaQueryChainWrapper<Article> wrapper = new LambdaQueryChainWrapper<>(articleMapper);
                List<Article> articles = wrapper.eq(Article::getUserId, userId).select(Article::getId).list();
                // List<Integer> collect = articles.stream().map(Article::getId).collect(Collectors.toList());
                List<Integer> collect = articles.stream().map(Article::getId).collect(Collectors.toList());
                // PoetryCache.put(CommonConst.USER_ARTICLE_LIST + userId.toString(), collect, CommonConst.EXPIRE);
                qingSiCache.put(CommonConst.USER_ARTICLE_LIST + userId.toString(), collect, CommonConst.EXPIRE);
                return collect;
            }
        }
    }

    public List<List<Integer>> getArticleIds(String searchText) {
        // List<Article> articles = (List<Article>) PoetryCache.get(CommonConst.ARTICLE_LIST);

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
                    // PoetryCache.put(CommonConst.ARTICLE_LIST, articles);
                    if (CollectionUtils.isEmpty(articles)) {
                        // 为空直接返回
                        return new ArrayList<>();
                    }
                    qingSiCache.put(CommonConst.ARTICLE_LIST, articles);
                }
            }
        }

        List<List<Integer>> ids = new ArrayList<>();
        List<Integer> titleIds = new ArrayList<>();
        // List<Integer> contentIds = new ArrayList<>(); 暂时不用内容匹配

        for (Article article : articles) {
            if (StringUtil.matchString(article.getArticleTitle(), searchText)) {
                titleIds.add(article.getId());
            }
            // else if (StringUtil.matchString(article.getArticleContent(), searchText)) {
            //     contentIds.add(article.getId());
            // }
        }

        ids.add(titleIds);
        // ids.add(contentIds);
        return ids;
    }

    public List<Sort> getSortInfo() {
        // List<Sort> sortInfo = (List<Sort>) PoetryCache.get(CommonConst.SORT_INFO);
        List<Sort> sortInfo = JSON.parseObject(qingSiCache.get(CommonConst.SORT_INFO), new TypeReference<List<Sort>>() {
        });
        if (sortInfo != null) {
            return sortInfo;
        }

        synchronized (CommonConst.SORT_INFO.intern()) {
            sortInfo = JSON.parseObject(qingSiCache.get(CommonConst.SORT_INFO), new TypeReference<List<Sort>>() {
            });
            if (sortInfo == null) {
                List<Sort> sorts = new LambdaQueryChainWrapper<>(sortMapper).list();
                if (!CollectionUtils.isEmpty(sorts)) {
                    sorts.forEach(sort -> {
                        LambdaQueryChainWrapper<Article> sortWrapper = new LambdaQueryChainWrapper<>(articleMapper);
                        Integer countOfSort = Math.toIntExact(sortWrapper.eq(Article::getSortId, sort.getId()).count());
                        sort.setCountOfSort(countOfSort);

                        LambdaQueryChainWrapper<Label> wrapper = new LambdaQueryChainWrapper<>(labelMapper);
                        List<Label> labels = wrapper.eq(Label::getSortId, sort.getId()).list();
                        if (!CollectionUtils.isEmpty(labels)) {
                            labels.forEach(label -> {
                                LambdaQueryChainWrapper<Article> labelWrapper = new LambdaQueryChainWrapper<>(articleMapper);
                                Integer countOfLabel = Math.toIntExact(labelWrapper.eq(Article::getLabelId, label.getId()).count());
                                label.setCountOfLabel(countOfLabel);
                            });
                            sort.setLabels(labels);
                        }
                    });
                }
                // PoetryCache.put(CommonConst.SORT_INFO, sorts);
                qingSiCache.put(CommonConst.SORT_INFO, sorts);
                return sorts;
            } else {
                return sortInfo;
            }
        }
    }
}


