package io.charqe.qs.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.charqe.qs.common.config.IdNumLongSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记"数值型大整数"字段：运行时与 OpenAPI 都必须保持数字
 *
 * <p>背景：全站约定 {@code Long} 只用于 ID，并由 JacksonConfig + LongIdSchemaConverter 成对地把
 * 运行时值与 OpenAPI schema 一起改成字符串。极少数字段确实是 Long 语义但必须保持数字
 * （目前仅 {@code fileSize}：数据库列 {@code bigint UNSIGNED}，字节数可能超过 int 范围，
 * 前端要拿它做除法格式化），用本注解显式豁免。
 *
 * <p>两侧都要写：{@code @JsonSerialize(using = IdNumLongSerializer.class)} 管 Jackson 运行时，
 * {@code @Schema(type = "integer")} 管 OpenAPI schema（由
 * {@link io.charqe.qs.common.config.LongIdSchemaConverter} 读取后放行给默认转换链）。
 * 只写一侧就会重新制造"类型说谎"。
 *
 * <p>为什么不用 {@code @JsonFormat(shape = NUMBER)}：实测 jackson-databind 2.13.5 下它
 * 无法覆盖类型级注册的 {@code ToStringSerializer}，字段照样下发为字符串（见 IdNumFormatProbe
 * 结论与 IdSerializationGuardTest 的运行态断言）。属性级显式序列化器才优先于全局注册。
 *
 * <p>不得用于 ID 字段，也不得用于 Integer 字段（Integer 本来就是数字，无需豁免）。
 * 由 IdSerializationGuardTest 在 CI 强制。
 *
 * @author sjq
 * @since 2026-10-26
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@JacksonAnnotationsInside
@JsonSerialize(using = IdNumLongSerializer.class)
@Schema(type = "integer", format = "int64")
public @interface IdNum {
}
