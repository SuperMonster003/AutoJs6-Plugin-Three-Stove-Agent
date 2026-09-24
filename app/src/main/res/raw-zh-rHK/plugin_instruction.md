AI Agent 把一句自然語言目標變成執行 AutoJs6 的 Android 裝置上的實際操作. 它或者從使用者登記給智能代理使用的指令碼中挑選一個, 補齊參數並執行; 或者透過無障礙節點樹觀察畫面, 按觀察, 決策, 操作, 驗證的循環逐步操作, 直到達成目標, 需要使用者確認, 或預算用盡. 它回應 [AutoJs6 討論 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

開發預覽: P6.1 任務台與 P6.2 任務歷史已可用. ai.agent API 需要 AutoJs6 build 5293 或更新版本. 自訂預設及其他介面繼續按 P6.3-P6.7 實施, 穩健性與發佈門檻仍在 P7/P8. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 使用方法

1. 在安裝了 AutoJs6 組建 5289 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `AI Agent` 已被識別並啟用它. 官方發佈套件會自動通過簽名驗證.
3. 開啟 AI Agent 並連接 AutoJs6, 輸入目標, 選擇預設後開始. 在任務卡片中回答詢問或確認操作, 點擊最近任務查看詳情.
4. 在啟動器的 "指令碼目錄" 中設定附加目錄, 每行一個絕對路徑. 儲存後由宿主校驗並套用; 任務只能縮小已批准的目錄範圍.
5. 最多 200 條任務 / 32 MiB. 優先清理最久未查看的已結束任務. 重跑會把原目標和預設填入任務台, 核對後點選開始任務再次執行. 清空歷史會保留執行中的任務. 匯出保留診斷計數, 工具名稱和確認結果. 目標, 參數, 觀察內容及腳本結果會移除. 請選擇檔案儲存位置.

連接指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).
