# File 模块功能逻辑分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + 七牛云/本地存储 + MapStruct

---

## 模块概览

```
file/
  ├── controller/
  │   ├── admin/
  │   │   ├── FileAdminController.java         # 管理端：上传/列表/删除/保存记录
  │   │   └── vo/
  │   │       ├── FileBaseVO.java              # 文件基础请求对象
  │   │       ├── FileCreateReqVO.java         # 文件创建请求
  │   │       └── FileRespVO.java              # 文件响应对象
  │   ├── web/
  │   │   └── FileController.java              # 前台（未实现，待开发）
  │   └── weblinks/
  │       └── QiniuController.java             # 七牛云前端直传凭证
  ├── service/
  │   ├── FileInfoService.java                 # 继承 CoreService<FileInfo>
  │   ├── FileInfoServiceImpl.java             # 含 save() 自定义逻辑
  │   ├── FileBizRefService.java               # 继承 CoreService<FileBizRef>
  │   └── FileBizRefServiceImpl.java           # 纯继承，无自定义逻辑
  ├── domain/
  │   ├── pojo/
  │   │   ├── FileInfo.java                    # 文件信息实体
  │   │   └── FileBizRef.java                  # 文件业务关系实体
  │   ├── dao/
  │   │   ├── FileStorageMapper.java           # 继承 CoreMapper<FileInfo>
  │   │   └── FileBizRelationMapper.java       # 继承 CoreMapper<FileBizRef>
  │   └── enums/
  │       └── BizTypeEnum.java                 # 业务类型枚举
  └── convert/
      └── FileConvert.java                     # MapStruct 转换器
```

**基础设施层：**
```
infra/storage/
  ├── StoreService.java                        # 存储服务接口
  ├── StoreEnum.java                           # 存储平台枚举
  ├── FileStorageService.java                  # 存储平台路由分发器
  ├── LocalUtil.java                           # 本地文件存储实现
  └── QiniuUtil.java                           # 七牛云存储实现
```

---

## 场景一：管理端上传文件

**Controller** `POST /file/admin/upload`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | 是 | 文件本体 |
| type | String | 是 | 文件类型 |
| relativePath | String | 是 | 相对路径 |
| storeType | Integer | 否 | 存储类型（1=本地, 2=七牛云等） |

**注解：** `@LoginCheck`

**Service 流程：**

```
@PostMapping("/upload")
public R<String> upload(@RequestParam("file") MultipartFile file,
                        @ModelAttribute FileVO fileVO) {

    // Step 1: 参数校验
    if (file == null || file.isEmpty()) {
        return R.fail("文件不能为空");
    }
    if (!StringUtils.hasText(fileVO.getType())) {
        return R.fail("文件类型不能为空");
    }
    if (!StringUtils.hasText(fileVO.getRelativePath())) {
        return R.fail("文件路径不能为空");
    }

    // Step 2: 获取存储服务实例（路由到对应平台）
    StoreService storageService = storeService.getFileStorage(fileVO.getStoreType());

    // Step 3: 执行上传（不同存储平台实现不同逻辑）
    FileVO result = storageService.saveFile(fileVO);

    // Step 4: 返回访问 URL
    return R.success(result.getVisitPath());
}
```

**存储平台路由逻辑（FileStorageService）：**

```
public StoreService getFileStorage(String storeType) {
    if (!StringUtils.hasText(storeType)) {
        // 未指定时使用默认存储平台（${store.type} 配置）
        storeType = defaultStoreType;
    }
    StoreService service = storeServices.get(storeType);
    if (service == null) {
        throw new BizException("不支持的存储平台: " + storeType);
    }
    return service;
}
```

---

## 场景二：保存文件记录到数据库

**Controller** `POST /file/admin/save`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fileName | String | 否 | 文件名称 |
| filePath | String | 是 | 文件路径 |
| fileSize | Long | 否 | 文件大小 |
| fileType | String | 是 | 文件类型 |
| storageType | Integer | 否 | 存储类型 |
| accessUrl | String | 否 | 访问 URL |

**注解：** `@LoginCheck`

**Service 流程：**

