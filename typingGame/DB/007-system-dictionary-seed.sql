-- 系统词库种子。可重复执行：按名称和词条去重。
-- 回滚：DELETE FROM system_dictionary_words WHERE dict_id IN (SELECT dict_id FROM system_dictionaries WHERE name = '基础英文');
--       DELETE FROM system_dictionaries WHERE name = '基础英文';

INSERT INTO system_dictionaries (name, description, word_count, category, is_active)
SELECT '基础英文', '内置种子词库，供词源模式取词', 8, 'english', 1
WHERE NOT EXISTS (SELECT 1 FROM system_dictionaries WHERE name = '基础英文');

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
      SELECT 1 FROM system_dictionary_words existing
      WHERE existing.dict_id = d.dict_id AND existing.word = seed.word
  );

UPDATE system_dictionaries d
SET word_count = (
    SELECT COUNT(*) FROM system_dictionary_words w WHERE w.dict_id = d.dict_id
)
WHERE d.name = '基础英文';
