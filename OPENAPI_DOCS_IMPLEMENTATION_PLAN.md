# OpenAPI 文档规范实施计划

## 1. 项目概述

本项目基于 Spring Boot 2.7.x、JDK 17 构建，已集成 knife4j-openapi3-spring-boot-starter 4.5.0 版本。为确保所有 API 接口都能生成规范、完整的 OpenAPI 3.0 文档，需要为项目中所有 Controller 类、接口方法以及数据模型添加符合规范的注释。

## 2. 实施目标

- 为所有 Controller 类添加 @Tag 注解
- 为所有接口方法添加 @Operation、@ApiResponses、@Parameter 等注解
- 为所有数据模型类（POJO、VO、DTO）及其字段添加 @Schema 注解
- 确保生成的 OpenAPI 文档完整、准确、易于理解
- 保持注释风格统一，命名规范一致

## 3. 实施计划

### 3.1 准备阶段

1. **环境检查**
   - 确认项目已正确集成 knife4j-openapi3-spring-boot-starter 4.5.0
   - 确认项目使用 JDK 17
   - 启动项目，访问 Knife4j 界面（默认地址：http://localhost:18081/doc.html），验证当前文档生成情况

2. **依赖检查**
   - 确认项目中已包含以下依赖：
     ```xml
     <dependency>
         <groupId>com.github.xiaoymin</groupId>
         <artifactId>knife4j-openapi3-spring-boot-starter</artifactId>
         <version>4.5.0</version>
     </dependency>
     ```

### 3.2 规范制定阶段

1. **制定注释规范模板**（见下文）
2. **确定统一的命名规范**
3. **制定验证标准**

### 3.3 实施阶段

1. **优先处理核心模块**
   - 文章模块（Article）
   - 用户模块（User）
   - 评论模块（Comment）

2. **按模块依次实施**
   - 为每个模块的 Controller 类添加 @Tag 注解
   - 为每个接口方法添加 @Operation、@ApiResponses、@Parameter 等注解
   - 为每个数据模型类及其字段添加 @Schema 注解

3. **边实施边验证**
   - 每完成一个模块，通过 Knife4j 界面验证文档生成情况
   - 及时修复发现的问题

### 3.4 验证阶段

1. **全面验证**
   - 访问 Knife4j 界面，检查所有模块的文档生成情况
   - 确保所有接口都有完整的注释
   - 确保所有数据模型都有完整的注释

2. **测试接口调用**
   - 通过 Knife4j 界面测试接口调用
   - 验证请求参数和响应格式是否正确

3. **文档导出**
   - 导出 OpenAPI 文档（JSON 或 YAML 格式）
   - 检查导出文档的完整性和准确性

## 4. 注释规范模板

### 4.1 Controller 类规范

**模板**：
```java
/**
 * [模块名称]控制
 * 
 * @author Joy
 * @since [创建日期]
 */
@Tag(name = "[模块名称]", description = "[模块功能描述]")
@RestController
@RequestMapping("/[模块路径]")
public class [Controller类名] {
    // 类实现
}
```

**示例**：
```java
/**
 * 文章控制器
 * 
 * 负责文章的增删改查、点赞、评论等操作
 * 
 * @author Joy
 * @since 2021-08-13
 */
@Tag(name = "article", description = "负责文章的增删改查、点赞、评论等操作")
@RestController
@RequestMapping("/article")
public class ArticleController {
    // 类实现
}
```

### 4.2 接口方法规范

**模板**：
```java
/**
 * [接口功能描述]
 * 
 * @param [参数名] [参数描述]
 * @return [返回结果描述]
 */
@Operation(summary = "[接口功能概述]", description = "[接口详细描述，包括业务逻辑和使用场景]")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "操作成功"),
    @ApiResponse(responseCode = "400", description = "请求参数错误"),
    @ApiResponse(responseCode = "401", description = "未授权"),
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
})
@[请求方法]("/[接口路径]")
public R<[返回类型]> [方法名](
    @Parameter(name = "[参数名]", description = "[参数描述]", required = [是否必填], example = "[示例值]") 
    @[参数注解] [参数类型] [参数名]) {
    // 方法实现
}
```

**示例**：
```java
/**
 * 保存文章
 * 
 * 用于创建新文章，包括文章标题、内容、分类、标签等信息
 * 
 * @param articleVO 文章信息
 * @return 保存结果
 */
@Operation(summary = "save", description = "用于创建新文章，包括文章标题、内容、分类、标签等信息。创建成功后会清除相关缓存，确保数据一致性。")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "保存成功"),
    @ApiResponse(responseCode = "400", description = "请求参数错误，如文章标题或内容为空"),
    @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
})
@LoginCheck(1)
@PostMapping("/save")
public R saveArticle(
    @Parameter(name = "articleVO", description = "文章信息对象", required = true, example = "{\"articleTitle\":\"测试文章\",\"articleContent\":\"文章内容\"}") 
    @Validated @RequestBody ArticleVO articleVO) {
    // 方法实现
}
```

