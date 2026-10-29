-- =============================================================================
-- 打字游戏数据库建库脚本（整库重建）
-- 数据库：typing_word_game
-- 适用版本：MySQL 8.0.19+（8.0.30 实测；种子数据使用行别名 upsert 语法）
-- 字符集：utf8mb4 / utf8mb4_unicode_ci
--
-- 整库重建脚本，由实体定义 + 历史增量脚本合并生成（2026-10-30）
--
-- 说明：
--   1. 本文件是数据库 schema 的唯一来源。表结构以 JPA 实体定义为准，
--      历史增量脚本 001~014 的全部变更已并入本文件，不再需要单独执行。
--   2. 本脚本执行时会先 DROP 再 CREATE，原有数据会全部丢失，仅保留文末种子数据。
--   3. 44 张表的列名、类型、长度、默认值、中文注释与索引以实体注解为准；实体未显式
--      声明长度/精度的列沿用历史脚本（如 VARCHAR(50)、DECIMAL(5,2)），避免按 JPA
--      隐式默认值放大成 VARCHAR(255)、DECIMAL(19,2)。历史脚本中的外键约束（含实体
--      @ForeignKey 命名）集中在本文件“附录 A”中创建，共 42 条。
--   4. 建表顺序已按父表在前排列；外键用 ALTER TABLE 统一追加，避免 DROP/CREATE 顺序耦合。
--
-- 表分组（对应微服务）：
--   用户域    user-svc / libs-kernel : user_info、好友关系、每日统计、字符统计
--   对地域    game-core-svc          : 会话、轮次、怪物、对局记录、规则配置
--   配置域    admin-svc              : 后台配置模板、系统参数
--   模型域    game-core-svc          : 难度/遗传模型初始参数与适配日志
--   消息域    chat-svc               : 好友消息、会话、附件、系统通知
--   词库域    dict-svc               : 自定义/系统词库与词条
--   排行榜域  game-core-svc          : 排行榜条目
--   AI 域     ai-svc                 : AI 会话与消息
--   实验室域  lab-svc                : 群体智能算法、参数预设、实验快照
--   后台域    admin-svc              : 后台操作日志
-- =============================================================================

CREATE DATABASE IF NOT EXISTS typing_word_game DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE typing_word_game;

SET NAMES utf8mb4;

-- 设置SQL模式（与基线脚本保持一致，严格模式便于及早发现字段不匹配）
SET sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- 建表期间关闭外键检查，附录 A 统一追加 42 条外键后再开启
SET FOREIGN_KEY_CHECKS = 0;

-- 兼容清理：旧 baseline 曾用于排行榜去重的临时备份表，不属于当前 44 张表
DROP TABLE IF EXISTS leaderboard_entry_duplicate_backup_20261007;

-- =============================================================================
-- 一、用户域（user-svc / libs-kernel）
-- =============================================================================

