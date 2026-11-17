3-Stove Agent 是在手機上代為執行操作的智慧代理. 用一句話描述目標後, 它借助 AutoJs6 查看畫面並逐步點按, 輸入與捲動, 或挑選已登記的指令碼補齊參數後執行, 直到完成目標. 付款, 傳送, 刪除等敏感操作執行前需要確認, 每個任務都有步數與時長上限.

### 開始使用

1. 安裝並啟用 [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), 在其中加入線上模型或匯入本機模型. 3-Stove Agent 透過它呼叫模型.
2. 開啟 3-Stove Agent. 首頁提示未連接時, 點按 "連接 AutoJs6".
3. 點按頂列的模型膠囊選擇模型, 或保留 "自動" (優先使用本機模型).
4. 在輸入列填寫目標, 點按 "開始任務". 任務卡片顯示每一步的進度, 需要回答問題或確認操作時也在此處理.

### 主要功能

- **任務與歷史**: 任務進行中可以查看步驟, 回答詢問或隨時停止. "任務歷史" 保留已結束的任務, 可以分享摘要或再次執行 (再次執行只把原目標填入輸入列, 核對後再開始).
- **確認與操作權限**: 唯讀操作直接執行; 付款, 傳送, 刪除, 寫入檔案等敏感操作在一般設定下先確認, 可以只允許一次或在本次任務內一律允許. 設定中的 "操作權限" 可選 "標準", "審慎" 或 "完全存取" (無需確認, 開啟期間有醒目標示).
- **預設**: 把常用的工具組, 預算與固定上下文儲存為預設, 開始任務前在輸入列的預設標籤中選擇. 開啟 "計畫模式" 後, 任務先提出 3 至 8 步的計畫, 經審閱後再執行.
- **記憶**: 儲存需要智慧代理記住的偏好, 任務開始時自動附帶. 智慧代理提議的每條記憶都需要確認後才會儲存; 請勿在記憶中儲存密碼等憑證.
- **登記指令碼**: 在指令碼開頭加入 `@agent` 註解 (範例見下文) 後, 智慧代理即可挑選並執行該指令碼. 工作目錄以外的指令碼位置在 "指令碼目錄" 中加入.
- **懸浮球**: 在設定中啟用後, 可以在其他應用程式上方輸入目標, 處理確認或停止任務.
- **快速開始**: 把文字分享給 3-Stove Agent, 使用桌面捷徑 "新增任務", 或把預設釘選到桌面. 這些入口只產生草稿, 點按 "開始任務" 後才執行.
- **語音**: 在設定中開啟 "啟用語音輸入" 後, 輸入框旁顯示麥克風; 安裝了 3-Macaw Speech 時直接離線辨識. "常駐監聽" (預設關閉) 辨識到喚醒詞 "小炉小炉" 或 "Hey Stove" 後接收一句任務. 語音結果同樣只成為草稿.
- **更多工具**: 在設定中可以啟用文字辨識 (需要 OCR 外掛), 其他外掛提供的工具 (例如 3-Macaw Speech 的朗讀與轉寫), MCP 伺服器提供的工具, 以及由模型產生且經審閱後才執行的指令碼.

### 在指令碼中使用

在 AutoJs6 指令碼中使用 `ai.agent.run` 發起任務, 詢問與確認仍在 3-Stove Agent 的介面中處理:

```js
let run = ai.agent.run('讀取 Android 版本, 根據實際觀察結果報告.', {
    tools: ['observe', 'user'],
    interaction: 'plugin',
    budget: { maxSteps: 8 },
});
run.on('progress', (event) => console.log(event.message));
run.result.then(
    (result) => console.log(result.status, result.summary),
    (error) => console.error(error.code, error.message),
);
```

- 請檢查結果的 `status`: Promise 兌現時, 結果仍可能是 `completed`, `partial`, `failed`, `blocked` 或 `cancelled`. 呼叫 `run.cancel()` 可以停止任務.
- 在選項中加入 `preset: 'your-preset-name'` 可使用已儲存的預設, 加入 `plan: true` 可讓任務先提出計畫.
- 完整的選項與事件見 [AutoJs6 文件 > ai.agent](https://docs.autojs6.com/#/ai).

如需讓智慧代理執行自訂指令碼, 將下例儲存為 AutoJs6 工作目錄中的 `text-counter.js`. 此後提出統計一段文字字元數的需求即可, 缺少參數時智慧代理會先詢問:

```js
/**
 * @agent
 * @description Count Unicode characters in the supplied text
 * @param {string} text Text to count
 * @risk readonly
 * @confirm never
 * @timeout 10000
 */
let context = ai.agent.context();
if (!context) throw Error('Start this registered script through 3-Stove Agent');
let text = new java.lang.String(context.parameters.text);
ai.agent.result({ characters: text.codePointCount(0, text.length()) });
```

### 執行需求

- Android 7.0 或更高版本, AutoJs6 6.8.0 或更高版本.
- 模型由 [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI) 提供, 3-Stove Agent 本身不儲存模型憑證.
- 畫面操作使用 AutoJs6 的無障礙服務. AutoJs6 已設定 Root, 安全設定或 Shizuku 時會自動開啟, 否則依提示手動開啟.
- 通知權限用於背景確認與任務提醒; 懸浮球需要 "顯示在其他應用程式上層" 權限.

### 常見問題

**需要 AutoJs6 的原因**

3-Stove Agent 負責任務與介面, 模型呼叫, 畫面操作與指令碼執行均經由 AutoJs6 完成. 未連接 AutoJs6 時只能查看歷史, 無法開始新任務; 連線中斷會使進行中的任務受阻, 重新連接後不會自動重播.

**付款需要確認的情形**

在一般設定下每次付款都需要單獨確認, 批准下單或其他操作不等於批准付款, 逾時視為拒絕. "完全存取" 會略過付款確認, 請僅在信任目標與模型時開啟.

**本機模型較慢或無法完成任務的原因**

模型可以載入不代表能夠完成任務, 建議從簡單的任務開始並檢查結果. 較舊的手機上首次呼叫可能超過 5 分鐘的時限, 等待期間任務台顯示已等待的秒數, 逾時後結果會寫明可能的原因與對策.

**常駐監聽停止的原因**

常駐監聽不隨開機啟動, 開啟任務台或設定後恢復. ColorOS 或 MIUI 上需要允許自動啟動並加入電池白名單, 否則常駐服務可能被系統結束; 其他應用程式錄音時也無法辨識喚醒詞.

工具清單, 指令碼登記格式與權限說明見 [專案主頁](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent).
