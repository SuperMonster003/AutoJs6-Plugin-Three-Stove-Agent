AI Agent 把一句自然語言目標變成執行 AutoJs6 的 Android 裝置上的實際操作. 它或者從使用者登記給智能代理使用的指令碼中挑選一個, 補齊參數並執行; 或者透過無障礙節點樹觀察畫面, 按觀察, 決策, 操作, 驗證的循環逐步操作, 直到達成目標, 需要使用者確認, 或預算用盡. 它回應 [AutoJs6 討論 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

版本 1.0.0 是路線圖的 P0 開發預覽: 外掛身份, AutoJs6 發現契約 (INFO 服務, Wake Activity 與 `org.autojs.plugin.AI_AGENT` 服務佔位) 以及一個顯示主程式狀態的啟動頁. 智能代理循環, 指令碼目錄, `ai.agent` API 與任務台尚未實作; 進度與證據記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md). 外掛將要求 AutoJs6 組建 5283 或更高版本.

### 使用方法

1. 在安裝了 AutoJs6 組建 5283 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `AI Agent` 已被識別並啟用它. 官方發佈套件會自動通過簽名驗證.
3. 從啟動器開啟 AI Agent: 本預覽版的頁面只顯示是否安裝了相容的 AutoJs6 主程式. 任務台, 抽屜項目與 `ai.agent` API 隨後續路線圖階段提供.

連接指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).
