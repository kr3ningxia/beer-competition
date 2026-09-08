-- 为参赛酒款增加比赛内箱号，箱号由比赛边界隔离。

ALTER TABLE `beer_entry`
  ADD COLUMN `box_number` varchar(20) DEFAULT NULL COMMENT '比赛内样品箱号' AFTER `stored_flag`,
  ADD KEY `idx_beer_entry_competition_box` (`competition_id`, `box_number`);
