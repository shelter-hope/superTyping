-- =============================================================================
-- dict-svc / typing_game_dict 初始 schema（Flyway V1）
-- 来源：DB/typing_word_game.sql（2026-10-30 合并版）按实体归属切分，共 4 张表
-- 跨 schema 外键已降级为逻辑引用（服务间通过 /api/v1/internal/** 校验），
-- 同 schema 外键保留在本文件末尾。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS custom_dictionaries;
CREATE TABLE custom_dictionaries (
    dict_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '词库ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    name VARCHAR(100) NOT NULL COMMENT '词库名称',
    description VARCHAR(500) NULL COMMENT '词库描述',
    word_count INT DEFAULT 0 COMMENT '单词数量',
    is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开（1=公开/0=私有）',
    file_url VARCHAR(255) NULL COMMENT '词库文件URL',
    file_name VARCHAR(100) NULL COMMENT '词库文件名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_custom_dictionaries_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义词库';

DROP TABLE IF EXISTS dictionary_words;
CREATE TABLE dictionary_words (
    word_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '单词ID',
    dict_id INT NOT NULL COMMENT '关联词库ID',
    word VARCHAR(100) NOT NULL COMMENT '单词',
    total_practice_count INT DEFAULT 0 COMMENT '该单词的总练习次数',
    error_practice_count INT DEFAULT 0 COMMENT '错误练习次数',
    proficiency_level INT DEFAULT 1 COMMENT '玩家对该单词的熟练度等级（1-5）',
    difficulty_level INT DEFAULT 1 COMMENT '难度等级（1-5）',
    is_focus_word TINYINT(1) DEFAULT 0 COMMENT '是否为重点训练单词（1=是/0=否）',
    user_error_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '该单词的用户错误率（%）',
    user_avg_input_time BIGINT DEFAULT 0 COMMENT '用户平均输入时长（毫秒）',
    user_continuous_correct INT DEFAULT 0 COMMENT '连续正确输入次数（连胜次数）',
    last_practice_time DATETIME NULL COMMENT '最后一次练习时间',
    frequency DECIMAL(5,2) DEFAULT 0.00 COMMENT '使用频率',
    KEY idx_dictionary_words_dict_word (dict_id, word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义词库单词表';

DROP TABLE IF EXISTS system_dictionaries;
CREATE TABLE system_dictionaries (
    dict_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '词库ID',
    name VARCHAR(100) NOT NULL COMMENT '词库名称',
    description VARCHAR(500) NULL COMMENT '词库描述',
    word_count INT DEFAULT 0 COMMENT '单词数量',
    category VARCHAR(50) NULL COMMENT '词库分类',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用（1=启用/0=禁用）',
    file_url VARCHAR(255) NULL COMMENT '词库文件URL',
    file_name VARCHAR(100) NULL COMMENT '词库文件名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_system_dictionaries_name (name),
    KEY idx_system_dictionaries_category (category),
    KEY idx_system_dictionaries_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统词库表';

DROP TABLE IF EXISTS system_dictionary_words;
CREATE TABLE system_dictionary_words (
    word_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '单词ID',
    dict_id INT NOT NULL COMMENT '关联词库ID',
    word VARCHAR(100) NOT NULL COMMENT '单词',
    total_count INT DEFAULT 0 COMMENT '该单词的总练习次数',
    error_count INT DEFAULT 0 COMMENT '错误次数',
    difficulty_level INT DEFAULT 1 COMMENT '难度等级（1-5）',
    error_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '该单词的总用户错误率（%）',
    user_avg_input_time BIGINT DEFAULT 0 COMMENT '用户平均输入时长（毫秒）',
    frequency DECIMAL(5,2) DEFAULT 0.00 COMMENT '使用频率',
    UNIQUE KEY uk_system_dictionary_words_dict_word (dict_id, word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统词库单词表';

SET FOREIGN_KEY_CHECKS = 1;

-- 同 schema 外键
ALTER TABLE dictionary_words ADD CONSTRAINT fk_dictionary_words_dict FOREIGN KEY (dict_id) REFERENCES custom_dictionaries(dict_id) ON DELETE CASCADE;
ALTER TABLE system_dictionary_words ADD CONSTRAINT fk_system_dictionary_words_dict FOREIGN KEY (dict_id) REFERENCES system_dictionaries(dict_id) ON DELETE CASCADE;

