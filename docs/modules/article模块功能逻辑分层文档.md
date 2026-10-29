# Article 模块功能逻辑详细分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + Redis 缓存 + @Async 邮件

---

## 模块概览

```
article/
  ├── controller/
  │   ├── web/
  │   │   └── ArticleController.java          # 用户端：发布/编辑/删除/列表/详情/点赞
  │   └── admin/
  │       └── AdminArticleController.java     # 管理端：文章管理/状态切换
  ├── service/
  │   ├── ArticleService.java
  │   └── ArticleServiceImpl.java
  ├── domain/
  │   ├── pojo/Article.java                   # 文章实体
  │   ├── dao/ArticleMapper.java
  │   └── dto/vo/                             # ArticleDTO, ArticlePageDTO, ArticleVO, ArticlePageVO
  └── convert/
      └── ArticleConvert.java                 # MapStruct 转换器
```

---

## 场景一：发布文章

**Controller** `POST /article/save`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| articleTitle | String | 是 | 文章标题 |
| articleContent | String | 是 | 文章内容（HTML） |
| sortId | Long | 是 | 分类ID |
| labelId | Long | 否 | 标签ID |
| articleCover | String | 否 | 封面图URL |
| password | String | 否 | 密码保护密码 |
| tips | String | 否 | 摘要提示 |

**Service 核心事务流程：**

```
@Transactional(rollbackFor = Exception.class)
public void saveArticle(ArticleDTO dto) {

    // Step 1: 参数校验
    if (!StringUtils.hasText(dto.getTitle())) {
        throw new BizException("标题不能为空");
    }
    if (!StringUtils.hasText(dto.getContent())) {
        throw new BizException("内容不能为空");
    }
    if (dto.getSortId() == null) {
        throw new BizException("请选择分类");
    }

    // Step 2: 分类存在性校验
    Category category = categoryService.getById(dto.getSortId());
    if (category == null || category.getIsDel() == 1) {
        throw new BizException("分类不存在");
    }

    // Step 3: 构建文章实体
    Article article = new Article();
    article.setUserId(currentUserId());
    article.setTitle(dto.getTitle());
    article.setContent(dto.getContent());
    article.setSortId(dto.getSortId());
    article.setLabelId(dto.getLabelId());
    article.setArticleCover(dto.getCover());
    article.setPassword(dto.getPassword());
    article.setTips(dto.getTips());
    article.setViewStatus(true);   // 默认可见
    article.setCommentStatus(true); // 默认开启评论
    article.setRecommendStatus(false); // 默认不推荐

    // Step 4: 保存文章
    save(article);

    // Step 5: 清除分类缓存
    qingSiCache.del(CommonConst.SORT_ARTICLE_LIST);

    // Step 6: 发送订阅邮件（异步，异常不阻断）
    try {
        sendSubscribeEmail(article);
    } catch (Exception e) {
        log.warn("订阅邮件发送失败: {}", e.getMessage());
    }
}
```

**邮件通知逻辑：**

```
private void sendSubscribeEmail(Article article) {
    // 查询订阅了该标签的所有用户
    List<User> subscribers = userMapper.selectList(
        Wrappers.<User>lambdaQuery()
            .like(User::getSubscribe, article.getLabelId().toString())
    );

    for (User user : subscribers) {
        try {
            mailUtil.sendMail(
                user.getEmail(),
                "您的标签「" + article.getTitle() + "」更新了",
                buildEmailContent(article)
            );
        } catch (Exception e) {
            log.error("邮件发送失败: userId={}, error={}", user.getId(), e.getMessage());
        }
    }
}
```

---

## 场景二：编辑文章

**Controller** `POST /article/update`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文章ID |
| 其他字段 | - | 否 | 需要更新的字段 |

**Service 流程：**

```
@Transactional(rollbackFor = Exception.class)
public void updateArticle(ArticleDTO dto) {

    // Step 1: 查询文章
    Article article = getById(dto.getId());
    if (article == null) {
        throw new BizException("文章不存在");
    }

    // Step 2: 所有权校验
    if (!article.getUserId().equals(currentUserId())) {
        throw new BizException("无权编辑该文章");
    }

    // Step 3: 部分更新
    Article update = new Article();
    update.setId(dto.getId());
    update.setTitle(dto.getTitle());
    update.setContent(dto.getContent());
    update.setSortId(dto.getSortId());
    update.setLabelId(dto.getLabelId());
    update.setArticleCover(dto.getCover());
    update.setPassword(dto.getPassword());
    update.setTips(dto.getTips());
    update.setUpdateBy(currentUsername());

    updateById(update);

    // Step 4: 清除缓存
    qingSiCache.del(CommonConst.SORT_ARTICLE_LIST);
}
```

---

## 场景三：删除文章

**Controller** `GET /article/del?id={id}`

**Service 流程：**

```
public void deleteArticle(Long id) {

    // Step 1: 查询文章
    Article article = getById(id);
    if (article == null) {
        throw new BizException("文章不存在");
    }

    // Step 2: 所有权校验
    if (!article.getUserId().equals(currentUserId())) {
        throw new BizException("无权删除该文章");
    }

    // Step 3: 逻辑删除（BaseDO.isDel = 1）
    removeById(id);

    // Step 4: 清除缓存
    qingSiCache.del(CommonConst.SORT_ARTICLE_LIST);
}
```

