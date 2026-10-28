package io.charqe.qs;

import io.charqe.qs.common.annotation.IdNum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ID 序列化守卫测试（全局 Long→字符串约定的强制器）
 *
 * <p>机制：全站只有两处代码决定 ID 的对外形态——{@code JacksonConfig}（运行时把 Long 序列化为字符串，
 * 且刻意不覆盖原生 long）与 {@code LongIdSchemaConverter}（把 Long 的 OpenAPI schema 同步改为 string）。
 * 因此 Java 类型本身就是契约：{@code Long} 只表示 ID，统计/分页/权重等数值一律 Integer，
 * 字节数这类确需 bigint 的数值用 {@link IdNum} 显式豁免。字段级注解不再参与正确性，
 * 也就不存在"169 个字段里只有 21 个记得加注解"的漂移（那是分类标签 NaN 事故的根因）。
 *
 * <p>本测试因此守护的是类型边界，共五条规则：
 * <ol>
 *   <li>Long 字段必须是 ID 语义命名，或显式标注 {@link IdNum}；否则它会被全局序列化成字符串而说谎；</li>
 *   <li>{@link IdNum} 只能用于非 ID 语义的 Long 字段（防止误标到 sort/count 或 ID 上）；</li>
 *   <li>ID 语义字段不得声明为 Integer/int：Integer 不被全局机制改写，会成为唯一一个数字 id；</li>
 *   <li>ID 语义字段不得声明为原生 long：{@code addSerializer(Long.class)} 不命中 primitive，
 *       会静默输出数字，是最难发现的一类失配。</li>
 *   <li>HTTP 形参（{@code @RequestParam}/{@code @PathVariable}）的 ID 同样必须 Long：它们不进字段，
 *       前四条永远扫不到，却直接决定 OpenAPI 下发的 int32/string，danmaku 删除接口生成
 *       {@code id: number} 就是这条盲区漏出来的。</li>
 * </ol>
 *
 * <p>未覆盖：本仓编译未开 {@code -parameters}，方法形参名不可反射，因此只能按注解里的 HTTP 参数名判定；
 * 服务层形参（如 {@code subscribe(Long labelId)}）不进入 HTTP 契约，靠约定与评审保障。
 *
 * <p>用纯反射实现（ArchUnit 擅长包依赖与类级规则，字段类型断言用反射更直接）。
 * 本仓默认 settings.xml 里 {@code skipTests=true}，需显式 {@code -DskipTests=false} 才会执行。
 *
 * @author sjq
 * @since 2026-10-26
 */
class IdSerializationGuardTest {

    /** ID 字段命名：id、驼峰结尾的 xxxId/xxxID，以及命名不带 Id 但语义是 ID 的历史字段 */
    private static final Pattern ID_FIELD = Pattern.compile("^(id|[a-zA-Z][a-zA-Z0-9]*[iI]d|delBy)$");

    /** HTTP 形参命名：单值 ID 与批量 ID 列表（ids/xxxIds） */
    private static final Pattern ID_PARAM = Pattern.compile("^(id|[a-zA-Z][a-zA-Z0-9]*[iI]d)$");
    private static final Pattern ID_LIST_PARAM = Pattern.compile("^(ids|[a-zA-Z][a-zA-Z0-9]*[iI]ds)$");

    /**
     * 允许保持 Integer 的 ID 语义字段：数据库列本身是 int UNSIGNED，不是 bigint 主键
     */
    private static final Set<String> INTEGER_ID_ALLOWED = Set.of(
            "io.charqe.qs.modules.file.domain.pojo.FileInfo#tenantId"
    );

    /** 参与序列化的包段：只有这些层里的类才会出现在 JSON 出入参与 OpenAPI 里 */
    private static final Set<String> SERIALIZED_PACKAGES = Set.of("controller", "domain", "vo", "dto", "pojo", "auth", "model");

    private static final Path CLASSES_ROOT = Paths.get("target/classes");
    private static final Path CLASSES = CLASSES_ROOT.resolve("io").resolve("charqe").resolve("qs");

