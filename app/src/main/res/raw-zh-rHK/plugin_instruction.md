AI Agent 把一句自然語言目標變成執行 AutoJs6 的 Android 裝置上的實際操作. 它或者從使用者登記給智能代理使用的指令碼中挑選一個, 補齊參數並執行; 或者透過無障礙節點樹觀察畫面, 按觀察, 決策, 操作, 驗證的循環逐步操作, 直到達成目標, 需要使用者確認, 或預算用盡. 它回應 [AutoJs6 討論 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

開發預覽: 登記指令碼, 畫面操作與 ai.agent 任務 API 已接通. 任務 API 需要 AutoJs6 組建 5293 或更高版本; 完整任務台繼續按 P6 實施, 穩定性驗收繼續按 P7 實施. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 使用方法

1. 在安裝了 AutoJs6 組建 5289 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `AI Agent` 已被識別並啟用它. 官方發佈套件會自動通過簽名驗證.
3. 啟動器支援請求宿主連接, 15 秒逾時後引導在 AutoJs6 啟用 AI Agent 並授權.
4. 在啟動器的 "指令碼目錄" 中設定附加目錄, 每行一個絕對路徑. 儲存後由宿主校驗並套用; 任務只能縮小已批准的目錄範圍.

連接指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).
