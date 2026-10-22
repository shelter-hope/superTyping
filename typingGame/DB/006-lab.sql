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

INSERT INTO lab_algorithm (id, name, description, enabled, sort_order, default_params, schema_version)
SELECT 'pso', 'PSO 粒子群', '粒子记住自己和群体去过的最好位置，在多峰地形上聚拢。', 1, 2,
       JSON_OBJECT('count', 80, 'inertia', 0.7, 'cognitive', 1.4, 'social', 1.4, 'maxSpeed', 3), 1
WHERE NOT EXISTS (SELECT 1 FROM lab_algorithm WHERE id = 'pso');

INSERT INTO lab_preset (user_id, algorithm_id, name, params, is_public)
SELECT NULL, 'pso', '课堂演示',
       JSON_OBJECT('count', 120, 'inertia', 0.6, 'cognitive', 1.6, 'social', 1.2, 'maxSpeed', 2.5), 1
WHERE NOT EXISTS (SELECT 1 FROM lab_preset WHERE user_id IS NULL AND algorithm_id = 'pso' AND name = '课堂演示');

INSERT INTO lab_algorithm (id, name, description, enabled, sort_order, default_params, schema_version)
SELECT 'ant-colony', '蚁群', '蚂蚁把走过的路留下信息素，短回路会逐渐变亮。', 1, 3,
       JSON_OBJECT('ants', 40, 'alpha', 1, 'beta', 2, 'evaporation', 0.2, 'deposit', 2), 1
WHERE NOT EXISTS (SELECT 1 FROM lab_algorithm WHERE id = 'ant-colony');

INSERT INTO lab_preset (user_id, algorithm_id, name, params, is_public)
SELECT NULL, 'ant-colony', '课堂演示',
       JSON_OBJECT('ants', 60, 'alpha', 1, 'beta', 3, 'evaporation', 0.15, 'deposit', 2), 1
WHERE NOT EXISTS (SELECT 1 FROM lab_preset WHERE user_id IS NULL AND algorithm_id = 'ant-colony' AND name = '课堂演示');

INSERT INTO lab_algorithm (id, name, description, enabled, sort_order, default_params, schema_version)
SELECT 'genetic', '遗传算法', '种群通过选择、交叉和变异靠近一条隐藏的目标基因组。', 1, 4,
       JSON_OBJECT('population', 60, 'mutationRate', 0.05, 'crossoverRate', 0.8, 'elitism', 2), 1
WHERE NOT EXISTS (SELECT 1 FROM lab_algorithm WHERE id = 'genetic');

INSERT INTO lab_preset (user_id, algorithm_id, name, params, is_public)
SELECT NULL, 'genetic', '课堂演示',
       JSON_OBJECT('population', 80, 'mutationRate', 0.04, 'crossoverRate', 0.9, 'elitism', 4), 1
WHERE NOT EXISTS (SELECT 1 FROM lab_preset WHERE user_id IS NULL AND algorithm_id = 'genetic' AND name = '课堂演示');

