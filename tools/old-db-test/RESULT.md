# 旧数据库实测结果（2026-10-26，本地 sqlite3 版）

## 结论

- 按旧版 `AppDatabase_Impl`（v5）CREATE 原文建的旧库：`user_version = 5`，
  `room_master_table` identity_hash = `1cfaa182d7af917923832c86bf4dc41d`。
- 跑新版 `MIGRATION_5_6`（只建 `RecurringRecord` 表）后：版本升到 6，
  9 张表齐全，种子数据一行没丢（Record 2 条、Assets 现金 90000 分 /
  银行卡 500000 分、Budget 1 条）。
- 应用常用查询在迁移后的库上都能跑：按分类合计、按账户查账单、
  备注或账户名搜索；`integrity_check` = ok，`foreign_key_check` 无违规。
- 新版 8 张旧表（Record / RecordType / Assets / AssetsModifyRecord /
  AssetsSummaryRecord / AssetsTransferRecord / Budget / Reimburse）的列
  与旧库逐个比对，全部一致，无缺列、无多余列。

## 尚未覆盖（需真机 / 模拟器）

- Room 打开旧库时的 identity_hash 校验：本地没有 Android 环境，
  无法跑 Room 编译器算新版 hash。按 Room 规则，旧库版本是 5、
  新版是 6，走迁移路径时会先跑迁移再更新 hash，不会在旧 hash
  上直接报错；但最终确认仍需在真机或模拟器上用一份真实旧库
  打开一次（打开 + 记一笔 + 统计），再删掉本段说明。
- 本地没有找到任何真实旧版 `MoneyKeeper.db` 文件（全盘搜过），
  本次实测用的是按旧版 CREATE 原文重建的等效旧库，不是用户
  手机上导出的真实文件。
