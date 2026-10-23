package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

/**
 * 文件响应VO
 *
 * @author Joy
 * @since 2026-01-21 19:01
 */
@Data
public class FileRespVO extends FileBaseVO {

    private Long id;

    /** 存储文件名，由服务端根据存储路径派生。 */
    private String storedName;

    /** 文件分类，由服务端根据 MIME 类型派生。 */
    private String fileCategory;

    /** 小写扩展名，由服务端根据存储路径派生。 */
    private String fileExt;
}
