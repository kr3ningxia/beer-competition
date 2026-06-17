-- 银行转账改为单酒款闭环。
-- 执行前请先备份测试库；当前系统仍在测试阶段，旧的多酒款转账记录不再作为业务形态保留。

ALTER TABLE bank_transfer_payment
  ADD COLUMN beer_entry_id bigint NULL AFTER competition_id,
  ADD COLUMN entry_payment_id bigint NULL AFTER beer_entry_id,
  ADD KEY idx_bank_transfer_entry (beer_entry_id),
  ADD KEY idx_bank_transfer_entry_payment (entry_payment_id);

UPDATE bank_transfer_payment btp
JOIN (
  SELECT bank_transfer_payment_id, MIN(id) AS relation_id
  FROM bank_transfer_payment_entry
  GROUP BY bank_transfer_payment_id
) picked ON picked.bank_transfer_payment_id = btp.id
JOIN bank_transfer_payment_entry btpe ON btpe.id = picked.relation_id
SET btp.beer_entry_id = btpe.beer_entry_id,
    btp.entry_payment_id = btpe.entry_payment_id
WHERE btp.beer_entry_id IS NULL;

UPDATE bank_transfer_payment btp
JOIN entry_payment ep ON ep.bank_transfer_id = btp.id
SET btp.beer_entry_id = ep.beer_entry_id,
    btp.entry_payment_id = ep.id
WHERE btp.beer_entry_id IS NULL;

DELETE FROM bank_transfer_payment
WHERE beer_entry_id IS NULL
   OR entry_payment_id IS NULL;

ALTER TABLE bank_transfer_payment
  MODIFY COLUMN beer_entry_id bigint NOT NULL,
  MODIFY COLUMN entry_payment_id bigint NOT NULL;

DROP TABLE IF EXISTS bank_transfer_payment_entry;
