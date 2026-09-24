package io.charqe.qs.modules.moment.controller.web;


import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.enums.VisibilityEnum;
import io.charqe.qs.modules.article.domain.dao.ArticleMapper;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.moment.domain.pojo.Moment;
import io.charqe.qs.modules.moment.service.MomentService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.StrUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 动态管理
 *
 * @author Joy
 * @since 2025-12-31 20:23
 */

@RestController
@RequestMapping("/moment")
@Tag(name = "moment", description = "动态管理")
public class MomentController {

    @Resource
    private MomentService momentService;


}
