package io.charqe.qs.infra.thirdparty.baidumap.dto;

import lombok.Data;

/**
 * 访问日志实体百度API地址转化DTO
 *
 * @author Joy
 * @since 2026-01-24 17:37
 */
@Data
public class AccessLogConvertDTO {

    private String country;
    private String province;
    private String city;
    private String district;
}
