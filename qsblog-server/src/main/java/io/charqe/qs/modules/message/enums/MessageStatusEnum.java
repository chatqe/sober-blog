package io.charqe.qs.modules.message.enums;

import io.charqe.qs.common.enums.BaseEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum MessageStatusEnum implements BaseEnum<Integer> {

    NORMAL(1, "正常"),
    PENDING(2, "审核中"),
    REJECTED(3, "拒绝"),
    HIDDEN(4, "隐藏");

    private final Integer code;
    private final String desc;

    MessageStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getDesc() { return desc; }

    public static MessageStatusEnum getByCode(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static Map<Integer, String> getEnumMap() {
        return Arrays.stream(values())
                .collect(Collectors.toMap(MessageStatusEnum::getCode, MessageStatusEnum::getDesc));
    }
}
