package io.charqe.qs;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.charqe.qs.common.annotation.IdNum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 豁免机制的运行态探针（保留为回归用例，不是临时脚本）
 *
 * <p>固定两条事实，避免以后再凭直觉选方案：
 * 1) {@code @JsonFormat(shape = NUMBER)} 在 jackson-databind 2.13.5 下无法覆盖类型级注册的
 *    ToStringSerializer —— 所以它不能用来保护非 ID 数值；
 * 2) {@code @IdNum}（内部是属性级 @JsonSerialize(using = IdNumLongSerializer.class)）能覆盖，
 *    且不影响同类的普通 Long 字段（ID 仍为字符串）。
 *
 * 复现方式：把 @IdNum 的注解定义改回 @JsonFormat(NUMBER)，本测试第二个断言会失败。
 */
class IdNumFormatProbeTest {

    /** 与 JacksonConfig 一致的运行时约定：Long 一律下发字符串 */
    private static ObjectMapper idToStringMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("probe");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        mapper.registerModule(module);
        return mapper;
    }

    public static class FormatProbe {
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        private Long formatNumberField = 9812L;

        private Long plainField = 9812L;

        public Long getFormatNumberField() { return formatNumberField; }
        public Long getPlainField() { return plainField; }
    }

    public static class IdNumProbe {
        @IdNum
        private Long exemptField = 9812L;

        private Long idLikeField = 16L;

        public Long getExemptField() { return exemptField; }
        public Long getIdLikeField() { return idLikeField; }
    }

    @Test
    void jsonFormatNumberCannotOverrideGlobalLongSerializer() throws Exception {
        String json = idToStringMapper().writeValueAsString(new FormatProbe());
        Assertions.assertEquals("{\"formatNumberField\":\"9812\",\"plainField\":\"9812\"}", json,
                "@JsonFormat(NUMBER) 竟覆盖了全局 Long 序列化器？说明 Jackson 版本行为已变化，需重新评估豁免方案");
    }

    @Test
    void idNumKeepsFieldNumericWhileOtherLongsStayString() throws Exception {
        String json = idToStringMapper().writeValueAsString(new IdNumProbe());
        Assertions.assertEquals("{\"exemptField\":9812,\"idLikeField\":\"16\"}", json,
                "@IdNum 豁免未按预期生效：豁免字段必须是 JSON 数字，未标注的 Long 必须仍是字符串");
    }
}
