package io.charqe.qs.modules.file.controller.web;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileDetailRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileListReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRefBindReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRefListReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRefUnbindReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.controller.web.vo.FileConfigVO;
import io.charqe.qs.modules.file.controller.web.vo.FileUploadTokenReqVO;
import io.charqe.qs.modules.file.controller.web.vo.FileUploadTokenRespVO;
import io.charqe.qs.modules.file.domain.pojo.FileBizRef;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.enums.FileSceneEnum;
import io.charqe.qs.modules.file.service.FileBizRefService;
import io.charqe.qs.modules.file.service.FileInfoService;
import io.charqe.qs.modules.file.service.FileMediaService;
import io.charqe.qs.modules.file.service.FileUploadService;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件control
 *
 * @author Joy
 * @since 2026-06-21 4:04
 */

@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private FileUploadService fileUploadService;

    @Resource
    private FileBizRefService fileBizRefService;

    @Resource
    private FileMediaService fileMediaService;

    @Resource
    private AuthSessionService sessions;

    /** 详情缓存策略：仅在 ETag 未命中时下发，其余情况依靠浏览器条件请求。 */
    @Value("${file.detail.cache-control:public, max-age=300}")
    private String detailCacheControl;

    /**
     * 下发服务端 Key 与上传凭证：前端不得自造存储路径。
     */
    @PostMapping("/uploadToken")
    @LoginCheck(scope = SessionScope.EITHER)
    public R<FileUploadTokenRespVO> uploadToken(@Valid @RequestBody FileUploadTokenReqVO req) {
        User user = sessions.current(SessionScope.EITHER, false).user();
        return R.success("获取成功", fileUploadService.token(req, user));
    }

    /**
     * 文件模块公开配置：存储类型、限额、可上传场景。
     */
    @GetMapping("/config")
    public R<FileConfigVO> config() {
        return R.success(fileUploadService.config());
    }

    /**
     * 统一登记（后台与 C 端共用）：拥有者一律取自会话，C 端只允许头像/动态/评论场景。
     */
    @PostMapping("/register")
    @LoginCheck(scope = SessionScope.EITHER)
    public R<FileRespVO> register(@Valid @RequestBody FileCreateReqVO vo) {
        User user = sessions.current(SessionScope.EITHER, false).user();
        String ownerType = isManager(user) ? "admin" : "user";
        String scene = FileSceneEnum.normalize(vo.getScene());
        if ("user".equals(ownerType) && !FileSceneEnum.clientUploadable(scene)) {
            return R.fail("该场景不允许当前账号上传");
        }
        vo.setScene(scene);
        return R.success("登记成功", fileInfoService.register(vo, user.getId(), ownerType));
    }

    private boolean isManager(User user) {
        return user.getUserType() != null && user.getUserType() <= 1;
    }

    /**
     * 我的图片分页：仅返回当前会话账号自己上传的文件，供头像等选图场景使用。
     * <p>
     * 与后台 {@code /file/admin/list} 的区别是拥有者强制取自会话，不得由入参指定。
     */
    @PostMapping("/my/list")
    @LoginCheck(scope = SessionScope.EITHER)
    public R<PageResult<FileInfo>> myList(@RequestBody FileListReqVO req) {
        User user = sessions.current(SessionScope.EITHER, false).user();
        req.normalize();
        IPage<FileInfo> page = fileInfoService.page(
                new Page<>(Math.max(req.getCurrent(), 1), Math.max(req.getSize(), 1)),
                Wrappers.<FileInfo>lambdaQuery()
                        .eq(FileInfo::getOwnerId, user.getId())
                        .eq(StringUtils.hasText(req.getScene()), FileInfo::getScene, req.getScene())
                        .like(StringUtils.hasText(req.getKeyword()), FileInfo::getOriginalName, req.getKeyword())
                        .orderByDesc(FileInfo::getCreateTime));

        return R.success(PageResult.build(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }

    /**
     * 媒体详情：返回完整访问地址、ETag 与三档派生图地址。
     * <p>
     * HEAD 请求由 Spring 按 GET 映射处理并丢弃响应体；{@code If-None-Match} 命中时返回 304。
     */
    @GetMapping("/detail/{id}")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public ResponseEntity<?> detail(@PathVariable("id") Long id,
                                    @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false)
                                    String ifNoneMatch) {
        FileDetailRespVO detail = fileMediaService.detail(id);
        if (detail == null) {
            return ResponseEntity.ok(R.fail(404, "文件不存在"));
        }
        String etag = detail.getEtag() == null ? null : "\"" + detail.getEtag() + "\"";
        if (etag != null && matchesEtag(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header(HttpHeaders.CACHE_CONTROL, detailCacheControl)
                    .eTag(etag)
                    .build();
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, detailCacheControl);
        if (etag != null) {
            builder.eTag(etag);
        }
        return builder.body(R.success(detail));
    }

    /** If-None-Match 支持多值、弱校验前缀与通配。 */
    private boolean matchesEtag(String header, String etag) {
        if (!StringUtils.hasText(header)) {
            return false;
        }
        for (String candidate : header.split(",")) {
            String value = candidate.trim();
            if (value.startsWith("W/")) {
                value = value.substring(2);
            }
            if ("*".equals(value) || etag.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 绑定文件引用：业务对象落库后登记，重复绑定幂等。
     */
    @PostMapping("/ref/bind")
    @LoginCheck(scope = SessionScope.EITHER)
    public R<FileBizRef> bindRef(@Valid @RequestBody FileRefBindReqVO req) {
        User user = sessions.current(SessionScope.EITHER, false).user();
        return R.success("绑定成功", fileBizRefService.bind(req, user.getId(), isManager(user)));
    }

    /**
     * 解绑文件引用：记录不存在时返回 false（幂等）。
     */
    @PostMapping("/ref/unbind")
    @LoginCheck(scope = SessionScope.EITHER)
    public R<Boolean> unbindRef(@Valid @RequestBody FileRefUnbindReqVO req) {
        User user = sessions.current(SessionScope.EITHER, false).user();
        return R.success("解绑成功", fileBizRefService.unbind(req, user.getId(), isManager(user)));
    }

    /**
     * 查询文件引用：统一返回 {@code PageResult}。
     */
    @PostMapping("/ref/list")
    @LoginCheck(scope = SessionScope.EITHER)
    public R<PageResult<FileBizRef>> listRefs(@Valid @RequestBody FileRefListReqVO req) {
        User user = sessions.current(SessionScope.EITHER, false).user();
        IPage<FileBizRef> page = fileBizRefService.pageRefs(req, user.getId(), isManager(user));
        return R.success(PageResult.build(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }
}
