package io.charqe.qs.modules.moment.controller.admin;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentPageQueryDTO;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentSaveReqDTO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentDetailVO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentListVO;
import io.charqe.qs.modules.moment.service.MomentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 动态管理控制器（后台管理）
 *
 * @author Joy
 * @since 2026-06-25
 */
@RestController
@RequestMapping("/admin/moment")
@Tag(name = "adminMoment", description = "管理后台-动态管理")
public class AdminMomentController {

    @Resource
    private MomentService momentService;


}
