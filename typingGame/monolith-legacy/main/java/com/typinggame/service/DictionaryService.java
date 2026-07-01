package com.typinggame.service;

import com.typinggame.dto.dictionary.DictionaryCreateDTO;
import com.typinggame.entity.dictionaries.CustomDictionary;
import com.typinggame.entity.dictionaries.SystemDictionary;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 处理词库相关业务逻辑
 */
public interface DictionaryService {
    /**
     * 获取系统词库列表
     * @return 列表
     */
    List<SystemDictionary> getSystemDictionaries();

    /**
     * 获取用户自定义词库列表
     * @param userId 用户ID
     * @return 列表
     */
    List<CustomDictionary> getUserDictionaries(Integer userId);

    /**
     * 创建自定义词库
     * @param userId 用户ID
     * @param dto 创建信息
     * @param file 词库文件
     * @return 创建的词库
     */
    CustomDictionary createCustomDictionary(Integer userId, DictionaryCreateDTO dto, MultipartFile file);

    /**
     * 删除自定义词库
     * @param userId 用户ID
     * @param dictId 词库ID
     * @return 是否成功
     */
    boolean deleteCustomDictionary(Integer userId, Integer dictId);
}
