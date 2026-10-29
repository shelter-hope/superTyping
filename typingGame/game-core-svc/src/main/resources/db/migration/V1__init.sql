-- =============================================================================
-- game-core-svc / typing_game_game 初始 schema（Flyway V1）
-- 来源：DB/typing_word_game.sql（2026-10-30 合并版）按实体归属切分，共 24 张表
-- 跨 schema 外键已降级为逻辑引用（服务间通过 /api/v1/internal/** 校验），
-- 同 schema 外键保留在本文件末尾。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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

DROP TABLE IF EXISTS string_parents;
CREATE TABLE string_parents (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id INT NOT NULL COMMENT '关联用户ID，每个用户仅一条',
    str_parents VARCHAR(255) NOT NULL COMMENT '父本记录对应的怪兽ID，逗号分隔',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_string_parents_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字符串父本表';

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

DROP TABLE IF EXISTS system_monster_commo_configs;
CREATE TABLE system_monster_commo_configs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    min_speed DECIMAL(3,2) NOT NULL DEFAULT 0.80 COMMENT '最小行进速度',
    max_speed DECIMAL(3,2) NOT NULL DEFAULT 3.00 COMMENT '最大行进速度',
    min_str_length INT NOT NULL DEFAULT 1 COMMENT '最小字符长度',
    max_str_length INT NOT NULL DEFAULT 15 COMMENT '最大字符长度',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统怪物公共基础信息配置表';

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

DROP TABLE IF EXISTS model_constants;
CREATE TABLE model_constants (
    constant_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '常量ID',
    max_speed DECIMAL(5,2) DEFAULT 150.00 COMMENT '最大参考速度（字符/分钟）',
    ref_peak_speed DECIMAL(5,2) DEFAULT 120.00 COMMENT '参考峰值速度（字符/分钟）',
    initial_difficulty DECIMAL(3,2) DEFAULT 0.30 COMMENT '初始难度系数',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型常量表';

SET FOREIGN_KEY_CHECKS = 1;

-- 同 schema 外键
ALTER TABLE game_round ADD CONSTRAINT fk_game_round_session FOREIGN KEY (session_id) REFERENCES game_session(session_id);
ALTER TABLE monster_data ADD CONSTRAINT fk_monster_data_record FOREIGN KEY (record_id) REFERENCES game_record(record_id) ON DELETE CASCADE;
ALTER TABLE monster_attempt ADD CONSTRAINT fk_monster_attempt_session FOREIGN KEY (session_id) REFERENCES game_session(session_id);
ALTER TABLE monster_attempt ADD CONSTRAINT fk_monster_attempt_round FOREIGN KEY (round_id) REFERENCES game_round(round_id);
ALTER TABLE monster_parent_relation ADD CONSTRAINT fk_parent_relation_child FOREIGN KEY (child_attempt_id) REFERENCES monster_attempt(attempt_id);
ALTER TABLE monster_parent_relation ADD CONSTRAINT fk_parent_relation_a FOREIGN KEY (parent_a_attempt_id) REFERENCES monster_attempt(attempt_id);
ALTER TABLE monster_parent_relation ADD CONSTRAINT fk_parent_relation_b FOREIGN KEY (parent_b_attempt_id) REFERENCES monster_attempt(attempt_id);
ALTER TABLE leaderboard_entry ADD CONSTRAINT fk_leaderboard_entry_record FOREIGN KEY (record_id) REFERENCES game_record(record_id) ON DELETE CASCADE;
ALTER TABLE game_char_type_configs ADD CONSTRAINT fk_game_char_type_configs_mode FOREIGN KEY (game_mode_id) REFERENCES game_mode_configs(id) ON DELETE CASCADE;
ALTER TABLE user_model_adapt_logs ADD CONSTRAINT fk_user_adapt_logs_record FOREIGN KEY (game_record_id) REFERENCES game_record(record_id) ON DELETE CASCADE;