---

## 场景四：文章列表查询

**Controller** `POST /article/listPage`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页条数 |
| searchKey | String | 否 | 搜索关键词 |
| sortId | Long | 否 | 按分类筛选 |
| labelId | Long | 否 | 按标签筛选 |

**Service 流程：**

```
public PageResult<ArticlePageVO> listArticles(ArticlePageDTO dto) {

    LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

    // 基础条件：未删除 + 可见 + 正常状态
    wrapper.eq(Article::getIsDel, 0);
    wrapper.eq(Article::getViewStatus, true);

    // 分类筛选
    if (dto.getSortId() != null) {
        wrapper.eq(Article::getSortId, dto.getSortId());
    }

    // 标签筛选
    if (dto.getLabelId() != null) {
        wrapper.eq(Article::getLabelId, dto.getLabelId());
    }

    // 关键词搜索（标题模糊匹配）
    if (StringUtils.hasText(dto.getSearchKey())) {
        wrapper.and(w -> w.like(Article::getTitle, dto.getSearchKey())
                .or().like(Article::getContent, dto.getSearchKey()));
    }

    // 排序：推荐文章优先，然后按时间倒序
    wrapper.orderByDesc(Article::getRecommendStatus, Article::getCreateTime);

    // 分页查询
    Page<Article> page = page(dto.toPage(), wrapper);

    // 结果转换：裁剪内容、补充用户名、评论数
    return buildArticlePageVO(page);
}
```

**内容裁剪逻辑：**

```
private ArticleVO buildArticleVO(Article article) {
    ArticleVO vo = ArticleConvert.INSTANCE.toVO(article);

    // 裁剪正文到 SUMMARY 字符（80字），去除 Markdown
    if (StringUtils.hasText(article.getContent())) {
        vo.setSummary(StrUtil.trimContent(article.getContent(), CommonConst.SUMMARY));
    }

    // 补充用户名
    User user = userCacheService.get(article.getUserId());
    vo.setUsername(user != null ? user.getUsername() : "匿名用户");

    // 统计评论数
    vo.setCommentCount(commentService.countByBizId(article.getId()));

    return vo;
}
```

---

## 场景五：文章详情查询（含密码保护）

