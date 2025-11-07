package com.ld.poetry.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ld.poetry.aop.ResourceCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.LabelMapper;
import com.ld.poetry.dao.SortMapper;
import com.ld.poetry.dto.ArticleDTO;
import com.ld.poetry.entity.*;
import com.ld.poetry.enums.CommentTypeEnum;
import com.ld.poetry.enums.PoetryEnum;
import com.ld.poetry.handle.ServiceException;
import com.ld.poetry.service.ArticleService;
import com.ld.poetry.service.LikeRecordService;
import com.ld.poetry.service.UserService;
import com.ld.poetry.utils.CommonQuery;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.utils.cache.PoetryCache;
import com.ld.poetry.utils.cache.QingSiCache;
import com.ld.poetry.utils.mail.MailUtil;
import com.ld.poetry.vo.ArticleVO;
import com.ld.poetry.vo.BaseRequestVO;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 文章表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private UserService userService;

    @Autowired
    private MailUtil mailUtil;

    @Autowired
    private SortMapper sortMapper;

    @Autowired
    private LabelMapper labelMapper;

    @Resource
    private QingSiCache qingSiCache;


    @Resource
    private LikeRecordService likeRecordService;

    @Value("${user.subscribe.format}")
    private String subscribeFormat;


    @Override
    public PoetryResult saveArticle(ArticleVO articleVO) {
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && !StringUtils.hasText(articleVO.getPassword())) {
            return PoetryResult.fail("请设置文章密码！");
        }
        Article article = new Article();
        if (StringUtils.hasText(articleVO.getArticleCover())) {
            article.setArticleCover(articleVO.getArticleCover());
        }
        if (StringUtils.hasText(articleVO.getVideoUrl())) {
            article.setVideoUrl(articleVO.getVideoUrl());
        }
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && StringUtils.hasText(articleVO.getPassword())) {
            article.setPassword(articleVO.getPassword());
            article.setTips(articleVO.getTips());
        }
        article.setViewStatus(articleVO.getViewStatus());
        article.setCommentStatus(articleVO.getCommentStatus());
        article.setRecommendStatus(articleVO.getRecommendStatus());
        article.setArticleTitle(articleVO.getArticleTitle());
        article.setArticleContent(articleVO.getArticleContent());
        article.setSortId(articleVO.getSortId());
        article.setLabelId(articleVO.getLabelId());
        article.setUserId(PoetryUtil.getUserId());
        save(article);

        PoetryCache.remove(CommonConst.SORT_INFO);

        try {
            if (articleVO.getViewStatus()) {
                List<User> users = userService.lambdaQuery().select(User::getEmail, User::getSubscribe).eq(User::getUserStatus, PoetryEnum.STATUS_ENABLE.getCode()).list();
                List<String> emails = users.stream().filter(u -> {
                    List<Integer> sub = JSON.parseArray(u.getSubscribe(), Integer.class);
                    return !CollectionUtils.isEmpty(sub) && sub.contains(articleVO.getLabelId());
                }).map(User::getEmail).collect(Collectors.toList());

                if (!CollectionUtils.isEmpty(emails)) {
                    LambdaQueryChainWrapper<Label> wrapper = new LambdaQueryChainWrapper<>(labelMapper);
                    Label label = wrapper.select(Label::getLabelName).eq(Label::getId, articleVO.getLabelId()).one();
                    String text = getSubscribeMail(label.getLabelName(), articleVO.getArticleTitle());
                    WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
                    mailUtil.sendMailMessage(emails, "您有一封来自" + (webInfo == null ? "POETIZE" : webInfo.getWebName()) + "的回执！", text);
                }
            }
        } catch (Exception e) {
            log.error("订阅邮件发送失败：", e);
        }
        return PoetryResult.success();
    }

    private String getSubscribeMail(String labelName, String articleTitle) {
        WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
        String webName = (webInfo == null ? "POETIZE" : webInfo.getWebName());
        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.notificationMail, PoetryUtil.getAdminUser().getUsername()),
                PoetryUtil.getAdminUser().getUsername(),
                String.format(subscribeFormat, labelName, articleTitle),
                "",
                webName);
    }

    @Override
    public PoetryResult deleteArticle(Integer id) {
        Integer userId = PoetryUtil.getUserId();
        lambdaUpdate().eq(Article::getId, id)
                .eq(Article::getUserId, userId)
                .remove();
        PoetryCache.remove(CommonConst.SORT_INFO);
        return PoetryResult.success();
    }

    @Override
    public PoetryResult updateArticle(ArticleVO articleVO) {
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && !StringUtils.hasText(articleVO.getPassword())) {
            return PoetryResult.fail("请设置文章密码！");
        }

        Integer userId = PoetryUtil.getUserId();
        LambdaUpdateChainWrapper<Article> updateChainWrapper = lambdaUpdate()
                .eq(Article::getId, articleVO.getId())
                .eq(Article::getUserId, userId)
                .set(Article::getLabelId, articleVO.getLabelId())
                .set(Article::getSortId, articleVO.getSortId())
                .set(Article::getArticleTitle, articleVO.getArticleTitle())
                .set(Article::getUpdateBy, PoetryUtil.getUsername())
                .set(Article::getUpdateTime, LocalDateTime.now())
                .set(Article::getVideoUrl, StringUtils.hasText(articleVO.getVideoUrl()) ? articleVO.getVideoUrl() : null)
                .set(Article::getArticleContent, articleVO.getArticleContent());

        if (StringUtils.hasText(articleVO.getArticleCover())) {
            updateChainWrapper.set(Article::getArticleCover, articleVO.getArticleCover());
        }
        if (articleVO.getCommentStatus() != null) {
            updateChainWrapper.set(Article::getCommentStatus, articleVO.getCommentStatus());
        }
        if (articleVO.getRecommendStatus() != null) {
            updateChainWrapper.set(Article::getRecommendStatus, articleVO.getRecommendStatus());
        }
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && StringUtils.hasText(articleVO.getPassword())) {
            updateChainWrapper.set(Article::getPassword, articleVO.getPassword());
            updateChainWrapper.set(StringUtils.hasText(articleVO.getTips()), Article::getTips, articleVO.getTips());
        }
        if (articleVO.getViewStatus() != null) {
            updateChainWrapper.set(Article::getViewStatus, articleVO.getViewStatus());
        }
        updateChainWrapper.update();
        PoetryCache.remove(CommonConst.SORT_INFO);
        return PoetryResult.success();
    }

    @Override
    public PoetryResult<Page> listArticle(BaseRequestVO baseRequestVO) {
        List<Integer> ids = null;
        List<List<Integer>> idList = null;
        if (StringUtils.hasText(baseRequestVO.getArticleSearch())) {
            idList = commonQuery.getArticleIds(baseRequestVO.getArticleSearch());
            ids = idList.stream().flatMap(Collection::stream).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(ids)) {
                baseRequestVO.setRecords(new ArrayList<>());
                return PoetryResult.success(baseRequestVO);
            }
        }

        // LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery();
        // lambdaQuery.in(!CollectionUtils.isEmpty(ids), Article::getId, ids);
        // lambdaQuery.like(StringUtils.hasText(baseRequestVO.getSearchKey()), Article::getArticleTitle, baseRequestVO.getSearchKey());
        // lambdaQuery.eq(baseRequestVO.getRecommendStatus() != null && baseRequestVO.getRecommendStatus(), Article::getRecommendStatus, PoetryEnum.STATUS_ENABLE.getCode());
        //
        //
        // if (baseRequestVO.getLabelId() != null) {
        //     lambdaQuery.eq(Article::getLabelId, baseRequestVO.getLabelId());
        // } else if (baseRequestVO.getSortId() != null) {
        //     lambdaQuery.eq(Article::getSortId, baseRequestVO.getSortId());
        // }
        //
        // lambdaQuery.orderByDesc(Article::getCreateTime);
        //
        // lambdaQuery.page(baseRequestVO);
        BaseRequestVO articlePage = articleMapper.selectPage(baseRequestVO, Wrappers.<Article>lambdaQuery()
                .in(!CollectionUtils.isEmpty(ids), Article::getId, ids)
                .like(StringUtils.hasText(baseRequestVO.getSearchKey()), Article::getArticleTitle, baseRequestVO.getArticleSearch())
                .eq(baseRequestVO.getRecommendStatus() != null && baseRequestVO.getRecommendStatus(), Article::getRecommendStatus, PoetryEnum.STATUS_ENABLE.getCode())
                .eq(baseRequestVO.getLabelId() != null, Article::getLabelId, baseRequestVO.getLabelId())
                .orderByDesc(Article::getCreateTime));


        // List<Article> records = baseRequestVO.getRecords();
        List<Article> records = articlePage.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            List<ArticleVO> articles = new ArrayList<>();
            List<ArticleVO> titles = new ArrayList<>();
            List<ArticleVO> contents = new ArrayList<>();

            for (Article article : records) {
                if (article.getArticleContent().length() > CommonConst.SUMMARY) {
                    article.setArticleContent(article.getArticleContent().substring(0, CommonConst.SUMMARY).replace("`", "").replace("#", "").replace(">", ""));
                }
                ArticleVO articleVO = buildArticleVO(article, false);
                articleVO.setHasVideo(StringUtils.hasText(articleVO.getVideoUrl()));
                articleVO.setPassword(null);
                articleVO.setVideoUrl(null);
                if (CollectionUtils.isEmpty(ids)) {
                    articles.add(articleVO);
                } else if (idList.get(0).contains(articleVO.getId())) {
                    titles.add(articleVO);
                } else if (idList.get(1).contains(articleVO.getId())) {
                    contents.add(articleVO);
                }
            }

            List<ArticleVO> collect = new ArrayList<>();
            collect.addAll(articles);
            collect.addAll(titles);
            collect.addAll(contents);
            // baseRequestVO.setRecords(collect);
            articlePage.setRecords(collect);
        }
        // return PoetryResult.success(baseRequestVO);
        return PoetryResult.success(articlePage);
    }

    @Override
    @ResourceCheck(CommonConst.RESOURCE_ARTICLE_DOC)
    public PoetryResult<ArticleVO> getArticleById(Integer id, String password) {
        LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(Article::getId, id);

        Article article = lambdaQuery.one();
        if (article == null) {
            return PoetryResult.success();
        }
        if (!article.getViewStatus() && (!StringUtils.hasText(password) || !password.equals(article.getPassword()))) {
            return PoetryResult.fail("密码错误" + (StringUtils.hasText(article.getTips()) ? article.getTips() : "请联系作者获取密码"));
        }
        articleMapper.updateViewCount(id);
        article.setPassword(null);
        if (StringUtils.hasText(article.getVideoUrl())) {
            article.setVideoUrl(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).encryptBase64(article.getVideoUrl()));
        }
        ArticleVO articleVO = buildArticleVO(article, false);
        return PoetryResult.success(articleVO);
    }

    @Override
    public PoetryResult<Page> listAdminArticle(BaseRequestVO baseRequestVO, Boolean isBoss) {
        LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery();
        lambdaQuery.select(Article.class, a -> !a.getColumn().equals("article_content"));
        if (!isBoss) {
            lambdaQuery.eq(Article::getUserId, PoetryUtil.getUserId());
        } else {
            if (baseRequestVO.getUserId() != null) {
                lambdaQuery.eq(Article::getUserId, baseRequestVO.getUserId());
            }
        }
        if (StringUtils.hasText(baseRequestVO.getSearchKey())) {
            lambdaQuery.like(Article::getArticleTitle, baseRequestVO.getSearchKey());
        }
        if (baseRequestVO.getRecommendStatus() != null && baseRequestVO.getRecommendStatus()) {
            lambdaQuery.eq(Article::getRecommendStatus, PoetryEnum.STATUS_ENABLE.getCode());
        }

        if (baseRequestVO.getLabelId() != null) {
            lambdaQuery.eq(Article::getLabelId, baseRequestVO.getLabelId());
        }

        if (baseRequestVO.getSortId() != null) {
            lambdaQuery.eq(Article::getSortId, baseRequestVO.getSortId());
        }

        lambdaQuery.orderByDesc(Article::getCreateTime).page(baseRequestVO);

        List<Article> records = baseRequestVO.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            List<ArticleVO> collect = records.stream().map(article -> {
                article.setPassword(null);
                ArticleVO articleVO = buildArticleVO(article, true);
                return articleVO;
            }).collect(Collectors.toList());
            baseRequestVO.setRecords(collect);
        }
        return PoetryResult.success(baseRequestVO);
    }

    @Override
    public PoetryResult<ArticleVO> getArticleByIdForUser(Integer id) {
        LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(Article::getId, id).eq(Article::getUserId, PoetryUtil.getUserId());
        Article article = lambdaQuery.one();
        if (article == null) {
            return PoetryResult.fail("文章不存在！");
        }
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(article, articleVO);
        return PoetryResult.success(articleVO);
    }

    /**
     * 主页文章展示
     * @return
     */
    @Override
    public PoetryResult<Map<Integer, List<ArticleVO>>> listSortArticle() {
        // Map<Integer, List<ArticleVO>> result = (Map<Integer, List<ArticleVO>>) PoetryCache.get(CommonConst.SORT_ARTICLE_LIST);
        Map<Integer, List<ArticleVO>> result = JSON.parseObject(qingSiCache.get(CommonConst.SORT_ARTICLE_LIST), new TypeReference<Map<Integer, List<ArticleVO>>>() {
        });
        if (result != null) {
            return PoetryResult.success(result);
        }

        synchronized (CommonConst.SORT_ARTICLE_LIST.intern()) {
            result = JSON.parseObject(qingSiCache.get(CommonConst.SORT_ARTICLE_LIST), new TypeReference<Map<Integer, List<ArticleVO>>>() {
            });
            if (result == null) {
                Map<Integer, List<ArticleVO>> map = new HashMap<>();

                // 获取所有展示分类
                List<Sort> sorts = new LambdaQueryChainWrapper<>(sortMapper)
                        .eq(Sort::getStatus,1)
                        .select(Sort::getId).list();

                List<Article> articles = list(Wrappers.<Article>lambdaQuery()
                        .eq(Article::getDeleted, 0));

                // stream 分组
                Map<Integer, List<Article>> sortArticleMap = articles.stream().collect(Collectors.groupingBy(Article::getSortId));
                sortArticleMap.forEach((sortId, articleList) -> {
                    // 根据权重计算倒序排序
                    articleList.sort(Comparator.comparing(Article::getWeight).reversed());
                    // 获取前6条数据
                    articleList=   articleList.stream().limit(6).collect(Collectors.toList());
                    List<ArticleVO> articleVOList = articleList.stream().map(article -> {
                        if (article.getArticleContent().length() > CommonConst.SUMMARY) {
                            article.setArticleContent(article.getArticleContent().substring(0, CommonConst.SUMMARY).replace("`", "").replace("#", "").replace(">", ""));
                        }

                        ArticleVO vo = buildArticleVO(article, false);
                        vo.setHasVideo(StringUtils.hasText(article.getVideoUrl()));
                        vo.setPassword(null);
                        vo.setVideoUrl(null);
                        return vo;
                    }).collect(Collectors.toList());
                    map.put(sortId, articleVOList);
                });

                // for (Sort sort : sorts) {
                //     LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery()
                //             .eq(Article::getSortId, sort.getId())
                //             .orderByDesc(Article::getCreateTime)
                //             .last("limit 6");
                //     List<Article> articleList = lambdaQuery.list();
                //     if (CollectionUtils.isEmpty(articleList)) {
                //         continue;
                //     }
                //
                //     List<ArticleVO> articleVOList = articleList.stream().map(article -> {
                //         if (article.getArticleContent().length() > CommonConst.SUMMARY) {
                //             article.setArticleContent(article.getArticleContent().substring(0, CommonConst.SUMMARY).replace("`", "").replace("#", "").replace(">", ""));
                //         }
                //
                //         ArticleVO vo = buildArticleVO(article, false);
                //         vo.setHasVideo(StringUtils.hasText(article.getVideoUrl()));
                //         vo.setPassword(null);
                //         vo.setVideoUrl(null);
                //         return vo;
                //     }).collect(Collectors.toList());
                //     map.put(sort.getId(), articleVOList);
                // }

                // PoetryCache.put(CommonConst.SORT_ARTICLE_LIST, map, CommonConst.TOKEN_INTERVAL);
                qingSiCache.put(CommonConst.SORT_ARTICLE_LIST, map, CommonConst.TOKEN_INTERVAL);
                return PoetryResult.success(map);
            } else {
                return PoetryResult.success(result);
            }
        }
    }

    /**
     * 添加点赞数
     *
     * @param dto
     * @return
     */
    @Override
    public String addArticleLikeCount(ArticleDTO dto) {
        // 文章判空
        Article byId = getById(dto.getArticleId());
        if (Objects.isNull(byId)) {
            throw new ServiceException("文章不存在");
        }
        // 如果用户id不为空,点赞表新增记录
        if (Objects.nonNull(dto.getUserId())) {
            // 判断用户是否已经点赞
            Boolean hasLike = qingSiCache.hasHashKey(CommonConst.USER_LIKES.concat(dto.getArticleId().toString()), dto.getUserId().toString());
            // 点赞
            if (dto.getOperation() == 1 && !hasLike) {
                // 点赞
                LikeRecord likeRecord = new LikeRecord();
                likeRecord.setUserId(dto.getUserId());
                likeRecord.setTargetType(PoetryEnum.LIKE_TARGET_ARTICLE.getCode());
                likeRecord.setTargetId(dto.getArticleId());
                likeRecord.setCreateTime(new Date());
                likeRecordService.save(likeRecord);
            }
            // 取消点赞
            if (dto.getOperation() == 0 && hasLike) {
                qingSiCache.delHashKey(CommonConst.USER_LIKES.concat(dto.getArticleId().toString()), dto.getUserId().toString());
                // 删除点赞记录
                likeRecordService.remove(Wrappers.<LikeRecord>lambdaQuery()
                        .eq(LikeRecord::getUserId, dto.getUserId())
                        .eq(LikeRecord::getTargetId, dto.getArticleId())
                        .eq(LikeRecord::getTargetType, PoetryEnum.LIKE_TARGET_ARTICLE.getCode()));

                this.update(Wrappers.<Article>lambdaUpdate().
                        eq(Article::getId, dto.getArticleId())
                        .set(Article::getLikeCount, byId.getLikeCount() - 1));
                return "取消点赞成功";
            }

        }

        // 更新点赞数
        this.update(Wrappers.<Article>lambdaUpdate().
                eq(Article::getId, dto.getArticleId())
                .set(Article::getLikeCount, byId.getLikeCount() + 1)
        );
        return "点赞成功";
    }

    /**
     * 判断用户是否已经点赞
     * @param articleId
     * @return
     */
    @Override
    public boolean checkHasLike(String articleId) {
        Article byId = getById(Long.valueOf(articleId));
        if (Objects.isNull(byId)){
            throw new ServiceException("文章不存在");
        }
        Integer userId = PoetryUtil.getUserId();

        LikeRecord one = likeRecordService.getOne(Wrappers.<LikeRecord>lambdaQuery()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, articleId)
                .eq(LikeRecord::getTargetType, PoetryEnum.LIKE_TARGET_ARTICLE.getCode()));
        if (Objects.nonNull(one)){
            return true;
        }
        return false;
    }

    private ArticleVO buildArticleVO(Article article, Boolean isAdmin) {
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(article, articleVO);
        if (!isAdmin) {
            if (!StringUtils.hasText(articleVO.getArticleCover())) {
                articleVO.setArticleCover(PoetryUtil.getRandomCover(articleVO.getId().toString()));
            }
        }

        User user = commonQuery.getUser(articleVO.getUserId());
        if (user != null && StringUtils.hasText(user.getUsername())) {
            articleVO.setUsername(user.getUsername());
        } else if (!isAdmin) {
            articleVO.setUsername(PoetryUtil.getRandomName(articleVO.getUserId().toString()));
        }
        if (articleVO.getCommentStatus()) {
            articleVO.setCommentCount(commonQuery.getCommentCount(articleVO.getId(), CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode()));
        } else {
            articleVO.setCommentCount(0);
        }

        List<Sort> sortInfo = commonQuery.getSortInfo();
        if (!CollectionUtils.isEmpty(sortInfo)) {
            for (Sort s : sortInfo) {
                if (s.getId().intValue() == articleVO.getSortId().intValue()) {
                    Sort sort = new Sort();
                    BeanUtils.copyProperties(s, sort);
                    sort.setLabels(null);
                    articleVO.setSort(sort);
                    if (!CollectionUtils.isEmpty(s.getLabels())) {
                        for (int j = 0; j < s.getLabels().size(); j++) {
                            Label l = s.getLabels().get(j);
                            if (l.getId().intValue() == articleVO.getLabelId().intValue()) {
                                Label label = new Label();
                                BeanUtils.copyProperties(l, label);
                                articleVO.setLabel(label);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return articleVO;
    }
}