```
@PostMapping("/save")
public R save(@RequestBody FileCreateReqVO vo) {

    // Step 1: 校验必要字段
    if (!StringUtils.hasText(vo.getFileType())) {
        return R.fail("文件类型不能为空");
    }
    if (!StringUtils.hasText(vo.getFilePath())) {
        return R.fail("文件路径不能为空");
    }

    // Step 2: MapStruct 转换 VO -> Entity
    FileInfo entity = fileConvert.convert(vo);

    // Step 3: 保存数据库
    fileInfoService.save(entity);

    return R.success();
}
```

---

## 场景三：管理端文件列表查询

**Controller** `POST /file/admin/list`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页条数 |
| fileName | String | 否 | 文件名过滤 |
| filePath | String | 否 | 文件路径过滤 |
| fileSize | Long | 否 | 文件大小过滤 |
| fileType | String | 否 | 文件类型过滤 |
| storageType | Integer | 否 | 存储类型过滤 |
| accessUrl | String | 否 | 访问 URL 过滤 |

**注解：** `@LoginCheck`

**Service 流程：**

```
@PostMapping("/list")
public R<Page> list(@RequestBody BaseReqVO baseReqVO) {

    // Step 1: 构建查询条件
    LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();

    wrapper.like(StringUtils.hasText(baseReqVO.getFileName()),
                 FileInfo::getOriginalName, baseReqVO.getFileName());
    wrapper.like(StringUtils.hasText(baseReqVO.getFilePath()),
                 FileInfo::getStoragePath, baseReqVO.getFilePath());
    wrapper.eq(baseReqVO.getFileSize() != null,
                 FileInfo::getFileSize, baseReqVO.getFileSize());
    wrapper.eq(StringUtils.hasText(baseReqVO.getFileType()),
                 FileInfo::getFileCategory, baseReqVO.getFileType());
    wrapper.eq(baseReqVO.getStorageType() != null,
                 FileInfo::getThirdPartyName, baseReqVO.getStorageType());
    wrapper.like(StringUtils.hasText(baseReqVO.getAccessUrl()),
                 FileInfo::getCdnDomain, baseReqVO.getAccessUrl());

    // Step 2: 分页查询
    Page<FileInfo> page = fileInfoService.page(baseReqVO.toPage(), wrapper);

    // Step 3: MapStruct 转换 Entity -> VO
    Page<FileRespVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
    result.setRecords(fileConvert.convertToRespVOS(page.getRecords()));

    return R.success(result);
}
```

---

## 场景四：删除文件

**Controller** `POST /file/admin/delete`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文件 ID |

**注解：** `@LoginCheck`

**Service 流程：**

```
@PostMapping("/delete")
public R delete(@RequestParam("id") Long id) {
    fileInfoService.removeById(id);
    return R.success();
}
```

---

## 场景五：七牛云前端直传凭证

