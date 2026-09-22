AI Agent 把一句自然語言目標變成執行 AutoJs6 的 Android 裝置上的實際操作. 它或者從使用者登記給智慧代理使用的指令碼中挑選一個, 補齊參數並執行; 或者透過無障礙節點樹觀察畫面, 依觀察, 決策, 操作, 驗證的循環逐步操作, 直到達成目標, 需要使用者確認, 或預算用盡. 它回應 [AutoJs6 討論 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

版本 1.0.0 的外掛執行階段仍為 P0 開發預覽: INFO 服務, Wake Activity, `org.autojs.plugin.AI_AGENT` 佔位服務與宿主狀態啟動頁. 宿主已實作 P1 的契約, 代理, 畫面觀察, 指令碼登記執行及側邊欄和外掛中心入口. 外掛的智慧體循環與指令碼選擇, `ai.agent` API 和任務台仍待後續階段. 最低要求為 AutoJs6 組建 5285; 進度與證據見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 使用方式

1. 在安裝了 AutoJs6 組建 5285 或更新版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `AI Agent` 已被識別並啟用它. 官方發行套件會自動通過簽章驗證.
3. 從啟動器或 AutoJs6 側邊欄的管理入口開啟 AI Agent: 本預覽版只顯示宿主狀態. 任務台和 `ai.agent` API 隨後續階段提供.

連線指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).
