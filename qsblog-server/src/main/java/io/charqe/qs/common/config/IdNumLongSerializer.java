package io.charqe.qs.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.charqe.qs.common.annotation.IdNum;

import java.io.IOException;

/**
 * Long 的"数值豁免"序列化器
 *
 * <p>背景：JacksonConfig 用 {@code addSerializer(Long.class, ToStringSerializer.instance)}
 * 把全站 Long（约定只用于 ID）改写成字符串。实测 jackson-databind 2.13.5 下
 * {@code @JsonFormat(shape = NUMBER)} 无法覆盖这个类型级注册——字符串照旧下发，
 * 于是"用 @JsonFormat 保护非 ID 数值"这条路是假的，会留下"schema 是数字、运行态是字符串"
 * 的新说谎（正是本项目最初要消灭的问题）。
 *
 * <p>本序列化器走属性级通道：字段上的 {@code @JsonSerialize(using = IdNumLongSerializer.class)}
 * 显式声明优先于类型级全局注册，因此能真正把 fileSize 这类 bigint 数值保持为数字下发。
 * 本类只应由 {@link IdNum} 引入，不直接手写在其他字段上；是否真的生效由
 * IdSerializationGuardTest 用纯 Jackson 断言，不依赖注解语义推理。
 *
 * @author sjq
 * @since 2026-10-26
 */
public class IdNumLongSerializer extends JsonSerializer<Long> {

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeNumber(value.longValue());
        }
    }
}
