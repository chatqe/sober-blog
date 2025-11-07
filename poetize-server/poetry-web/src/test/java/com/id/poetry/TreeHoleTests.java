package com.Id.poetry;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ld.poetry.PoetryApplication;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.entity.TreeHole;
import com.ld.poetry.service.TreeHoleService;
import com.ld.poetry.utils.QingsiUtil;
import com.ld.poetry.utils.cache.QingSiCache;
import com.ld.poetry.utils.storage.QiniuUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.util.List;
import java.util.Random;

/**
 * 树洞 测试类
 *
 * @author sjq
 * @since 2025-03-03 17:22
 */
@SpringBootTest(classes = PoetryApplication.class)
@ActiveProfiles("dev")
@Slf4j
public class TreeHoleTests {

    @Resource
    private TreeHoleService treeHoleService;

    @Resource
    private QingSiCache qingSiCache;

    @Resource
    private QiniuUtil qiniuUtil;

    @Test
    void saveTreeHoleList() {

        String listStr="[\n" +
                "  {\n" +
                "    \"id\": 3252,\n" +
                "    \"avatar\": \"https://file.poetize.cn/userAvatar/1064801740981653674469.jpg\",\n" +
                "    \"message\": \"我是搞清楚了原来在这里\",\n" +
                "    \"createTime\": \"2025-03-03T14:02:24\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3251,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383642997568\",\n" +
                "    \"message\": \"6666\",\n" +
                "    \"createTime\": \"2025-03-03T13:48:52\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3250,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383640738096\",\n" +
                "    \"message\": \"\uD83C\uDFA3\",\n" +
                "    \"createTime\": \"2025-03-03T09:17:00\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3249,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383640738091\",\n" +
                "    \"message\": \"泰裤辣\",\n" +
                "    \"createTime\": \"2025-03-02T18:26:38\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3247,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641712032\",\n" +
                "    \"message\": \"牛逼\",\n" +
                "    \"createTime\": \"2025-03-01T20:39:26\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3246,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383642997556\",\n" +
                "    \"message\": \"膜拜大佬\",\n" +
                "    \"createTime\": \"2025-03-01T20:39:15\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3244,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383640738091\",\n" +
                "    \"message\": \"Hello\",\n" +
                "    \"createTime\": \"2025-03-01T03:33:01\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3241,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641712032\",\n" +
                "    \"message\": \"好看\",\n" +
                "    \"createTime\": \"2025-02-26T09:30:59\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3223,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383642997556\",\n" +
                "    \"message\": \"一个字牛\",\n" +
                "    \"createTime\": \"2025-02-19T14:32:03\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3211,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383640738096\",\n" +
                "    \"message\": \"很喜欢大佬做的个人博客\",\n" +
                "    \"createTime\": \"2025-02-15T22:18:43\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3207,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383640738098\",\n" +
                "    \"message\": \"牛逼牛逼\",\n" +
                "    \"createTime\": \"2025-02-13T10:55:16\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3203,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641089662\",\n" +
                "    \"message\": \"流浪地球\",\n" +
                "    \"createTime\": \"2025-02-10T18:08:30\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3202,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641089655\",\n" +
                "    \"message\": \"帅\",\n" +
                "    \"createTime\": \"2025-02-10T18:03:15\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3200,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641476174\",\n" +
                "    \"message\": \"大佬学习\",\n" +
                "    \"createTime\": \"2025-02-09T19:15:38\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3195,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641089668\",\n" +
                "    \"message\": \"哈哈哈哈哈哈哈哈哈哈\",\n" +
                "    \"createTime\": \"2025-02-08T15:33:16\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3194,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641476173\",\n" +
                "    \"message\": \"牛\",\n" +
                "    \"createTime\": \"2025-02-08T15:33:06\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3189,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641089654\",\n" +
                "    \"message\": \"厉害!!!!\",\n" +
                "    \"createTime\": \"2025-02-05T00:57:01\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3187,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383640738096\",\n" +
                "    \"message\": \"我想去外太空\",\n" +
                "    \"createTime\": \"2025-02-04T13:00:01\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3184,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641712031\",\n" +
                "    \"message\": \"2025年1月27日21:46:32\",\n" +
                "    \"createTime\": \"2025-01-27T21:46:35\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3182,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383640738096\",\n" +
                "    \"message\": \"FYF到此一游\",\n" +
                "    \"createTime\": \"2025-01-23T09:58:27\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3175,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641089668\",\n" +
                "    \"message\": \"厉害\",\n" +
                "    \"createTime\": \"2025-01-20T20:59:18\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3167,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641712039\",\n" +
                "    \"message\": \"这网页整个设计，很牛逼\",\n" +
                "    \"createTime\": \"2025-01-18T20:32:11\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3166,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641089668\",\n" +
                "    \"message\": \"牛逼\",\n" +
                "    \"createTime\": \"2025-01-18T08:47:43\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3165,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641476167\",\n" +
                "    \"message\": \"唯美！！！\",\n" +
                "    \"createTime\": \"2025-01-17T16:59:22\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 3164,\n" +
                "    \"avatar\": \"https://file.poetize.cn/randomAvatar/Sara116383641712031\",\n" +
                "    \"message\": \"哈哈哈\",\n" +
                "    \"createTime\": \"2025-01-16T20:06:09\"\n" +
                "  }\n" +
                "]";


        List<TreeHole> treeHoles = JSON.parseObject(listStr, new TypeReference<List<TreeHole>>() {
        });

        log.info("list:{}",treeHoles);

        boolean b = treeHoleService.saveBatch(treeHoles);
        log.info(b?"保存成功":"保存失败");
    }

    @Test
    void updateTreeHoleUrl() {
        // List<String> randomAvatars = JSON.parseObject(qingSiCache.get(CommonConst.PATH_TYPE_RANDOM_AVATAR), new TypeReference<List<String>>() {
        // });

        List<String> randomAvatars = qiniuUtil.list("qingsi/static/avatar/");

        // List<TreeHole> needUpdateList = treeHoleService.list(Wrappers.<TreeHole>lambdaQuery().like(TreeHole::getAvatar, "poetize"));
        log.info("randomAvatars:{}",randomAvatars.size());

        List<TreeHole> needUpdateList = treeHoleService.list(Wrappers.<TreeHole>lambdaQuery().like(TreeHole::getAvatar, "corrain"));
        log.info("list:{}",needUpdateList.size());
        needUpdateList.forEach(treeHole -> {
            treeHole.setAvatar(randomAvatars.get(RandomUtil.randomInt(0, randomAvatars.size())));
        });
        boolean b = treeHoleService.updateBatchById(needUpdateList);
        log.info(b?"更新成功":"更新失败");
        // treeHoleService.remove(Wrappers.<TreeHole>lambdaQuery().like(TreeHole::getAvatar, "poetize"));
    }
}
