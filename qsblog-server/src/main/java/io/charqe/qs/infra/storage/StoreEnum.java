package io.charqe.qs.infra.storage;

import io.charqe.qs.common.exception.BizException;
import org.springframework.util.StringUtils;

public enum StoreEnum {

    QINIU("qiniu"),
    LOCAL("local");

    private String code;

    StoreEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static StoreEnum existCode(String code) {
        if (StringUtils.hasText(code)) {
            StoreEnum[] values = StoreEnum.values();
            for (StoreEnum typeEnum : values) {
                if (typeEnum.getCode().equals(code)) {
                    return typeEnum;
                }
            }
        }
        throw new BizException("存储平台不支持：" + code);
    }
}


