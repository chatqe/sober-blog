package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

/**
 * 文件删除入参（JSON 体）。
 * <p>
 * {@code force=true} 为永久删除：跳过回收站，同时清理存储对象；
 * 文件仍被业务引用时返回 409，需先解绑。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileDeleteReqVO {

    private Long id;

    /** 是否永久删除，默认 false（仅移入回收站） */
    private Boolean force;
}
