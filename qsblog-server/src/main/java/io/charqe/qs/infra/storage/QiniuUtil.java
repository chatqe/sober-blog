package io.charqe.qs.infra.storage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.processing.OperationManager;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.model.BatchStatus;
import com.qiniu.storage.model.BucketInfo;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.qiniu.util.UrlSafeBase64;
import io.charqe.qs.common.FileVO;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.resource.domain.pojo.Resource;
import io.charqe.qs.modules.resource.service.ResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * qiniu工具类
 *
 * @author Joy
 * @since 2026-01-02 19:45
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "qiniu.enable", havingValue = "true")
public class QiniuUtil implements StoreService {

    /**
     * 七牛云
     */
    @Value("${qiniu.accessKey}")
    private String accessKey;

    @Value("${qiniu.secretKey}")
    private String secretKey;

    @Value("${qiniu.bucket}")
    private String bucket;

    @Value("${qiniu.downloadUrl}")
    private String downloadUrl;

    @Value("${qiniu.pipeline}")
    private String pipeline;

    @Value("${qiniu.pfop-size-limit}")
    private long pfopSizeLimit;


    private static final long EXPIRE_SECONDS = 60L;
    private static final long F_SIZE_LIMIT = 20971520L;

    @Autowired
    private ResourceService resourceService;

    public String getToken(String key) {
        StringMap putPolicy = new StringMap();
        putPolicy.put("fsizeLimit", F_SIZE_LIMIT);
        Auth auth = Auth.create(accessKey, secretKey);
        return auth.uploadToken(bucket, key, EXPIRE_SECONDS, putPolicy);
    }

