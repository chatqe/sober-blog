package io.charqe.qs.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * .env 文件加载器
 * 在 Spring 解析 ${...} 占位符之前将 .env 中的变量注入环境
 * 支持多路径查找：IDEA 工作目录 / 模块目录 / classpath
 *
 * @author Joy
 * @since 2026-06-12
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String ENV_FILE_NAME = ".env";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> envMap = loadEnvFile();
        if (!envMap.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dotenv", envMap));
            System.out.println("[Dotenv] 已加载 .env，共 " + envMap.size() + " 个变量");
        } else {
            System.out.println("[Dotenv] 未找到 .env 文件，跳过加载");
        }
    }

    private Map<String, Object> loadEnvFile() {
        Map<String, Object> map = new HashMap<>();

        // 多路径查找 .env（按优先级）
        String[] searchPaths = {
                "./" + ENV_FILE_NAME,                        // 1. JVM 工作目录
                "../" + ENV_FILE_NAME,                       // 2. 上级目录
                "./qsblog-server/" + ENV_FILE_NAME,          // 3. IDEA 从根目录启动时
                System.getProperty("user.home") + "/" + ENV_FILE_NAME // 4. 用户目录
        };

        Resource resource = null;
        for (String path : searchPaths) {
            FileSystemResource fsr = new FileSystemResource(path);
            if (fsr.exists()) {
                resource = fsr;
                System.out.println("[Dotenv] 找到: " + fsr.getFile().getAbsolutePath());
                break;
            }
        }
        // 5. classpath
        if (resource == null) {
            ClassPathResource cpr = new ClassPathResource(ENV_FILE_NAME);
            if (cpr.exists()) {
                resource = cpr;
                System.out.println("[Dotenv] 从 classpath 加载");
            }
        }

        if (resource == null) {
            return map;
        }

        try {
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    map.put(key, value);
                }
            }
        } catch (Exception e) {
            System.err.println("[Dotenv] 读取 .env 失败: " + e.getMessage());
        }
        return map;
    }
}
