package io.charqe.qs.config;

import com.alibaba.fastjson.JSON;
import io.charqe.qs.common.R;
import io.charqe.qs.enums.CodeMsg;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.storage.FileFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class YouthFilter extends OncePerRequestFilter {

    @Resource
    private CommonQuery commonQuery;

    @Resource
    private FileFilter fileFilter;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        if (!"OPTIONS".equals(httpServletRequest.getMethod())) {
            try {
                // 保存访问记录
                commonQuery.saveHistory(BlogUtil.getIpAddr(httpServletRequest));
            } catch (Exception e) {
            }

            if (fileFilter.doFilterFile(httpServletRequest, httpServletResponse)) {
                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
                httpServletResponse.setContentType("application/json;charset=UTF-8");
                httpServletResponse.getWriter().write(JSON.toJSONString(R.fail(CodeMsg.PARAMETER_ERROR.getCode(), CodeMsg.PARAMETER_ERROR.getMsg())));
                return;
            }
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}


