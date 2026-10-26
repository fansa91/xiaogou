#!/bin/bash
# 旧数据库实测（本地 sqlite3 版）：
# 1. 按旧版 v5 CREATE 建库并种数据；2. 跑新版 MIGRATION_5_6；
# 3. 核对表结构与数据；4. 跑应用常用查询。
# 退出码 0 = 全部通过；失败会在哪一步停下并打印。
set -e
cd "$(dirname "$0")"
DB=old-test.db
MIGRATED=migrated-test.db
rm -f "$DB" "$MIGRATED"
sqlite3 "$DB" < build-old-db.sql
echo "== 旧库版本与 identity_hash =="
sqlite3 "$DB" "PRAGMA user_version; SELECT identity_hash FROM room_master_table WHERE id = 42;"
echo "== 旧库各表行数 =="
sqlite3 "$DB" "SELECT 'Record', COUNT(*) FROM Record UNION ALL SELECT 'RecordType', COUNT(*) FROM RecordType UNION ALL SELECT 'Assets', COUNT(*) FROM Assets UNION ALL SELECT 'Budget', COUNT(*) FROM Budget UNION ALL SELECT 'Reimburse', COUNT(*) FROM Reimburse UNION ALL SELECT 'AssetsTransferRecord', COUNT(*) FROM AssetsTransferRecord;"
cp "$DB" "$MIGRATED"
# 新版 MIGRATION_5_6（AppDatabase.kt 原文）：
sqlite3 "$MIGRATED" "CREATE TABLE IF NOT EXISTS \`RecurringRecord\` (\`id\` INTEGER PRIMARY KEY AUTOINCREMENT, \`money\` INTEGER NOT NULL, \`remark\` TEXT, \`record_type_id\` INTEGER NOT NULL, \`record_type\` INTEGER NOT NULL, \`assets_id\` INTEGER NOT NULL, \`cycle_type\` INTEGER NOT NULL, \`cycle_value\` INTEGER NOT NULL, \`start_time\` INTEGER, \`end_time\` INTEGER, \`next_time\` INTEGER NOT NULL, \`last_time\` INTEGER, \`state\` INTEGER NOT NULL, \`create_time\` INTEGER); PRAGMA user_version = 6;"
echo "== 迁移后版本与表 =="
sqlite3 "$MIGRATED" "PRAGMA user_version; SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name;"
echo "== 迁移后数据仍在（Record / Assets / Budget） =="
sqlite3 "$MIGRATED" "SELECT COUNT(*) FROM Record; SELECT id, name, money FROM Assets ORDER BY id; SELECT COUNT(*) FROM Budget;"
echo "== 应用常用查询 =="
echo "-- 当月按分类合计（sumsByType）:"
sqlite3 "$MIGRATED" "SELECT record_type_id AS recordTypeId, COALESCE(SUM(money), 0) AS total FROM \`Record\` WHERE time BETWEEN 1759276800000 AND 1761955200000 GROUP BY record_type_id;"
echo "-- 按账户查账单（recordsByAsset）:"
sqlite3 "$MIGRATED" "SELECT id, money, remark FROM \`Record\` WHERE assets_id = 1 ORDER BY time DESC, id DESC;"
echo "-- 搜索（备注或账户名）:"
sqlite3 "$MIGRATED" "SELECT \`Record\`.id FROM \`Record\` LEFT JOIN \`Assets\` ON \`Record\`.assets_id = \`Assets\`.id WHERE \`Record\`.remark LIKE '%午%' OR \`Assets\`.name LIKE '%午%';"
echo "-- 完整性检查:"
sqlite3 "$MIGRATED" "PRAGMA integrity_check; PRAGMA foreign_key_check;"
echo "OLD_DB_TEST_OK"