### 4.3 数据模型规范

#### 4.3.1 类注释模板

**模板**：
```java
/**
 * [模型名称] [模型类型]
 * 
 * 用于[模型用途描述]
 * 
 * @author [作者名]
 * @since [创建日期]
 */
@Schema(name = "[模型名称]", description = "[模型详细描述]")
@Data
public class [模型类名] {
    // 类字段
}
```

**示例**：
```java
/**
 * 文章 VO
 * 
 * 用于文章的前端展示和交互
 * 
 * @author sara
 * @since 2021-08-13
 */
@Schema(name = "ArticleVO", description = "文章视图对象，用于文章的前端展示和交互")
@Data
public class ArticleVO {
    // 类字段
}
```

#### 4.3.2 字段注释模板

**模板**：
```java
@Schema(
    name = "[字段名]",
    description = "[字段描述]",
    required = [是否必填],
    example = "[示例值]",
    minLength = [最小长度],
    maxLength = [最大长度],
    format = "[格式约束]",
    [其他约束属性] = [约束值]
)
private [字段类型] [字段名];
```

**示例**：
```java
@Schema(
    name = "articleTitle",
    description = "文章标题",
    required = true,
    example = "测试文章标题",
    minLength = 1,
    maxLength = 200
)
@NotBlank(message = "文章标题不能为空")
private String articleTitle;

@Schema(
    name = "createTime",
    description = "创建时间",
    example = "2023-01-01 12:00:00",
    format = "date-time"
)
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createTime;
```

## 5. 实施示例

### 5.1 Controller 类示例

```java
/**
 * 文章控制器
 * 
 * 负责文章的增删改查、点赞、评论等操作
 * 
 * @author Joy
 * @since 2021-08-13
 */
@Tag(name = "article", description = "负责文章的增删改查、点赞、评论等操作")
@Slf4j
@RestController
@RequestMapping("/article")
public class ArticleController {
    // 类实现
}
```

### 5.2 接口方法示例

```java
/**
 * 查询文章分页
 * 
 * 用于获取文章列表，支持分页、排序等功能
 * 
 * @param dto 分页查询参数
 * @return 文章分页列表
 */
@Operation(summary = "listPage", description = "用于获取文章列表，支持分页、排序等功能。返回结果包含文章基本信息、分类、标签等。")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "查询成功，返回文章分页列表"),
    @ApiResponse(responseCode = "400", description = "请求参数错误"),
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
})
@PostMapping("/listPage")
public R<PageResult> listPage(
    @Parameter(name = "dto", description = "文章分页查询参数", required = true, example = "{\"pageNum\":1,\"pageSize\":10}") 
    @RequestBody ArticlePageDTO dto) {
    return R.success(articleService.listArticle(dto));
}
```

### 5.3 数据模型示例

