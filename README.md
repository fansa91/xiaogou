# 小狗记账（Xiaogou Bookkeeping）

> 小狗记账是一款 Android 记账应用：记一笔收支、管理多个账户、按分类做月预算、
> 看月度与分类统计；同时支持周期记账、预算提醒，以及加密的 WebDAV 自动备份。

---

## 1. 应用概览

| 项目 | 说明 |
| --- | --- |
| 应用名称 | 小狗记账 |
| 包名 | `com.xiaogou.bookkeeping` |
| 版本 | `5.1.0`（versionCode 33） |
| 运行环境 | minSdk 21 / targetSdk 31 / compileSdk 31 |
| 构建 | AGP 7.4.2 / Kotlin 1.7.20，Gradle 工程 |
| 技术栈 | Java / Kotlin、MVVM（ViewModel + LiveData）、Repository、RxJava2、Room、OkHttp、AlarmManager + BroadcastReceiver，传统 View 体系 |
| 数据存储 | 本地 SQLite / Room 数据库；金额以分为单位存储，界面以元显示 |
| 语言 | 默认、简体中文、繁体中文、英文等多语言资源 |

---

## 2. 功能总览

### 2.1 日常记账

- 记支出、记收入，选分类、选账户、填金额与备注。
- 分类可管理，支持按分类查看与搜索历史记录。
- 记账后同步更新对应账户余额，手动记账与自动生成的记录效果一致。

### 2.2 资产与转账

- 多账户管理：新增账户、查看账户详情与变动记录。
- 账户之间转账，转账记录单独保存。
- 支持报销记录。

### 2.3 预算

- 按分类设置当月预算。
- 每日检查一次当月预算执行情况：
  - 当月该分类支出达到预算的 **80%**，推送「预算即将用尽」；
  - 达到 **100%**，推送「预算超支」。
- 同一预算在同一月内只提醒一次，避免重复打扰。

### 2.4 统计与查询

- 按月、按分类统计收支，可查看分类明细与历史趋势。
- 支持搜索记录。
- 桌面小部件：今日账单、本月饼图、快捷记账等。

### 2.5 周期记账

- 适合房租、会员费、工资等固定收支：建一条规则，到期自动生成一笔记录。
- 入口：设置页 → 「周期记账」。
- 规则列表显示金额、周期、下次执行时间与启用开关，支持编辑与删除。
- 周期类型：每天、每周、每月、每年；新建规则的首次执行时间统一取当天或下一个满足日的 09:00。
- 规则可暂停；暂停期间不生成记录。规则可设生效与结束时间，不设结束时间即长期有效。
- 关机、多日未打开应用导致漏掉的周期会补跑，单次补跑上限 100 次。

### 2.6 备份

- 设置页提供 WebDAV 备份与 Google Drive 备份。
- WebDAV 支持手动备份与每日自动备份；自动备份用 AlarmManager 每天 09:00 在后台执行，重启后由开机接收器重新注册，应用被强行停止后需要重新打开应用才能恢复闹钟。
- WebDAV 备份默认加密上传（见第 4 节），恢复时先解密再还原。
- Google Drive 备份在打开应用时触发。

### 2.7 通知与权限

三个通知渠道，互不干扰：

| 渠道 | 级别 | 用途 |
| --- | --- | --- |
| `channel_backup` | 低 | 备份完成 / 备份失败 |
| `channel_recurring` | 默认 | 周期记账已生成 |
| `channel_budget` | 高 | 预算即将用尽 / 预算超支 |

Android 13+ 需要 `POST_NOTIFICATIONS` 运行时权限，首次打开周期记账页时会申请；
备份结果通知、周期记账通知、预算提醒都依赖该权限。

### 2.8 其他

- 应用锁。
- 分类管理、账户管理等入口集中在设置页。
- 隐私政策地址目前为占位地址（`https://www.example.com/xiaogou/privacy-policy`），
  正式发布前请替换为正式地址。

---

## 3. 周期记账详解

### 3.1 规则表 `RecurringRecord`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | INTEGER PK AUTOINCREMENT | 规则 ID |
| `money` | INTEGER NOT NULL | 金额（分） |
| `remark` | TEXT | 备注 |
| `record_type_id` | INTEGER NOT NULL | 分类 ID |
| `record_type` | INTEGER NOT NULL | 0 = 支出，1 = 收入 |
| `assets_id` | INTEGER NOT NULL | 账户 ID，-1 = 不计入账户 |
| `cycle_type` | INTEGER NOT NULL | 0 = 每天，1 = 每周，2 = 每月，3 = 每年 |
| `cycle_value` | INTEGER NOT NULL | 每周：`Calendar.DAY_OF_WEEK`（1 = 周日 … 7 = 周六）；每月：几号；每年：Mdd，如 1225 表示 12 月 25 日；每天：忽略 |
| `start_time` / `end_time` | INTEGER | 生效 / 结束时间，结束时间为空表示长期有效 |
| `next_time` / `last_time` | INTEGER | 下次 / 上次执行时间 |
| `state` | INTEGER NOT NULL | 0 = 启用，1 = 暂停 |
| `create_time` | INTEGER | 创建时间 |

