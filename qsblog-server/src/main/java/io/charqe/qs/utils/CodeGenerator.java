package io.charqe.qs.utils;

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;
import java.util.Scanner;

/**
 * 模块化代码生成器
 * 适用于当前项目的模块化架构
 *
 * @author Joy
 * @since 2026-01-19 16:23
 */
public class CodeGenerator {

    /**
     * 读取控制台输入
     */
    public static String scanner(String tip) {
        try (Scanner scanner = new Scanner(System.in)) {
            StringBuilder help = new StringBuilder();
            help.append("请输入" + tip + "：");
            System.out.println(help.toString());
            if (scanner.hasNext()) {
                String ipt = scanner.next();
                if (StringUtils.isNotBlank(ipt)) {
                    return ipt;
                }
            }
        }
        throw new MybatisPlusException("请输入正确的" + tip + "！");
    }

    /**
     * 主方法入口
     */
    public static void main(String[] args) {
        // ========== 数据库配置 ==========
        String dbUrl = "jdbc:mysql://localhost:3306/blog_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai";
        String dbUsername = "root";
        String dbPassword = "daxiongroot";

        // ========== 项目配置 ==========
        // 获取当前类所在的项目路径，确保生成到正确的位置
        String projectPath = System.getProperty("user.dir");
        // 确保项目路径指向 qsblog-server 模块
        if (!projectPath.endsWith("qsblog-server")) {
            projectPath = projectPath + "/qsblog-server";
        }
        String author = "Joy";
        String parentPackage = "io.charqe.qs";
        
        // ========== 模块配置 ==========
        String moduleName = scanner("模块名（如：article、user、comment）");
        String tableNames = scanner("表名，多个英文逗号分割");
        String tablePrefix = scanner("表前缀（如：blog_，为空请输入none）");
        if ("none".equals(tablePrefix)) {
            tablePrefix = "";
        }

        // 核心生成器构建
        String finalTablePrefix = tablePrefix;
        String finalProjectPath = projectPath;
        String finalProjectPath1 = projectPath;
        FastAutoGenerator.create(dbUrl, dbUsername, dbPassword)
                // ========== 全局配置 ==========
                .globalConfig(builder -> {
                    builder.author(author) // 设置作者
                            .outputDir(finalProjectPath1 + "/src/main/java") // 代码输出目录
                            .commentDate("yyyy-MM-dd") // 注释日期格式
                            .enableSwagger() // 开启Swagger注解
                            .disableOpenDir(); // 生成后不自动打开文件夹
                })

                // ========== 包配置 ==========
                .packageConfig(builder -> {
                    builder.parent(parentPackage) // 根包名
                            .moduleName(moduleName) // 模块名
                            .entity("domain.pojo") // 实体类包名
                            .mapper("domain.dao") // Mapper接口包名
                            .service("service") // Service接口包名
                            .serviceImpl("service.impl") // Service实现类包名
                            .controller("controller") // Controller包名
                            // 指定Mapper XML输出路径
                            .pathInfo(Collections.singletonMap(OutputFile.xml, 
                                    finalProjectPath + "/src/main/resources/mapper/" + moduleName));
                })

                // ========== 策略配置 ==========
                .strategyConfig(builder -> {
                    // 1. 表配置
                    String[] tables = tableNames.split(",");
                    builder.addInclude(tables); // 要生成的表名
                    
                    // 处理表前缀，避免空字符串问题
                    if (StringUtils.isNotBlank(finalTablePrefix)) {
                        String[] prefixes = finalTablePrefix.split(",");
                        builder.addTablePrefix(prefixes); // 表前缀
                    }

                    // 2. 实体类生成策略
                    builder.entityBuilder()
                            .enableLombok() // 开启Lombok支持
                            .enableTableFieldAnnotation(); // 开启字段注解
                    // 逻辑删除字段配置在父类BaseDO中

                    // 3. Mapper策略
                    builder.mapperBuilder()
                            .enableBaseResultMap() // 生成基础ResultMap
                            .enableBaseColumnList() // 生成基础ColumnList
                            .formatMapperFileName("%sMapper") // Mapper文件名格式
                            .formatXmlFileName("%sMapper"); // XML文件名格式

                    // 4. Service策略
                    builder.serviceBuilder()
                            .formatServiceFileName("%sService") // Service接口文件名格式
                            .formatServiceImplFileName("%sServiceImpl"); // Service实现类文件名格式

                    // 5. Controller策略
                    builder.controllerBuilder()
                            .enableRestStyle() // 开启RestController
                            .enableHyphenStyle() // URL驼峰转连字符
                            .formatFileName("%sController"); // Controller文件名格式
                })

                // ========== 模板引擎配置 ==========
                .templateEngine(new FreemarkerTemplateEngine())

                // ========== 执行生成 ==========
                .execute();
    }
}
