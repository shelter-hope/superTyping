-- 群体智能实验室
-- 可重复执行：表使用 CREATE TABLE IF NOT EXISTS，种子使用 NOT EXISTS。
-- 回滚：
--   DROP TABLE IF EXISTS lab_preset;
--   DROP TABLE IF EXISTS lab_algorithm;

CREATE TABLE IF NOT EXISTS lab_algorithm (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  default_params JSON NOT NULL,
  schema_version INT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS lab_preset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NULL,
  algorithm_id VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  params JSON NOT NULL,
  is_public TINYINT(1) NOT NULL DEFAULT 0,
  CONSTRAINT fk_lab_preset_algorithm FOREIGN KEY (algorithm_id) REFERENCES lab_algorithm (id)
);

INSERT INTO lab_algorithm (id, name, description, enabled, sort_order, default_params, schema_version)
SELECT 'boids', 'Boids 鸟群', '分离、对齐和凝聚三条局部规则产生全局涌现。', 1, 1,
       JSON_OBJECT(
         'count', 80,
         'visionRadius', 40,
         'visionAngle', 3.141592653589793,
         'maxSpeed', 2.2,
         'maxForce', 0.05,
         'separation', 1.4,
         'alignment', 0.8,
         'cohesion', 0.6
       ),
       1
WHERE NOT EXISTS (SELECT 1 FROM lab_algorithm WHERE id = 'boids');

INSERT INTO lab_preset (user_id, algorithm_id, name, params, is_public)
SELECT NULL, 'boids', '课堂演示',
       JSON_OBJECT(
         'count', 160,
         'visionRadius', 48,
         'visionAngle', 3.141592653589793,
         'maxSpeed', 2.4,
         'maxForce', 0.06,
         'separation', 1.2,
         'alignment', 1.1,
         'cohesion', 0.8
       ),
       1
WHERE NOT EXISTS (
  SELECT 1 FROM lab_preset WHERE user_id IS NULL AND algorithm_id = 'boids' AND name = '课堂演示'
);