应用启动时会幂等建表（`CREATE TABLE IF NOT EXISTS`），已建表时不重复建。

### 3.2 执行逻辑

- `RecurringRecordHelper.ensureTable()`：建表，幂等。
- `RecurringRecordHelper.generateDue(now)`：找出 `state = 0 且 next_time <= now` 的规则，
  按应用内记账流程生成 `Record` 并同步更新账户余额，生成后推送通知并推进 `next_time`。
- `computeNextTime(from, cycleType, cycleValue)`：按周期推进下次执行时间；
  `firstOccurrence(...)`：新建规则时计算首次执行时间。
- `RecurringRecordReceiver`：每日 09:00 执行一次，生成到期记录。
- `BudgetNotifyReceiver`：每日 09:00 检查一次当月预算，见 2.3 节；
  同月去重键为 `key_budget_notified_<预算 id>_<yyyyMM>`。
- `RecurringRecordDao`：周期规则的 Room DAO 接口，与 `RecurringRecordHelper` 的 SQL 逻辑对应；
  数据库版本升级时，可把 `RecurringRecord` 注册进 `AppDatabase`
 （`@Database(entities = {..., RecurringRecord}, version = 6)`），迁移 SQL 与建表 SQL 一致，
  升级时已有数据不受影响。

### 3.3 相关文件

```
app/src/main/java/com/xiaogou/pig/database/entity/RecurringRecord.java
app/src/main/java/com/xiaogou/pig/database/dao/RecurringRecordDao.java
app/src/main/java/com/xiaogou/pig/worker/RecurringRecordHelper.java
app/src/main/java/com/xiaogou/pig/worker/RecurringRecordReceiver.java
app/src/main/java/com/xiaogou/pig/worker/BudgetNotifyReceiver.java
app/src/main/java/com/xiaogou/bookkeeping/util/DailyAlarm.java
app/src/main/java/com/xiaogou/bookkeeping/util/BootReceiver.java
app/src/main/java/com/xiaogou/pig/ui/recurring/RecurringRecordActivity.java
app/src/main/res/layout/activity_recurring.xml
app/src/main/res/layout/item_recurring.xml
app/src/main/res/layout/dialog_recurring.xml
```

---

## 4. 自动备份与备份加密详解

### 4.1 相关文件

| 文件 | 作用 |
| --- | --- |
| `bookkeeping/backup/BackupCrypto.java` | 备份加密：AES-256-GCM + PBKDF2WithHmacSHA256 |
| `bookkeeping/backup/BackupManager.java` | WebDAV（Sardine）上传、下载、列目录封装 |
| `bookkeeping/backup/AutoBackupReceiver.java` | 每日自动备份接收器（AlarmManager） |
| `bookkeeping/util/DailyAlarm.java` | 每日 09:00 闹钟、联网/电量检查、开机重注册工具 |
| `bookkeeping/util/BootReceiver.java` | 开机后重新注册三个每日闹钟 |
| `bookkeeping/util/NotifyHelper.java` | 通知渠道与 Android 13+ 权限申请 |

### 4.2 加密格式

- 算法：AES-256-GCM，密钥由用户设置的 WebDAV 密码经 PBKDF2（10 万次迭代）派生，
  无需另外记一个备份密码。
- 文件格式：`magic "XGZ1"` + 16 字节 salt + 12 字节 IV + 密文 + 16 字节认证标签。
- 备份加密密钥就是 WebDAV 密码：服务端与本地必须用同一密码才能解密，
  **忘记密码将无法解密云端备份，请务必妥善保管**。
- 下载恢复时先尝试解密；解密失败的按未加密备份文件处理，直接拷贝还原。

### 4.3 自动备份工作方式

- 任务名：`xiaogou_auto_backup`，每日一次，重复注册时保持已有任务不重复。
- 触发条件：设置页的自动备份开关已开启、WebDAV 已配置、距离上次备份超过 24 小时、已联网、电量不在低位。未联网或电量低时跳过本次，等次日闹钟再试。
- 约束条件：已联网、电量不在低位；失败时线性退避 30 分钟后重试。
- 手动备份：上传前先加密为 `.enc.tmp` 再上传，上传完成后删除临时文件；
  下载后先解密再还原。
- 自动备份开启时注册闹钟，未配置 WebDAV 时取消闹钟；
  应用启动时初始化通知渠道、注册三个每日闹钟、建周期记账表；开机后由 BootReceiver 重新注册闹钟。

---

## 5. 目录结构

