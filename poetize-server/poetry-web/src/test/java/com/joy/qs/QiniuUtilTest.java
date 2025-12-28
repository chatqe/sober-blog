package com.joy.qs;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.utils.cache.QingSiCache;
import com.joy.qs.utils.storage.QiniuUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * qiniu测试类
 *
 * @author sjq
 * @since 2025-03-05 17:03
 */
@SpringBootTest(classes = BlogApplication.class)
@ActiveProfiles("dev")
@Slf4j
public class QiniuUtilTest {

    @Resource
    private QiniuUtil qiniuUtil;

    @Resource
    private QingSiCache qingSiCache;

    /**
     * 更新缓存
     */
    @Test
    public void test() {
        // qiniuUtil.fileList();
        // qiniuUtil.getBucketInfo();
        List<String> randomAvatars = qiniuUtil.list("qingsi/static/avatar/");
        // 缓存
        qingSiCache.put(CommonConst.PATH_TYPE_RANDOM_AVATAR, randomAvatars);


    }

    /**
     * 1) webp转码
     */
    @Test
    public void convertProductDir() {
        /* 1. 捞出所有待转码文件（不带域名） */
        // List<String> srcKeys = qiniuUtil.listNoDomain("qingsi/static/avatar/");
        // List<String> srcKeys = qiniuUtil.listNoDomain("qingsi/static/backgroundImg/");
        // List<String> srcKeys = qiniuUtil.listNoDomain("backgroundImg/");
        List<String> srcKeys = qiniuUtil.listNoDomain("articleCover/");
        log.info("共扫描到 {} 张图片", srcKeys.size());

        /* 2. 只保留 jpg/jpeg/png，防止目录里混了其他文件 */
        List<String> filterKeys = srcKeys.stream()
                .filter(k -> {
                    String kLow = k.toLowerCase();
                    return kLow.endsWith(".jpg") ||
                            kLow.endsWith(".jpeg") ||
                            kLow.endsWith(".png");
                })
                .collect(Collectors.toList());
        log.info("符合格式 {} 张", filterKeys.size());
        if (CollUtil.isEmpty(filterKeys)) return;

        /* 3. 批量提交转码任务（质量 85） */
        Map<String, String> key2Pid = qiniuUtil.batchSubmitPfop(filterKeys, 85);
        log.info("已提交 {} 个任务", key2Pid.size());

        /* 4. 轮询直到全部结束 */
        Map<String, Integer> finalResult = new ConcurrentHashMap<>();
        while (true) {
            // 还剩多少没出结果
            long pending = key2Pid.entrySet().stream()
                    .filter(e -> !finalResult.containsKey(e.getKey()))
                    .count();
            if (pending == 0) break;

            log.info("还剩 {} 个任务未完成，3 秒后继续查询...", pending);
            ThreadUtil.safeSleep(3000);

            // 批量查状态（并行）
            key2Pid.entrySet().parallelStream()
                    .filter(e -> !finalResult.containsKey(e.getKey()))
                    .forEach(e -> {
                        int code = qiniuUtil.queryPfopStatus(e.getValue());
                        if (code != 1 && code != 2) {   // 1等待 2进行中 0成功 3失败
                            finalResult.put(e.getKey(), code);
                        }
                    });
        }

        /* 5. 统计结果 */
        long succ = finalResult.values().stream().filter(c -> c == 0).count();
        long fail = finalResult.values().stream().filter(c -> c == 3).count();
        log.info("转码完成，成功：{}，失败：{}", succ, fail);

        // 打印失败的 key，方便重试
        finalResult.entrySet().stream()
                .filter(e -> e.getValue() == 3)
                .forEach(e -> log.warn("转码失败 key={}", e.getKey()));


    }

    /**
     * 2)删除 压缩前旧图片 =>  非 webp 且大于250kb 图片
     */
    @Test
    public void delUnWebpImg() {
        // List<String> delKeys = qiniuUtil.listLargeNonWebp("qingsi/static/avatar/");
        // List<String> delKeys = qiniuUtil.listLargeNonWebp("qingsi/static/backgroundImg/");
        // List<String> delKeys = qiniuUtil.listLargeNonWebp("backgroundImg/");
        List<String> delKeys = qiniuUtil.listLargeNonWebp("articleCover/");
        log.info("需要删除的有 {} 张图片", delKeys.size());
        qiniuUtil.deleteFile(delKeys);
    }
}
