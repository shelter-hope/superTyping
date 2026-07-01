-- MySQL 8.0.3 数据库建库脚本
-- 数据库名称：typing_word_game
-- 生成时间：2026-01-14
-- 基于数据库正式设计草案版本1和版本1_完善版
-- 修改说明：将所有ENUM类型替换为VARCHAR类型，优化外键关联逻辑

-- 创建数据库
CREATE DATABASE IF NOT EXISTS typing_word_game DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE typing_word_game;

-- 设置SQL模式
SET sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- 1. 基础信息表

-- 1.1 用户信息表 (user_info)
CREATE TABLE IF NOT EXISTS user_info (
    user_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户唯一ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '登录账号',
    password VARCHAR(100) NOT NULL COMMENT '加密密码',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    signature VARCHAR(200) COMMENT '个性签名',
    avatar VARCHAR(255) COMMENT '用户头像URL',
    best_score INT DEFAULT 0 COMMENT '历史最佳积分',
    best_round INT DEFAULT 0 COMMENT '历史最佳轮次',
    best_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '历史最佳速度（字符/分钟）',
    best_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '历史最佳准确率（%）',
    avg_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均打字速度（字符/分钟）',
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均准确率（%）',
    peak_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '峰值准确率（%）',
    game_num INT DEFAULT 0 COMMENT '游戏总场数',
    role VARCHAR(10) DEFAULT 'user' COMMENT '角色（user/admin）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- 创建索引
CREATE INDEX idx_username ON user_info(username);

-- 1.2 玩家每日汇总表 (user_daily_stats)
CREATE TABLE IF NOT EXISTS user_daily_stats (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    stat_date DATE NOT NULL COMMENT '统计日期（yyyy-MM-dd）',
    game_count INT DEFAULT 0 COMMENT '当日游戏场数',
    total_score INT DEFAULT 0 COMMENT '当日总积分',
    avg_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '当日平均速度（字符/分钟）',
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '当日平均准确率（%）',
    peak_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '当日峰值速度',
    peak_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '当日峰值准确率',
    total_chars INT DEFAULT 0 COMMENT '当日总输入字符数',
    correct_chars INT DEFAULT 0 COMMENT '当日正确输入字符数',
    game_mode VARCHAR(20) COMMENT '游戏模式',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (user_id, stat_date) COMMENT '用户+日期唯一，避免重复统计'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家每日汇总表';

-- 创建索引
CREATE INDEX idx_user_date ON user_daily_stats(user_id, stat_date);

-- 1.3 玩家历史快照表 (user_info_snapshot)
CREATE TABLE IF NOT EXISTS user_info_snapshot (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    snapshot_time DATETIME NOT NULL COMMENT '快照时间',
    best_score INT DEFAULT 0 COMMENT '快照时的历史最佳积分',
    best_round INT DEFAULT 0 COMMENT '快照时的历史最佳轮次',
    best_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '快照时的历史最佳速度',
    best_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '快照时的历史最佳准确率',
    avg_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '快照时的平均速度',
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '快照时的平均准确率',
    game_num INT DEFAULT 0 COMMENT '快照时的游戏总场数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家历史快照表';

-- 创建索引
CREATE INDEX idx_user_time ON user_info_snapshot(user_id, snapshot_time);

-- 1.4 字符统计表 (character_stats)
CREATE TABLE IF NOT EXISTS character_stats (
    stat_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    char_value VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '字符（单个字符，支持中英文/符号，区分大小写）',
    char_type VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字符类型',
    total_attempts INT DEFAULT 0 COMMENT '总尝试次数',
    correct_attempts INT DEFAULT 0 COMMENT '正确次数',
    error_count INT DEFAULT 0 COMMENT '总错误次数',
    average_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均准确率（%）',
    recent_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '最近10次准确率（%）',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    UNIQUE KEY uk_user_char (user_id, char_value) COMMENT '用户+字符唯一，避免重复统计'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字符统计表';

-- 创建索引
CREATE UNIQUE INDEX idx_user_char ON character_stats(user_id, char_value);
CREATE INDEX idx_char_type ON character_stats(char_type);

-- 1.5 字符类型配置表 (character_type_configs)
CREATE TABLE IF NOT EXISTS character_type_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '字符类型ID',
    char_type VARCHAR(20) UNIQUE NOT NULL COMMENT '字符类型',
    description VARCHAR(200) COMMENT '类型的描述',
    regex_pattern VARCHAR(255) NOT NULL COMMENT '正则匹配式',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字符类型配置表';

-- 创建索引
CREATE INDEX idx_char_type ON character_type_configs(char_type);

-- 2. 游戏相关表

-- 2.1 游戏记录表 (game_record)
CREATE TABLE IF NOT EXISTS game_record (
    record_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    game_mode VARCHAR(30) NOT NULL COMMENT '游戏模式',
    selected_chars VARCHAR(100) COMMENT '选择的字符类型（逗号分隔，如lower,number,punct）',
    round_num INT DEFAULT 1 COMMENT '游戏轮数',
    total_score INT DEFAULT 0 COMMENT '单轮总积分',
    total_chars INT DEFAULT 0 COMMENT '总输入字符数',
    correct_chars INT DEFAULT 0 COMMENT '正确输入字符数',
    avg_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均打字速度（字符/分钟）',
    peak_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '峰值打字速度（字符/分钟）',
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均准确率（%）',
    peak_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '峰值准确率（%）',
    game_duration INT DEFAULT 0 COMMENT '游戏时长（毫秒）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    time_period TIME COMMENT '时间段（hh:mm:ss）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏记录表';

-- 创建索引
CREATE INDEX idx_user_game ON game_record(user_id, game_mode);
CREATE INDEX idx_create_time ON game_record(create_time);

-- 2.2 游戏模式配置表 (game_mode_configs)
CREATE TABLE IF NOT EXISTS game_mode_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    mode_name VARCHAR(30) UNIQUE NOT NULL COMMENT '游戏模式',
    description VARCHAR(200) COMMENT '模式描述',
    mode_k0 DECIMAL(3,2) NOT NULL COMMENT '模式专属系数（难度增长系数）',
    monster_init_num INT DEFAULT 3 COMMENT '初始化首轮怪物生成数量',
    monster_type_pro VARCHAR(255) NOT NULL COMMENT '各种怪物的类型的占比，以json存储',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id INT COMMENT '创建者的用户ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_id INT COMMENT '更新者的用户ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏模式配置表';

-- 2.3 游戏字符类型配置表 (game_char_type_configs)
CREATE TABLE IF NOT EXISTS game_char_type_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    game_mode_id INT NOT NULL COMMENT '关联游戏模式ID',
    char_type VARCHAR(100) NOT NULL COMMENT '勾选的字符类型，逗号隔开',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏字符类型配置表';

-- 创建索引
CREATE INDEX idx_user_game_mode ON game_char_type_configs(user_id, game_mode_id);

-- 2.4 怪兽数据表 (monster_data)
CREATE TABLE IF NOT EXISTS monster_data (
    monster_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '怪兽ID',
    record_id INT NOT NULL COMMENT '关联游戏记录ID',
    monster_type VARCHAR(20) NOT NULL COMMENT '怪兽类型',
    target_str VARCHAR(50) NOT NULL COMMENT '携带字符串',
    speed DECIMAL(3,2) NOT NULL COMMENT '行进速度（像素/帧）',
    survival_time INT DEFAULT 0 COMMENT '存活时间（毫秒）',
    destroy_cost_time INT COMMENT '消灭耗时（毫秒，未消灭则为NULL）',
    total_count INT DEFAULT 0 COMMENT '输入总词次数',
    input_error_count INT DEFAULT 0 COMMENT '输入错误次数',
    is_true_mean TINYINT(1) DEFAULT 0 COMMENT '是否为有意义字符串（1=是/0=否）',
    score INT DEFAULT 0 COMMENT '消灭后积分',
    spawn_wave INT COMMENT '产生于第几轮',
    is_parent TINYINT(1) DEFAULT 0 COMMENT '是否为下一轮父本',
    difficulty_level TINYINT(1) DEFAULT 1 COMMENT '难度等级（1-5）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='怪兽数据表';

-- 创建索引
CREATE INDEX idx_record_monster ON monster_data(record_id, monster_type);

-- 2.5 系统怪物基础信息配置表 (system_monster_configs)
CREATE TABLE IF NOT EXISTS system_monster_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    init_speed DECIMAL(3,2) NOT NULL COMMENT '初始化行进速度（像素/帧）',
    init_str_length INT NOT NULL COMMENT '初始化字符长度',
    type VARCHAR(20) UNIQUE NOT NULL COMMENT '怪兽类型',
    score INT NOT NULL COMMENT '积分值',
    description VARCHAR(200) COMMENT '效果描述',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统怪物基础信息配置表';

-- 2.6 系统怪物类型基础信息配置表 (system_monster_type_configs)
CREATE TABLE IF NOT EXISTS system_monster_type_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    init_speed DECIMAL(3,2) NOT NULL DEFAULT 1.0 COMMENT '初始化行进速度（像素/帧）',
    init_str_length INT NOT NULL DEFAULT 3 COMMENT '初始化字符长度',
    type VARCHAR(20) UNIQUE NOT NULL COMMENT '怪兽类型',
    score INT NOT NULL COMMENT '积分值',
    description VARCHAR(200) COMMENT '效果描述',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统怪物类型基础信息配置表';

-- 2.7 系统怪物公共基础信息配置表 (system_monster_commo_configs)
CREATE TABLE IF NOT EXISTS system_monster_commo_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    min_speed DECIMAL(3,2) NOT NULL DEFAULT 0.8 COMMENT '最小行进速度',
    max_speed DECIMAL(3,2) NOT NULL DEFAULT 3.0 COMMENT '最大行进速度',
    min_str_length INT NOT NULL DEFAULT 1 COMMENT '最小字符长度',
    max_str_length INT NOT NULL DEFAULT 15 COMMENT '最大字符长度',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统怪物公共基础信息配置表';

-- 2.8 用户怪物基础信息表 (user_monster_configs)
CREATE TABLE IF NOT EXISTS user_monster_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    init_speed DECIMAL(3,2) NOT NULL COMMENT '初始化行进速度（像素/帧）',
    init_str_length INT NOT NULL COMMENT '初始化字符长度',
    type VARCHAR(20) NOT NULL COMMENT '怪兽类型',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_monster (user_id, type) COMMENT '用户+怪兽类型唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户怪物基础信息表';

-- 创建索引
CREATE INDEX idx_user_monster ON user_monster_configs(user_id, type);

-- 2.7 用户游戏模式配置表 (user_game_mode_configs)
CREATE TABLE IF NOT EXISTS user_game_mode_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    mode_name VARCHAR(30) NOT NULL COMMENT '游戏模式',
    monster_init_num INT DEFAULT 3 COMMENT '初始化首轮怪物生成数量',
    monster_type_pro VARCHAR(255) NOT NULL COMMENT '各种怪物的类型的占比，以json存储',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户游戏模式配置表';

-- 创建索引
CREATE INDEX idx_user_game_mode ON user_game_mode_configs(user_id, mode_name);

-- 3. 词库相关表

-- 3.1 用户自定义词库 (custom_dictionaries)
CREATE TABLE IF NOT EXISTS custom_dictionaries (
    dict_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '词库ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    name VARCHAR(100) NOT NULL COMMENT '词库名称',
    description VARCHAR(500) COMMENT '词库描述',
    word_count INT DEFAULT 0 COMMENT '单词数量',
    is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开（1=公开/0=私有）',
    file_url VARCHAR(255) COMMENT '词库文件URL',
    file_name VARCHAR(100) COMMENT '词库文件名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义词库';

-- 创建索引
CREATE INDEX idx_user_dict ON custom_dictionaries(user_id);

-- 3.2 自定义词库单词表 (dictionary_words)
CREATE TABLE IF NOT EXISTS dictionary_words (
    word_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '单词ID',
    dict_id INT NOT NULL COMMENT '关联词库ID',
    word VARCHAR(100) NOT NULL COMMENT '单词',
    total_practice_count INT DEFAULT 0 COMMENT '该单词的总练习次数',
    error_practice_count INT DEFAULT 0 COMMENT '错误次数',
    proficiency_level TINYINT(1) DEFAULT 1 COMMENT '玩家对该单词的熟练度等级（1-5）',
    difficulty_level TINYINT(1) DEFAULT 1 COMMENT '难度等级（1-5）',
    is_focus_word TINYINT(1) DEFAULT 0 COMMENT '是否为重点训练单词（1=是/0=否）',
    user_error_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '该单词的用户错误率（%）',
    user_avg_input_time BIGINT DEFAULT 0 COMMENT '用户平均输入时长（毫秒）',
    user_continuous_correct INT DEFAULT 0 COMMENT '连续正确输入次数（连胜次数）',
    last_practice_time DATETIME COMMENT '最后一次练习时间',
    frequency DECIMAL(5,2) DEFAULT 0.00 COMMENT '使用频率'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义词库单词表';

-- 创建索引
CREATE INDEX idx_dict_word ON dictionary_words(dict_id, word);

-- 3.3 系统词库表 (system_dictionaries)
CREATE TABLE IF NOT EXISTS system_dictionaries (
    dict_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '词库ID',
    name VARCHAR(100) NOT NULL COMMENT '词库名称',
    description VARCHAR(500) COMMENT '词库描述',
    word_count INT DEFAULT 0 COMMENT '单词数量',
    category VARCHAR(50) COMMENT '词库分类',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用（1=启用/0=禁用）',
    file_url VARCHAR(255) COMMENT '词库文件URL',
    file_name VARCHAR(100) COMMENT '词库文件名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统词库表';

-- 创建索引
CREATE INDEX idx_category ON system_dictionaries(category);
CREATE INDEX idx_is_active ON system_dictionaries(is_active);

-- 3.4 系统词库单词表 (system_dictionary_words)
CREATE TABLE IF NOT EXISTS system_dictionary_words (
    word_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '单词ID',
    dict_id INT NOT NULL COMMENT '关联词库ID',
    word VARCHAR(100) NOT NULL COMMENT '单词',
    total_count INT DEFAULT 0 COMMENT '该单词的总练习次数',
    error_count INT DEFAULT 0 COMMENT '错误次数',
    difficulty_level TINYINT(1) DEFAULT 1 COMMENT '难度等级（1-5）',
    error_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '该单词的总用户错误率（%）',
    user_avg_input_time BIGINT DEFAULT 0 COMMENT '用户平均输入时长（毫秒）',
    frequency DECIMAL(5,2) DEFAULT 0.00 COMMENT '使用频率'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统词库单词表';

-- 创建索引
CREATE INDEX idx_system_dict_word ON system_dictionary_words(dict_id, word);

-- 4. 排行榜相关表

-- 4.1 排行榜条目表 (leaderboard_entry)
CREATE TABLE IF NOT EXISTS leaderboard_entry (
    entry_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '条目ID',
    record_id INT NOT NULL COMMENT '游戏记录ID',
    user_id INT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户登录账号',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '玩家头像URL',
    game_mode VARCHAR(30) NOT NULL COMMENT '游戏模式',
    score INT NOT NULL COMMENT '单场游戏总积分',
    rounds INT DEFAULT 1 COMMENT '游戏轮数',
    avg_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均打字速度（字符/分钟）',
    peak_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '峰值打字速度（字符/分钟）',
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均准确率（%）',
    peak_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '峰值准确率（%）',
    game_duration INT DEFAULT 0 COMMENT '游戏时长（毫秒）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '游戏记录创建时间',
    ranking INT DEFAULT 0 COMMENT '排行榜名次'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜条目表';

-- 创建索引
CREATE INDEX idx_game_mode_score ON leaderboard_entry(game_mode, score DESC);
CREATE INDEX idx_created_at ON leaderboard_entry(created_at);
CREATE INDEX idx_user_rank ON leaderboard_entry(user_id, game_mode);

-- 5. 数学模型相关表

-- 5.1 系统数学模型初始化参数表 (model_init_params)
CREATE TABLE IF NOT EXISTS model_init_params (
    param_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    game_mode VARCHAR(30) UNIQUE NOT NULL COMMENT '游戏模式',
    init_difficulty DECIMAL(3,2) DEFAULT 0.30 COMMENT '初始化系数',
    weight_real_accuracy DECIMAL(3,2) DEFAULT 0.35 COMMENT '实时准确率权重WA',
    weight_real_speed DECIMAL(3,2) DEFAULT 0.25 COMMENT '实时速度权重WS',
    weight_char_error_rate DECIMAL(3,2) DEFAULT 0.20 COMMENT '单字符错误率权重WE',
    weight_history_accuracy DECIMAL(3,2) DEFAULT 0.10 COMMENT '历史平均准确率权重WAh',
    weight_history_peak_speed DECIMAL(3,2) DEFAULT 0.05 COMMENT '历史峰值速度权重WSh',
    weight_error_char_ratio DECIMAL(3,2) DEFAULT 0.05 COMMENT '易错字符占比权重WRe',
    weight_current_round DECIMAL(3,2) DEFAULT 0.10 COMMENT '轮次权重WK',
    fatigue_buffer_flag TINYINT(1) DEFAULT 0 COMMENT '疲劳缓冲标记',
    progress_speed_flag TINYINT(1) DEFAULT 0 COMMENT '进步速度标记',
    rollback_flag TINYINT(1) DEFAULT 0 COMMENT '回退机制标记',
    max_adjust_ratio DECIMAL(3,2) DEFAULT 0.30 COMMENT '单次调节幅度上限（30%）',
    smooth_adjust_coeff DECIMAL(3,2) DEFAULT 0.80 COMMENT '平滑调节系数α',
    consecutive_mismatch_round INT DEFAULT 0 COMMENT '连续难度不匹配轮次',
    last_adjust_time DATETIME COMMENT '最后一次参数调整时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统数学模型初始化参数表';

-- 5.2 用户个性化数学模型参数表 (user_model_params)
CREATE TABLE IF NOT EXISTS user_model_params (
    param_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    game_mode VARCHAR(30) NOT NULL COMMENT '游戏模式',
    latest_round INT DEFAULT 1 COMMENT '最近一次更新的轮次',
    last_difficulty DECIMAL(3,2) DEFAULT 0.30 COMMENT '上一轮的系数',
    weight_real_accuracy DECIMAL(3,2) DEFAULT 0.35 COMMENT '实时准确率权重WA',
    weight_real_speed DECIMAL(3,2) DEFAULT 0.25 COMMENT '实时速度权重WS',
    weight_char_error_rate DECIMAL(3,2) DEFAULT 0.20 COMMENT '单字符错误率权重WE',
    weight_history_accuracy DECIMAL(3,2) DEFAULT 0.10 COMMENT '历史平均准确率权重WAh',
    weight_history_peak_speed DECIMAL(3,2) DEFAULT 0.05 COMMENT '历史峰值速度权重WSh',
    weight_error_char_ratio DECIMAL(3,2) DEFAULT 0.05 COMMENT '易错字符占比权重WRe',
    weight_current_round DECIMAL(3,2) DEFAULT 0.10 COMMENT '轮次权重WK',
    fatigue_buffer_flag TINYINT(1) DEFAULT 0 COMMENT '疲劳缓冲标记（0=未触发/1=已触发）',
    progress_speed_flag TINYINT(1) DEFAULT 0 COMMENT '进步速度标记（0=未触发/1=已触发）',
    rollback_flag TINYINT(1) DEFAULT 0 COMMENT '回退机制标记（0=未触发/1=已触发）',
    max_adjust_ratio DECIMAL(3,2) DEFAULT 0.30 COMMENT '单次调节幅度上限（30%）',
    smooth_adjust_coeff DECIMAL(3,2) DEFAULT 0.80 COMMENT '平滑调节系数α',
    consecutive_mismatch_round INT DEFAULT 0 COMMENT '连续难度不匹配轮次',
    last_adjust_time DATETIME COMMENT '最后一次参数调整时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_mode (user_id, game_mode) COMMENT '用户+游戏模式唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户个性化数学模型参数表';

-- 创建索引
CREATE UNIQUE INDEX idx_user_mode ON user_model_params(user_id, game_mode);

-- 5.3 玩家模型适配日志表 (user_model_adapt_logs)
CREATE TABLE IF NOT EXISTS user_model_adapt_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    game_record_id INT NOT NULL COMMENT '关联游戏记录ID',
    round_num INT NOT NULL COMMENT '游戏轮次',
    before_difficulty DECIMAL(3,2) COMMENT '调整前难度系数',
    after_difficulty DECIMAL(3,2) COMMENT '调整后难度系数',
    adjust_params JSON COMMENT '调整的参数及变化',
    trigger_rule VARCHAR(100) COMMENT '调整触发规则',
    log_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '日志生成时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家模型适配日志表';

-- 创建索引
CREATE INDEX idx_user_adapt_log ON user_model_adapt_logs(user_id, log_time);

-- 5.4 模型常量表 (model_constants)
CREATE TABLE IF NOT EXISTS model_constants (
    constant_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '常量ID',
    max_speed DECIMAL(5,2) DEFAULT 150.00 COMMENT '最大参考速度（字符/分钟）',
    ref_peak_speed DECIMAL(5,2) DEFAULT 120.00 COMMENT '参考峰值速度（字符/分钟）',
    initial_difficulty DECIMAL(3,2) DEFAULT 0.30 COMMENT '初始难度系数',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型常量表';

-- 6. 系统参数表

-- 6.1 系统参数表 (system_params)
CREATE TABLE IF NOT EXISTS system_params (
    param_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '参数ID',
    param_name VARCHAR(50) UNIQUE NOT NULL COMMENT '参数名称',
    param_value VARCHAR(255) NOT NULL COMMENT '参数值',
    param_type VARCHAR(20) NOT NULL COMMENT '参数类型',
    description VARCHAR(200) COMMENT '参数描述',
    is_system TINYINT(1) DEFAULT 1 COMMENT '是否系统参数（1=是/0=否）',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用（1=启用/0=禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数表';

-- 创建索引
CREATE INDEX idx_param_name ON system_params(param_name);
CREATE INDEX idx_param_type ON system_params(param_type);

-- 7. 用户应用配置表

-- 7.1 用户应用配置表 (user_app_configs)
CREATE TABLE IF NOT EXISTS user_app_configs (
    config_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    user_id INT UNIQUE NOT NULL COMMENT '关联用户ID',
    is_music TINYINT(1) DEFAULT 1 COMMENT '是否启用音效',
    is_sound_effect TINYINT(1) DEFAULT 1 COMMENT '是否启用音效效果',
    show_game_records TINYINT(1) DEFAULT 1 COMMENT '是否显示游戏记录',
    show_rankings TINYINT(1) DEFAULT 1 COMMENT '是否显示排行榜',
    is_searchable TINYINT(1) DEFAULT 1 COMMENT '是否可被检索',
    dark_mode TINYINT(1) DEFAULT 0 COMMENT '是否启用暗黑模式',
    notifications_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用通知',
    auto_save TINYINT(1) DEFAULT 1 COMMENT '是否自动保存游戏进度',
    language VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言设置',
    font_size INT DEFAULT 16 COMMENT '字体大小',
    theme VARCHAR(20) DEFAULT 'default' COMMENT '主题设置',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_config (user_id) COMMENT '每个用户只能有一条配置记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户应用配置表';

-- 创建索引
CREATE UNIQUE INDEX idx_user_config ON user_app_configs(user_id);

-- 8. 字符串父本表

-- 8.1 字符串父本表 (string_parents)
CREATE TABLE IF NOT EXISTS string_parents (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    str_parents VARCHAR(255) NOT NULL COMMENT '父本记录对应的怪兽ID，逗号分隔',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_parents (user_id) COMMENT '每个用户只有一条父本记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字符串父本表';

-- 创建索引
CREATE UNIQUE INDEX idx_user_parents ON string_parents(user_id);

-- 9. 社交系统表

-- 9.1 好友关系表 (friend_relation)
CREATE TABLE IF NOT EXISTS friend_relation (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '发起好友申请的用户ID',
    friend_user_id INT NOT NULL COMMENT '接收好友申请的用户ID',
    relation_status TINYINT NOT NULL COMMENT '关系状态 0-申请中 1-已成为好友 2-已拉黑 3-已拒绝',
    apply_time DATETIME NOT NULL COMMENT '申请时间',
    agree_time DATETIME COMMENT '同意时间 状态为1时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    UNIQUE KEY uk_user_friend (user_id, friend_user_id) COMMENT '避免重复好友关系'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户好友关系表';

-- 创建索引
CREATE INDEX idx_friend_user ON friend_relation(friend_user_id) COMMENT '被加好友的用户索引';
CREATE INDEX idx_status ON friend_relation(relation_status) COMMENT '按关系状态查询';

-- 9.2 用户对话表 (user_conversation)
CREATE TABLE IF NOT EXISTS user_conversation (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '当前用户ID',
    target_user_id INT NOT NULL COMMENT '对话的好友用户ID',
    last_message_id INT COMMENT '最后一条消息ID',
    last_message_content VARCHAR(500) COMMENT '最后一条消息内容 用于会话列表预览',
    last_message_time DATETIME COMMENT '最后一条消息发送时间',
    unread_count INT NOT NULL DEFAULT 0 COMMENT '未读消息数',
    is_top TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
    is_mute TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否静音 0-否 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话更新时间',
    UNIQUE KEY uk_user_target (user_id, target_user_id) COMMENT '每个用户与好友的会话唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户好友对话会话表';

-- 创建索引
CREATE INDEX idx_last_msg_time ON user_conversation(last_message_time) COMMENT '按最后消息时间排序';

-- 9.3 好友聊天消息表 (friend_chat_message)
CREATE TABLE IF NOT EXISTS friend_chat_message (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id INT NOT NULL COMMENT '关联的会话ID',
    sender_id INT NOT NULL COMMENT '消息发送者ID',
    receiver_id INT NOT NULL COMMENT '消息接收者ID',
    message_type TINYINT NOT NULL COMMENT '消息类型 1-文本 2-表情 3-游戏邀请 4-战绩分享',
    message_content VARCHAR(1000) NOT NULL COMMENT '消息内容 文本/表情编码/邀请参数JSON',
    send_time DATETIME NOT NULL COMMENT '消息发送时间',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态 0-未读 1-已读',
    read_time DATETIME COMMENT '阅读时间 状态为1时必填',
    is_recall TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否撤回 0-否 1-是',
    recall_time DATETIME COMMENT '撤回时间 状态为1时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友聊天消息表';

-- 创建索引
CREATE INDEX idx_conversation ON friend_chat_message(conversation_id) COMMENT '按会话查询消息';
CREATE INDEX idx_sender_receiver ON friend_chat_message(sender_id, receiver_id) COMMENT '按收发双方查询';
CREATE INDEX idx_send_time ON friend_chat_message(send_time) COMMENT '按发送时间排序';

-- 9.4 系统消息表 (system_notification)
CREATE TABLE IF NOT EXISTS system_notification (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    user_id INT NOT NULL COMMENT '接收消息的用户ID 0表示全体用户',
    notification_type TINYINT NOT NULL COMMENT '消息类型 1-好友申请 2-系统公告 3-游戏奖励 4-任务提醒 5-异常通知',
    notification_title VARCHAR(100) NOT NULL COMMENT '消息标题',
    notification_content VARCHAR(500) NOT NULL COMMENT '消息内容',
    business_id INT COMMENT '关联业务ID 如好友申请的relation_id/奖励的record_id',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '阅读状态 0-未读 1-已读',
    is_persistent TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否持久化 0-临时消息（如弹窗） 1-持久消息（如通知列表）',
    expire_time DATETIME COMMENT '消息过期时间 NULL表示永不过期',
    send_time DATETIME NOT NULL COMMENT '消息发送时间',
    read_time DATETIME COMMENT '阅读时间 状态为1时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息通知表';

-- 创建索引
CREATE INDEX idx_user_read ON system_notification(user_id, is_read) COMMENT '按用户+阅读状态查询';
CREATE INDEX idx_send_time ON system_notification(send_time) COMMENT '按发送时间排序';
CREATE INDEX idx_expire_time ON system_notification(expire_time) COMMENT '清理过期消息索引';

-- 10. AI对话系统表

-- 10.1 AI对话会话表 (ai_conversation)
CREATE TABLE IF NOT EXISTS ai_conversation (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    ai_robot_id INT NOT NULL DEFAULT 1 COMMENT 'AI机器人ID 区分不同功能的AI 1-打字陪练 2-问题解答 3-单词学习',
    conversation_title VARCHAR(100) COMMENT '会话标题 自动提取第一条消息或用户自定义',
    last_message_id INT COMMENT '最后一条消息ID',
    last_message_time DATETIME COMMENT '最后一条消息时间',
    is_top TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
    is_mute TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否静音 0-否 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话更新时间',
    UNIQUE KEY uk_user_ai (user_id, ai_robot_id, conversation_title) COMMENT '用户与同AI的会话标题唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与AI的对话会话表';

-- 创建索引
CREATE INDEX idx_ai_robot ON ai_conversation(ai_robot_id) COMMENT '按AI类型查询';

-- 10.2 AI对话消息表 (ai_chat_message)
CREATE TABLE IF NOT EXISTS ai_chat_message (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id INT NOT NULL COMMENT '关联的AI会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    ai_robot_id INT NOT NULL COMMENT 'AI机器人ID',
    message_role TINYINT NOT NULL COMMENT '消息角色 1-用户 2-AI助手',
    message_type TINYINT NOT NULL COMMENT '消息类型 1-文本 2-打字练习指令 3-单词查询 4-错误反馈',
    message_content TEXT NOT NULL COMMENT '消息内容 文本/指令参数JSON',
    send_time DATETIME NOT NULL COMMENT '消息发送时间',
    is_success TINYINT(1) COMMENT 'AI指令执行状态 仅类型2/3有效 0-失败 1-成功 NULL-无指令',
    error_msg VARCHAR(200) COMMENT '指令执行失败原因 is_success为0时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与AI的聊天消息表';

-- 创建索引
CREATE INDEX idx_conversation ON ai_chat_message(conversation_id) COMMENT '按会话查询消息';
CREATE INDEX idx_role_time ON ai_chat_message(message_role, send_time) COMMENT '按角色+时间排序';

-- 7. 添加外键约束
ALTER TABLE user_daily_stats ADD CONSTRAINT fk_user_daily_stats_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_info_snapshot ADD CONSTRAINT fk_user_info_snapshot_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE character_stats ADD CONSTRAINT fk_character_stats_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE game_record ADD CONSTRAINT fk_game_record_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE monster_data ADD CONSTRAINT fk_monster_data_record FOREIGN KEY (record_id) REFERENCES game_record(record_id) ON DELETE CASCADE;
ALTER TABLE custom_dictionaries ADD CONSTRAINT fk_custom_dictionaries_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE dictionary_words ADD CONSTRAINT fk_dictionary_words_dict FOREIGN KEY (dict_id) REFERENCES custom_dictionaries(dict_id) ON DELETE CASCADE;
ALTER TABLE system_dictionary_words ADD CONSTRAINT fk_system_dictionary_words_dict FOREIGN KEY (dict_id) REFERENCES system_dictionaries(dict_id) ON DELETE CASCADE;
ALTER TABLE leaderboard_entry ADD CONSTRAINT fk_leaderboard_entry_record FOREIGN KEY (record_id) REFERENCES game_record(record_id) ON DELETE CASCADE;
ALTER TABLE leaderboard_entry ADD CONSTRAINT fk_leaderboard_entry_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_model_params ADD CONSTRAINT fk_user_model_params_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE game_mode_configs ADD CONSTRAINT fk_game_mode_configs_create FOREIGN KEY (create_id) REFERENCES user_info(user_id) ON DELETE SET NULL;
ALTER TABLE game_mode_configs ADD CONSTRAINT fk_game_mode_configs_update FOREIGN KEY (update_id) REFERENCES user_info(user_id) ON DELETE SET NULL;
ALTER TABLE game_char_type_configs ADD CONSTRAINT fk_game_char_type_configs_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE game_char_type_configs ADD CONSTRAINT fk_game_char_type_configs_mode FOREIGN KEY (game_mode_id) REFERENCES game_mode_configs(id) ON DELETE CASCADE;
ALTER TABLE user_monster_configs ADD CONSTRAINT fk_user_monster_configs_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_app_configs ADD CONSTRAINT fk_user_app_configs_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE string_parents ADD CONSTRAINT fk_string_parents_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
-- 添加新表的外键约束
ALTER TABLE user_game_mode_configs ADD CONSTRAINT fk_user_game_mode_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
-- 移除game_mode外键约束，因为game_mode现在是VARCHAR类型，与game_mode_configs的mode_name字段类型匹配
-- 保留其他外键约束
ALTER TABLE user_model_adapt_logs ADD CONSTRAINT fk_user_adapt_logs_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_model_adapt_logs ADD CONSTRAINT fk_user_adapt_logs_record FOREIGN KEY (game_record_id) REFERENCES game_record(record_id) ON DELETE CASCADE;

-- 添加社交系统表外键约束
ALTER TABLE friend_relation ADD CONSTRAINT fk_friend_relation_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE friend_relation ADD CONSTRAINT fk_friend_relation_friend FOREIGN KEY (friend_user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_conversation ADD CONSTRAINT fk_user_conversation_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_conversation ADD CONSTRAINT fk_user_conversation_target FOREIGN KEY (target_user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE friend_chat_message ADD CONSTRAINT fk_friend_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES user_conversation(id) ON DELETE CASCADE;
ALTER TABLE friend_chat_message ADD CONSTRAINT fk_friend_chat_message_sender FOREIGN KEY (sender_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE friend_chat_message ADD CONSTRAINT fk_friend_chat_message_receiver FOREIGN KEY (receiver_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE system_notification ADD CONSTRAINT fk_system_notification_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;

-- 添加AI对话系统表外键约束
ALTER TABLE ai_conversation ADD CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE ai_chat_message ADD CONSTRAINT fk_ai_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE;
ALTER TABLE ai_chat_message ADD CONSTRAINT fk_ai_chat_message_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;

-- 8. 插入初始数据

-- 8.1 字符类型配置
INSERT INTO character_type_configs (char_type, description, regex_pattern) VALUES
('lower', '小写字母', '^[a-z]+$'),
('upper', '大写字母', '^[A-Z]+$'),
('number', '数字', '^[0-9]+$'),
('punct', '标点符号', '^[\\p{P}]+$'),
('operator', '运算符号', '^[\\+\\-\\*\\/\\=\\<\\>]+$'),
('chinese', '中文字符', '^[\\u4e00-\\u9fa5]+$'),
('special', '特殊字符', '^[^a-zA-Z0-9\\u4e00-\\u9fa5\\+\\-\\*\\/\\=\\<\\>]+$')
ON DUPLICATE KEY UPDATE description = VALUES(description), regex_pattern = VALUES(regex_pattern);

-- 8.2 游戏模式配置
INSERT INTO game_mode_configs (mode_name, description, mode_k0, monster_init_num, monster_type_pro) VALUES
('ENDLESS_ADAPT', '无尽适应', 1.1, 3, '{"NORMAL": 0.5, "ATTACKER": 0.2, "EVASIVE": 0.15, "SMART": 0.15}'),
('WORD_SOURCE', '词库模式', 1.0, 3, '{"NORMAL": 0.5, "ATTACKER": 0.2, "EVASIVE": 0.15, "SMART": 0.15}'),
('LIMIT_TIME', '限时冲刺（趣味模式）', 1.0, 4, '{"NORMAL": 0.5, "ATTACKER": 0.2, "EVASIVE": 0.15, "SMART": 0.15}')
ON DUPLICATE KEY UPDATE description = VALUES(description), mode_k0 = VALUES(mode_k0), monster_init_num = VALUES(monster_init_num), monster_type_pro = VALUES(monster_type_pro);

-- 8.3 系统怪物配置
INSERT INTO system_monster_configs (init_speed, init_str_length, type, score, description) VALUES
(0.5, 5, 'NORMAL', 10, '普通怪物，速度适中，携带普通字符串'),
(0.8, 4, 'ATTACKER', 15, '攻击型怪物，速度较快，携带较短字符串'),
(0.6, 6, 'EVASIVE', 12, '闪避型怪物，速度中等，携带较长字符串'),
(0.4, 7, 'SMART', 20, '智能型怪物，速度较慢，携带有意义的单词')
ON DUPLICATE KEY UPDATE init_speed = VALUES(init_speed), init_str_length = VALUES(init_str_length), score = VALUES(score), description = VALUES(description);

-- 8.4 模型常量
INSERT INTO model_constants (max_speed, ref_peak_speed, initial_difficulty) VALUES
(150.00, 120.00, 0.30)
ON DUPLICATE KEY UPDATE max_speed = VALUES(max_speed), ref_peak_speed = VALUES(ref_peak_speed), initial_difficulty = VALUES(initial_difficulty);

-- 8.4.1 模型初始化参数（按模式，供无 user_model_params 时兜底）
INSERT INTO model_init_params (game_mode, init_difficulty, weight_real_accuracy, weight_real_speed, weight_char_error_rate, weight_history_accuracy, weight_history_peak_speed, weight_error_char_ratio, weight_current_round, smooth_adjust_coeff, max_adjust_ratio) VALUES
('ENDLESS_ADAPT', 0.30, 0.35, 0.25, 0.20, 0.10, 0.05, 0.05, 0.10, 0.80, 0.30),
('WORD_SOURCE', 0.30, 0.35, 0.25, 0.20, 0.10, 0.05, 0.05, 0.10, 0.80, 0.30),
('LIMIT_TIME', 0.30, 0.35, 0.25, 0.20, 0.10, 0.05, 0.05, 0.10, 0.80, 0.30)
ON DUPLICATE KEY UPDATE init_difficulty = VALUES(init_difficulty), weight_real_accuracy = VALUES(weight_real_accuracy), weight_real_speed = VALUES(weight_real_speed), weight_char_error_rate = VALUES(weight_char_error_rate), weight_history_accuracy = VALUES(weight_history_accuracy), weight_history_peak_speed = VALUES(weight_history_peak_speed), weight_error_char_ratio = VALUES(weight_error_char_ratio), weight_current_round = VALUES(weight_current_round), smooth_adjust_coeff = VALUES(smooth_adjust_coeff), max_adjust_ratio = VALUES(max_adjust_ratio);

-- 8.5 系统参数
INSERT INTO system_params (param_name, param_value, param_type, description) VALUES
('game_max_round', '100', 'number', '游戏最大轮次'),
('monster_max_speed', '2.0', 'number', '怪物最大速度'),
('monster_min_speed', '0.1', 'number', '怪物最小速度'),
('word_max_length', '20', 'number', '单词最大长度'),
('word_min_length', '1', 'number', '单词最小长度'),
('game_time_limit', '300000', 'number', '游戏时间限制（毫秒）'),
('score_per_word', '10', 'number', '每个单词基础分数'),
('speed_bonus_factor', '0.1', 'number', '速度奖励系数'),
('accuracy_bonus_factor', '0.2', 'number', '准确率奖励系数'),
('difficulty_adjust_interval', '3', 'number', '难度调整间隔（轮次）'),
('enable_sound', '1', 'boolean', '是否启用音效'),
('enable_visual_effects', '1', 'boolean', '是否启用视觉效果'),
('leaderboard_top_count', '100', 'number', '排行榜显示数量'),
('daily_reset_time', '00:00:00', 'string', '每日数据重置时间'),
('length_std_dev_ratio', '0.25', 'ratio', '字符串长度标准差系数'),
('length_min', '1', 'integer', '字符串最小长度'),
('length_max', '15', 'integer', '字符串最大长度')
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value), description = VALUES(description);

-- 8.6 创建管理员用户（密码：admin123，使用bcrypt加密）
INSERT INTO user_info (username, password, nickname, role) VALUES
('admin', '$2a$10$ErIkTsllHveFnbk9lyrjsuzfvEJX0SpFtIfDp/BEJgCgDxKxsfP/u', '管理员', 'admin')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), role = VALUES(role);

-- 8.7 创建测试用户（密码：test123，使用bcrypt加密）
INSERT INTO user_info (username, password, nickname, role) VALUES
('test', '$2a$10$WhFNEMcnX8WkaxtDFrJICuEXesfhIj8WKAKhf0XU5wnz1r4Hzr9xS', '测试用户', 'user')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), role = VALUES(role);

-- 9. 优化设置
-- 设置innodb_buffer_pool_size为服务器内存的50-80%，这里设置为1GB作为示例
SET GLOBAL innodb_buffer_pool_size = 1073741824;

-- 启用慢查询日志，设置阈值为2秒
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- 优化连接数
SET GLOBAL max_connections = 100;

-- 提交事务
COMMIT;

-- 输出建库完成信息
SELECT '数据库typing_word_game创建完成！' AS message;
SELECT '初始数据插入完成！' AS message;
SELECT '管理员账号：admin，密码：admin123' AS message;
SELECT '测试账号：test，密码：test123' AS message;