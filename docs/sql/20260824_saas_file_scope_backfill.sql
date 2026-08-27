-- 文件范围补齐与公开地址收口。
-- 执行前请完成数据库备份；本脚本只回填可由现有关联稳定推导的记录。

-- 历史厂商头像通过 brewery.avatar_asset_id 绑定到厂商账号。
UPDATE file_asset f
JOIN brewery b ON b.avatar_asset_id = f.id
JOIN portal_account p ON p.brewery_id = b.id
SET f.owner_type = 'PORTAL_ACCOUNT',
    f.owner_id = p.id
WHERE f.business_type = 'BREWERY_AVATAR'
  AND (f.owner_type IS NULL OR f.owner_id IS NULL);

-- 历史赞助商 Logo 通过比赛关联补齐组织范围。
UPDATE file_asset f
JOIN competition_sponsor s ON s.logo_asset_id = f.id
JOIN competition c ON c.id = s.competition_id
SET f.owner_type = 'COMPETITION',
    f.owner_id = c.id,
    f.organizer_id = c.organizer_id
WHERE f.business_type = 'COMPETITION_SPONSOR_LOGO';

-- 银行转账凭证通过转账记录关联比赛。
UPDATE file_asset f
JOIN bank_transfer_payment btp ON btp.voucher_asset_id = f.id
JOIN competition c ON c.id = btp.competition_id
SET f.organizer_id = c.organizer_id
WHERE f.business_type = 'BANK_TRANSFER_VOUCHER';

-- 退款凭证通过退款 -> 酒款 -> 比赛关联比赛。
UPDATE file_asset f
JOIN entry_refund r ON r.offline_refund_voucher_asset_id = f.id
JOIN beer_entry e ON e.id = r.beer_entry_id
JOIN competition c ON c.id = e.competition_id
SET f.owner_type = 'ENTRY_REFUND',
    f.owner_id = r.id,
    f.organizer_id = c.organizer_id
WHERE f.business_type = 'OFFLINE_REFUND_VOUCHER';

-- 奖状通过奖项 -> 比赛关联组织范围。
UPDATE file_asset f
JOIN award_result a ON a.certificate_asset_id = f.id
JOIN competition c ON c.id = a.competition_id
SET f.owner_type = 'AWARD_RESULT',
    f.owner_id = a.id,
    f.organizer_id = c.organizer_id,
    f.public_url = NULL
WHERE f.business_type = 'AWARD_CERTIFICATE';

-- 敏感文件不再保留可直达的公开地址。
UPDATE file_asset
SET public_url = NULL
WHERE business_type IN ('BANK_TRANSFER_VOUCHER', 'OFFLINE_REFUND_VOUCHER',
                        'AWARD_CERTIFICATE', 'ORGANIZER_APPLICATION_MATERIAL');

-- 可公开文件统一由应用层受控地址生成，旧地址清空后由新代码返回 /api/portal/public/files/{id}。
UPDATE file_asset
SET public_url = NULL
WHERE business_type IN ('BREWERY_AVATAR', 'COMPETITION_SPONSOR_LOGO');
