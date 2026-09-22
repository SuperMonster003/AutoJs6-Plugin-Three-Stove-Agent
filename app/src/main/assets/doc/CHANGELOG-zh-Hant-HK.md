******

### 發行歷史

******

# v1.0.0

###### 2026/09/23

* `提示` P0 開發預覽: 外掛身份, AutoJs6 發現契約與顯示主程式狀態的啟動頁. 智能代理循環, 指令碼目錄, ai.agent API 與任務台尚未實作. 詳見 ROADMAP.md.
* `提示` 宿主端的 AI Agent 契約, 能力與模型代理, 螢幕觀察及腳本登記執行已實作, 插件任務執行功能仍處於開發階段
* `新增` 外掛身份 `ai-agent`, 含 INFO 服務, Wake Activity, 執行於 `:agent` 程序的 `org.autojs.plugin.AI_AGENT` 服務佔位, 以及顯示是否安裝了相容 AutoJs6 主程式的啟動頁
* `新增` 10 種語言的 README, 外掛中心說明與更新日誌
* `依賴` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 主程式組建 6.8.0 / 5282, MPL 2.0) 作為共用外掛契約, 以 SHA-256 鎖定於 `locks/host-api-aars.lock`