-- 1.1 用户信息表
DROP TABLE IF EXISTS user_info;
CREATE TABLE user_info (
    user_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户唯一ID',
    username VARCHAR(50) NOT NULL COMMENT '登录账号，唯一',
    password VARCHAR(100) NOT NULL COMMENT '加密密码（bcrypt）',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    signature VARCHAR(200) NULL COMMENT '个性签名',
    avatar VARCHAR(255) NULL COMMENT '用户头像URL',
    best_score INT DEFAULT 0 COMMENT '历史最佳积分',
    best_round INT DEFAULT 0 COMMENT '历史最佳轮次',
    best_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '历史最佳速度（字符/分钟）',
    best_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '历史最佳准确率（%）',
    avg_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均打字速度（字符/分钟）',
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均准确率（%）',
    peak_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '峰值准确率（%）',
    game_num INT DEFAULT 0 COMMENT '游戏总场数',
    role VARCHAR(10) DEFAULT 'user' COMMENT '角色（user/admin）',
    status INT NOT NULL DEFAULT 1 COMMENT '账号状态 1-正常 0-禁用',
    last_login_time DATETIME NULL COMMENT '最近登录时间',
    enabled TINYINT(1) NULL DEFAULT 1 COMMENT '是否允许登录 1-允许 0-禁用，NULL 视为允许',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_info_username (username),
    KEY idx_user_info_last_login (last_login_time),
    KEY idx_user_info_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- 1.2 玩家每日汇总表
DROP TABLE IF EXISTS user_daily_stats;
CREATE TABLE user_daily_stats (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    stat_date DATETIME NOT NULL COMMENT '统计日期（取当天 00:00:00）',
    game_count INT DEFAULT 0 COMMENT '当日游戏场数',
    total_score INT DEFAULT 0 COMMENT '当日总积分',
    avg_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '当日平均速度（字符/分钟）',
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '当日平均准确率（%）',
    peak_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '当日峰值速度',
    peak_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '当日峰值准确率',
    total_chars INT DEFAULT 0 COMMENT '当日总输入字符数',
    correct_chars INT DEFAULT 0 COMMENT '当日正确输入字符数',
    game_mode VARCHAR(20) NULL COMMENT '游戏模式',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_daily_stats_user_date (user_id, stat_date),
    KEY idx_user_daily_stats_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家每日汇总表';

-- 1.3 玩家历史快照表
DROP TABLE IF EXISTS user_info_snapshot;
CREATE TABLE user_info_snapshot (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    snapshot_time DATETIME NOT NULL COMMENT '快照时间',
    best_score INT DEFAULT 0 COMMENT '快照时的历史最佳积分',
    best_round INT DEFAULT 0 COMMENT '快照时的历史最佳轮次',
    best_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '快照时的历史最佳速度',
    best_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '快照时的历史最佳准确率',
    avg_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '快照时的平均速度',
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '快照时的平均准确率',
    game_num INT DEFAULT 0 COMMENT '快照时的游戏总场数',
    KEY idx_user_info_snapshot_user_time (user_id, snapshot_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家历史快照表';

-- 1.4 字符统计表
DROP TABLE IF EXISTS character_stats;
CREATE TABLE character_stats (
    stat_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    char_value VARCHAR(10) COLLATE utf8mb4_bin NOT NULL COMMENT '字符（单个字符，支持中英文/符号，区分大小写）',
    char_type VARCHAR(20) NOT NULL COMMENT '字符类型（lower/upper/number/punct/chinese/special）',
    total_attempts INT DEFAULT 0 COMMENT '总尝试次数',
    correct_attempts INT DEFAULT 0 COMMENT '正确次数',
    error_count INT DEFAULT 0 COMMENT '总错误次数',
    average_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均准确率（%）',
    recent_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '最近10次准确率（%）',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    UNIQUE KEY uk_character_stats_user_char (user_id, char_value),
    KEY idx_character_stats_char_type (char_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字符统计表';

-- 1.5 字符类型配置表
DROP TABLE IF EXISTS character_type_configs;
CREATE TABLE character_type_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '字符类型ID',
    char_type VARCHAR(20) NOT NULL COMMENT '字符类型标识（lower/upper/number/punct/operator/chinese/special）',
    description VARCHAR(200) NULL COMMENT '类型的描述',
    regex_pattern VARCHAR(255) NOT NULL COMMENT '正则匹配式',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_character_type_configs_char_type (char_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字符类型配置表';

-- 1.6 按键混淆档案表（记录「该打 A 却打成 B」，供遗传算法定向变异）
DROP TABLE IF EXISTS key_confusion;
CREATE TABLE key_confusion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id INT NOT NULL COMMENT '关联用户ID',
    expected_char VARCHAR(10) COLLATE utf8mb4_bin NOT NULL COMMENT '本该输入的字符（区分大小写）',
    typed_char VARCHAR(10) COLLATE utf8mb4_bin NOT NULL COMMENT '实际打出来的字符（区分大小写）',
    confusion_count INT NOT NULL DEFAULT 0 COMMENT '累计混淆次数',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    UNIQUE KEY uk_key_confusion_user_pair (user_id, expected_char, typed_char),
    KEY idx_key_confusion_user_count (user_id, confusion_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='按键混淆档案';

-- 1.7 好友关系表
DROP TABLE IF EXISTS friend_relation;
CREATE TABLE friend_relation (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '发起好友申请的用户ID',
    friend_user_id INT NOT NULL COMMENT '接收好友申请的用户ID',
    relation_status INT NOT NULL COMMENT '关系状态 0-申请中 1-已成为好友 2-已拉黑 3-已拒绝',
    apply_message VARCHAR(200) NULL COMMENT '申请附言',
    blocked_by INT NULL COMMENT '拉黑方用户ID，仅 relation_status=2 时有值',
    apply_time DATETIME NOT NULL COMMENT '申请时间',
    agree_time DATETIME NULL COMMENT '同意时间，状态为1时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    UNIQUE KEY uk_friend_relation_user_friend (user_id, friend_user_id),
    KEY idx_friend_relation_friend_user (friend_user_id),
    KEY idx_friend_relation_status (relation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户好友关系表';

-- 1.8 用户应用配置表
DROP TABLE IF EXISTS user_app_configs;
CREATE TABLE user_app_configs (
    config_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    user_id INT NOT NULL COMMENT '关联用户ID，一个用户仅一行',
    is_music TINYINT(1) DEFAULT 1 COMMENT '是否启用音乐',
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
    UNIQUE KEY uk_user_app_configs_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户应用配置表';

-- 1.9 用户会话表（好友私聊会话列表）
DROP TABLE IF EXISTS user_conversation;
CREATE TABLE user_conversation (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '当前用户ID',
    target_user_id INT NOT NULL COMMENT '对话的好友用户ID',
    last_message_id INT NULL COMMENT '最后一条消息ID',
    last_message_content VARCHAR(500) NULL COMMENT '最后一条消息内容，用于会话列表预览',
    last_message_time DATETIME NULL COMMENT '最后一条消息发送时间',
    unread_count INT NOT NULL DEFAULT 0 COMMENT '未读消息数',
    is_top TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
    is_mute TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否静音 0-否 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话更新时间',
    UNIQUE KEY uk_user_conversation_user_target (user_id, target_user_id),
    KEY idx_user_conversation_last_msg_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户好友对话会话表';

-- 1.10 字符串父本表
DROP TABLE IF EXISTS string_parents;
CREATE TABLE string_parents (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id INT NOT NULL COMMENT '关联用户ID，每个用户仅一条',
    str_parents VARCHAR(255) NOT NULL COMMENT '父本记录对应的怪兽ID，逗号分隔',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_string_parents_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字符串父本表';


-- =============================================================================
-- 二、对地域（game-core-svc）
-- =============================================================================

-- 2.1 游戏会话表
DROP TABLE IF EXISTS game_session;
CREATE TABLE game_session (
    session_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '游戏会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    game_mode VARCHAR(30) NOT NULL COMMENT '游戏模式（ENDLESS_ADAPT/WORD_SOURCE/LIMIT_TIME）',
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '会话状态 RUNNING/COMPLETED/ABORTED',
    current_round INT NOT NULL DEFAULT 1 COMMENT '当前轮次',
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time DATETIME NULL COMMENT '结束时间',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_game_session_user_time (user_id, created_at),
    KEY idx_game_session_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏会话表';

-- 2.2 游戏轮次与 RoundPlan 表
DROP TABLE IF EXISTS game_round;
CREATE TABLE game_round (
    round_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '游戏轮次ID',
    session_id BIGINT NOT NULL COMMENT '游戏会话ID',
    round_num INT NOT NULL COMMENT '轮次编号',
    plan_version BIGINT NOT NULL COMMENT 'RoundPlan版本',
    formula_version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '难度公式版本',
    difficulty_before DECIMAL(6,4) NULL COMMENT '轮前难度，0~1',
    difficulty_after DECIMAL(6,4) NULL COMMENT '轮后难度，0~1',
    target_accuracy DECIMAL(6,4) NOT NULL DEFAULT 0.8500 COMMENT '目标准确率，0~1',
    actual_accuracy DECIMAL(6,4) NULL COMMENT '实际准确率，0~1',
    avg_speed DECIMAL(8,2) NULL COMMENT '平均速度，字符/分钟',
    monster_speed DECIMAL(8,3) NOT NULL COMMENT '怪物速度，像素/秒',
    monster_count INT NOT NULL COMMENT '主怪物数量',
    string_length INT NOT NULL COMMENT '目标字符串长度',
    normal_ratio INT NOT NULL DEFAULT 80 COMMENT '普通怪物比例',
    attacker_ratio INT NOT NULL DEFAULT 5 COMMENT '攻击怪物比例',
    evasive_ratio INT NOT NULL DEFAULT 10 COMMENT '回避怪物比例',
    smart_ratio INT NOT NULL DEFAULT 5 COMMENT '智慧怪物比例',
    plan_payload LONGTEXT NULL COMMENT '本轮怪物计划JSON快照',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '轮次状态 CREATED/RUNNING/COMPLETED/ABORTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_at DATETIME NULL COMMENT '完成时间',
    UNIQUE KEY uk_game_round_session_round_plan (session_id, round_num, plan_version),
    KEY idx_game_round_session_status (session_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏轮次和RoundPlan表';

-- 2.3 轮次怪物尝试结果表
DROP TABLE IF EXISTS monster_attempt;
CREATE TABLE monster_attempt (
    attempt_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '怪物尝试ID',
    session_id BIGINT NOT NULL COMMENT '游戏会话ID',
    round_id BIGINT NOT NULL COMMENT '游戏轮次ID',
    plan_version BIGINT NOT NULL COMMENT 'RoundPlan版本',
    monster_key VARCHAR(255) NULL COMMENT '客户端怪物标识',
    target_str VARCHAR(200) NOT NULL COMMENT '目标字符串',
    monster_type VARCHAR(20) NOT NULL COMMENT '怪物类型（NORMAL/ATTACKER/EVASIVE/SMART）',
    speed DECIMAL(8,3) NULL COMMENT '怪物速度，像素/秒',
    input_count INT NOT NULL DEFAULT 0 COMMENT '输入总次数',
    correct_count INT NOT NULL DEFAULT 0 COMMENT '正确输入次数',
    error_count INT NOT NULL DEFAULT 0 COMMENT '错误输入次数',
    result VARCHAR(20) NOT NULL COMMENT 'KILLED/MISSED/EVADED/COLLISION/TIMEOUT/ABANDONED',
    client_score INT NULL COMMENT '客户端上报分数，仅用于审计',
    verified_score INT NOT NULL DEFAULT 0 COMMENT '服务端核验分数',
    fitness DECIMAL(10,4) NULL COMMENT '父本适应度',
    generation INT NOT NULL DEFAULT 0 COMMENT '遗传代数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    eliminated_at DATETIME NULL COMMENT '结束时间',
    UNIQUE KEY uk_monster_attempt_round_key (round_id, monster_key),
    KEY idx_monster_attempt_session (session_id, created_at),
    KEY idx_monster_attempt_round_result (round_id, result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮次怪物尝试结果';

-- 2.4 怪物遗传父子关系表
DROP TABLE IF EXISTS monster_parent_relation;
CREATE TABLE monster_parent_relation (
    relation_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '父子关系ID',
    child_attempt_id BIGINT NOT NULL COMMENT '子代怪物尝试ID',
    parent_a_attempt_id BIGINT NULL COMMENT '父本A尝试ID',
    parent_b_attempt_id BIGINT NULL COMMENT '父本B尝试ID',
    crossover_point INT NULL COMMENT '交叉位置',
    mutation_rate DECIMAL(6,4) NULL COMMENT '变异率',
    mutation_positions VARCHAR(255) NULL COMMENT '变异位置列表',
    mutation_chars VARCHAR(255) NULL COMMENT '变异字符列表',
    generation INT NOT NULL DEFAULT 0 COMMENT '遗传代数',
    algorithm_version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '遗传算法版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_monster_parent_relation_child (child_attempt_id),
    KEY idx_monster_parent_relation_parent_a (parent_a_attempt_id),
    KEY idx_monster_parent_relation_parent_b (parent_b_attempt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='怪物遗传父子关系';

-- 2.5 游戏记录表
DROP TABLE IF EXISTS game_record;
CREATE TABLE game_record (
    record_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    game_mode VARCHAR(30) NOT NULL COMMENT '游戏模式',
    selected_chars VARCHAR(100) NULL COMMENT '选择的字符类型（逗号分隔，如lower,number,punct）',
    round_num INT DEFAULT 1 COMMENT '游戏轮数',
    total_score INT DEFAULT 0 COMMENT '单轮总积分',
    total_chars INT DEFAULT 0 COMMENT '总输入字符数',
    correct_chars INT DEFAULT 0 COMMENT '正确输入字符数',
    avg_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均打字速度（字符/分钟）',
    peak_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '峰值打字速度（字符/分钟）',
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均准确率（%）',
    peak_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '峰值准确率（%）',
    game_duration INT DEFAULT 0 COMMENT '游戏时长（毫秒）',
    session_id BIGINT NULL COMMENT '关联游戏会话ID',
    last_round_id BIGINT NULL COMMENT '最后完成轮次ID',
    accuracy_ratio DECIMAL(6,4) NULL COMMENT '准确率，0~1（新口径）',
    game_duration_ms BIGINT NULL COMMENT '游戏时长，毫秒（新口径）',
    data_version INT NOT NULL DEFAULT 1 COMMENT '数据口径版本',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    KEY idx_game_record_user_mode (user_id, game_mode),
    KEY idx_game_record_create_time (create_time),
    KEY idx_game_record_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏记录表';

-- 2.6 怪兽数据表（旧口径单局明细，保留供历史查询）
DROP TABLE IF EXISTS monster_data;
CREATE TABLE monster_data (
    monster_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '怪兽ID',
    record_id INT NOT NULL COMMENT '关联游戏记录ID',
    round_id BIGINT NULL COMMENT '关联游戏轮次ID',
    monster_type VARCHAR(20) NOT NULL COMMENT '怪兽类型（NORMAL/ATTACKER/EVASIVE/SMART）',
    target_str VARCHAR(50) NOT NULL COMMENT '携带字符串',
    speed DECIMAL(3,2) NOT NULL COMMENT '行进速度（像素/帧）',
    survival_time INT DEFAULT 0 COMMENT '存活时间（毫秒）',
    destroy_cost_time INT NULL COMMENT '消灭耗时（毫秒，未消灭则为NULL）',
    total_count INT DEFAULT 0 COMMENT '输入总词次数',
    input_error_count INT DEFAULT 0 COMMENT '输入错误次数',
    input_count INT NULL COMMENT '输入总次数（新口径）',
    correct_count INT NULL COMMENT '正确输入次数（新口径）',
    error_count INT NULL COMMENT '错误输入次数（新口径）',
    result VARCHAR(20) NULL COMMENT 'KILLED/MISSED/EVADED/COLLISION/TIMEOUT',
    is_true_mean INT DEFAULT 0 COMMENT '是否为有意义字符串（1=是/0=否）',
    score INT DEFAULT 0 COMMENT '消灭后积分',
    difficulty_level INT DEFAULT 1 COMMENT '难度等级（1-5）',
    spawn_wave INT NULL COMMENT '产生于第几轮',
    is_parent TINYINT(1) DEFAULT 0 COMMENT '是否为下一轮父本（1=是/0=否）',
    generation INT NULL COMMENT '遗传代数',
    plan_version BIGINT NULL COMMENT 'RoundPlan版本',
    KEY idx_monster_data_record_type (record_id, monster_type),
    KEY idx_monster_data_round (round_id, monster_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='怪兽数据表';

-- 2.7 游戏规则配置表（difficulty / genetic 等规则的多版本 payload）
DROP TABLE IF EXISTS game_rule_config;
CREATE TABLE game_rule_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_key VARCHAR(64) NOT NULL COMMENT '规则键（difficulty/genetic）',
    version INT NOT NULL COMMENT '规则版本号',
    payload JSON NOT NULL COMMENT '规则参数 JSON',
    status VARCHAR(16) NOT NULL COMMENT '状态 draft/active/archived',
    update_id INT NULL COMMENT '最后修改人用户ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
    UNIQUE KEY uk_game_rule_config_key_version (rule_key, version),
    KEY idx_game_rule_config_key_status (rule_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏规则配置表';

-- 2.8 游戏模式配置表
DROP TABLE IF EXISTS game_mode_configs;
CREATE TABLE game_mode_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    mode_name VARCHAR(30) NOT NULL COMMENT '游戏模式',
    description VARCHAR(200) NULL COMMENT '模式描述',
    mode_k0 DECIMAL(3,2) NOT NULL COMMENT '模式专属系数（难度增长系数）',
    monster_init_num INT DEFAULT 3 COMMENT '初始化首轮怪物生成数量',
    monster_type_pro VARCHAR(255) NOT NULL COMMENT '各种怪物的类型的占比，以json存储',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id INT NULL COMMENT '创建者的用户ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_id INT NULL COMMENT '更新者的用户ID',
    UNIQUE KEY uk_game_mode_configs_mode_name (mode_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏模式配置表';

-- 2.9 游戏字符类型配置表（每个用户在每种模式下勾选的字符类型）
DROP TABLE IF EXISTS game_char_type_configs;
CREATE TABLE game_char_type_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    game_mode_id INT NOT NULL COMMENT '关联游戏模式ID（game_mode_configs.id）',
    char_type VARCHAR(100) NOT NULL COMMENT '勾选的字符类型，逗号隔开',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_game_char_type_configs_user_mode (user_id, game_mode_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏字符类型配置表';

-- 2.10 系统怪物基础信息配置表
DROP TABLE IF EXISTS system_monster_configs;
CREATE TABLE system_monster_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    init_speed DOUBLE NOT NULL COMMENT '初始化行进速度（像素/帧）',
    init_str_length INT NOT NULL COMMENT '初始化字符长度',
    type VARCHAR(20) NOT NULL COMMENT '怪兽类型',
    score INT NOT NULL COMMENT '积分值',
    description VARCHAR(200) NULL COMMENT '效果描述',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_system_monster_configs_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统怪物基础信息配置表';

-- 2.11 系统怪物类型基础信息配置表（后台可维护版本）
DROP TABLE IF EXISTS system_monster_type_configs;
CREATE TABLE system_monster_type_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    init_speed DECIMAL(3,2) NOT NULL DEFAULT 1.0 COMMENT '初始化行进速度（像素/帧）',
    init_str_length INT NOT NULL DEFAULT 3 COMMENT '初始化字符长度',
    type VARCHAR(20) NOT NULL COMMENT '怪兽类型',
    score INT NOT NULL COMMENT '积分值',
    description VARCHAR(200) NULL COMMENT '效果描述',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_system_monster_type_configs_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统怪物类型基础信息配置表';

-- 2.12 系统怪物公共基础信息配置表（速度/长度上下界）
DROP TABLE IF EXISTS system_monster_commo_configs;
CREATE TABLE system_monster_commo_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    min_speed DECIMAL(3,2) NOT NULL DEFAULT 0.80 COMMENT '最小行进速度',
    max_speed DECIMAL(3,2) NOT NULL DEFAULT 3.00 COMMENT '最大行进速度',
    min_str_length INT NOT NULL DEFAULT 1 COMMENT '最小字符长度',
    max_str_length INT NOT NULL DEFAULT 15 COMMENT '最大字符长度',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统怪物公共基础信息配置表';

-- 2.13 用户怪物基础信息表
DROP TABLE IF EXISTS user_monster_configs;
CREATE TABLE user_monster_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    init_speed DECIMAL(3,2) NOT NULL COMMENT '初始化行进速度（像素/帧）',
    init_str_length INT NOT NULL COMMENT '初始化字符长度',
    type VARCHAR(20) NOT NULL COMMENT '怪兽类型',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_monster_configs_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户怪物基础信息表';

-- 2.14 用户游戏模式配置表
DROP TABLE IF EXISTS user_game_mode_configs;
CREATE TABLE user_game_mode_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    mode_name VARCHAR(30) NOT NULL COMMENT '游戏模式',
    monster_init_num INT NOT NULL DEFAULT 3 COMMENT '初始化首轮怪物生成数量',
    monster_type_pro VARCHAR(255) NOT NULL COMMENT '各种怪物的类型的占比，以json存储',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_game_mode_configs_user_mode (user_id, mode_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户游戏模式配置表';

-- =============================================================================
-- 三、排行榜域（game-core-svc）
-- =============================================================================

-- 3.1 排行榜条目表（每用户每模式仅一条，保留最高分）
DROP TABLE IF EXISTS leaderboard_entry;
CREATE TABLE leaderboard_entry (
    entry_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '条目ID',
    record_id INT NOT NULL COMMENT '游戏记录ID',
    user_id INT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户登录账号',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar VARCHAR(255) NULL COMMENT '玩家头像URL',
    game_mode VARCHAR(30) NOT NULL COMMENT '游戏模式',
    score INT NOT NULL COMMENT '单场游戏总积分',
    rounds INT DEFAULT 1 COMMENT '游戏轮数',
    avg_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均打字速度（字符/分钟）',
    peak_speed DECIMAL(5,2) DEFAULT 0.00 COMMENT '峰值打字速度（字符/分钟）',
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均准确率（%）',
    peak_accuracy DECIMAL(5,2) DEFAULT 0.00 COMMENT '峰值准确率（%）',
    game_duration INT DEFAULT 0 COMMENT '游戏时长（毫秒）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '游戏记录创建时间',
    ranking INT DEFAULT 0 COMMENT '排行榜名次',
    UNIQUE KEY uk_leaderboard_entry_user_mode (user_id, game_mode),
    KEY idx_leaderboard_entry_mode_score (game_mode, score DESC),
    KEY idx_leaderboard_entry_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜条目表';


-- =============================================================================
-- 四、配置域（admin-svc / game-core-svc）
-- =============================================================================

-- 4.1 后台配置模板表
DROP TABLE IF EXISTS config_template;
CREATE TABLE config_template (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    config_json TEXT NOT NULL COMMENT '配置快照 JSON',
    created_by INT NULL COMMENT '创建人用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_template_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台配置模板';

-- 4.2 系统参数表（后台可编辑的键值配置，param_type=json 时存 JSON 字符串）
DROP TABLE IF EXISTS system_params;
CREATE TABLE system_params (
    param_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '参数ID',
    param_name VARCHAR(50) NOT NULL COMMENT '参数名称',
    param_value VARCHAR(2000) NOT NULL COMMENT '参数值（json 类型为 JSON 字符串）',
    param_type VARCHAR(20) NOT NULL COMMENT '参数类型：number/string/boolean/ratio/integer/json',
    description VARCHAR(200) NULL COMMENT '参数描述',
    is_system TINYINT(1) DEFAULT 1 COMMENT '是否系统参数（1=是/0=否）',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用（1=启用/0=禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_system_params_param_name (param_name),
    KEY idx_system_params_param_type (param_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数表';

-- =============================================================================
-- 五、模型域（game-core-svc / libs-kernel）
-- =============================================================================

-- 5.1 系统数学模型初始化参数表（按游戏模式）
DROP TABLE IF EXISTS model_init_params;
CREATE TABLE model_init_params (
    param_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    game_mode VARCHAR(30) NOT NULL COMMENT '游戏模式',
    init_difficulty DECIMAL(3,2) DEFAULT 0.30 COMMENT '初始化系数',
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
    last_adjust_time DATETIME NULL COMMENT '最后一次参数调整时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_model_init_params_game_mode (game_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统数学模型初始化参数表';

-- 5.2 用户个性化数学模型参数表
DROP TABLE IF EXISTS user_model_params;
CREATE TABLE user_model_params (
    param_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    game_mode VARCHAR(30) NOT NULL COMMENT '游戏模式',
    latest_round INT DEFAULT 1 COMMENT '最近一次更新的轮次',
    last_difficulty DECIMAL(3,2) DEFAULT 0.30 COMMENT '上一轮的难度系数',
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
    crossover_rate DECIMAL(5,4) NULL COMMENT '遗传交叉概率',
    mutation_rate DECIMAL(5,4) NULL COMMENT '遗传变异概率',
    speed_adjust_coeff DECIMAL(5,4) NULL COMMENT '速度调节系数β',
    length_adjust_coeff DECIMAL(5,4) NULL COMMENT '长度调节系数γ',
    consecutive_mismatch_round INT DEFAULT 0 COMMENT '连续难度不匹配轮次',
    last_adjust_time DATETIME NULL COMMENT '最后一次参数调整时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_model_params_user_mode (user_id, game_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户个性化数学模型参数表';

-- 5.3 玩家模型适配日志表
DROP TABLE IF EXISTS user_model_adapt_logs;
CREATE TABLE user_model_adapt_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id INT NOT NULL COMMENT '关联用户ID',
    game_record_id INT NULL COMMENT '关联游戏记录ID（轮次日志可为 null）',
    game_mode VARCHAR(32) NULL COMMENT '游戏模式',
    session_id BIGINT NULL COMMENT '游戏会话ID',
    round_num INT NOT NULL COMMENT '游戏轮次',
    before_difficulty DECIMAL(6,4) NULL COMMENT '调整前难度系数',
    after_difficulty DECIMAL(6,4) NULL COMMENT '调整后难度系数',
    adjust_params JSON NULL COMMENT '调整的参数及变化',
    trigger_rule VARCHAR(100) NULL COMMENT '调整触发规则',
    log_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '日志生成时间',
    KEY idx_user_model_adapt_logs_user_time (user_id, log_time),
    KEY idx_user_model_adapt_logs_record (game_record_id),
    KEY idx_user_model_adapt_logs_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家模型适配日志表';

-- 5.4 模型常量表（全局单行）
DROP TABLE IF EXISTS model_constants;
CREATE TABLE model_constants (
    constant_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '常量ID',
    max_speed DECIMAL(5,2) DEFAULT 150.00 COMMENT '最大参考速度（字符/分钟）',
    ref_peak_speed DECIMAL(5,2) DEFAULT 120.00 COMMENT '参考峰值速度（字符/分钟）',
    initial_difficulty DECIMAL(3,2) DEFAULT 0.30 COMMENT '初始难度系数',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型常量表';

-- =============================================================================
-- 六、消息域（chat-svc）
-- =============================================================================

-- 6.1 好友聊天消息表
DROP TABLE IF EXISTS friend_chat_message;
CREATE TABLE friend_chat_message (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id INT NOT NULL COMMENT '关联的会话ID（user_conversation.id）',
    sender_id INT NOT NULL COMMENT '消息发送者ID',
    receiver_id INT NOT NULL COMMENT '消息接收者ID',
    message_type INT NOT NULL COMMENT '消息类型 1-文本 2-表情 3-游戏邀请 4-战绩分享 5-图片 6-文件 9-系统私信',
    message_content VARCHAR(1000) NOT NULL COMMENT '消息内容 文本/表情编码/邀请参数JSON',
    client_msg_id VARCHAR(64) NULL COMMENT '客户端幂等键',
    server_seq INT NULL COMMENT '服务端序号',
    send_time DATETIME NOT NULL COMMENT '消息发送时间',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态 0-未读 1-已读',
    read_time DATETIME NULL COMMENT '阅读时间，状态为1时必填',
    is_recall TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否撤回 0-否 1-是',
    recall_time DATETIME NULL COMMENT '撤回时间，状态为1时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    KEY idx_friend_chat_message_conversation_time (conversation_id, send_time),
    KEY idx_friend_chat_message_sender_client (sender_id, client_msg_id),
    KEY idx_friend_chat_message_sender_receiver (sender_id, receiver_id),
    KEY idx_friend_chat_message_receiver_read (receiver_id, read_status),
    KEY idx_friend_chat_message_send_time (send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友聊天消息表';

-- 6.2 聊天附件表（文件不进入公开 /uploads）
DROP TABLE IF EXISTS chat_attachment;
CREATE TABLE chat_attachment (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID',
    owner_id INT NOT NULL COMMENT '上传者用户ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(80) NOT NULL COMMENT '磁盘文件名',
    media_type VARCHAR(100) NOT NULL COMMENT '媒体类型',
    size_bytes BIGINT NOT NULL COMMENT '字节数',
    kind INT NOT NULL COMMENT '5-图片 6-文件，与消息类型一致',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    KEY idx_chat_attachment_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天附件';

-- 6.3 系统消息通知表
DROP TABLE IF EXISTS system_notification;
CREATE TABLE system_notification (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    user_id INT NOT NULL COMMENT '接收消息的用户ID 0表示全体用户',
    notification_type INT NOT NULL COMMENT '消息类型 1-好友申请 2-系统公告 3-游戏奖励 4-任务提醒 5-异常通知',
    notification_title VARCHAR(100) NOT NULL COMMENT '消息标题',
    notification_content VARCHAR(500) NOT NULL COMMENT '消息内容',
    business_id INT NULL COMMENT '关联业务ID 如好友申请的relation_id/奖励的record_id',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '阅读状态 0-未读 1-已读',
    is_persistent TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否持久化 0-临时消息（如弹窗） 1-持久消息（如通知列表）',
    expire_time DATETIME NULL COMMENT '消息过期时间 NULL表示永不过期',
    send_time DATETIME NOT NULL COMMENT '消息发送时间',
    read_time DATETIME NULL COMMENT '阅读时间，状态为1时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    KEY idx_system_notification_user_read (user_id, is_read),
    KEY idx_system_notification_send_time (send_time),
    KEY idx_system_notification_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息通知表';

-- =============================================================================
-- 七、词库域（dict-svc）
-- =============================================================================

-- 7.1 用户自定义词库
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

-- 7.2 自定义词库单词表
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

-- 7.3 系统词库表
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

-- 7.4 系统词库单词表
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

-- =============================================================================
-- 八、AI 域（ai-svc）
-- =============================================================================

-- 8.1 AI 对话会话表
DROP TABLE IF EXISTS ai_conversation;
CREATE TABLE ai_conversation (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    ai_robot_id INT NOT NULL COMMENT 'AI机器人ID 区分不同功能的AI 1-打字陪练 2-问题解答 3-单词学习',
    conversation_title VARCHAR(100) NULL COMMENT '会话标题 自动提取第一条消息或用户自定义',
    last_message_id INT NULL COMMENT '最后一条消息ID',
    last_message_time DATETIME NULL COMMENT '最后一条消息时间',
    is_top TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
    is_mute TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否静音 0-否 1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话更新时间',
    UNIQUE KEY uk_ai_conversation_user_robot_title (user_id, ai_robot_id, conversation_title),
    KEY idx_ai_conversation_robot (ai_robot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与AI的对话会话表';

-- 8.2 AI 对话消息表
DROP TABLE IF EXISTS ai_chat_message;
CREATE TABLE ai_chat_message (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id INT NOT NULL COMMENT '关联的AI会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    ai_robot_id INT NOT NULL COMMENT 'AI机器人ID',
    message_role INT NOT NULL COMMENT '消息角色 1-用户 2-AI助手',
    message_type INT NOT NULL COMMENT '消息类型 1-文本 2-打字练习指令 3-单词查询 4-错误反馈',
    message_content TEXT NOT NULL COMMENT '消息内容 文本/指令参数JSON',
    send_time DATETIME NOT NULL COMMENT '消息发送时间',
    is_success TINYINT(1) NULL COMMENT 'AI指令执行状态 仅类型2/3有效 0-失败 1-成功 NULL-无指令',
    error_msg VARCHAR(200) NULL COMMENT '指令执行失败原因 is_success为0时必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    KEY idx_ai_chat_message_conversation (conversation_id),
    KEY idx_ai_chat_message_role_time (message_role, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与AI的聊天消息表';

-- =============================================================================
-- 九、实验室域（lab-svc）
-- =============================================================================

-- 9.1 实验室算法注册表
DROP TABLE IF EXISTS lab_algorithm;
CREATE TABLE lab_algorithm (
    id VARCHAR(64) PRIMARY KEY COMMENT '算法ID',
    name VARCHAR(100) NOT NULL COMMENT '算法名称',
    description VARCHAR(500) NULL COMMENT '算法描述',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    default_params JSON NOT NULL COMMENT '默认参数 JSON',
    schema_version INT NOT NULL DEFAULT 1 COMMENT '参数 schema 版本'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验室算法注册表';

-- 9.2 实验室参数预设表
DROP TABLE IF EXISTS lab_preset;
CREATE TABLE lab_preset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预设ID',
    user_id INT NULL COMMENT '所属用户ID，NULL 为公共预设',
    algorithm_id VARCHAR(64) NOT NULL COMMENT '关联算法ID',
    name VARCHAR(100) NOT NULL COMMENT '预设名称',
    params JSON NOT NULL COMMENT '参数 JSON',
    is_public TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开 1-公开 0-私有',
    KEY idx_lab_preset_algorithm (algorithm_id),
    KEY idx_lab_preset_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验室参数预设表';

-- 9.3 实验室实验快照表（客户端入库，服务端不重跑）
DROP TABLE IF EXISTS lab_run;
CREATE TABLE lab_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实验ID',
    user_id INT NOT NULL COMMENT '发起用户ID',
    algorithm_id VARCHAR(64) NOT NULL COMMENT '关联算法ID',
    plugin_id VARCHAR(64) NOT NULL COMMENT '采集插件ID',
    schema_version INT NOT NULL DEFAULT 1 COMMENT '参数 schema 版本',
    engine_kind VARCHAR(32) NOT NULL DEFAULT 'client_snapshot' COMMENT '采集方式',
    title VARCHAR(120) NULL COMMENT '实验标题',
    notes VARCHAR(1000) NULL COMMENT '实验备注',
    seed INT NOT NULL COMMENT '随机种子',
    params_json JSON NOT NULL COMMENT '本次实验参数 JSON',
    preset_id BIGINT NULL COMMENT '来源预设ID',
    status VARCHAR(16) NOT NULL DEFAULT 'completed' COMMENT '实验状态',
    started_at DATETIME NULL COMMENT '开始时间',
    ended_at DATETIME NULL COMMENT '结束时间',
    duration_ms INT NULL COMMENT '耗时（毫秒）',
    summary_json JSON NULL COMMENT '汇总结果 JSON',
    metrics_json JSON NULL COMMENT '指标序列 JSON',
    client_meta_json JSON NULL COMMENT '客户端环境 JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_lab_run_user_created (user_id, created_at DESC),
    KEY idx_lab_run_user_algo (user_id, algorithm_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验室实验快照表';

-- =============================================================================
-- 十、后台域（admin-svc）
-- =============================================================================

-- 10.1 后台操作日志表
DROP TABLE IF EXISTS admin_operation_log;
CREATE TABLE admin_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operator_id INT NULL COMMENT '操作人用户ID',
    method VARCHAR(10) NOT NULL COMMENT 'HTTP 方法',
    path VARCHAR(255) NOT NULL COMMENT '接口路径',
    query_string VARCHAR(500) NULL COMMENT '查询参数',
    request_body TEXT NULL COMMENT '请求体（截断至 2000 字符）',
    response_status INT NULL COMMENT '响应状态码',
    success TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否成功 1-是 0-否',
    ip VARCHAR(64) NULL COMMENT '客户端 IP',
    duration_ms INT NULL COMMENT '耗时（毫秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_admin_operation_log_operator (operator_id, create_time),
    KEY idx_admin_operation_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台操作日志';

-- =============================================================================
-- 附录 A：外键约束（42 条）
--   来源：旧 baseline 的 33 条外键 + 001/002/006/010/014 迁移脚本中的 9 条外键；
--   命名与实体 @ForeignKey(name=...) 保持一致。建表阶段已关闭外键检查，这里统一
--   在全部表创建完成后追加，因此不受建表先后顺序影响。
-- =============================================================================

-- A.1 用户域
ALTER TABLE user_daily_stats ADD CONSTRAINT fk_user_daily_stats_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_info_snapshot ADD CONSTRAINT fk_user_info_snapshot_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE character_stats ADD CONSTRAINT fk_character_stats_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE custom_dictionaries ADD CONSTRAINT fk_custom_dictionaries_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_model_params ADD CONSTRAINT fk_user_model_params_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_monster_configs ADD CONSTRAINT fk_user_monster_configs_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_app_configs ADD CONSTRAINT fk_user_app_configs_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE string_parents ADD CONSTRAINT fk_string_parents_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_game_mode_configs ADD CONSTRAINT fk_user_game_mode_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE game_char_type_configs ADD CONSTRAINT fk_game_char_type_configs_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE friend_relation ADD CONSTRAINT fk_friend_relation_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE friend_relation ADD CONSTRAINT fk_friend_relation_friend FOREIGN KEY (friend_user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_conversation ADD CONSTRAINT fk_user_conversation_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_conversation ADD CONSTRAINT fk_user_conversation_target FOREIGN KEY (target_user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE chat_attachment ADD CONSTRAINT fk_chat_attachment_owner FOREIGN KEY (owner_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE system_notification ADD CONSTRAINT fk_system_notification_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;

-- A.2 对地域
ALTER TABLE game_session ADD CONSTRAINT fk_game_session_user FOREIGN KEY (user_id) REFERENCES user_info(user_id);
ALTER TABLE game_round ADD CONSTRAINT fk_game_round_session FOREIGN KEY (session_id) REFERENCES game_session(session_id);
ALTER TABLE game_record ADD CONSTRAINT fk_game_record_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE monster_data ADD CONSTRAINT fk_monster_data_record FOREIGN KEY (record_id) REFERENCES game_record(record_id) ON DELETE CASCADE;
ALTER TABLE monster_attempt ADD CONSTRAINT fk_monster_attempt_session FOREIGN KEY (session_id) REFERENCES game_session(session_id);
ALTER TABLE monster_attempt ADD CONSTRAINT fk_monster_attempt_round FOREIGN KEY (round_id) REFERENCES game_round(round_id);
ALTER TABLE monster_parent_relation ADD CONSTRAINT fk_parent_relation_child FOREIGN KEY (child_attempt_id) REFERENCES monster_attempt(attempt_id);
ALTER TABLE monster_parent_relation ADD CONSTRAINT fk_parent_relation_a FOREIGN KEY (parent_a_attempt_id) REFERENCES monster_attempt(attempt_id);
ALTER TABLE monster_parent_relation ADD CONSTRAINT fk_parent_relation_b FOREIGN KEY (parent_b_attempt_id) REFERENCES monster_attempt(attempt_id);
ALTER TABLE leaderboard_entry ADD CONSTRAINT fk_leaderboard_entry_record FOREIGN KEY (record_id) REFERENCES game_record(record_id) ON DELETE CASCADE;
ALTER TABLE leaderboard_entry ADD CONSTRAINT fk_leaderboard_entry_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE game_char_type_configs ADD CONSTRAINT fk_game_char_type_configs_mode FOREIGN KEY (game_mode_id) REFERENCES game_mode_configs(id) ON DELETE CASCADE;
ALTER TABLE game_mode_configs ADD CONSTRAINT fk_game_mode_configs_create FOREIGN KEY (create_id) REFERENCES user_info(user_id) ON DELETE SET NULL;
ALTER TABLE game_mode_configs ADD CONSTRAINT fk_game_mode_configs_update FOREIGN KEY (update_id) REFERENCES user_info(user_id) ON DELETE SET NULL;

-- A.3 模型 / 配置域
ALTER TABLE user_model_adapt_logs ADD CONSTRAINT fk_user_adapt_logs_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_model_adapt_logs ADD CONSTRAINT fk_user_adapt_logs_record FOREIGN KEY (game_record_id) REFERENCES game_record(record_id) ON DELETE CASCADE;

-- A.4 消息域
ALTER TABLE friend_chat_message ADD CONSTRAINT fk_friend_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES user_conversation(id) ON DELETE CASCADE;
ALTER TABLE friend_chat_message ADD CONSTRAINT fk_friend_chat_message_sender FOREIGN KEY (sender_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE friend_chat_message ADD CONSTRAINT fk_friend_chat_message_receiver FOREIGN KEY (receiver_id) REFERENCES user_info(user_id) ON DELETE CASCADE;

-- A.5 词库域
ALTER TABLE dictionary_words ADD CONSTRAINT fk_dictionary_words_dict FOREIGN KEY (dict_id) REFERENCES custom_dictionaries(dict_id) ON DELETE CASCADE;
ALTER TABLE system_dictionary_words ADD CONSTRAINT fk_system_dictionary_words_dict FOREIGN KEY (dict_id) REFERENCES system_dictionaries(dict_id) ON DELETE CASCADE;

-- A.6 AI 域
ALTER TABLE ai_conversation ADD CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE ai_chat_message ADD CONSTRAINT fk_ai_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE;
ALTER TABLE ai_chat_message ADD CONSTRAINT fk_ai_chat_message_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;

-- A.7 实验室域
ALTER TABLE lab_preset ADD CONSTRAINT fk_lab_preset_algorithm FOREIGN KEY (algorithm_id) REFERENCES lab_algorithm(id) ON DELETE CASCADE;
ALTER TABLE lab_run ADD CONSTRAINT fk_lab_run_algorithm FOREIGN KEY (algorithm_id) REFERENCES lab_algorithm(id) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;


-- =============================================================================
-- 基础种子数据
--   只保留代码首次启动就必须存在的配置：游戏模式、怪物、字符类型、模型初值、
--   规则 payload、实验室算法、系统词库，以及后台管理台的三个规则参数。
--   全部使用幂等写法（行别名 upsert / NOT EXISTS），重复执行不会产生重复行。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 11.1 字符类型配置
-- -----------------------------------------------------------------------------
INSERT INTO character_type_configs (char_type, description, regex_pattern) VALUES
('lower', '小写字母', '^[a-z]+$'),
('upper', '大写字母', '^[A-Z]+$'),
('number', '数字', '^[0-9]+$'),
('punct', '标点符号', '^[\\p{P}]+$'),
('operator', '运算符号', '^[\\+\\-\\*\\/\\=\\<\\>]+$'),
('chinese', '中文字符', '^[\\u4e00-\\u9fa5]+$'),
('special', '特殊字符', '^[^a-zA-Z0-9\\u4e00-\\u9fa5\\+\\-\\*\\/\\=\\<\\>]+$')
AS new ON DUPLICATE KEY UPDATE description = new.description, regex_pattern = new.regex_pattern;

-- -----------------------------------------------------------------------------
-- 11.2 游戏模式配置（GameMode 枚举：ENDLESS_ADAPT / WORD_SOURCE / LIMIT_TIME）
-- -----------------------------------------------------------------------------
INSERT INTO game_mode_configs (mode_name, description, mode_k0, monster_init_num, monster_type_pro) VALUES
('ENDLESS_ADAPT', '无尽适应', 1.1, 3, '{"NORMAL": 0.5, "ATTACKER": 0.2, "EVASIVE": 0.15, "SMART": 0.15}'),
('WORD_SOURCE', '词源界定', 1.0, 3, '{"NORMAL": 0.5, "ATTACKER": 0.2, "EVASIVE": 0.15, "SMART": 0.15}'),
('LIMIT_TIME', '限时挑战（趣味模式）', 1.0, 4, '{"NORMAL": 0.5, "ATTACKER": 0.2, "EVASIVE": 0.15, "SMART": 0.15}')
AS new ON DUPLICATE KEY UPDATE description = new.description, mode_k0 = new.mode_k0,
    monster_init_num = new.monster_init_num, monster_type_pro = new.monster_type_pro;

-- -----------------------------------------------------------------------------
-- 11.3 系统怪物基础配置（用户初始化时按此表复制到 user_monster_configs）
-- -----------------------------------------------------------------------------
INSERT INTO system_monster_configs (init_speed, init_str_length, type, score, description) VALUES
(0.5, 5, 'NORMAL', 10, '普通怪物，速度适中，携带普通字符串'),
(0.8, 4, 'ATTACKER', 15, '攻击型怪物，速度较快，携带较短字符串'),
(0.6, 6, 'EVASIVE', 12, '闪避型怪物，速度中等，携带较长字符串'),
(0.4, 7, 'SMART', 20, '智能型怪物，速度较慢，携带有意义的单词')
AS new ON DUPLICATE KEY UPDATE init_speed = new.init_speed, init_str_length = new.init_str_length,
    score = new.score, description = new.description;

-- -----------------------------------------------------------------------------
-- 11.4 系统怪物类型配置（后台可维护版本，与 11.3 保持同样的四类）
-- -----------------------------------------------------------------------------
INSERT INTO system_monster_type_configs (init_speed, init_str_length, type, score, description, is_active) VALUES
(0.50, 5, 'NORMAL', 10, '普通怪物，速度适中，携带普通字符串', 1),
(0.80, 4, 'ATTACKER', 15, '攻击型怪物，速度较快，携带较短字符串', 1),
(0.60, 6, 'EVASIVE', 12, '闪避型怪物，速度中等，携带较长字符串', 1),
(0.40, 7, 'SMART', 20, '智能型怪物，速度较慢，携带有意义的单词', 1)
AS new ON DUPLICATE KEY UPDATE init_speed = new.init_speed, init_str_length = new.init_str_length,
    score = new.score, description = new.description, is_active = new.is_active;

-- -----------------------------------------------------------------------------
-- 11.5 系统怪物公共边界（速度/长度上下界，全局单行）
-- -----------------------------------------------------------------------------
INSERT INTO system_monster_commo_configs (min_speed, max_speed, min_str_length, max_str_length)
SELECT 0.80, 3.00, 1, 15
WHERE NOT EXISTS (SELECT 1 FROM system_monster_commo_configs);

-- -----------------------------------------------------------------------------
-- 11.6 模型常量（全局单行）
-- -----------------------------------------------------------------------------
INSERT INTO model_constants (max_speed, ref_peak_speed, initial_difficulty)
SELECT 150.00, 120.00, 0.30
WHERE NOT EXISTS (SELECT 1 FROM model_constants);

-- -----------------------------------------------------------------------------
-- 11.7 分模式的模型初始化参数（用户注册时复制到 user_model_params）
-- -----------------------------------------------------------------------------
INSERT INTO model_init_params (game_mode, init_difficulty, weight_real_accuracy, weight_real_speed,
    weight_char_error_rate, weight_history_accuracy, weight_history_peak_speed, weight_error_char_ratio,
    weight_current_round, smooth_adjust_coeff, max_adjust_ratio) VALUES
('ENDLESS_ADAPT', 0.30, 0.35, 0.25, 0.20, 0.10, 0.05, 0.05, 0.10, 0.80, 0.30),
('WORD_SOURCE',   0.30, 0.35, 0.25, 0.20, 0.10, 0.05, 0.05, 0.10, 0.80, 0.30),
('LIMIT_TIME',    0.30, 0.35, 0.25, 0.20, 0.10, 0.05, 0.05, 0.10, 0.80, 0.30)
AS new ON DUPLICATE KEY UPDATE init_difficulty = new.init_difficulty,
    weight_real_accuracy = new.weight_real_accuracy, weight_real_speed = new.weight_real_speed,
    weight_char_error_rate = new.weight_char_error_rate, weight_history_accuracy = new.weight_history_accuracy,
    weight_history_peak_speed = new.weight_history_peak_speed, weight_error_char_ratio = new.weight_error_char_ratio,
    weight_current_round = new.weight_current_round, smooth_adjust_coeff = new.smooth_adjust_coeff,
    max_adjust_ratio = new.max_adjust_ratio;

-- -----------------------------------------------------------------------------
-- 11.8 游戏规则配置
--   difficulty：v2 logistic 对数几率调节器（基线 005 + 006 全量参数 + 009 + 011 合并后的当前 payload）
--   genetic   ：v2 精英保留 + 锦标赛选择 + 双点交叉（基线 005 + 012 合并后的当前 payload）
--   两行都是 status='active' 的当前生效版本，字段与 DifficultyRule / GeneticRule 实体一一对应。
-- -----------------------------------------------------------------------------
INSERT INTO game_rule_config (rule_key, version, payload, status, update_id) VALUES
('difficulty', 1, CAST('{
  "weightAccuracy": 1.0,
  "weightNormalizedSpeed": 0.0,
  "weightHistorySpeed": 0.0,
  "weightComposure": 1.0,
  "weightRound": 0.0,
  "targetAccuracy": 0.85,
  "gain": 0.35,
  "step": 0.05,
  "logitSlope": 6.0,
  "deadband": 0.03,
  "lambdaUp": 0.25,
  "lambdaDown": 0.5,
  "speedErrorScale": 2.0,
  "weakExposureGain": 1.0,
  "minDifficulty": 0.15,
  "maxDifficulty": 0.95,
  "initialDifficulty": 0.30,
  "maxReferenceSpeed": 150,
  "refPeakSpeed": 120,
  "weakCharDivisor": 10,
  "speedSpan": 1.0,
  "countBase": 3,
  "countRoundFactor": 6,
  "countDifficultyFactor": 16,
  "countMin": 3,
  "countMax": 25,
  "normalBase": 80,
  "normalSlope": 0.45,
  "normalMin": 35,
  "normalMax": 80,
  "attackBase": 5,
  "attackSlope": 0.15,
  "attackMin": 5,
  "attackMax": 20,
  "evasiveBase": 8,
  "evasiveSlope": 0.12,
  "evasiveMin": 5,
  "evasiveMax": 25,
  "roundLogBase": 32,
  "highAccuracyAdvanceThreshold": 85,
  "lowAccuracyRollbackThreshold": 50
}' AS JSON), 'active', 0),
('genetic', 1, CAST('{
  "inRoundParentLimit": 10,
  "crossRoundParentLimit": 8,
  "inRoundMutationRate": 0.15,
  "crossRoundMutationRate": 0.25,
  "crossover": "TWO_POINT",
  "preferWeakChars": true,
  "eliteRatio": 0.1,
  "tournamentSize": 3,
  "fitnessScoreWeight": 1,
  "fitnessAccuracyWeight": 100,
  "fitnessErrorPenalty": 2,
  "charPool": []
}' AS JSON), 'active', 0)
AS new ON DUPLICATE KEY UPDATE payload = new.payload, status = new.status;

-- -----------------------------------------------------------------------------
-- 11.9 后台管理台规则参数（RuleAdminService.RULE_PARAMS，缺失时取 RULE_DEFAULTS）
-- -----------------------------------------------------------------------------
INSERT INTO system_params (param_name, param_value, param_type, description, is_system, is_active) VALUES
('admin.character_rules', '{"charTypes":["lower","upper","number","punct"]}', 'json', '后台配置：字符类型规则', 1, 1),
('admin.movement_rules', '{"minSpeed":0.8,"maxSpeed":3.0}', 'json', '后台配置：怪物速度上下界', 1, 1),
('admin.difficulty_params', '{"minDifficulty":0.0,"maxDifficulty":1.0}', 'json', '后台配置：难度上下界', 1, 1)
AS new ON DUPLICATE KEY UPDATE param_value = new.param_value, param_type = new.param_type,
    description = new.description;

-- -----------------------------------------------------------------------------
-- 11.10 实验室算法注册（lab-svc 启动即可选算法）
-- -----------------------------------------------------------------------------
INSERT INTO lab_algorithm (id, name, description, enabled, sort_order, default_params, schema_version) VALUES
('boids', 'Boids 鸟群', '分离、对齐和凝聚三条局部规则产生全局涌现。', 1, 1,
 CAST('{"count":80,"visionRadius":40,"visionAngle":3.141592653589793,"maxSpeed":2.2,"maxForce":0.05,"separation":1.4,"alignment":0.8,"cohesion":0.6}' AS JSON), 1),
('pso', 'PSO 粒子群', '粒子记住自己和群体去过的最好位置，在多峰地形上聚拢。', 1, 2,
 CAST('{"count":80,"inertia":0.7,"cognitive":1.4,"social":1.4,"maxSpeed":3}' AS JSON), 1),
('ant-colony', '蚁群', '蚂蚁把走过的路留下信息素，短回路会逐渐变亮。', 1, 3,
 CAST('{"ants":40,"alpha":1,"beta":2,"evaporation":0.2,"deposit":2}' AS JSON), 1),
('genetic', '遗传算法', '种群通过选择、交叉和变异靠近一条隐藏的目标基因组。', 1, 4,
 CAST('{"population":60,"mutationRate":0.05,"crossoverRate":0.8,"elitism":2}' AS JSON), 1)
AS new ON DUPLICATE KEY UPDATE name = new.name, description = new.description,
    enabled = new.enabled, sort_order = new.sort_order,
    default_params = new.default_params, schema_version = new.schema_version;

-- -----------------------------------------------------------------------------
-- 11.11 实验室公共预设（user_id IS NULL 表示公共预设）
-- -----------------------------------------------------------------------------
INSERT INTO lab_preset (user_id, algorithm_id, name, params, is_public)
SELECT NULL, seed.algorithm_id, '课堂演示', CAST(seed.params AS JSON), 1
FROM (
    SELECT 'boids' AS algorithm_id, '{"count":160,"visionRadius":48,"visionAngle":3.141592653589793,"maxSpeed":2.4,"maxForce":0.06,"separation":1.2,"alignment":1.1,"cohesion":0.8}' AS params UNION ALL
    SELECT 'pso', '{"count":120,"inertia":0.6,"cognitive":1.6,"social":1.2,"maxSpeed":2.5}' UNION ALL
    SELECT 'ant-colony', '{"ants":60,"alpha":1,"beta":3,"evaporation":0.15,"deposit":2}' UNION ALL
    SELECT 'genetic', '{"population":80,"mutationRate":0.04,"crossoverRate":0.9,"elitism":4}'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM lab_preset existing
    WHERE existing.user_id IS NULL
      AND existing.algorithm_id = seed.algorithm_id
      AND existing.name = '课堂演示'
);

-- -----------------------------------------------------------------------------
-- 11.12 系统词库种子（词源模式取词的最小可用集合）
-- -----------------------------------------------------------------------------
INSERT INTO system_dictionaries (name, description, word_count, category, is_active) VALUES
('基础英文', '内置种子词库，供词源模式取词', 8, 'english', 1)
AS new ON DUPLICATE KEY UPDATE description = new.description, category = new.category, is_active = new.is_active;

INSERT INTO system_dictionary_words (dict_id, word, difficulty_level)
SELECT d.dict_id, seed.word, seed.level
FROM system_dictionaries d
JOIN (
    SELECT 'cat' AS word, 1 AS level UNION ALL
    SELECT 'tree', 2 UNION ALL
    SELECT 'river', 2 UNION ALL
    SELECT 'bridge', 3 UNION ALL
    SELECT 'planet', 3 UNION ALL
    SELECT 'quantum', 5 UNION ALL
    SELECT 'syntax', 4 UNION ALL
    SELECT 'garden', 2
) seed
WHERE d.name = '基础英文'
  AND NOT EXISTS (
      SELECT 1 FROM system_dictionary_words w
      WHERE w.dict_id = d.dict_id AND w.word = seed.word
  );

-- 回填系统词库的词条数量
UPDATE system_dictionaries d
SET word_count = (SELECT COUNT(*) FROM system_dictionary_words w WHERE w.dict_id = d.dict_id)
WHERE d.name IN ('基础英文');

-- -----------------------------------------------------------------------------
-- 11.13 旧 baseline 的基础系统参数（17 条，原样保留并去重）
--   这些参数仍被难度模型、计分与前端展示读取；后台新增的 admin.* 参数见 11.9。
-- -----------------------------------------------------------------------------
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
AS new ON DUPLICATE KEY UPDATE param_value = new.param_value,
    param_type = new.param_type, description = new.description;

-- -----------------------------------------------------------------------------
-- 11.14 内置账号（与旧 baseline 一致；首次登录后请立即改密）
--   admin / admin123，test / test123，密码均为 bcrypt 哈希。
-- -----------------------------------------------------------------------------
INSERT INTO user_info (username, password, nickname, role) VALUES
('admin', '$2a$10$ErIkTsllHveFnbk9lyrjsuzfvEJX0SpFtIfDp/BEJgCgDxKxsfP/u', '管理员', 'admin')
AS new ON DUPLICATE KEY UPDATE nickname = new.nickname, role = new.role;

INSERT INTO user_info (username, password, nickname, role) VALUES
('test', '$2a$10$WhFNEMcnX8WkaxtDFrJICuEXesfhIj8WKAKhf0XU5wnz1r4Hzr9xS', '测试用户', 'user')
AS new ON DUPLICATE KEY UPDATE nickname = new.nickname, role = new.role;
