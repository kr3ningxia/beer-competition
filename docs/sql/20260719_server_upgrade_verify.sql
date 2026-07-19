-- 服务器升级后数据校验脚本（只读）。
-- 请先执行 20260718_multi_entry_registration.sql 和
-- 20260719_competition_list_indexes.sql，再执行本文件。

-- 1. 历史数据关联完整性：各项 actual_value 应为 0。
-- 没有对应厂商账号的极旧酒款会使用 portal_account_id=0，不影响聚合订单关联。
SELECT 'beer_entry_without_registration_batch' AS item,
       COUNT(*) AS actual_value,
       0 AS expected_value
FROM beer_entry
WHERE registration_batch_id IS NULL
UNION ALL
SELECT 'entry_payment_without_payment_order', COUNT(*), 0
FROM entry_payment
WHERE payment_order_id IS NULL
UNION ALL
SELECT 'entry_payment_without_order_item', COUNT(*), 0
FROM entry_payment ep
LEFT JOIN payment_order_item poi ON poi.entry_payment_id = ep.id
WHERE poi.id IS NULL
UNION ALL
SELECT 'entry_refund_without_order_item', COUNT(*), 0
FROM entry_refund
WHERE payment_order_item_id IS NULL
UNION ALL
SELECT 'bank_transfer_with_entry_without_payment_order', COUNT(*), 0
FROM bank_transfer_payment
WHERE entry_payment_id IS NOT NULL
  AND payment_order_id IS NULL;

-- 2. 新表记录量：用于和原业务表数量交叉核对。
SELECT 'beer_entry' AS item, COUNT(*) AS actual_value FROM beer_entry
UNION ALL SELECT 'entry_payment', COUNT(*) FROM entry_payment
UNION ALL SELECT 'registration_batch', COUNT(*) FROM registration_batch
UNION ALL SELECT 'payment_order', COUNT(*) FROM payment_order
UNION ALL SELECT 'payment_order_item', COUNT(*) FROM payment_order_item;

-- 3. 列表聚合查询执行计划：key 应优先使用新增联合索引。
EXPLAIN
SELECT competition_id,
       COUNT(*) AS total_count,
       SUM(CASE WHEN status = 'PENDING_PAYMENT' THEN 1 ELSE 0 END) AS pending_payment_count,
       SUM(CASE WHEN status <> 'CANCELED' THEN 1 ELSE 0 END) AS registered_count,
       SUM(CASE WHEN stored_flag = 1 THEN 1 ELSE 0 END) AS stored_count,
       SUM(CASE WHEN status = 'CANCELED' THEN 1 ELSE 0 END) AS canceled_count,
       SUM(CASE WHEN status IN ('RESULT_PUBLISHED', 'PUBLISHED') THEN 1 ELSE 0 END) AS result_published_count
FROM beer_entry
GROUP BY competition_id;
