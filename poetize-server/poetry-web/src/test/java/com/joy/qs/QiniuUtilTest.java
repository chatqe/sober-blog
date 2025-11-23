package com.joy.qs;

import com.joy.qs.constants.CommonConst;
import com.joy.qs.utils.cache.QingSiCache;
import com.joy.qs.utils.storage.QiniuUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.util.List;

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



    @Test
    public void test() {
        // qiniuUtil.fileList();
        // qiniuUtil.getBucketInfo();
        List<String> randomAvatars = qiniuUtil.list("qingsi/static/avatar/");
        // 缓存
        qingSiCache.put(CommonConst.PATH_TYPE_RANDOM_AVATAR,randomAvatars);


    }
}
