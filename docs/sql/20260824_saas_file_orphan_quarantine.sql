-- SaaS 文件孤立记录隔离。
--
-- 目的：保护无法从退款、转账或奖项关系追溯组织范围的敏感文件。
-- 处理原则：保留元数据和存储对象，清空旧的直达公开地址；业务层因缺少关联
-- 记录会拒绝后台下载。完成备份后执行，脚本可重复执行。

SET NAMES utf8mb4;

SELECT id, business_type, owner_type, owner_id, organizer_id, public_url
  FROM file_asset
 WHERE organizer_id IS NULL
   AND business_type IN (
       'BANK_TRANSFER_VOUCHER',
       'OFFLINE_REFUND_VOUCHER',
       'AWARD_CERTIFICATE',
       'ORGANIZER_APPLICATION_MATERIAL'
   )
 ORDER BY id;

UPDATE file_asset
   SET public_url = NULL
 WHERE organizer_id IS NULL
   AND business_type IN (
       'BANK_TRANSFER_VOUCHER',
       'OFFLINE_REFUND_VOUCHER',
       'AWARD_CERTIFICATE',
       'ORGANIZER_APPLICATION_MATERIAL'
   );

SELECT id, business_type, owner_type, owner_id, organizer_id, public_url
  FROM file_asset
 WHERE organizer_id IS NULL
   AND business_type IN (
       'BANK_TRANSFER_VOUCHER',
       'OFFLINE_REFUND_VOUCHER',
       'AWARD_CERTIFICATE',
       'ORGANIZER_APPLICATION_MATERIAL'
   )
 ORDER BY id;

-- 注意：若对象存储桶仍允许匿名读取，仅清空数据库 URL 不能撤销历史直达地址。
-- 发布前必须确认对应对象前缀已改为私有，并只允许应用层受控下载。
