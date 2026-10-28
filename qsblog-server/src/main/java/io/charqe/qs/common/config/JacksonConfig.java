package io.charqe.qs.common.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ID 全链路字符串：Long 的全局序列化约定
 *
 * <p>约定：Java 侧的 {@code Long} 只用于 ID 语义（数据库 bigint 主键/外键），JSON 出参一律为字符串；
 * 非 ID 的数值一律使用 {@code Integer}/{@code Boolean}/枚举，不得声明为 Long。
 * 该约定由 IdSerializationGuardTest 在 CI 强制。
 *
 * <p>为什么不逐字段加 @JsonFormat：@JsonFormat 只改 Jackson 运行时，不改 springdoc 生成的 OpenAPI
 * schema（schema 由 ModelResolver 依据 Java 类型静态推断），实测会造成"运行时字符串、TS 类型 number"
 * 的双向说谎。这里改为全局注册序列化器，并由 {@link LongIdSchemaConverter} 同步改写 schema，
 * 两侧同源，不存在漏标。
 *
 * <p>刻意不注册 {@code Long.TYPE}（原生 long）：时间戳（AuthSessionVO.issuedAt 等）与响应信封的
 * {@code R.currentTimeMillis} 是 primitive long，语义是数值毫秒，前端要做相减与 Number.isFinite 校验，
 * 必须保持数字。primitive long 与包装 Long 因此成为唯一需要警惕的区分，不得随意改成 Long。
 *
 * @author sjq
 * @since 2026-10-26
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule idLongToStringModule() {
        SimpleModule module = new SimpleModule("qs-id-long-to-string");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        return module;
    }
}
