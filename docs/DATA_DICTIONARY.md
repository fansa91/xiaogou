# 小狗记账数据字典（Phase 0 盘点）

> 来源：旧版数据库的 CREATE 语句与实体类，Phase 0 逐表核对后记录。
> 重写版必须保持以下口径，否则旧数据、旧备份无法接续。

## 基本口径

- 数据库文件名：`MoneyKeeper.db`，不改名、不换库。
- 金额：全部以分为单位存 `money` 等整数字段（Long），显示时除以 100。
- 时间：全部以毫秒时间戳存 `time` / `create_time` 等字段；统计按月分组时
  沿用旧口径：取时间戳前 10 位（秒）再分组，避免统计结果漂移。
- 迁移策略：新 Room 打开旧文件时只做加表/加列类迁移，不删表、不改列类型；
  上线前必须用一份旧版 `MoneyKeeper.db` 副本做“打开 + 记一笔 + 统计 + 备份”回归测试。

## 表清单（Room CREATE 语句原文口径）

| 表 | 关键列 |
| --- | --- |
| `Record` | `id` INTEGER PRIMARY KEY AUTOINCREMENT；`money` INTEGER（分）；`remark` TEXT；`time` INTEGER（毫秒）；`create_time` INTEGER；`record_type_id` INTEGER NOT NULL（外键 → `RecordType.id`）；`assets_id` INTEGER（外键 → `Assets.id`，可空） |
| `RecordType` | `id`；`name` TEXT；`img_name` TEXT；`type` INTEGER NOT NULL（0 支出 / 1 收入）；`ranking` INTEGER NOT NULL；`state` INTEGER NOT NULL；`parent_id` INTEGER NOT NULL（子类型用父 id 关联） |
| `Assets` | `id`；`name` TEXT NOT NULL；`img_name` TEXT NOT NULL；`type` INTEGER NOT NULL；`state` INTEGER NOT NULL；`remark` TEXT NOT NULL；`create_time` INTEGER NOT NULL；`money` INTEGER NOT NULL（分，当前余额）；`ranking` INTEGER；`init_money` INTEGER NOT NULL（分，初始金额） |
| `AssetsModifyRecord` | `id`；`assets_id` INTEGER NOT NULL（外键 → `Assets.id`，DELETE CASCADE）；`money` INTEGER NOT NULL；`money_before` INTEGER NOT NULL；`state` INTEGER NOT NULL；`create_time` INTEGER NOT NULL |
| `AssetsSummaryRecord` | `id`；`time` INTEGER NOT NULL；`assets_id` INTEGER NOT NULL；`assets_money` INTEGER NOT NULL；`liabilities_money` INTEGER NOT NULL |
| `AssetsTransferRecord` | `id`；`state` INTEGER NOT NULL；`create_time` INTEGER NOT NULL；`time` INTEGER NOT NULL；`assets_id_form` INTEGER NOT NULL（外键 → `Assets.id`，DELETE CASCADE）；`assets_id_to` INTEGER NOT NULL（同上）；`remark` TEXT NOT NULL；`money` INTEGER NOT NULL（分） |
| `Budget` | `id`；`record_type_id` INTEGER NOT NULL；`remark` TEXT NOT NULL；`month` INTEGER（年月，如 202610）；`money` INTEGER NOT NULL（分） |
| `Reimburse` | `id`；`money` INTEGER NOT NULL（分）；`reimburse_money` INTEGER NOT NULL（分）；`remark` TEXT；`time` INTEGER；`create_time` INTEGER；`record_type_id` INTEGER NOT NULL（外键 → `RecordType.id`）；`reimburse_state` INTEGER NOT NULL；`assets_id` INTEGER NOT NULL（外键 → `Assets.id`） |
| `RecurringRecord` | 不在 Room 内，用直接 SQL 建表（旧版实现如此，Phase 1 按旧版建表语句复现）：`id`、`money`（分）、`remark`、`record_type_id`、`record_type`（0 支出 / 1 收入，冗余存储）、`assets_id`（-1 = 不计入账户）、`cycle_type`、`cycle_value`、`start_time`、`end_time`、`next_time`、`last_time`、`state`（0 启用 / 1 暂停）、`create_time` |

## 周期规则（RecurringRecord）

- `cycle_type`：0 每天、1 每周（`Calendar.DAY_OF_WEEK`，1=周日…7=周六）、
  2 每月（日）、3 每年（Mdd，如 1225）。
- 首次执行取当天或下一个满足日的 09:00；漏跑单次补跑上限 100 次。
- 建表用幂等 `ensureTable()` 思路：表已存在时不重建、不清数据。

## 设置键与备份格式（保持兼容）

- 设置键（SharedPreferences / DataStore 迁移时键名不变）：
  `key_web_dav_auto_backup`、`key_web_dav_auto_backup_time`、
  `key_budget_notified_<id>_<yyyyMM>`（预算同月去重键）。
- 备份加密：AES-256-GCM + PBKDF2WithHmacSHA256（100,000 次），密钥由 WebDAV 密码派生；
  文件格式：magic `XGZ1` + 16 字节 salt + 12 字节 IV + 密文 + 16 字节 tag。
- 服务端目录：`ElephantBookkeeping/`（服务端约定，重写时先不改，否则与已有备份断链）。
- 备份恢复：先解密再还原；解密失败时按旧版行为回退（Phase 3 按旧版 BackupManager 行为核对后写进测试）。

## 通知渠道（Phase 2/3 落地时不变）

- `channel_backup`（LOW）、`channel_recurring`（DEFAULT）、`channel_budget`（HIGH）。
- 每日 09:00 检查备份/周期/预算；预算同一月同一预算只提醒一次；
  80% = 即将用尽，100% = 超支。
- Android 13+ 的 `POST_NOTIFICATIONS` 权限流程不变。