```
xiaogou/
├── settings.gradle                    # include ':app'
├── build.gradle                       # AGP 7.4.2 / Kotlin 1.7.20
├── gradle.properties
├── .gitignore
├── README.md                          # 本文档
└── app/
    ├── build.gradle                   # 应用配置（第二种路线：无 Maven 实现依赖，库源码在树内）
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml        # 包名、权限、每日闹钟接收器、开机接收器、RecurringRecordActivity 等
        ├── java/
        │   └── com/xiaogou/
        │       ├── bookkeeping/       # 备份加密、自动备份、通知
        │       │   ├── backup/BackupCrypto.java
        │       │   ├── backup/BackupManager.java
        │       │   ├── backup/AutoBackupReceiver.java
        │       │   ├── util/DailyAlarm.java
        │       │   ├── util/BootReceiver.java
        │       │   └── util/NotifyHelper.java
        │       ├── pig/               # 记账、资产、预算、统计等业务代码
        │       │   ├── database/entity/RecurringRecord.java
        │       │   ├── database/dao/RecurringRecordDao.java
        │       │   ├── worker/        # 周期记账、预算提醒
        │       │   └── ui/recurring/RecurringRecordActivity.java
        │       └── quick/             # 首页等
        └── res/
            ├── layout/activity_recurring.xml
            ├── layout/item_recurring.xml
            ├── layout/dialog_recurring.xml
            └── values/strings.xml     # 含周期记账文案与周期选项数组
```

本地数据库包含记账（`Record`）、分类（`RecordType`）、资产（`Assets`）、
资产变动与转账（`AssetsModifyRecord` / `AssetsTransferRecord`）、
预算（`Budget`）、报销（`Reimburse`）、周期规则（`RecurringRecord`）等表。

---

## 6. 构建说明

### 6.1 构建

在 Android Studio 中打开工程根目录，等待 Gradle 同步完成，即可运行或打包。
本仓库没有提交 Gradle Wrapper，如需命令行构建，请用本地已安装的 Gradle 或
为工程添加 Wrapper 后再构建。

`app/build.gradle` 关键配置（第二种路线）：

```groovy
android {
    compileSdk 31
    namespace "com.xiaogou.pig"  // aapt 生成的 R 类放在这个包下
    defaultConfig {
        applicationId "com.xiaogou.bookkeeping"
        minSdk 21
        targetSdk 31
        versionCode 33
        versionName "5.1.0"
    }
}

dependencies {
    // 无 Maven 实现依赖。
    // 应用代码和第三方库源码都已在源码树里，引入同名 Maven 依赖
    // 会在编译/dex 时报 Duplicate class。
    // 定时任务和通知已改成 AlarmManager + BroadcastReceiver 和
    // 系统 Notification.Builder，不再需要 WorkManager / Startup / Core 的依赖。
}
```

GitHub Actions 构建见 `.github/workflows/android.yml`（JDK 11、Android SDK 31、
Gradle 7.5，编 debug 包并上传为 artifact）。

### 6.2 构建前必须处理的三件事

1. **跑 R 类转换脚本**：`python3 .github/scripts/convert-r-and-fix-q0.py`。
   该脚本删掉所有手写 `R$*.java` 和 `com/xiaogou/bookkeeping/R.java`，
   把 `R$string.xxx`、`R$layout.xxx` 等引用改成 aapt 生成的
   `R.string.xxx`、`R.layout.xxx` 形式，并把 R 类包名统一为
   `com.xiaogou.pig`。该脚本只在 CI 工作区跑，不会把转换后的文件提交回仓库；
   本地用 Android Studio 构建时也需要先跑一次。
2. **修复 `q0/z.java` 的占位写法**：该文件有 8 处 `?? N/K/P = ...` 写法，
   上面那个脚本会统一改成 `LiveData N/K/P = ...`（返回值都是 LiveData，
   见 p0 DAO 接口）。这是全树唯一存在该问题的文件。
3. **签名**：生成自己的签名 keystore 用于 release 包；`applicationId`
   已是 `com.xiaogou.bookkeeping`，建议 `versionCode 33 / versionName 5.1.0`。
   debug 包由 Gradle 自动用 debug keystore 签名，可直接安装测试。

---

## 7. 功能验证清单（真机 / 模拟器）

1. 设置页出现「周期记账」分组 → 点击进入管理页。
2. 添加规则：如「房租 3000 元 / 支出 / 每月 1 号」→ 列表显示「下次执行」时间。
3. 把系统时间调到次日 09:00 后，等待闹钟触发（或用 `adb shell am broadcast -n com.xiaogou.bookkeeping/com.xiaogou.pig.worker.RecurringRecordReceiver` 手动触发）→ 首页出现一笔自动生成的支出记录，账户余额同步扣减，并收到通知。
4. 设置 → WebDAV 备份：开启自动备份 → 手动点备份 → 服务器上的备份文件为加密文件（开头 `XGZ1`）。
5. 卸载重装（或清数据）→ 从 WebDAV 恢复 → 数据完整还原。
6. 预算页设置当月预算 → 记一笔超过 80% 的支出 → 次日收到「预算即将用尽」通知；
   同月不会重复提醒。

---

*文档版本：与小狗记账 5.1.0 源码树同步（2026-10）。*
