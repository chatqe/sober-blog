package com.joy.qs;

import com.joy.qs.utils.BaiduIPLocator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * baidu测试类
 *
 * @author sjq
 * @since 2025-07-13 18:22
 */
@SpringBootTest(classes = BlogApplication.class)
@Slf4j
public class BaiduApiTest {

    @Resource
    private BaiduIPLocator baiduIPLocator;
    @Test
    public void test() {
       // String ip="113.105.68.23";//CN|广东省|深圳市|龙岗区|None|100|92|84
        String ip="111.60.23.2";//CN|湖北省|武汉市|黄陂区|None|100|89|88
        String location = baiduIPLocator.getLocationByIp(ip);
        log.info("location: {}", location);
    }
}
