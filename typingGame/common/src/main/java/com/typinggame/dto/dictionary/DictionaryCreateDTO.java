package com.typinggame.dto.dictionary;

import lombok.Data;

@Data
public class DictionaryCreateDTO {
    private String name;
    private String description;
    private Boolean isPublic;
    // File is handled via MultipartFile in controller
}
