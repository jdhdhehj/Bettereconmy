# BetterEconomy

BetterEconomy 是一個為 Purpur 伺服器設計的經濟插件，提供基本的玩家餘額管理、轉帳、排行榜與交易紀錄功能，並支援跨伺服器同步與 Vault / PlaceholderAPI 整合。

## 功能特色

 - /balance：查看自己的餘額 — 權限：`economy.balance.self`
 - /balance <player>：查看指定玩家餘額 — 權限：`economy.balance.others`
 - /baltop [page]：查看餘額排行榜 — 權限：`economy.baltop`
 - /pay <player> <amount>：轉帳給其他玩家 — 權限：`economy.pay`
- 交易歷史紀錄
- 支援 SQLite / MySQL 儲存
 - /economy give <player> <amount>：增加玩家餘額 — 權限：`economy.admin.give`
 - /economy take <player> <amount>：扣除玩家餘額 — 權限：`economy.admin.take`
 - /economy set <player> <amount>：設定玩家餘額 — 權限：`economy.admin.set`
 - /economy wipe：重置全服經濟資料 — 權限：`economy.admin.wipe`
 - /economy confirm：確認 wipe 操作 — 權限：`economy.admin.wipe`
 - /economy history <player> [page]：查看交易紀錄 — 權限：`economy.admin.history`
 - /economy reload：重新載入設定檔 — 權限：`economy.admin.reload`
- 伺服器核心: Purpur

## 安裝方式

1. 將打包好的插件檔案放入伺服器的 plugins 資料夾。
2. 啟動伺服器一次，讓插件生成預設設定檔。
3. 依據需求修改以下檔案：
   - plugins/BetterEconomy/config.yml
   - plugins/BetterEconomy/messages.yml
4. 重新啟動伺服器。

## 指令列表

### 玩家指令

- /balance：查看自己的餘額
- /balance <player>：查看指定玩家餘額
- /baltop [page]：查看餘額排行榜
- /pay <player> <amount>：轉帳給其他玩家

### 管理員指令

- /economy give <player> <amount>：增加玩家餘額
- /economy take <player> <amount>：扣除玩家餘額
- /economy set <player> <amount>：設定玩家餘額
- /economy wipe：重置全服經濟資料
- /economy confirm：確認 wipe 操作
- /economy history <player> [page]：查看交易紀錄
- /economy reload：重新載入設定檔

## 權限設定

- economy.balance.self
- economy.balance.others
- economy.baltop
- economy.pay
- economy.admin.give
- economy.admin.take
- economy.admin.set
- economy.admin.wipe
- economy.admin.history
- economy.admin.reload

## 設定檔說明

### config.yml

主要包含：

- 資料庫設定（SQLite / MySQL）
- 跨伺服器同步設定
- 經濟系統參數（起始金額、上限、允許負數）
- 轉帳設定
- 排行榜設定
- 交易歷史設定
- Vault / PlaceholderAPI 整合設定

### messages.yml

用於自訂插件訊息，支援色碼與 MiniMessage 格式。

## 建置方式

若你要從原始碼重新打包，請在環境中使用 Java 25，然後執行：

```bash
mvn clean package
```

產生的插件檔案會位於：

- target/better-economy-1.0.0.jar

## 注意事項

- 若使用 MySQL，請先在資料庫中建立對應資料庫。
- 跨伺服器同步功能只有在 MySQL 模式下才會生效。
- 若要使用 Vault 或 PlaceholderAPI 整合，請確保相應依賴已安裝。
- 若使用 Vault 與 LuckPerms，離線玩家的 `economy.exempt.cap` 權限也會正確判斷。

## 授權

本專案採用 MIT / Apache 風格授權，請依照專案 LICENSE 內容使用。
