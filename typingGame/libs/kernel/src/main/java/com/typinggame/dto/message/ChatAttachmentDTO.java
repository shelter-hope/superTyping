package com.typinggame.dto.message;

import lombok.Data;

@Data
public class ChatAttachmentDTO {
    private Integer id;
    private String originalName;
    private String mediaType;
    private Long sizeBytes;
    private Integer kind;
}
