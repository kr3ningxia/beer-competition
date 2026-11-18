-- 厂商端首页支持运营手动指定重点赛事。
-- 配置方式：把目标赛事的 home_featured 置为 1，同一时间只保留一场；其余赛事保持 0。
-- 未配置时，首页按原有规则自动选取（平台赛事优先，报名截止时间最早优先）。
ALTER TABLE `competition`
  ADD COLUMN `home_featured` tinyint NOT NULL DEFAULT 0
    COMMENT '首页重点赛事：1 时优先展示在厂商端首页主位'
    AFTER `logistics_visibility`;
