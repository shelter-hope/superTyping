-- 游戏规则配置。可重复执行。
-- 回滚：DROP TABLE IF EXISTS game_rule_config;

CREATE TABLE IF NOT EXISTS game_rule_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_key VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(16) NOT NULL,
    update_id INT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_game_rule_version (rule_key, version)
);

INSERT INTO game_rule_config (rule_key, version, payload, status, update_id)
SELECT 'difficulty', 1, CAST('{
  "weightAccuracy":1.0,
  "weightNormalizedSpeed":0.0,
  "weightHistorySpeed":0.0,
  "weightComposure":1.0,
  "weightRound":0.0,
  "targetAccuracy":0.85,
  "gain":0.35,
  "step":0.05,
  "minDifficulty":0.15,
  "maxDifficulty":0.95,
  "initialDifficulty":0.30,
  "maxReferenceSpeed":150,
  "refPeakSpeed":120,
  "weakCharDivisor":10,
  "speedSpan":1.0,
  "countBase":3,
  "countRoundFactor":6,
  "countDifficultyFactor":16,
  "countMin":3,
  "countMax":25,
  "normalBase":80,
  "normalSlope":0.45,
  "normalMin":35,
  "normalMax":80,
  "attackBase":5,
  "attackSlope":0.15,
  "attackMin":5,
  "attackMax":20,
  "evasiveBase":8,
  "evasiveSlope":0.12,
  "evasiveMin":5,
  "evasiveMax":25,
  "roundLogBase":32
}' AS JSON), 'active', 0
WHERE NOT EXISTS (SELECT 1 FROM game_rule_config WHERE rule_key = 'difficulty' AND version = 1);

INSERT INTO game_rule_config (rule_key, version, payload, status, update_id)
SELECT 'genetic', 1, CAST('{
  "inRoundParentLimit":10,
  "crossRoundParentLimit":8,
  "inRoundMutationRate":0.15,
  "crossRoundMutationRate":0.25,
  "crossover":"SINGLE_POINT",
  "preferWeakChars":true,
  "fitnessScoreWeight":1,
  "fitnessAccuracyWeight":100,
  "fitnessErrorPenalty":2
}' AS JSON), 'active', 0
WHERE NOT EXISTS (SELECT 1 FROM game_rule_config WHERE rule_key = 'genetic' AND version = 1);
