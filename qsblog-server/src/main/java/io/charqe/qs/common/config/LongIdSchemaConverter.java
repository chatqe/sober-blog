package io.charqe.qs.common.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Iterator;

/**
 * ID 全链路字符串：把 Long 的 OpenAPI schema 改写为 string
 *
 * <p>与 {@link JacksonConfig} 成对使用：Jackson 决定运行时下发的是字符串，本转换器决定 /v3/api-docs
 * 里声明的也是字符串，orval 才能生成出诚实的 TS 类型。只做 Jackson 侧改造而不动 schema，
 * 前端类型会继续说谎（已实测确认 @JsonFormat 不影响 springdoc 的静态推断）。
 *
 * <p>判定依据是 Java 类型本身，因此不存在“注解漏标”；代价是 Long 的全站语义必须唯一
 * （Long 只用于 ID），非 ID 数值一律 Integer，由 IdSerializationGuardTest 强制。
 *
 * <p>原生 {@code long} 的 rawClass 是 {@code Long.TYPE} 而非 {@code Long.class}，故时间戳等
 * primitive long 不会被改写，与 JacksonConfig 的注册范围保持一致。
 *
 * <p>字段上的 {@code @Schema} 元数据必须在此手工回填：转换器一旦提前返回 schema，springdoc 默认的
 * 属性注解处理就不会再执行（实测 106 个被改写的属性中 73 个 description 直接丢失）。
 * 若字段显式声明了非 string 的 {@code @Schema(type = ...)}（例如 {@code @IdNum} 标注的 fileSize），
 * 则放行给默认转换链，保留逐字段的覆盖能力。
 *
 * @author sjq
 * @since 2026-10-26
 */
@Component
public class LongIdSchemaConverter implements ModelConverter {

    @Override
    public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
        java.lang.reflect.Type type = annotatedType.getType();
        JavaType javaType = type == null ? null : TypeFactory.defaultInstance().constructType(type);
        if (javaType == null || !Long.class.equals(javaType.getRawClass())) {
            return next(annotatedType, context, chain);
        }

        io.swagger.v3.oas.annotations.media.Schema declared = findSchemaAnnotation(annotatedType);
        if (declared != null && (declared.hidden() || isExplicitOtherType(declared.type()))) {
            // 字段自己声明了 schema 类型（如 @IdNum 的 integer/int64）或被标记隐藏，以显式声明为准
            return next(annotatedType, context, chain);
        }

        StringSchema schema = new StringSchema();
        if (declared != null) {
            if (StringUtils.hasText(declared.description())) {
                schema.setDescription(declared.description());
            }
            if (StringUtils.hasText(declared.example())) {
                schema.setExample(declared.example());
            }
            if (StringUtils.hasText(declared.format())) {
                schema.setFormat(declared.format());
            }
            if (declared.nullable()) {
                schema.setNullable(true);
            }
        }
        return schema;
    }

    private boolean isExplicitOtherType(String declaredType) {
        return StringUtils.hasText(declaredType) && !"string".equals(declaredType);
    }

    private io.swagger.v3.oas.annotations.media.Schema findSchemaAnnotation(AnnotatedType annotatedType) {
        Annotation[] annotations = annotatedType.getCtxAnnotations();
        if (annotations == null) {
            return null;
        }
        for (Annotation annotation : annotations) {
            if (annotation instanceof io.swagger.v3.oas.annotations.media.Schema) {
                return (io.swagger.v3.oas.annotations.media.Schema) annotation;
            }
        }
        // 组合注解（如 @IdNum）把 @Schema 放在自己的元注解上，ctxAnnotations 只包含直接注解，需下钻一层
        for (Annotation annotation : annotations) {
            for (Annotation meta : annotation.annotationType().getDeclaredAnnotations()) {
                if (meta instanceof io.swagger.v3.oas.annotations.media.Schema) {
                    return (io.swagger.v3.oas.annotations.media.Schema) meta;
                }
            }
        }
        return null;
    }

    private Schema next(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (chain.hasNext()) {
            return chain.next().resolve(annotatedType, context, chain);
        }
        return null;
    }
}
