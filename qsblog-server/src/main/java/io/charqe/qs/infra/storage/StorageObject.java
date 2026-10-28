package io.charqe.qs.infra.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对象存储中的裸对象描述：孤儿扫描只用 key/size/mimeType 判断登记状态。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageObject {

    /** bucket 内裸 key（不含 CDN 域名） */
    private String key;

    /** 对象大小（字节） */
    private Long size;

    /** 对象 MIME，七牛对未知类型可能返回 null */
    private String mimeType;

    /** 上传时间（七牛 putTime，100ns 时间戳，仅作展示） */
    private Long putTime;
}