**Controller** `GET /article/getArticleById?id={id}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文章ID |
| password | String | 否 | 密码保护文章的密码 |

**Service 流程：**

```
public ArticleVO getArticleDetail(Long id, String password) {

    // Step 1: 查询文章
    Article article = getById(id);
    if (article == null) {
        throw new BizException("文章不存在");
    }

    // Step 2: 权限校验
    if (article.getIsDel() == 1) {
        throw new BizException("文章已被删除");
    }
    if (!article.getViewStatus()) {
        throw new BizException("该文章已关闭浏览");
    }

    // Step 3: 密码保护校验
    if (StringUtils.hasText(article.getPassword())) {
        if (!StringUtils.hasText(password) ||
            !article.getPassword().equals(password)) {
            throw new BizException("请输入文章密码");
        }
    }

    // Step 4: 浏览量自增（异步）
    incrementViewCountAsync(id);

    // Step 5: 构建 VO
    ArticleVO vo = buildArticleVO(article);

    // Step 6: 视频URL AES 解密
    if (StringUtils.hasText(vo.getVideoUrl())) {
        vo.setVideoUrl(AESUtil.decrypt(vo.getVideoUrl()));
    }

    return vo;
}
```

---

## 场景六：首页分类文章列表（缓存优化）

**Controller** `GET /article/listSortArticle`

**Service 核心逻辑（双重检查锁 + Redis 缓存）：**

```
public List<ArticlePageVO> listSortArticles() {

    // Step 1: 尝试从缓存读取
    String cacheKey = CommonConst.SORT_ARTICLE_LIST;
    String cached = qingSiCache.get(cacheKey);
    if (StringUtils.hasText(cached)) {
        return JSON.parseArray(cached, ArticlePageVO.class);
    }

    // Step 2: 双重检查锁
    synchronized (this) {
        cached = qingSiCache.get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return JSON.parseArray(cached, ArticlePageVO.class);
        }

        // Step 3: 查询所有可见分类
        List<Category> categories = categoryService.lambdaQuery()
            .eq(Category::getIsDel, 0)
            .list();

        List<ArticlePageVO> result = new ArrayList<>();

        for (Category category : categories) {
            // 查询该分类下的文章
            List<Article> articles = lambdaQuery()
                .eq(Article::getSortId, category.getId())
                .eq(Article::getViewStatus, true)
                .orderByDesc(Article::getRecommendStatus, Article::getCreateTime)
                .list();

            // 按权重排序 (viewCount + likeCount * 2)，取前6篇
            articles.sort(Comparator.comparingInt(Article::getWeight).reversed());
            List<Article> top6 = articles.subList(0, Math.min(6, articles.size()));

            // 构建 VO
            for (Article article : top6) {
                result.add(buildArticleVO(article));
            }
        }

        // Step 4: 缓存结果（TTL = TOKEN_INTERVAL = 3600秒）
        qingSiCache.put(cacheKey, JSON.toJSONString(result), CommonConst.TOKEN_INTERVAL);

        return result;
    }
}
```

---

## 场景七：文章点赞

**Controller** `POST /article/addArticleLikeCount`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文章ID |

**Service 流程：**

```
public void toggleArticleLike(Long id) {

    // Step 1: 文章存在性校验
    Article article = getById(id);
    if (article == null) {
        throw new BizException("文章不存在");
    }

    // Step 2: 用户是否已点赞
    String userLikeKey = CommonConst.USER_LIKES + id;
    Boolean hasLiked = redisTemplate.opsForHash().hasKey(userLikeKey, currentUserId());

    if (hasLiked) {
        // 取消点赞：删除 Redis 记录 + 减少数据库计数
        redisTemplate.opsForHash().delete(userLikeKey, currentUserId());
        article.setLikeCount(Math.max(0, article.getLikeCount() - 1));
        updateById(article);
    } else {
        // 点赞：写入 Redis 记录 + 增加数据库计数
        redisTemplate.opsForHash().put(userLikeKey, currentUserId(), System.currentTimeMillis());
        article.setLikeCount(article.getLikeCount() + 1);
        updateById(article);
    }

    // Step 3: 清除缓存
    qingSiCache.del(CommonConst.SORT_ARTICLE_LIST);
}
```

---

## 场景八：管理端文章状态切换

**Controller** `GET /admin/article/changeArticleStatus`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文章ID |
| type | String | 是 | viewStatus / commentStatus / recommendStatus |
| value | Boolean | 是 | 新状态值 |

**Service 流程：**

```
@Transactional(rollbackFor = Exception.class)
public void toggleArticleStatus(Long id, String type, Boolean value) {

    // Step 1: 查询文章
    Article article = getById(id);
    if (article == null) {
        throw new BizException("文章不存在");
    }

    // Step 2: 所有权校验
    if (!article.getUserId().equals(currentUserId())) {
        throw new BizException("无权修改该文章状态");
    }

    // Step 3: 条件更新
    Article update = new Article();
    update.setId(id);
    switch (type) {
        case "viewStatus":
            update.setViewStatus(value);
            break;
        case "commentStatus":
            update.setCommentStatus(value);
            break;
        case "recommendStatus":
            update.setRecommendStatus(value);
            break;
        default:
            throw new BizException("未知的状态类型");
    }

    updateById(update);

    // Step 4: 清除缓存
    qingSiCache.del(CommonConst.SORT_ARTICLE_LIST);
}
```

---

## 附录：状态与权限矩阵

| 操作 | 权限要求 | 校验规则 |
|------|---------|---------|
| 发布文章 | `@LoginCheck(1)` | 用户本人 |
| 编辑文章 | `@LoginCheck(1)` | 用户本人 |
| 删除文章 | `@LoginCheck(1)` | 用户本人 |
| 查看详情 | 公开 | 未删除 + 可见 + 密码正确 |
| 点赞 | 公开 | 文章存在 |
| 列表查询 | 公开 | 未删除 + 可见 |
| 管理端状态切换 | `@LoginCheck(1)` | 用户本人 |
| 管理端全量列表 | `@LoginCheck(0)` | 管理员 |

---

## 待实现清单

| 功能 | 优先级 | 说明 |
|------|--------|------|
| Admin 全量文章管理 | P1 | 管理员查看/编辑/删除所有文章 |
| 文章回收站 | P2 | 软删除后可恢复 |
| 文章版本历史 | P3 | 记录每次修改 |
| OpenAPI 注解补充 | P3 | 已有部分，需完善 |

---

## DDL 变更留痕

### 2026-10-29 新增 `publish_status`（草稿箱）

为支持后台“草稿箱”，`article` 表新增发布状态列（此前无任何草稿/发布维度；`view_status` 仅表示“公开/密码可见的隐藏发布文”，语义上不能复用为草稿）。

```sql
ALTER TABLE `article`
  ADD COLUMN `publish_status` tinyint NOT NULL DEFAULT 1 COMMENT '发布状态[0:草稿，1:已发布]' AFTER `comment_status`;
```

- 存量文章默认 `1`（已发布），行为不变。
- 草稿（`publish_status=0`）仅后台草稿箱可见；前台公开读路径一律排除：首页分类区块（`listCategoryArticle`）、文章总分页（`listArticle`）、公开详情（`/article/getArticleById` 拦截）、标签文章数（`countByTagId`）、搜索集合（`getArticleIds`）。
- “存为草稿”走 `/article/save`（`publishStatus=0`）；草稿编辑走 `/admin/article/getArticleWithPwd`（后台专用，不拦截草稿）；发布/转草稿走 `/admin/article/changeArticleStatus` 新增的 `publishStatus` 入参。
- 缓存语义变更：`article_list`→`article_list_v2`、`category_article_list_v4`→`category_article_list_v5`；save/update/changeArticleStatus 均清除相关缓存。
