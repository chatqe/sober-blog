package io.charqe.qs.modules.file.controller.admin.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.util.StringUtils;

import javax.validation.constraints.AssertTrue;
import java.net.URI;

/**
 * file创建请求vo
 *
 * @author Joy
 * @since 2026-01-21 18:51
 */
@Data
public class FileCreateReqVO extends FileBaseVO {

    /** CDN 前缀直接入参，不再从完整访问地址反向拆解。 */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "CDN 前缀必须是不含用户信息、查询参数或片段的 HTTP 地址或相对路径")
    public boolean isCdnDomainValid() {
        String domain = getCdnDomain();
        if (!StringUtils.hasText(domain)) return true;
        try {
            URI uri = URI.create(domain);
            if (uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getRawUserInfo() != null) {
                return false;
            }
            if (!uri.isAbsolute()) return uri.getRawAuthority() == null;
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
