-- =============================================================================
-- user-svc / typing_game_user 初始 schema（Flyway V1）
-- 来源：DB/typing_word_game.sql（2026-10-30 合并版）按实体归属切分，共 6 张表
-- 跨 schema 外键已降级为逻辑引用（服务间通过 /api/v1/internal/** 校验），
-- 同 schema 外键保留在本文件末尾。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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

SET FOREIGN_KEY_CHECKS = 1;

-- 同 schema 外键
ALTER TABLE user_daily_stats ADD CONSTRAINT fk_user_daily_stats_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_info_snapshot ADD CONSTRAINT fk_user_info_snapshot_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE user_app_configs ADD CONSTRAINT fk_user_app_configs_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE friend_relation ADD CONSTRAINT fk_friend_relation_user FOREIGN KEY (user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;
ALTER TABLE friend_relation ADD CONSTRAINT fk_friend_relation_friend FOREIGN KEY (friend_user_id) REFERENCES user_info(user_id) ON DELETE CASCADE;

