-- 变更编号: 003
-- 变更时间: 2026-10-07
-- 目的: 修复 RoundPlan 存储长度，并将排行榜约束为每用户每模式一条记录。
-- 执行前: 备份数据库。排行榜重复行会先备份到独立表，再按最高分规则清理。

SET @db_name = DATABASE();

-- RoundPlan JSON 不能使用 VARCHAR(255)。
SET @sql = IF(
    (SELECT COUNT(*)
       FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'game_round'
        AND column_name = 'plan_payload'
        AND data_type <> 'longtext') > 0,
    'ALTER TABLE game_round MODIFY COLUMN plan_payload LONGTEXT NULL COMMENT ''本轮怪物计划JSON快照''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 保留重复行的完整快照，便于审计或人工恢复。
CREATE TABLE IF NOT EXISTS leaderboard_entry_duplicate_backup_20261007 LIKE leaderboard_entry;

INSERT INTO leaderboard_entry_duplicate_backup_20261007
SELECT entry.*
  FROM leaderboard_entry entry
  JOIN (
        SELECT user_id, game_mode
          FROM leaderboard_entry
         GROUP BY user_id, game_mode
        HAVING COUNT(*) > 1
       ) duplicate_group
    ON duplicate_group.user_id = entry.user_id
   AND duplicate_group.game_mode = entry.game_mode
 WHERE NOT EXISTS (
       SELECT 1
         FROM leaderboard_entry_duplicate_backup_20261007 backup
        WHERE backup.entry_id = entry.entry_id
       );

-- 每组保留最高分；同分保留较新的记录；仍相同则保留较大的主键。
DELETE entry
  FROM leaderboard_entry entry
  JOIN leaderboard_entry better
    ON better.user_id = entry.user_id
   AND better.game_mode = entry.game_mode
   AND (
        better.score > entry.score
        OR (better.score = entry.score AND
            (COALESCE(better.created_at, '1000-01-01') > COALESCE(entry.created_at, '1000-01-01')
             OR (COALESCE(better.created_at, '1000-01-01') = COALESCE(entry.created_at, '1000-01-01')
                 AND better.entry_id > entry.entry_id)))
       );

SET @sql = IF(
    (SELECT COUNT(*)
       FROM information_schema.statistics
      WHERE table_schema = @db_name
        AND table_name = 'leaderboard_entry'
        AND index_name = 'uk_leaderboard_user_mode') = 0,
    'ALTER TABLE leaderboard_entry ADD CONSTRAINT uk_leaderboard_user_mode UNIQUE (user_id, game_mode)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 验证：以下查询应返回 0 行。
SELECT user_id, game_mode, COUNT(*) AS row_count
  FROM leaderboard_entry
 GROUP BY user_id, game_mode
HAVING COUNT(*) > 1;
