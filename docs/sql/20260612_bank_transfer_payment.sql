ALTER TABLE entry_payment
  ADD COLUMN bank_transfer_id bigint DEFAULT NULL AFTER wechat_transaction_id,
  ADD KEY idx_entry_payment_bank_transfer (bank_transfer_id);

ALTER TABLE file_asset
  ADD COLUMN owner_type varchar(32) DEFAULT NULL AFTER business_type,
  ADD COLUMN owner_id bigint DEFAULT NULL AFTER owner_type,
  ADD KEY idx_file_asset_owner (owner_type, owner_id);

CREATE TABLE bank_transfer_payment (
  id bigint NOT NULL AUTO_INCREMENT,
  transfer_no varchar(64) NOT NULL,
  brewery_id bigint NOT NULL,
  portal_account_id bigint NOT NULL,
  competition_id bigint NOT NULL,
  beer_entry_id bigint NOT NULL,
  entry_payment_id bigint NOT NULL,
  amount decimal(10,2) NOT NULL,
  payer_name varchar(128) DEFAULT NULL,
  transfer_time datetime NOT NULL,
  remark varchar(255) NOT NULL,
  voucher_asset_id bigint DEFAULT NULL,
  status varchar(32) NOT NULL,
  admin_id bigint DEFAULT NULL,
  admin_note varchar(255) DEFAULT NULL,
  submitted_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_time datetime DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bank_transfer_no (transfer_no),
  KEY idx_bank_transfer_status (status, submitted_time),
  KEY idx_bank_transfer_competition (competition_id, status),
  KEY idx_bank_transfer_brewery (brewery_id, status),
  KEY idx_bank_transfer_entry (beer_entry_id),
  KEY idx_bank_transfer_entry_payment (entry_payment_id),
  KEY idx_bank_transfer_voucher_asset (voucher_asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
