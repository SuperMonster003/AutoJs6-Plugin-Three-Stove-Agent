AI Agent 把一句自然語言目標變成執行 AutoJs6 的 Android 裝置上的實際操作. 它或者從使用者登記給智慧代理使用的指令碼中挑選一個, 補齊參數並執行; 或者透過無障礙節點樹觀察畫面, 依觀察, 決策, 操作, 驗證的循環逐步操作, 直到達成目標, 需要使用者確認, 或預算用盡. 它回應 [AutoJs6 討論 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

開發預覽: P6.1-P6.3 已提供任務台, 任務歷史與命名預設. ai.agent API 要求 AutoJs6 建置編號不低於 5293. 記憶管理及其餘介面繼續按 P6.4-P6.7 實施, 可靠性與發佈門檻仍在 P7/P8. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 使用方式

1. 在安裝了 AutoJs6 組建 5289 或更新版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `AI Agent` 已被識別並啟用它. 官方發行套件會自動通過簽章驗證.
3. 開啟 AI Agent 並連線 AutoJs6, 輸入目標, 選擇預設後開始. 在任務卡片中回答詢問或確認操作, 點選最近任務查看詳情.
4. 在啟動器的 "指令碼目錄" 中設定附加目錄, 每行一個絕對路徑. 儲存後由宿主校驗並套用; 任務只能縮小已批准的目錄範圍.
5. 最多 200 條任務 / 32 MiB. 優先清理最久未查看的已結束任務. 重跑會把原目標和預設填入任務台, 核對後點選開始任務再次執行. 清空歷史會保留執行中的任務. 匯出保留診斷計數, 工具名稱和確認結果. 目標, 參數, 觀察內容及腳本結果會移除. 請選擇檔案儲存位置.
6. 從任務台開啟 "預設" 儲存任務設定. 名稱是腳本與記憶的固定識別碼, 更名請複製預設. 內建 default 可編輯但不能刪除. 模型從宿主清單選擇, 也可保留自動選擇; 指定模型失效時失敗, 不自動換目標. 任務選項只能進一步收緊預設限制. 固定上下文與任務上下文合計最多 8 KiB. 記憶範圍可選全域及目前預設, 僅其中一種或關閉. 編輯或刪除預設不改變已排入佇列的任務. 私有儲存最多 32 個預設 / 1 MiB.

連線指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).
