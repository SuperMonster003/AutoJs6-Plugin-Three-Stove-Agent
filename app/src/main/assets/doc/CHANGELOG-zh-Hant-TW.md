******

### 發行歷史

******

# v1.0.0

###### 2026/09/23

* `提示` P0 開發預覽: 外掛身分, AutoJs6 探索契約與顯示主程式狀態的啟動頁. 智慧代理循環, 指令碼目錄, ai.agent API 與任務台尚未實作. 詳見 ROADMAP.md.
* `提示` 宿主的 AI Agent 契約, 能力與模型代理, 畫面觀察, 指令碼登記執行, 側邊欄與外掛中心入口已實作; 外掛任務執行仍在開發中
* `新增` 外掛身分 `ai-agent`, 含 INFO 服務, Wake Activity, 執行於 `:agent` 程序的 `org.autojs.plugin.AI_AGENT` 服務佔位, 以及顯示是否安裝了相容 AutoJs6 主程式的啟動頁
* `新增` 10 種語言的 README, 外掛中心說明與更新日誌
* `新增` Agent 核心工具目錄, 含 30 個工具, 分組准入, 參數 Schema, bridge 呼叫準備, 有界觀察與敏感風險提升; 執行階段接入隨後續階段提供
* `新增` Agent 決策核心, 含協定 Schema 變體, 嚴格及擷取式 JSON 解析, 工具/分支驗證, 最多兩次修復重試與中英文提示範本; 尚未接入任務執行
* `優化` 最低宿主要求確定為 AutoJs6 6.8.0 / 組建 5285, 與宿主 P1 介面及入口交付版本一致
* `相依性` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 主程式組建 6.8.0 / 5282, MPL 2.0) 作為共用外掛契約, 以 SHA-256 鎖定於 `locks/host-api-aars.lock`
* `相依性` 附加 Gson 版本 2.13.2, 用於有界嚴格 JSON 解析與 Schema 資料樹