    @Override
    public void deleteFile(List<String> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            //单次批量请求的文件数量不得超过1000
            String[] keyList = files.stream().map(path -> path.replace(downloadUrl, "")).toArray(String[]::new);
            BucketManager.BatchOperations batchOperations = new BucketManager.BatchOperations();
            batchOperations.addDeleteOp(bucket, keyList);
            Response response = bucketManager.batch(batchOperations);
            BatchStatus[] batchStatusList = response.jsonToObject(BatchStatus[].class);
            for (int i = 0; i < keyList.length; i++) {
                BatchStatus status = batchStatusList[i];
                String key = keyList[i];
                if (status.code == 200) {
                    log.info("文件删除成功：" + key);
                    resourceService.lambdaUpdate().eq(Resource::getPath, downloadUrl + key).remove();
                } else {
                    log.error("文件删除失败：" + key + "，原因：" + status.data.error);
                }
            }
        } catch (QiniuException ex) {
            log.error("文件删除失败：" + ex.response.toString());
        }
    }


    /**
     * 批量删除
     *
     * @param files
     */
    public void batchDel(List<String> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            //单次批量请求的文件数量不得超过1000
            String[] keyList = files.stream().map(path -> path.replace(downloadUrl, "")).toArray(String[]::new);
            BucketManager.BatchOperations batchOperations = new BucketManager.BatchOperations();
            batchOperations.addDeleteOp(bucket, keyList);
            Response response = bucketManager.batch(batchOperations);
            BatchStatus[] batchStatusList = response.jsonToObject(BatchStatus[].class);
            for (int i = 0; i < keyList.length; i++) {
                BatchStatus status = batchStatusList[i];
                String key = keyList[i];
                if (status.code == 200) {
                    log.info("文件删除成功：" + key);
                    // resourceService.lambdaUpdate().eq(Resource::getPath, downloadUrl + key).remove();
                } else {
                    log.error("文件删除失败：" + key + "，原因：" + status.data.error);
                }
            }
        } catch (QiniuException ex) {
            log.error("文件删除失败：" + ex.response.toString());
        }
    }

    @Override
    public FileVO saveFile(FileVO fileVO) {
        return null;
    }

    @Override
    public String getStoreName() {
        return StoreEnum.QINIU.getCode();
    }

    public void fileList() {
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            FileListing fileListing = bucketManager.listFilesV2(bucket, "/qingsi/static/avatar/", null, 100, null);
            log.info(fileListing.toString());
        } catch (QiniuException e) {
            throw new RuntimeException(e);
        }
    }

    public void getBucketInfo() {
        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            BucketInfo bucketInfo = bucketManager.getBucketInfo(bucket);
            log.info(bucketInfo.toString());
        } catch (QiniuException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Map<String, String>> getFileInfo(List<String> files) {
        Map<String, Map<String, String>> result = new HashMap<>();

        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            //单次批量请求的文件数量不得超过1000
            String[] keyList = files.toArray(new String[0]);
            BucketManager.BatchOperations batchOperations = new BucketManager.BatchOperations();
            batchOperations.addStatOps(bucket, keyList);
            Response response = bucketManager.batch(batchOperations);
            BatchStatus[] batchStatusList = response.jsonToObject(BatchStatus[].class);
            for (int i = 0; i < keyList.length; i++) {
                BatchStatus status = batchStatusList[i];
                String key = keyList[i];
                if (status.code == 200) {
                    //文件存在
                    Map<String, String> info = new HashMap<>();
                    info.put("size", String.valueOf(status.data.fsize));
                    info.put("mimeType", status.data.mimeType);
                    result.put(key, info);
                } else {
                    log.error(key + "：" + status.data.error);
                }
            }
        } catch (QiniuException ex) {
            log.error(ex.response.toString());
        }

        return result;
    }

    public void saveFileInfo() {
        List<Resource> resourceList = resourceService.lambdaQuery().select(Resource::getPath).list();
        List<String> paths = resourceList.stream().map(Resource::getPath).collect(Collectors.toList());

        //构造一个带指定 Region 对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        //文件名前缀
        String prefix = "";
        //每次迭代的长度限制，最大1000，推荐值 1000
        int limit = 1000;
        //指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
        String delimiter = "";
        //列举空间文件列表
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(bucket, prefix, limit, delimiter);

        List<Resource> resources = new ArrayList<>();

        while (fileListIterator.hasNext()) {
            FileInfo[] items = fileListIterator.next();
            for (FileInfo item : items) {
                if (item.fsize != 0L && !paths.contains(downloadUrl + item.key)) {
                    Resource re = new Resource();
                    re.setPath(downloadUrl + item.key);
                    re.setType(CommonConst.PATH_TYPE_ASSETS);
                    re.setSize(Integer.valueOf(Long.toString(item.fsize)));
                    re.setMimeType(item.mimeType);
                    re.setStoreType(StoreEnum.QINIU.getCode());
                    re.setUserId(Long.valueOf(CommonConst.ADMIN_USER_ID));
                    resources.add(re);
                }
            }
        }

        if (!CollectionUtils.isEmpty(resources)) {
            resourceService.saveBatch(resources);
            System.out.println("保存数量：" + resources.size());
        }
        System.out.println("同步完成");
    }

    /**
     * 获取文件大小
     *
     * @param key
     * @return
     */
    private long getFileSize(String key) {
        Map<String, Map<String, String>> info = getFileInfo(Collections.singletonList(key));
        Map<String, String> meta = info.get(key);
        if (meta == null) {
            throw new IllegalArgumentException("文件不存在 key=" + key);
        }
        return Long.parseLong(meta.getOrDefault("size", "0"));
    }


    /**
     * 获取图片列表 带域名url
     *
     * @param imgPrefix
     * @return
     */
    public List<String> list(String imgPrefix) {
        //构造一个带指定Zone对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        //...其他参数参考类注释
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);

        //文件名前缀
        String prefix = "";
        //每次迭代的长度限制，最大1000，推荐值 1000
        int limit = 120;
        //指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
        String delimiter = "";
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(bucket, prefix, limit, delimiter);

        ArrayList<String> randomAvatars = new ArrayList<>();
        while (fileListIterator.hasNext()) {
            //处理获取的file list结果
            FileInfo[] items = fileListIterator.next();
            for (FileInfo item : items) {
                //打印以"qingsi/static/avatar/"开头的文件的名称
                // if (StrUtil.startWithAny(item.key, "qingsi/static/avatar/", "backgroundImg")) {
                // if (StrUtil.startWithAny(item.key, "qingsi/static/avatar/")) {
                if (StrUtil.startWithAny(item.key, imgPrefix) && !item.key.equals(imgPrefix)) { //&& StrUtil.endWith(item.key, ".webp") 过滤为结尾为 .webp
                    // 拼接为 访问地址
                    randomAvatars.add(downloadUrl.concat(item.key));
                    // System.out.println(downloadUrl.concat(item.key));
                    // download(item.key);//下载
                }
            }
        }
        // log.info("randomAvatars:{}", randomAvatars);
        return randomAvatars;
    }

    /**
     * 查询指定前缀下 体积 > 250 kB 且后缀 ≠ .webp 的图片（返回不带域名 URL）
     *
     * @param imgPrefix 目录前缀
     * @return List<String>
     */
    public List<String> listLargeNonWebp(String imgPrefix) {
        long minSize = 250 * 1024;          // 250 kB

        Configuration cfg = new Configuration(Region.region0());
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);

        String prefix = "";
        int limit = 120;
        String delimiter = "";
        BucketManager.FileListIterator it = bucketManager.createFileListIterator(bucket, prefix, limit, delimiter);

        List<String> result = new ArrayList<>();
        while (it.hasNext()) {
            FileInfo[] items = it.next();
            for (FileInfo item : items) {
                String key = item.key;
                // 1. 前缀匹配  2. 排除目录本身  3. 体积闸门  4. 非 webp
                if (StrUtil.startWithAny(key, imgPrefix)
                        && !key.equals(imgPrefix)
                        && item.fsize >= minSize
                        && !StrUtil.endWithIgnoreCase(key, ".webp")) {
                    // result.add(downloadUrl.concat(key));
                    result.add(key);
                }
            }
        }
        return result;
    }

    /**
     * 获取图片列表 体积大于250kb 不带域名url
     *
     * @param imgPrefix
     * @return
     */
    public List<String> listNoDomain(String imgPrefix) {
        //构造一个带指定Zone对象的配置类
        Configuration cfg = new Configuration(Region.region0());
        //...其他参数参考类注释
        Auth auth = Auth.create(accessKey, secretKey);
        BucketManager bucketManager = new BucketManager(auth, cfg);

        //文件名前缀
        String prefix = "";
        //每次迭代的长度限制，最大1000，推荐值 1000
        int limit = 120;
        //指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
        String delimiter = "";
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(bucket, prefix, limit, delimiter);

        /* 3. 只收集 >= 250 kB 且符合前缀的文件 */
        long minSize = 250 * 1024;          // 250 kB
        ArrayList<String> imgKeys = new ArrayList<>();
        while (fileListIterator.hasNext()) {
            //处理获取的file list结果
            FileInfo[] items = fileListIterator.next();
            for (FileInfo item : items) {
                if (StrUtil.startWithAny(item.key, imgPrefix)
                        && !item.key.equals(imgPrefix)
                        && item.fsize >= minSize) {   // 关键：体积闸门
                    imgKeys.add(item.key);
                }
            }
        }
        // log.info("imgKeys:{}", imgKeys);
        return imgKeys;
    }


    /**
     * 提交单个七牛云持久化转码任务（pfop）
     * 支持源文件后缀：.jpg .JPG .jpeg .png → 统一输出 .webp
     *
     * @param srcKey   源空间中的文件名，例如 img/2025/12/a.JPG
     * @param quality  转码质量，推荐 75~85，视觉无损
     * @param pipeline 私有队列名，null 则走默认队列（免费账号并发 10）
     * @return persistentId  用于后续轮询任务状态
     */
    public String submitPfop(String srcKey, int quality) {
        /* ---------- 1. 基本合法性校验 ---------- */
        if (StrUtil.isBlank(srcKey)) {
            throw new IllegalArgumentException("srcKey 不能为空");
        }
        String srcKeyLow = srcKey.toLowerCase();
        if (!srcKeyLow.endsWith(".jpg") &&
                !srcKeyLow.endsWith(".jpeg") &&
                !srcKeyLow.endsWith(".png")) {
            throw new IllegalArgumentException("仅支持 jpg/jpeg/png 三种格式，当前 key=" + srcKey);
        }

        /* ---------- 2. 构造目标文件名（统一 .webp） ---------- */
        // 找到最后一个“.”，把后缀抹掉再拼 .webp，保留原大小写无关
        int lastDot = srcKey.lastIndexOf('.');
        String dstKey = srcKey.substring(0, lastDot) + ".webp";

        /* ---------- 3. 组装 fop 命令串 ---------- */
        // imageMogr2 是七牛图片处理指令，format/webp 指定输出格式
        // quality/xx 控制压缩率；strip 去掉 EXIF；interlace/1 输出渐进式 WebP
        String fops = StrUtil.format("imageMogr2/format/webp/quality/{}/strip/interlace/1", quality);

        // saveas 格式：目标bucket:目标key，需要 UrlSafeBase64
        String saveas = UrlSafeBase64.encodeToString(bucket + ":" + dstKey);
        String fopStr = fops + "|saveas/" + saveas;

        /* ---------- 4. 提交 pfop ---------- */
        Auth auth = Auth.create(accessKey, secretKey);
        Configuration cfg = new Configuration(Region.region0()); //region1()华东  region1() 华北，region2() 华南，regionNa0() 北美，regionAs0() 东南亚
        OperationManager op = new OperationManager(auth, cfg);
        try {
            // 最后一个参数 force 传 true，表示如果目标文件已存在就覆盖
            return op.pfop(bucket, srcKey, fopStr, pipeline, true);
        } catch (QiniuException e) {
            log.error("提交 pfop 失败, srcKey={}, code={}, error={}",
                    srcKey, e.code(), e.response.toString());
            throw new RuntimeException("提交转码任务失败", e);
        }
    }

    /**
     * 批量提交转码任务（并行流，可替换成自定义线程池）
     *
     * @param srcKeys 源文件 key 列表，元素可以是 img/a.JPG、img/b.png ...
     * @param quality 统一转码质量
     * @return Map<源key, persistentId>  方便后续轮询
     */
    public Map<String, String> batchSubmitPfop(List<String> srcKeys,
                                               int quality) {
        if (CollUtil.isEmpty(srcKeys)) {
            return Collections.emptyMap();
        }
        // 并行度默认是 CPU 核数，也可包装成 ExecutorService
        return srcKeys.parallelStream()
                .collect(Collectors.toMap(k -> k,
                        k -> submitPfop(k, quality)));
    }

    /**
     * 3. 轮询单任务状态
     *
     * @param persistentId 提交后拿到的 id
     * @return 0 成功  1 等待  2 进行中  3 失败
     */
    public int queryPfopStatus(String persistentId) {
        Auth auth = Auth.create(accessKey, secretKey);
        Configuration cfg = new Configuration(Region.region0());
        OperationManager op = new OperationManager(auth, cfg);
        try {
            return op.prefop(persistentId).code;
        } catch (QiniuException e) {
            log.error("查询 pfop 状态失败 pid={}, error={}", persistentId, e.response.toString());
            return -1;
        }
    }

    /* ---------- 5. 重载一个“最简”批量接口，默认质量 85，默认队列 ---------- */
    public Map<String, String> batchSubmitPfop(List<String> srcKeys) {
        return batchSubmitPfop(srcKeys, 85);
    }


}


