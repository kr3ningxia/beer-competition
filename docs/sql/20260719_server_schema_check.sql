-- 服务器数据库结构检查脚本（只读，可在升级前后重复执行）。
-- 目标版本：多酒款批次报名、聚合支付、比赛管理列表联合索引。

SELECT 'database' AS item, DATABASE() AS actual_value, 'beer_competition' AS expected_value;
SELECT 'mysql_version' AS item, VERSION() AS actual_value, '8.0+' AS expected_value;

-- 1. 新增表：升级后应返回 3 行，actual_value 均为 1。
SELECT expected.table_name AS item,
       COUNT(t.table_name) AS actual_value,
       1 AS expected_value
FROM (
  SELECT 'registration_batch' AS table_name
  UNION ALL SELECT 'payment_order'
  UNION ALL SELECT 'payment_order_item'
) expected
LEFT JOIN information_schema.tables t
  ON t.table_schema = DATABASE()
 AND t.table_name = expected.table_name
GROUP BY expected.table_name
ORDER BY expected.table_name;

-- 2. 新增字段：升级后应返回 4 行，actual_value 均为 1。
SELECT CONCAT(expected.table_name, '.', expected.column_name) AS item,
       COUNT(c.column_name) AS actual_value,
       1 AS expected_value
FROM (
  SELECT 'beer_entry' AS table_name, 'registration_batch_id' AS column_name
  UNION ALL SELECT 'entry_payment', 'payment_order_id'
  UNION ALL SELECT 'bank_transfer_payment', 'payment_order_id'
  UNION ALL SELECT 'entry_refund', 'payment_order_item_id'
) expected
LEFT JOIN information_schema.columns c
  ON c.table_schema = DATABASE()
 AND c.table_name = expected.table_name
 AND c.column_name = expected.column_name
GROUP BY expected.table_name, expected.column_name
ORDER BY expected.table_name, expected.column_name;

-- 3. 银行转账兼容聚合订单：升级后两个旧关联字段都应允许 NULL。
SELECT CONCAT(table_name, '.', column_name, '.nullable') AS item,
       is_nullable AS actual_value,
       'YES' AS expected_value
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'bank_transfer_payment'
  AND column_name IN ('beer_entry_id', 'entry_payment_id')
ORDER BY column_name;

-- 4. 新增索引：升级后应返回 5 行，actual_value 均为 1。
SELECT CONCAT(expected.table_name, '.', expected.index_name) AS item,
       COUNT(DISTINCT s.index_name) AS actual_value,
       1 AS expected_value
FROM (
  SELECT 'beer_entry' AS table_name, 'idx_beer_entry_batch' AS index_name
  UNION ALL SELECT 'entry_payment', 'idx_entry_payment_order'
  UNION ALL SELECT 'bank_transfer_payment', 'idx_bank_transfer_payment_order'
  UNION ALL SELECT 'entry_refund', 'idx_entry_refund_order_item'
  UNION ALL SELECT 'beer_entry', 'idx_beer_entry_competition_status_stored'
) expected
LEFT JOIN information_schema.statistics s
  ON s.table_schema = DATABASE()
 AND s.table_name = expected.table_name
 AND s.index_name = expected.index_name
GROUP BY expected.table_name, expected.index_name
ORDER BY expected.table_name, expected.index_name;

-- 5. 列表联合索引列顺序：升级后 actual_value 应为 competition_id,status,stored_flag。
SELECT 'beer_entry.idx_beer_entry_competition_status_stored.columns' AS item,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS actual_value,
       'competition_id,status,stored_flag' AS expected_value
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'beer_entry'
  AND index_name = 'idx_beer_entry_competition_status_stored';