    @Test
    void longFieldsMustBeIdSemanticOrExplicitlyExempt() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : loadSerializableClasses()) {
            for (Field field : declaredFields(clazz)) {
                if (field.getType() != Long.class) {
                    continue;
                }
                if (ID_FIELD.matcher(field.getName()).matches() || field.isAnnotationPresent(IdNum.class)) {
                    continue;
                }
                violations.add(clazz.getName() + "#" + field.getName());
            }
        }
        Assertions.assertTrue(violations.isEmpty(), () -> "非 ID 语义的 Long 字段会被全局序列化成字符串（" + violations.size() + " 处）：\n  "
                + String.join("\n  ", violations)
                + "\n修复方式：统计/分页/权重等数值请改为 Integer；确需 bigint 的数值（如字节数）加 io.charqe.qs.common.annotation.IdNum 显式豁免");
    }

    @Test
    void idNumMustOnlyExemptNonIdLongFields() throws IOException {
        List<String> misuse = new ArrayList<>();
        for (Class<?> clazz : loadSerializableClasses()) {
            for (Field field : declaredFields(clazz)) {
                if (!field.isAnnotationPresent(IdNum.class)) {
                    continue;
                }
                if (field.getType() != Long.class || ID_FIELD.matcher(field.getName()).matches()) {
                    misuse.add(clazz.getName() + "#" + field.getName() + " (" + field.getType().getSimpleName() + ")");
                }
            }
        }
        Assertions.assertTrue(misuse.isEmpty(), () -> "@IdNum 只能用于非 ID 语义的 Long 字段，ID 字段必须走全局字符串约定，误用位置：\n  "
                + String.join("\n  ", misuse));
    }

    @Test
    void idFieldsMustNotBeInteger() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : loadSerializableClasses()) {
            for (Field field : declaredFields(clazz)) {
                if (!ID_FIELD.matcher(field.getName()).matches()) {
                    continue;
                }
                Class<?> type = field.getType();
                boolean integerLike = type == Integer.class || type == int.class;
                if (integerLike && !INTEGER_ID_ALLOWED.contains(clazz.getName() + "#" + field.getName())) {
                    violations.add(clazz.getName() + "#" + field.getName() + " (" + type.getSimpleName() + ")");
                }
            }
        }
        Assertions.assertTrue(violations.isEmpty(), () -> "ID 字段声明为 Integer 不会被全局机制改写，会成为唯一一个数字 id（" + violations.size() + " 处）：\n  "
                + String.join("\n  ", violations)
                + "\n修复方式：改为 Long（数据库 bigint 主键/外键一律 Long）");
    }

    @Test
    void idFieldsMustNotBePrimitiveLong() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : loadSerializableClasses()) {
            for (Field field : declaredFields(clazz)) {
                if (field.getType() == long.class && ID_FIELD.matcher(field.getName()).matches()) {
                    violations.add(clazz.getName() + "#" + field.getName());
                }
            }
        }
        Assertions.assertTrue(violations.isEmpty(), () -> "原生 long 不被 addSerializer(Long.class) 命中，ID 用 long 会静默输出数字（" + violations.size() + " 处）：\n  "
                + String.join("\n  ", violations)
                + "\n修复方式：改为包装类型 Long；需要保持数字的大整数请用 @IdNum 并改名以脱离 ID 语义");
    }

    /**
     * 规则五：HTTP 形参的 ID 必须 Long。
     *
     * <p>只扫字段会留出真实盲区：{@code DanmakuController.del(@RequestParam("id") Integer id)} 会让
     * springdoc 下发 {@code {"type":"integer","format":"int32"}}，orval 生成 {@code id: number}，
     * 与全局“ID 一律字符串”直接冲突；而弹幕表主键是 {@code bigint AUTO_INCREMENT}，用 Integer 接还有溢出风险。
     */
    @Test
    void idRequestParametersMustBeLong() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : loadSerializableClasses()) {
            for (Method method : clazz.getDeclaredMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    String httpName = httpParamName(parameter);
                    if (httpName == null) {
                        continue;
                    }
                    Class<?> type = parameter.getType();
                    if (ID_PARAM.matcher(httpName).matches() && type != Long.class) {
                        violations.add(clazz.getName() + "#" + method.getName() + "(" + httpName + ") -> "
                                + type.getSimpleName());
                    }
                    if (ID_LIST_PARAM.matcher(httpName).matches() && List.class.isAssignableFrom(type)
                            && !isLongList(parameter.getParameterizedType())) {
                        violations.add(clazz.getName() + "#" + method.getName() + "(" + httpName + ") -> List非Long");
                    }
                }
            }
        }
        Assertions.assertTrue(violations.isEmpty(), () -> "HTTP 形参里的 ID 不是 Long，会被 springdoc 下发成 integer（"
                + violations.size() + " 处）：\n  " + String.join("\n  ", violations)
                + "\n修复方式：改为 Long（bigint 主键一律 Long），批量用 List<Long>");
    }

    /** 取 @RequestParam/@PathVariable 声明的 HTTP 参数名；未参与 HTTP 绑定的形参返回 null */
    private String httpParamName(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            String name = annotationName(annotation);
            if (name != null) {
                return name;
            }
        }
        return null;
    }

    /** 不直接依赖 spring-web 的注解类型，按注解全名识别，避免守卫与被测技术栈耦合过深 */
    private String annotationName(Annotation annotation) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        String typeName = annotationType.getName();
        if (!typeName.equals("org.springframework.web.bind.annotation.RequestParam")
                && !typeName.equals("org.springframework.web.bind.annotation.PathVariable")) {
            return null;
        }
        try {
            Object value = annotationType.getMethod("value").invoke(annotation);
            String name = value == null ? "" : value.toString();
            if (name.isEmpty()) {
                Object declared = annotationType.getMethod("name").invoke(annotation);
                name = declared == null ? "" : declared.toString();
            }
            // 未显式命名时 value 为空，此时参数名靠 -parameters，本仓未开，不推断
            return name.isEmpty() ? null : name;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private boolean isLongList(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type[] args = parameterizedType.getActualTypeArguments();
            return args.length == 1 && args[0] == Long.class;
        }
        return false;
    }

    private List<Field> declaredFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> !field.isSynthetic() && !Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toList());
    }

    private List<Class<?>> loadSerializableClasses() throws IOException {
        Assertions.assertTrue(Files.isDirectory(CLASSES), "未找到编译产物 " + CLASSES + "，请先执行 mvn test-compile");

        List<Path> files;
        try (var stream = Files.walk(CLASSES)) {
            files = stream.filter(p -> p.toString().endsWith(".class")).collect(Collectors.toList());
        }

        List<Class<?>> classes = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Path file : files) {
            String className = CLASSES_ROOT.relativize(file).toString()
                    .replace(File.separatorChar, '.')
                    .replaceAll("\\.class$", "");
            if (!inScope(className)) {
                continue;
            }
            try {
                classes.add(Class.forName(className));
            } catch (ClassNotFoundException | LinkageError e) {
                skipped.add(className);
            }
        }
        if (!skipped.isEmpty()) {
            // 编译产物里存在但无法单独加载（例如依赖容器才成立的类）：列出来而不是静默跳过，避免守卫悄悄失效
            System.out.println("[IdSerializationGuardTest] skipped unloadable classes: " + skipped);
        }
        System.out.println("[IdSerializationGuardTest] scanned serialized classes: " + classes.size());
        Assertions.assertTrue(classes.size() >= 80,
                "只扫描到 " + classes.size() + " 个类，扫描范围可能已失效（参见 SERIALIZED_PACKAGES）");
        return classes;
    }

    private boolean inScope(String className) {
        String pkg = className.contains(".") ? className.substring(0, className.lastIndexOf('.')) : "";
        // 内部类名形如 a.b.Outer$Inner，包名按 $ 之前截取
        int dollar = pkg.indexOf('$');
        if (dollar > 0) {
            pkg = pkg.substring(0, dollar);
        }
        Set<String> segments = new LinkedHashSet<>(Arrays.asList(pkg.split("\\.")));
        return segments.stream().anyMatch(SERIALIZED_PACKAGES::contains);
    }
}
