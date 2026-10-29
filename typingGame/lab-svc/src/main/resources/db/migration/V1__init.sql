-- =============================================================================
-- lab-svc / typing_game_lab 初始 schema（Flyway V1）
-- 来源：DB/typing_word_game.sql（2026-10-30 合并版）按实体归属切分，共 3 张表
-- 跨 schema 外键已降级为逻辑引用（服务间通过 /api/v1/internal/** 校验），
-- 同 schema 外键保留在本文件末尾。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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

SET FOREIGN_KEY_CHECKS = 1;

-- 同 schema 外键
ALTER TABLE lab_preset ADD CONSTRAINT fk_lab_preset_algorithm FOREIGN KEY (algorithm_id) REFERENCES lab_algorithm(id) ON DELETE CASCADE;
ALTER TABLE lab_run ADD CONSTRAINT fk_lab_run_algorithm FOREIGN KEY (algorithm_id) REFERENCES lab_algorithm(id) ON DELETE CASCADE;