```java
/**
 * 文章 VO
 * 
 * 用于文章的前端展示和交互
 * 
 * @author sara
 * @since 2021-08-13
 */
@Schema(name = "ArticleVO", description = "文章视图对象，用于文章的前端展示和交互")
@Data
public class ArticleVO {
    
    @Schema(name = "id", description = "文章ID", example = "1")
    private Integer id;
    
    @Schema(name = "userId", description = "用户ID", example = "1001")
    private Integer userId;
    
    @Schema(name = "articleCover", description = "文章封面图片URL", example = "https://example.com/cover.jpg")
    //查询为空时，随机选择
    private String articleCover;
    
    @Schema(
        name = "articleTitle",
        description = "文章标题",
        required = true,
        example = "测试文章标题",
        minLength = 1,
        maxLength = 200
    )
    @NotBlank(message = "文章标题不能为空")
    private String articleTitle;
    
    @Schema(
        name = "articleContent",
        description = "文章内容",
        required = true,
        example = "这是一篇测试文章的内容..."
    )
    @NotBlank(message = "文章内容不能为空")
    private String articleContent;
    
    @Schema(name = "viewCount", description = "浏览次数", example = "100")
    private Integer viewCount;
    
    @Schema(name = "likeCount", description = "点赞次数", example = "20")
    private Integer likeCount;
    
    @Schema(name = "commentStatus", description = "评论状态，true-开启，false-关闭", example = "true")
    private Boolean commentStatus;
    
    @Schema(name = "recommendStatus", description = "推荐状态，true-推荐，false-不推荐", example = "false")
    private Boolean recommendStatus;
    
    @Schema(name = "videoUrl", description = "视频URL", example = "https://example.com/video.mp4")
    private String videoUrl;
    
    @Schema(name = "password", description = "文章密码，为空则不加密", example = "123456")
    private String password;
    
    @Schema(name = "tips", description = "文章提示信息", example = "这是一篇加密文章")
    private String tips;
    
    @Schema(name = "viewStatus", description = "可见状态，true-公开，false-私有", example = "true")
    private Boolean viewStatus;
    
    @Schema(
        name = "createTime",
        description = "创建时间",
        example = "2023-01-01 12:00:00",
        format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @Schema(
        name = "updateTime",
        description = "更新时间",
        example = "2023-01-02 14:30:00",
        format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @Schema(name = "updateBy", description = "更新人", example = "admin")
    private String updateBy;
    
    @Schema(
        name = "sortId",
        description = "文章分类ID",
        required = true,
        example = "1"
    )
    @NotNull(message = "文章分类不能为空")
    private Integer sortId;
    
    @Schema(
        name = "labelId",
        description = "文章标签ID",
        required = true,
        example = "1"
    )
    @NotNull(message = "文章标签不能为空")
    private Integer labelId;
    
    @Schema(name = "commentCount", description = "评论数量", example = "5")
    // 需要查询封装
    private Integer commentCount;
    
    @Schema(name = "username", description = "用户名", example = "testuser")
    private String username;
    
    @Schema(name = "sort", description = "文章分类信息")
    private Sort sort;
    
    @Schema(name = "label", description = "文章标签信息")
    private Label label;
    
    @Schema(name = "hasVideo", description = "是否包含视频，true-是，false-否", example = "false")
    private Boolean hasVideo = false;
}
```

## 6. 实施指南

### 6.1 实施顺序

1. **先处理数据模型**：为所有数据模型类及其字段添加 @Schema 注解
2. **再处理 Controller 类**：为所有 Controller 类添加 @Tag 注解
3. **最后处理接口方法**：为所有接口方法添加 @Operation、@ApiResponses、@Parameter 等注解

### 6.2 注意事项

1. **注释内容要准确**：所有注释必须准确反映代码逻辑和业务含义，使用专业术语
2. **语言要简洁明了**：避免模糊不清的描述，使用简洁、专业的语言
3. **保持风格统一**：确保整个项目的注释风格统一，命名规范一致
4. **包含所有必要信息**：对于复杂业务逻辑或特殊处理的接口，需在注释中说明实现细节和注意事项
5. **使用示例值**：为参数和字段添加合理的示例值，便于理解和测试
6. **注意必填字段**：明确标注哪些字段是必填的，哪些是可选的

### 6.3 验证方法

1. **启动项目**：运行 Spring Boot 应用
2. **访问 Knife4j 界面**：在浏览器中访问 http://localhost:18081/doc.html
3. **检查文档生成情况**：
   - 检查所有模块是否都显示在文档中
   - 检查每个接口的描述、参数、响应是否完整
   - 检查每个数据模型的描述、字段是否完整
4. **测试接口调用**：通过 Knife4j 界面测试接口调用，验证文档的准确性
5. **导出文档**：导出 OpenAPI 文档（JSON 或 YAML 格式），检查文档的完整性和准确性

## 7. 常见问题及解决方案

### 7.1 文档不显示

- **原因**：可能是依赖未正确配置，或者 Controller 类未被扫描到
- **解决方案**：检查依赖配置，确保 Controller 类位于正确的包路径下，且被 Spring 扫描到

### 7.2 注解不生效

- **原因**：可能是使用了错误的注解，或者注解属性设置不正确
- **解决方案**：检查注解的包路径和属性设置，确保使用的是 OpenAPI 3.0 规范的注解

### 7.3 示例值不显示

- **原因**：可能是示例值设置不正确，或者格式不符合要求
- **解决方案**：检查示例值的格式，确保与字段类型匹配

## 8. 总结

通过本实施计划，我们将为项目中的所有 API 接口生成规范、完整的 OpenAPI 3.0 文档。这将有助于：

- 提高 API 接口的可理解性和易用性
- 方便前端开发人员对接 API
- 便于测试人员进行接口测试
- 提高项目的可维护性和扩展性

请团队成员严格按照本计划和规范模板执行，确保所有 API 接口都能生成高质量的 OpenAPI 文档。