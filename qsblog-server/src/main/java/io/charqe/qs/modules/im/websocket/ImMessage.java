package io.charqe.qs.modules.im.websocket;

import lombok.Data;

@Data
public class ImMessage {

    private Integer messageType;

    private String content;

    private Long fromId;

    private Long toId;

    private Long groupId;

    private String avatar;

    private String username;
}