**Controller** `GET /qiniu/getUpToken?key={key}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| key | String | 是 | 文件标识 |

**注解：** `@LoginCheck`、`@SaveCheck`、`@ConditionalOnBean(QiniuUtil.class)`

**Service 流程：**

```
@GetMapping("/getUpToken")
public R<String> getUpToken(@RequestParam("key") String key) {
    // 调用七牛云工具类生成上传凭证
    String token = qiniuUtil.getToken(key);
    return R.success(token);
}
```

**QiniuUtil.getToken() 实现：**

```
public String getToken(String key) {
    // 设置上传凭证有效期（EXPIRE_SECONDS = 60 秒）
    Auth auth = Auth.create(accessKey, secretKey);

    // 设置文件大小限制（F_SIZE_LIMIT = 20MB）
    PutPolicy putPolicy = new PutPolicy(bucket);
    putPolicy.fsizeLimit = F_SIZE_LIMIT;
    putPolicy.key = key;

    return auth.uploadToken(bucket, key, EXPIRE_SECONDS, putPolicy.build());
}
```

---

## 附录：实体字段文档

### FileInfo 实体

**表名：** `file_info`
**继承：** `BaseDO`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| tenantId | Integer | 租户 ID（SaaS 多租户扩展） |
| originalName | String | 原始文件名 |
| storedName | String | 存储文件名（UUID.ext） |
| fileHash | String | SHA256 哈希（去重 + 秒传） |
| storagePath | String | 存储路径（如 2024/06/18/abc123.jpg） |
| cdnDomain | String | CDN 域名 |
| fileSize | Long | 文件大小（字节） |
| fileCategory | String | 文件分类：image/video/document/audio/other |
| mimeType | String | MIME 类型 |
| fileExt | String | 文件扩展名（小写） |
| width | Integer | 图片/视频宽度（px） |
| height | Integer | 图片/视频高度（px） |
| duration | Integer | 音视频时长（秒） |
| thirdPartyName | Integer | 存储类型：1-本地 2-腾讯COS 3-七牛云 4-S3 5-阿里OSS 6-Minio |
| bucket | String | Bucket 名称 |
| region | String | 存储区域 |
| thirdPartyId | String | 第三方平台文件 ID |
| ownerId | Long | 文件拥有者 ID |
| ownerType | String | 拥有者类型：user/admin/system |
| status | Integer | 状态：1-正常 0-禁用 2-审核中 |
| refCount | Integer | 被业务引用次数 |

### FileBizRef 实体

**表名：** `file_biz_ref`
**继承：** `BaseDO`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| fileId | Long | 文件 ID（file_info.id） |
| bizId | Long | 业务 ID |
| bizType | Integer | 业务类型（BizTypeEnum） |
| sort | Integer | 排序序号 |
| status | Integer | 1-正常 0-隐藏 |

---

## 存储平台路由

### StoreEnum 枚举

| 枚举值 | code | 说明 |
|--------|------|------|
| QINIU | "qiniu" | 七牛云存储 |
| LOCAL | "local" | 本地磁盘存储 |

### StoreService 接口方法

| 方法 | 说明 |
|------|------|
| `deleteFile(List<String> files)` | 批量删除文件 |
| `saveFile(FileVO fileVO)` | 保存文件 |
| `getStoreName()` | 获取存储平台名称 |

### LocalUtil（本地存储）

- 条件激活：`@ConditionalOnProperty(name = "local.enable", havingValue = "true")`
- 校验路径合法性（不超过 5 级目录、不含前后缀斜杠）
- 使用 `MultipartFile.transferTo()` 保存文件

### QiniuUtil（七牛云存储）

- 条件激活：`@ConditionalOnProperty(name = "qiniu.enable", havingValue = "true")`
- 配置属性：`qiniu.accessKey`、`qiniu.secretKey`、`qiniu.bucket`、`qiniu.downloadUrl`
- 前端直传模式：`saveFile()` 返回 null（前端通过 token 直接上传到七牛云）
- 支持图片转码：`submitPfop()` 将 jpg/png 异步转码为 webp
- 批量删除：`batchDel()` 每次最多处理 1000 个文件

---

## 业务类型枚举（BizTypeEnum）

| 编码 | 描述 | 说明 |
|------|------|------|
| 1 | ARTICLE | 文章 |
| 2 | COMMENT | 评论 |
| 3 | VIDEO | 视频 |
| 4 | MUSIC | 音乐 |
| 5 | MOMENT | 微言 |
| 6 | CATEGORY | 专栏 |
| 7 | TAG | 标签 |

---

## 约束与权限矩阵

| 操作 | 权限要求 | 校验规则 |
|------|---------|---------|
| 管理端上传文件 | `@LoginCheck` | file/type/relativePath 非空 |
| 管理端保存文件记录 | `@LoginCheck` | fileType/filePath 非空 |
| 管理端文件列表 | `@LoginCheck` | 分页 + 可选过滤 |
| 管理端删除文件 | `@LoginCheck` | id 非空 |
| 七牛云直传凭证 | `@LoginCheck` + `@SaveCheck` | key 非空 |
| 前台文件上传 | 未实现 | - |

---

## 文件限流过滤器（FileFilter）

拦截路径：`/resource/upload`

| 用户类型 | 限制 |
|---------|------|
| 管理员 | 免限流 |
| 普通用户 | 每天最多 15 次（按 userId 和 IP 分别计数） |

---

## 待实现清单

| 问题 | 优先级 | 说明 |
|------|--------|------|
| FileController 前台上传未实现 | P1 | 注释标注"非管理级文件上传"待开发 |
| 文件去重未实现 | P2 | fileHash 字段已存在但 save() 中未做 SHA256 比对 |
| FileBizRef 引用管理未实现 | P2 | 文件与业务的关联关系未在实际流程中使用 |
| refCount 未维护 | P3 | 文件被引用次数未自动增减 |
| 本地存储路径校验不完善 | P3 | 仅校验目录深度，未校验目录穿越攻击 |
| 七牛云转码状态未回调 | P3 | submitPfop 提交任务后无状态回调处理 |
