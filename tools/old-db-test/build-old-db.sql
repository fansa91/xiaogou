-- 旧数据库实测：按旧版 AppDatabase_Impl（v5）的 CREATE 语句原样建一份旧库，
-- 再跑新版 MIGRATION_5_6，验证数据不丢、应用常用查询能跑。
-- 旧库 identity_hash：1cfaa182d7af917923832c86bf4dc41d；user_version = 5。
-- 注意：本地没有 Android 环境，Room 的 identity_hash 校验只能在真机/模拟器上
-- 跑；这里验证的是“迁移 SQL + 表结构 + 数据 + 查询”这一层。
-- v5 建表（旧版 CREATE 原文）：
CREATE TABLE IF NOT EXISTS `Record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `money` INTEGER, `remark` TEXT, `time` INTEGER, `create_time` INTEGER, `record_type_id` INTEGER NOT NULL, `assets_id` INTEGER, FOREIGN KEY(`record_type_id`) REFERENCES `RecordType`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`assets_id`) REFERENCES `Assets`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE TABLE IF NOT EXISTS `Reimburse` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `money` INTEGER NOT NULL, `reimburse_money` INTEGER NOT NULL, `remark` TEXT, `time` INTEGER, `create_time` INTEGER, `record_type_id` INTEGER NOT NULL, `reimburse_state` INTEGER NOT NULL, `assets_id` INTEGER NOT NULL, FOREIGN KEY(`record_type_id`) REFERENCES `RecordType`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`assets_id`) REFERENCES `Assets`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE TABLE IF NOT EXISTS `RecordType` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `img_name` TEXT, `type` INTEGER NOT NULL, `ranking` INTEGER NOT NULL, `state` INTEGER NOT NULL, `parent_id` INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS `Budget` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `record_type_id` INTEGER NOT NULL, `remark` TEXT NOT NULL, `month` INTEGER, `money` INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS `Assets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL, `img_name` TEXT NOT NULL, `type` INTEGER NOT NULL, `state` INTEGER NOT NULL, `remark` TEXT NOT NULL, `create_time` INTEGER NOT NULL, `money` INTEGER NOT NULL, `ranking` INTEGER, `init_money` INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS `AssetsModifyRecord` (`assets_id` INTEGER NOT NULL, `money` INTEGER NOT NULL, `money_before` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT, `state` INTEGER NOT NULL, `create_time` INTEGER NOT NULL, FOREIGN KEY(`assets_id`) REFERENCES `Assets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE TABLE IF NOT EXISTS `AssetsSummaryRecord` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `time` INTEGER NOT NULL, `assets_id` INTEGER NOT NULL, `assets_money` INTEGER NOT NULL, `liabilities_money` INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS `AssetsTransferRecord` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `state` INTEGER NOT NULL, `create_time` INTEGER NOT NULL, `time` INTEGER NOT NULL, `assets_id_form` INTEGER NOT NULL, `assets_id_to` INTEGER NOT NULL, `remark` TEXT NOT NULL, `money` INTEGER NOT NULL, FOREIGN KEY(`assets_id_form`) REFERENCES `Assets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`assets_id_to`) REFERENCES `Assets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT);
INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '1cfaa182d7af917923832c86bf4dc41d');
PRAGMA user_version = 5;
-- 种子数据（分、毫秒，与旧版口径一致）：
INSERT INTO RecordType (id, name, img_name, type, ranking, state, parent_id) VALUES
  (1, '餐饮', '', 0, 1, 1, -1),
  (2, '工资', '', 1, 1, 1, -1),
  (3, '午餐', '', 0, 99, 1, 1);
INSERT INTO Assets (id, name, img_name, type, state, remark, create_time, money, ranking, init_money) VALUES
  (1, '现金', '', 0, 1, '', 1761000000000, 90000, 1, 100000),
  (2, '银行卡', '', 1, 1, '', 1761000000000, 500000, 2, 500000);
INSERT INTO Record (id, money, remark, time, create_time, record_type_id, assets_id) VALUES
  (1, 2500, '午饭', 1761500000000, 1761500000000, 3, 1),
  (2, 800000, '十月工资', 1761500000000, 1761500000000, 2, 2);
INSERT INTO AssetsModifyRecord (assets_id, money, money_before, state, create_time) VALUES
  (1, 90000, 100000, 1, 1761500000000);
INSERT INTO Budget (id, record_type_id, remark, month, money) VALUES (1, 1, '', 202610, 100000);
INSERT INTO Reimburse (id, money, reimburse_money, remark, time, create_time, record_type_id, reimburse_state, assets_id) VALUES
  (1, 5000, 0, '打车', 1761500000000, 1761500000000, 1, 0, 1);
INSERT INTO AssetsTransferRecord (id, state, create_time, time, assets_id_form, assets_id_to, remark, money) VALUES
  (1, 1, 1761500000000, 1761500000000, 2, 1, '取现', 10000);
