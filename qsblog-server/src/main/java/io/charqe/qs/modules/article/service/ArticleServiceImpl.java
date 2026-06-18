package io.charqe.qs.modules.article.service;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.ResourceCheck;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.StatusEnum;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.exception.ServiceException;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.infra.thirdparty.mail.MailUtil;
import io.charqe.qs.modules.article.controller.web.dto.ArticleDTO;
import io.charqe.qs.modules.article.controller.web.dto.ArticlePageDTO;
import io.charqe.qs.modules.article.controller.web.vo.ArticlePageVO;
import io.charqe.qs.modules.article.controller.web.vo.ArticleVO;
import io.charqe.qs.modules.article.convert.ArticleConvert;
import io.charqe.qs.modules.article.domain.dao.ArticleMapper;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.article.enums.ArticleIErrorCode;
import io.charqe.qs.modules.behavior.service.BehaviorStateService;
import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.service.like.LikeService;
import io.charqe.qs.modules.category.domain.dao.CategoryMapper;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.modules.comment.enums.CommentTypeEnum;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.modules.tag.domain.dao.TagMapper;
import io.charqe.qs.modules.tag.domain.pojo.Tag;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.user.service.UserService;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Slf4j
@Service
public class ArticleServiceImpl extends CoreServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private CommonQuery commonQuery;

    @Resource
    private UserService userService;

    @Resource
    private MailUtil mailUtil;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private QingSiCache qingSiCache;


    @Resource
    private LikeService likeService;

    @Resource
    private BehaviorStateService behaviorStateService;

    @Value("${user.subscribe.format}")
    private String subscribeFormat;

    @Resource
    private ArticleConvert articleConvert;


    @Override
    public R saveArticle(ArticleVO articleVO) {
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && !StringUtils.hasText(articleVO.getPassword())) {
            return R.fail("请设置文章密码！");
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
        article.setUserId(BlogUtil.getUserId());
        save(article);

        qingSiCache.del(CommonConst.SORT_INFO);

        try {
            if (articleVO.getViewStatus()) {
                List<User> users = userService.lambdaQuery().select(User::getEmail, User::getSubscribe).eq(User::getUserStatus, StatusEnum.ENABLE.code()).list();
                List<String> emails = users.stream().filter(u -> {
                    List<Integer> sub = JSON.parseArray(u.getSubscribe(), Integer.class);
                    return !CollectionUtils.isEmpty(sub) && sub.contains(articleVO.getLabelId());
                }).map(User::getEmail).collect(Collectors.toList());

                if (!CollectionUtils.isEmpty(emails)) {
                    LambdaQueryChainWrapper<Tag> wrapper = new LambdaQueryChainWrapper<>(tagMapper);
                    Tag tag = wrapper.select(Tag::getName).eq(Tag::getId, articleVO.getLabelId()).one();
                    String text = getSubscribeMail(tag.getName(), articleVO.getArticleTitle());
                    WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
                    mailUtil.sendMailMessage(emails, "您有一封来自" + (webInfo == null ? "青肆" : webInfo.getWebName()) + "的回执！", text);
                }
            }
        } catch (Exception e) {
            log.error("订阅邮件发送失败：", e);
        }
        return R.success();
    }

    private String getSubscribeMail(String labelName, String articleTitle) {
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        String webName = (webInfo == null ? "POETIZE" : webInfo.getWebName());
        return String.format(mailUtil.getMailText(),
                webName,
                String.format(MailUtil.notificationMail, BlogUtil.getAdminUser().getUsername()),
                BlogUtil.getAdminUser().getUsername(),
                String.format(subscribeFormat, labelName, articleTitle),
                "",
                webName);
    }

    @Override
    public R deleteArticle(Integer id) {
        Long userId = BlogUtil.getUserId();
        lambdaUpdate().eq(Article::getId, id)
                .eq(Article::getUserId, userId)
                .remove();
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }

    @Override
    public R updateArticle(ArticleVO articleVO) {
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && !StringUtils.hasText(articleVO.getPassword())) {
            return R.fail("请设置文章密码！");
        }

        Long userId = BlogUtil.getUserId();
        LambdaUpdateChainWrapper<Article> updateChainWrapper = lambdaUpdate()
                .eq(Article::getId, articleVO.getId())
                .eq(Article::getUserId, userId)
                .set(Article::getLabelId, articleVO.getLabelId())
                .set(Article::getSortId, articleVO.getSortId())
                .set(Article::getArticleTitle, articleVO.getArticleTitle())
                .set(Article::getUpdateBy, BlogUtil.getUsername())
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
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }

    // @Override
    // public R<Page> listArticle(BaseReqVO baseReqVO) {
    //     List<Integer> ids = null;
    //     List<List<Integer>> idList = null;
    //     if (StringUtils.hasText(baseReqVO.getArticleSearch())) {
    //         idList = commonQuery.getArticleIds(baseReqVO.getArticleSearch());
    //         ids = idList.stream().flatMap(Collection::stream).collect(Collectors.toList());
    //         if (CollectionUtils.isEmpty(ids)) {
    //             baseReqVO.setRecords(new ArrayList<>());
    //             return R.success(baseReqVO);
    //         }
    //     }
    //
    //
    //     ArticlePageDTO articlePage = articleMapper.selectPage(, Wrappers.<Article>lambdaQuery()
    //             .in(!CollectionUtils.isEmpty(ids), Article::getId, ids)
    //             .like(StringUtils.hasText(baseReqVO.getSearchKey()), Article::getArticleTitle, baseReqVO.getArticleSearch())
    //             .eq(baseReqVO.getRecommendStatus() != null && baseReqVO.getRecommendStatus(), Article::getRecommendStatus, StatusEnum.ENABLE.code())
    //             .eq(baseReqVO.getLabelId() != null, Article::getLabelId, baseReqVO.getLabelId())
    //             .eq(baseReqVO.getSortId() != null, Article::getSortId, baseReqVO.getSortId())
    //             .orderByDesc(Article::getCreateTime));
    //
    //
    //     List<Article> records = articlePage.getList();
    //     if (!CollectionUtils.isEmpty(records)) {
    //         List<ArticleVO> articles = new ArrayList<>();
    //         List<ArticleVO> titles = new ArrayList<>();
    //         List<ArticleVO> contents = new ArrayList<>();
    //
    //         for (Article article : records) {
    //             if (article.getArticleContent().length() > CommonConst.SUMMARY) {
    //                 article.setArticleContent(article.getArticleContent().substring(0, CommonConst.SUMMARY).replace("`", "").replace("#", "").replace(">", ""));
    //             }
    //             ArticleVO articleVO = buildArticleVO(article, false);
    //             articleVO.setHasVideo(StringUtils.hasText(articleVO.getVideoUrl()));
    //             articleVO.setPassword(null);
    //             articleVO.setVideoUrl(null);
    //             if (CollectionUtils.isEmpty(ids)) {
    //                 articles.add(articleVO);
    //             } else if (idList.get(0).contains(articleVO.getId())) {
    //                 titles.add(articleVO);
    //             } else if (idList.get(1).contains(articleVO.getId())) {
    //                 contents.add(articleVO);
    //             }
    //         }
    //
    //         List<ArticleVO> collect = new ArrayList<>();
    //         collect.addAll(articles);
    //         collect.addAll(titles);
    //         collect.addAll(contents);
    //         // baseReqVO.setRecords(collect);
    //         articlePage.setRecords(collect);
    //     }
    //     // return R.success(baseReqVO);
    //     return R.success(articlePage);
    // }

    @Override
    @ResourceCheck(CommonConst.RESOURCE_ARTICLE_DOC)
    public R<ArticleVO> getArticleWithPwd(Long id, String password) {
        LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(Article::getId, id);

        Article article = lambdaQuery.one();
        if (article == null) {
            return R.success();
        }
        if (!article.getViewStatus() && (!StringUtils.hasText(password) || !password.equals(article.getPassword()))) {
            return R.fail("密码错误" + (StringUtils.hasText(article.getTips()) ? article.getTips() : "请联系作者获取密码"));
        }
        articleMapper.updateViewCount(id);
        article.setPassword(null);
        if (StringUtils.hasText(article.getVideoUrl())) {
            article.setVideoUrl(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).encryptBase64(article.getVideoUrl()));
        }
        ArticleVO articleVO = buildArticleVO(article, false);
        return R.success(articleVO);
    }

    @Override
    public R<Page> listAdminArticle(BaseReqVO baseReqVO, Boolean isBoss) {
        LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery();
        lambdaQuery.select(Article.class, a -> !a.getColumn().equals("article_content"));
        if (!isBoss) {
            lambdaQuery.eq(Article::getUserId, BlogUtil.getUserId());
        } else {
            if (baseReqVO.getUserId() != null) {
                lambdaQuery.eq(Article::getUserId, baseReqVO.getUserId());
            }
        }
        if (StringUtils.hasText(baseReqVO.getSearchKey())) {
            lambdaQuery.like(Article::getArticleTitle, baseReqVO.getSearchKey());
        }
        if (baseReqVO.getRecommendStatus() != null && baseReqVO.getRecommendStatus()) {
            lambdaQuery.eq(Article::getRecommendStatus, StatusEnum.ENABLE.code());
        }

        if (baseReqVO.getLabelId() != null) {
            lambdaQuery.eq(Article::getLabelId, baseReqVO.getLabelId());
        }

        if (baseReqVO.getSortId() != null) {
            lambdaQuery.eq(Article::getSortId, baseReqVO.getSortId());
        }

        lambdaQuery.orderByDesc(Article::getCreateTime).page(baseReqVO);

        List<Article> records = baseReqVO.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            List<ArticleVO> collect = records.stream().map(article -> {
                article.setPassword(null);
                ArticleVO articleVO = buildArticleVO(article, true);
                return articleVO;
            }).collect(Collectors.toList());
            baseReqVO.setRecords(collect);
        }
        return R.success(baseReqVO);
    }

    @Override
    public R<ArticleVO> getArticleById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            return R.fail(ArticleIErrorCode.NOT_FOUND);
        }

        return R.success(articleConvert.convert(article));
    }

    /**
     * 主页文章展示
     *
     * @return
     */
    @Override
    public R<Map<Long, List<ArticleVO>>> listSortArticle() {
        Map<Long, List<ArticleVO>> result = JSON.parseObject(qingSiCache.get(CommonConst.SORT_ARTICLE_LIST), new TypeReference<Map<Long, List<ArticleVO>>>() {
        });
        if (result != null) {
            return R.success(result);
        }

        synchronized (CommonConst.SORT_ARTICLE_LIST.intern()) {
            result = JSON.parseObject(qingSiCache.get(CommonConst.SORT_ARTICLE_LIST), new TypeReference<Map<Long, List<ArticleVO>>>() {
            });
            if (result == null) {
                Map<Long, List<ArticleVO>> map = new HashMap<>();

                // 获取所有展示分类
                List<Category> categories = new LambdaQueryChainWrapper<>(categoryMapper)
                        .eq(Category::getIsShow, 1)
                        .select(Category::getId).list();

                List<Article> articles = list(Wrappers.<Article>lambdaQuery()
                        .eq(Article::getIsDel, 0));

                // stream 分组
                Map<Long, List<Article>> sortArticleMap = articles.stream().collect(Collectors.groupingBy(Article::getSortId));
                sortArticleMap.forEach((sortId, articleList) -> {
                    // 根据权重计算倒序排序
                    articleList.sort(Comparator.comparing(Article::getWeight).reversed());
                    // 获取前6条数据
                    articleList = articleList.stream().limit(6).collect(Collectors.toList());
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

                qingSiCache.put(CommonConst.SORT_ARTICLE_LIST, map, CommonConst.TOKEN_INTERVAL);
                return R.success(map);
            } else {
                return R.success(result);
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
        // 如果用户id不为空,行为表新增记录
        if (Objects.nonNull(dto.getUserId())) {
            // 判断用户是否已经点赞
            Boolean hasLike = qingSiCache.hasHashKey(CommonConst.USER_LIKES.concat(dto.getArticleId().toString()), dto.getUserId().toString());
            // 点赞
            if (dto.getOperation() == 1 && !hasLike) {
                BehDTO behDTO = new BehDTO();
                behDTO.setBizId(dto.getArticleId());
                behDTO.setBizType(BizTypeEnum.ARTICLE.code());
                likeService.like(behDTO, dto.getUserId());
            }
            // 取消点赞
            if (dto.getOperation() == 0 && hasLike) {
                qingSiCache.delHashKey(CommonConst.USER_LIKES.concat(dto.getArticleId().toString()), dto.getUserId().toString());
                BehDTO behDTO = new BehDTO();
                behDTO.setBizId(dto.getArticleId());
                behDTO.setBizType(BizTypeEnum.ARTICLE.code());
                likeService.unlike(behDTO, dto.getUserId());

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
     *
     * @param articleId
     * @return
     */
    @Override
    public boolean checkHasLike(String articleId) {
        Article byId = getById(Long.valueOf(articleId));
        if (Objects.isNull(byId)) {
            throw new ServiceException("文章不存在");
        }
        Long userId = BlogUtil.getUserId();
        if (userId == null) {
            return false;
        }
        // 通过行为状态表判断用户是否已点赞
        return behaviorStateService.lambdaQuery()
                .eq(io.charqe.qs.modules.behavior.domain.pojo.BehaviorState::getUserId, userId)
                .eq(io.charqe.qs.modules.behavior.domain.pojo.BehaviorState::getBizId, Long.valueOf(articleId))
                .eq(io.charqe.qs.modules.behavior.domain.pojo.BehaviorState::getBizType, BizTypeEnum.ARTICLE.code())
                .eq(io.charqe.qs.modules.behavior.domain.pojo.BehaviorState::getAction, io.charqe.qs.modules.behavior.enums.BehActionType.LIKE.code())
                .eq(io.charqe.qs.modules.behavior.domain.pojo.BehaviorState::getStatus, 1)
                .count() > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setArticleStatus(Long articleId, Boolean viewStatus, Boolean commentStatus, Boolean recommendStatus) {
        LambdaUpdateWrapper<Article> wrapper = Lb.<Article>u()
                .eq(Article::getId, articleId)
                .eq(Article::getUserId, BlogUtil.getUserId());

        if (Objects.nonNull(viewStatus)) {
            wrapper.set(Article::getViewStatus, viewStatus);
        }
        if (Objects.nonNull(commentStatus)) {
            wrapper.set(Article::getCommentStatus, commentStatus);
        }
        if (Objects.nonNull(recommendStatus)) {
            wrapper.set(Article::getRecommendStatus, recommendStatus);
        }

        int row = articleMapper.update(wrapper);
        if (row == 0) {
            // 二次查询，明确失败原因（用于给前端准确的错误码）
            Article exist = articleMapper.selectById(articleId);
            if (exist == null) {
                throw new BizException(ArticleIErrorCode.NOT_FOUND);
            }
            throw new BizException(ArticleIErrorCode.UPDATE_FAILED);
        }

    }

    /**
     * 文章条件分页
     *
     * @param dto
     * @return
     */
    @Override
    public ArticlePageVO listArticle(ArticlePageDTO dto) {

        List<Long> ids = null;
        List<List<Long>> idList = null;
        if (StringUtils.hasText(dto.getArticleSearch())) {
            idList = commonQuery.getArticleIds(dto.getArticleSearch());
            ids = idList.stream().flatMap(Collection::stream).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(ids)) {
                return new ArticlePageVO();
            }
        }


        Page<Article> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<Article> articlePage = articleMapper.selectPage(page, Wrappers.<Article>lambdaQuery()
                .like(StringUtils.hasText(dto.getSearchKey()), Article::getArticleTitle, dto.getSearchKey())
                .like(StringUtils.hasText(dto.getArticleSearch()), Article::getArticleTitle, dto.getArticleSearch())
                .eq((Objects.nonNull(dto.getRecommendStatus()) && dto.getRecommendStatus()), Article::getRecommendStatus, StatusEnum.ENABLE.code())
                .eq(Objects.nonNull(dto.getSortId()), Article::getSortId, dto.getSortId())
                .eq(Objects.nonNull(dto.getLabelId()), Article::getLabelId, dto.getLabelId())
                .orderByDesc(Article::getCreateTime));

        PageResult<Article> pageResult = PageResult.fromMpPage(articlePage);
        ArticlePageVO articleListPage = ArticlePageVO.build(pageResult, dto.getDesc(), dto.getSearchKey(), dto.getSortId(), dto.getLabelId());
        List<Article> records = pageResult.getList();
        if (!CollectionUtils.isEmpty(records)) {
            List<ArticleVO> articles = new ArrayList<>();
            List<ArticleVO> titles = new ArrayList<>();
            // List<ArticleVO> contents = new ArrayList<>();

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
                    // 标题符合文章
                    titles.add(articleVO);
                }
                // else if (idList.get(1).contains(articleVO.getId())) {
                //     // 内容符合文章
                //     contents.add(articleVO);
                // }
            }

            List<ArticleVO> collect = new ArrayList<>();
            collect.addAll(articles);
            collect.addAll(titles);
            // if (!CollectionUtils.isEmpty(contents)) {
            //     collect.addAll(contents);
            // }
            articleListPage.setList(collect);
        }

        return articleListPage;
    }

    private ArticleVO buildArticleVO(Article article, Boolean isAdmin) {
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(article, articleVO);
        if (!isAdmin) {
            if (!StringUtils.hasText(articleVO.getArticleCover())) {
                articleVO.setArticleCover(BlogUtil.getRandomCover(articleVO.getId().toString()));
            }
        }

        User user = commonQuery.getUser(articleVO.getUserId());
        if (user != null && StringUtils.hasText(user.getUsername())) {
            articleVO.setUsername(user.getUsername());
        } else if (!isAdmin) {
            articleVO.setUsername(BlogUtil.getRandomName(articleVO.getUserId().toString()));
        }
        if (articleVO.getCommentStatus()) {
            articleVO.setCommentCount(commonQuery.getCommentCount(articleVO.getId(), CommentTypeEnum.ARTICLE.code()));
        } else {
            articleVO.setCommentCount(0);
        }

        List<Category> categoryInfo = commonQuery.getCategoryInfo();
        if (!CollectionUtils.isEmpty(categoryInfo)) {
            for (Category c : categoryInfo) {
                if (c.getId().intValue() == articleVO.getSortId().intValue()) {
                    articleVO.setCategory(c);
                    // 获取标签信息
                    Tag tag = tagMapper.selectById(articleVO.getLabelId());
                    if (tag != null) {
                        articleVO.setTag(tag);
                    }
                    break;
                }
            }
        }
        return articleVO;
    }
}


