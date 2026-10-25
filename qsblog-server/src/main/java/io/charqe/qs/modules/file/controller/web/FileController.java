package io.charqe.qs.modules.file.controller.web;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.controller.web.vo.FileConfigVO;
import io.charqe.qs.modules.file.controller.web.vo.FileUploadTokenReqVO;
import io.charqe.qs.modules.file.controller.web.vo.FileUploadTokenRespVO;
import io.charqe.qs.modules.file.enums.FileSceneEnum;
import io.charqe.qs.modules.file.service.FileInfoService;
import io.charqe.qs.modules.file.service.FileUploadService;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

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
    private AuthSessionService sessions;

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
}
