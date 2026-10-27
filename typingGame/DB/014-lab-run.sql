-- 实验室实验快照（客户端入库，服务端不重跑）
-- 回滚：DROP TABLE IF EXISTS lab_run;

CREATE TABLE IF NOT EXISTS lab_run (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  algorithm_id VARCHAR(64) NOT NULL,
  plugin_id VARCHAR(64) NOT NULL,
  schema_version INT NOT NULL DEFAULT 1,
  engine_kind VARCHAR(32) NOT NULL DEFAULT 'client_snapshot',
  title VARCHAR(120) NULL,
  notes VARCHAR(1000) NULL,
  seed INT NOT NULL,
  params_json JSON NOT NULL,
  preset_id BIGINT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'completed',
  started_at DATETIME NULL,
  ended_at DATETIME NULL,
  duration_ms INT NULL,
  summary_json JSON NULL,
  metrics_json JSON NULL,
  client_meta_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_lab_run_algorithm FOREIGN KEY (algorithm_id) REFERENCES lab_algorithm (id),
  INDEX idx_lab_run_user_created (user_id, created_at DESC),
  INDEX idx_lab_run_user_algo (user_id, algorithm_id, created_at DESC)
);
