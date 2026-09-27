<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>依自然語言目標在 AutoJs6 中選擇已登記指令碼並逐步操作介面完成任務</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-AI-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ar.md)

******

### 簡介

******

AI Agent 把一句自然語言目標變成執行 AutoJs6 的 Android 裝置上的實際操作. 它或者從使用者登記給智慧代理使用的指令碼中挑選一個, 補齊參數並執行; 或者透過無障礙節點樹觀察畫面, 依觀察, 決策, 操作, 驗證的循環逐步操作, 直到達成目標, 需要使用者確認, 或預算用盡. 它回應 [AutoJs6 討論 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

AI Agent 既是獨立任務台, 也是透過 ai.agent 呼叫的 AutoJs6 外掛. 內建裝置操作及模型呼叫由宿主代理; 可選 MCP 工具只連接使用者配置的伺服器. 不直接繫結模型 Provider, 不申請無障礙權限.

******

### 目前狀態

******

1.2.0 開發版本提供可選 MCP 工具, 原生工具呼叫, 截圖觀察及動態指令碼. AiGoCode gpt-5.6-sol 已通過 P9.2 初始圖片及工具結果圖片測試. P9.1 JSON/原生路徑均已完成 Wi-Fi 開啟及狀態回讀: 測試時暫時關閉目前熱點的自動連線, 模型經行動網路和 VPN 連線. 預設自動連線時 VPN 跨網路切換後的失敗仍未解決. 證據見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

******

### 功能

******

目前實作提供以下能力:

- 指令碼選擇: 透過 `project.json` 或 `@agent` 頭部註解登記的指令碼連同描述與參數 Schema 呈現給模型; 智慧代理挑選指令碼, 補齊參數, 在需要時請求確認, 在 AutoJs6 中執行並讀取結構化結果.
- 介面逐步操作: 智慧代理以緊湊文字形式觀察無障礙節點樹 (安裝了 OCR 外掛時還能讀取畫面文字), 然後經 AutoJs6 能力代理點擊, 輸入, 捲動與按鍵, 直到能夠驗證目標已達成.
- 安全設計: 唯讀工具自動執行; 敏感操作 (付款, 傳送, 刪除, 寫入檔案, shell, 座標手勢, 登記為敏感的指令碼) 預設需要確認; 每次任務都有步數, 模型呼叫次數, 時長與 token 預算. 確認可只允許一次, 也可在本次任務內一律允許; 設定亦提供審慎模式與完全存取, 後者略過確認並有醒目標示.
- 指令碼 API 與使用者介面: `ai.agent.run(goal, options)` 回傳帶事件, 回應與取消的 `AgentRun` 控制代碼; 獨立應用程式提供任務台, 歷史, 預設, 偏好記憶, 設定與發行歷史.
- 經主程式進行原生工具呼叫: 目錄 Schema, 整批參數驗證, 依序執行, 逐項確認, 工具結果接續及步驟記錄共用既有任務規則
- Android 11+ 經 AutoJs6 截圖觀察: screen_capture 縮放到最長邊 1280, JPEG 品質 70, 搭配視覺提示詞, 圖片 token 准入與原生工具結果圖片
- 經 script_run_source 執行生成的 JavaScript: script_dynamic 工具群組預設關閉, 每次展示原始碼摘要及可展開的完整原始碼, 可允許一次或在本次任務內一律允許; 完全存取略過此審閱. 支援逾時, 取消, 結構化結果及私有原始碼記錄. UTF-8 原始碼和其 JSON 字串編碼均限 8 KiB.
- 本機或外部 MCP 伺服器的所選工具, 按伺服器設定風險等級, mcp 工具群組預設關閉
- 統一獨立應用程式的導覽, 卡片, 任務時間軸, 分區清晰的設定與明暗外觀, 預算輸入提供即時驗證

### 介面截圖

以下為 Android API 37.1 上的真實英文介面, 使用專用範例任務及預設回應的示範模型. 圖片用於展示介面, 不作為真實模型任務成功的證據, 不含私人帳號資料. [截圖重現說明](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/README.md).

| 任務台 | 任務詳情 |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/workbench.png?raw=true" alt="任務台" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/detail.png?raw=true" alt="任務詳情" width="288" /> |
| 操作確認 | 懸浮任務輸入 |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="操作確認" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/floating.png?raw=true" alt="懸浮任務輸入" width="288" /> |

******

### 安裝

******

1. 在安裝了 AutoJs6 組建 5293 或更新版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `AI Agent` 已被識別並啟用它. 官方發行套件會自動通過簽章驗證.

安裝並啟用 [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), 在其中設定線上模型或匯入支援的本機模型. 目前宿主模型代理選用 3-Stone AI, 其他 Provider 需要宿主完成整合後才能使用. 在 AI Agent 首頁點按模型膠囊選擇模型. Connected to AutoJs6 僅表示宿主連線狀態.

### 相容性

支援 Android 7.0+ (API 24). 宿主附著要求 AutoJs6 6.8.0 / build 5289+, 完整任務 API 與本快速開始要求 build 5293+. 請使用包含 Agent 改動的宿主版本. 畫面操作需要宿主的無障礙服務; Agent 會先透過 AutoJs6 已設定的免打擾方式 (Root, 安全設定或 Shizuku) 自動啟動, 僅在失敗時提示手動開啟. OCR 為選用能力, 需要安裝並授權 OCR 外掛, 且宿主回報其可用. AI Agent 本身不儲存模型憑證, 不提供獨立無障礙服務.

### 介面快速開始

開啟 AI Agent 並連接 AutoJs6, 輸入目標並開始任務. 首頁的模型膠囊可選擇線上或本機模型, 或選擇自動 (優先本機模型, 否則使用第一個可用模型). 模型清單支援搜尋, 置頂常用模型及重複使用最近使用的模型, 標籤顯示已宣告的工具呼叫與圖片輸入能力. 任務台與懸浮球的新任務共用這個選擇, 不修改預設或正在執行的任務, 預設也不再包含模型. 可選的預設入口位於任務選項內. 在任務卡片中回答問題並查看進度.

### 指令碼快速開始

連接 AI Agent 並設定模型後, 在 AutoJs6 執行以下 JavaScript. 詢問與確認由外掛介面處理. 如需使用已儲存的設定, 在選項加入 `preset: "your-preset-name"`.

```javascript
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

請檢查 `result.status`: Promise 兌現的結果仍可能是 completed, partial, failed, blocked 或 cancelled. `run.cancel()` 可停止任務. 模型目標, 事件, 預算及指令碼處理互動見 [ai.agent API](https://docs.autojs6.com/#ai).

### 登記指令碼

將下例儲存為 AutoJs6 工作目錄或宿主已批准的指令碼目錄中的 `text-counter.js`. 檔案開頭的 `@agent` JSDoc 表示主動登記至目錄. 向 Agent 提出統計指定文字字元數的需求即可, 必填參數缺失時會先詢問.

```javascript
/**
 * @agent
 * @description Count Unicode characters in the supplied text
 * @param {string} text Text to count
 * @risk readonly
 * @confirm never
 * @timeout 10000
 */
let context = ai.agent.context();
if (!context) throw Error('Start this registered script through AI Agent');
let text = new java.lang.String(context.parameters.text);
ai.agent.result({ characters: text.codePointCount(0, text.length()) });
```

亦可在 `main.js` 旁放置以下 `project.json`, main.js 的程式碼與上例相同, 讀取 `ai.agent.context().parameters` 並呼叫 `ai.agent.result(...)`. 專案登記內容放在 `agent` 物件中.

```json
{
  "name": "Text counter",
  "main": "main.js",
  "agent": {
    "id": "text-counter",
    "description": "Count Unicode characters in the supplied text",
    "parameters": {
      "type": "object",
      "properties": { "text": { "type": "string" } },
      "required": ["text"],
      "additionalProperties": false
    },
    "risk": "readonly",
    "confirm": "never",
    "timeoutMs": 10000
  }
}
```

參數類型支援 string, number, integer 和 boolean, 不支援巢狀物件或陣列. sensitive 指令碼執行前需要確認, 選擇完全存取時除外. 請僅登記已審閱的指令碼, 風險宣告不會為 JavaScript 建立沙箱. [完整登記格式](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### 工具目錄

此表由封裝的 ToolCatalog 產生. 實際畫面目標可能提高風險等級, 審慎模式亦會確認所有非唯讀操作, 完全存取則對已啟用工具免確認. 設定, 預設, 任務選項及宿主授權共同限制可用工具組.

| 工具 | 分組 | 風險 | 預設 | 描述 |
| --- | --- | --- | --- | --- |
| `app_launch` | `act` | `NORMAL` | `on` | 按包名或显示名称打开应用. |
| `clipboard_get` | `act` | `READ_ONLY` | `on` | 读取剪贴板文字. |
| `clipboard_set` | `act` | `NORMAL` | `on` | 替换剪贴板文字. |
| `ui_click` | `act` | `NORMAL` | `on` | 点击一个已观察目标. |
| `ui_long_click` | `act` | `NORMAL` | `on` | 长按一个已观察目标. |
| `ui_press_key` | `act` | `NORMAL` | `on` | 执行 Android 导航或通知面板动作. |
| `ui_scroll` | `act` | `NORMAL` | `on` | 对一个已观察目标执行有界次数的滚动. |
| `ui_set_text` | `act` | `NORMAL` | `on` | 在一个已观察的可编辑目标上设置或追加文字. |
| `files_list` | `files` | `NORMAL` | `off` | 列出工作目录文件. |
| `files_read` | `files` | `NORMAL` | `off` | 读取有界工作目录文件文字. |
| `files_stat` | `files` | `NORMAL` | `off` | 读取工作目录文件信息. |
| `files_write` | `files` | `SENSITIVE` | `off` | 确认后写入工作目录文件. |
| `ui_click_xy` | `gesture` | `SENSITIVE` | `off` | 仅在手势组开启并确认后点击坐标. |
| `ui_gesture` | `gesture` | `SENSITIVE` | `off` | 确认后沿有界坐标路径执行手势. |
| `ui_swipe` | `gesture` | `SENSITIVE` | `off` | 确认后在两组坐标间滑动. |
| `memory_get` | `memory` | `READ_ONLY` | `on` | 读取当前作用域可用的偏好记忆. |
| `memory_propose` | `memory` | `SENSITIVE` | `on` | 提议由用户确认保存偏好, 不保存凭据. |
| `app_current` | `observe` | `READ_ONLY` | `on` | 读取当前窗口与应用. |
| `console_tail` | `observe` | `READ_ONLY` | `on` | 读取有界控制台尾部, 其中可能包含无关脚本. |
| `device_info` | `observe` | `READ_ONLY` | `on` | 读取设备信息. |
| `screen_capture` | `observe` | `READ_ONLY` | `auto (vision)` | 文本节点不足时为所选视觉模型捕获已解锁屏幕. 返回缩放后的 JPEG 观察, 不可直接作为设备坐标. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | 读取屏幕是否亮起. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | 在选择动作前观察当前无障碍节点树. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | 查找满足全部选择器条件的节点. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | 在时限内等待选择器目标出现或消失. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | 通过宿主 OCR 插件读取屏幕文字. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | 查找明确登记供智能体使用的脚本. |
| `script_run` | `script` | `NORMAL` | `on` | 按 ID 执行登记脚本, 校验参数并采用登记风险. |
| `script_stop` | `script` | `NORMAL` | `on` | 停止所属脚本执行. |
| `script_run_source` | `script_dynamic` | `SENSITIVE` | `off` | 逐次确认源码后以宿主脚本权限运行生成的 Rhino JavaScript. 无沙箱隔离. 源码含 JSON 转义最多 8192 UTF-8 字节. 使用 ai.agent.result(value) 返回结果. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | 确认后执行有时限的非 Root shell 命令. |
| `report_progress` | `user` | `READ_ONLY` | `on` | 报告有界进度, 不声明任务已完成. |

### 預設與記憶

從任務台開啟 "預設" 儲存任務設定. 名稱是腳本與記憶的固定識別碼, 更名請複製預設. 內建 default 可編輯但不能刪除. 預設不包含模型; 早期版本儲存在預設中的模型只對腳本保留. 任務選項只能進一步收緊預設限制. 固定上下文與任務上下文合計最多 8 KiB. 記憶範圍可選全域及目前預設, 僅其中一種或關閉. 編輯或刪除預設不改變已排入佇列的任務. 私有儲存最多 32 個預設 / 1 MiB.

開啟 "記憶" 檢視, 編輯, 刪除或備份偏好. 最多 500 筆 / 256 KiB, 保留作用域, 來源任務和時間資訊. memory_propose 與匯入的每筆記憶均須單獨確認. 未知預設作用域須先建立對應預設. 自動注入允許範圍內最新的完整項目, 最多 4 KiB; 目前預設的同名 key 覆蓋全域值. memory: false 僅關閉自動注入; 同時禁止查詢和提議請關閉 memory 工具組或選擇無記憶作用域. 匯出包含實際值及來源資訊. 請勿儲存憑證, 可識別的憑證鍵名和權杖格式會被拒絕.

### 使用方式

- 在啟動器的 "指令碼目錄" 中設定附加目錄, 每行一個絕對路徑. 儲存後由宿主校驗並套用; 任務只能縮小已批准的目錄範圍.
- 最多 200 條任務 / 32 MiB. 優先清理最久未查看的已結束任務. 重跑會把原目標和預設填入任務台, 核對後點選開始任務再次執行. 清空歷史會保留執行中的任務. 匯出保留診斷計數, 工具名稱和確認結果. 目標, 參數, 觀察內容及腳本結果會移除. 請選擇檔案儲存位置.
- 前景在任務台回答, 背景從高優先通知開啟對應請求. 確認頁顯示工具, 參數, 風險及剩餘時間. 目前工作階段一律允許會在本次任務結束前放行同一工具的同級風險操作, 亦適用於後續記憶提議或生成原始碼; 付款需另行授權. "記住此答案" 在允許的記憶作用域內產生單獨的 memory_propose 供檢閱. 確認通常等待 120 秒, 詢問最多 10 分鐘, 均受任務預算限制. 逾時回傳 USER_TIMEOUT, 由模型決定再次詢問或回報部分完成. 舊請求無法回答新請求. 背景提醒受通知權限與頻道設定影響.
- 從任務台開啟 "設定", 選擇工具組, 預算, 操作權限 (標準, 審慎或完全存取), 語音輸入及預設組態. 每項修改即時儲存, 對新任務生效. 完全存取讓已啟用的工具 (含付款) 免確認執行, 啟用期間任務台, 懸浮球及歷史詳情會顯示警示標示. gesture/files/shell/script_dynamic 初始關閉, OCR 亦需宿主提供可用且授權的外掛. 任務限制設為自動時沿用初始預設值, 時長以分鐘填寫, 設定值受協定上限約束, 預設與單次參數只能繼續收緊. 資料管理顯示項目數及位元組用量, 按類別清除須確認且不能有執行中的任務; 清除預設後還原內建 default. 預設, 記憶, 腳本目錄與 MCP 伺服器亦可從設定進入.
- 從首頁右上角選單開啟設定. 語言, 深色模式與主題色可跟隨 AutoJs6 或獨立設定, 語言與深色模式也可跟隨系統. 版本歷史和法律聲明內建, 可離線閱讀. 手動 GitHub 更新檢查快取成功結果 24 小時. 自動檢查預設關閉, 開啟後僅在應用程式使用期間每 12 小時最多嘗試一次, 失敗或遇到已忽略版本時保持安靜, 不自動下載 APK. 管理已忽略更新可逐項恢復版本提醒. 關於頁面顯示版本, 開發者, 原始碼, 授權條款與第三方聲明.
- 在設定中開啟懸浮球, 並授權顯示在其他應用程式上層. 預設關閉, 僅在 AutoJs6 已連線時顯示, 鎖定或中斷時隱藏, 閒置時不維持前景服務. 可拖曳調整位置, 點擊輸入目標並選擇預設, 查看詢問或確認, 停止任務. 收起卡片後恢復背景確認通知. 可將純文字分享至 AI Agent, 使用新增任務捷徑, 或在預設頁將預設及選填固定目標固定至主畫面. 所有入口先顯示可編輯草稿, 點擊開始任務才執行. 預設已刪除時不自動改用其他預設. 語音使用跟隨介面語言的系統辨識器, 不可用時隱藏, 結果只填入而不自動傳送.

### 常見問題

**為什麼需要 AutoJs6?**

外掛負責任務循環與介面, AutoJs6 負責模型存取, 無障礙操作及登記指令碼執行. 未連接相容宿主時可檢視歷史, 無法啟動新的裝置任務. 宿主斷開會阻塞活動任務, 重新連接不會自動重播任務.

**付款什麼時候需要確認?**

付款是獨立的敏感操作. 批准下單, 指令碼或其他操作不等於批准付款. 預設情況下每次識別到的付款動作均需單獨確認, 逾時視為拒絕. 在付款確認上選擇目前工作階段一律允許, 只會在本次任務內放行該工具的後續付款. 完全存取會略過付款確認, 請僅在信任目標及模型時啟用. 批准前請核對商家, 商品, 地址及金額.

**本機模型有哪些限制?**

模型能載入不代表任務能成功. 已記錄的 Gemma 4 E2B IT Wi-Fi 決策驗證案例未通過, 該目標保留 JSON 路徑. 原生工具呼叫也需要相容主程式及目標, 並保留參數, 確認和預算檢查. 請從小任務開始, 檢查 partial/failed 結果. 圖片輸入要求支援圖片的目標, AiGoCode gpt-5.6-sol 已通過初始圖片及工具結果圖片測試, 其他目標須個別驗證. 生成指令碼須明確啟用, 每份原始碼均依確認策略處理.

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 契約入口受 org.autojs.permission.PLUGIN 簽章權限保護. 啟動器 (也用於捷徑) 和 text/plain ACTION_SEND 分享目標為公開入口, 只接受有大小限制的目標/預設草稿. 外部 Intent 不能執行任務, 提交確認或改變授權. 設定, 語音結果與任務控制入口均不匯出.
- AI Agent 既是獨立任務台, 也是透過 ai.agent 呼叫的 AutoJs6 外掛. 內建裝置操作及模型呼叫由宿主代理; 可選 MCP 工具只連接使用者配置的伺服器. 不直接繫結模型 Provider, 不申請無障礙權限.
- 模型憑證仍由模型 Provider 保管, 模型呼叫經 AutoJs6. MCP Bearer 權杖使用 Android Keystore 加密後存於私人目錄, 不進入提示詞或歷史匯出. INTERNET 亦用於連接已配置的 MCP 伺服器; Android 17+ 本地網路權限僅從 MCP 設定主動申請. 遠端工具使用使用者為伺服器指定的風險等級, 初始為 SENSITIVE. 取消不回復遠端操作, 呼叫失敗不自動重放.
- 任務歷史, 預設與偏好記憶只儲存在外掛私有儲存空間; 備份與裝置轉移已停用.
- 截圖經 AutoJs6 傳送至所選模型, 該模型可能在線上執行. 截圖要求螢幕已解鎖且處於喚醒狀態. 步驟歷史只儲存尺寸和位元組數等中繼資料, 不儲存圖片內容. JSON 決策保留目前圖片, 直到其他觀察或使用者回答取代它; 原生工作階段在每批和工作階段限額內保留已有圖片, 每輪重新預留相應 token.
- 生成的指令碼以 AutoJs6 權限執行, 不受 JavaScript 沙箱隔離, 可執行已啟用工具群組以外的操作. 完整原始碼保存在私有步驟中, 仍遵守既有密碼遮蔽及歷史保留規則. 後續密碼遮蔽改變的原始碼無法作為原始指令碼儲存. 分享 .js 前請檢查內容.
- 完全存取只能在外掛私有設定中啟用, 模型輸出, 畫面內容, 指令碼請求與外部 Intent 都無法啟用或擴大它. 它對已啟用的工具 (含付款) 免確認, 但不啟用額外工具群組, 也不放寬預算與宿主授權. 無障礙由宿主依 AutoJs6 已設定的方式啟動, 外掛本身仍不申請無障礙權限.

請只從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 頁面或 AutoJs6 外掛中心取得外掛. 來源不明的安裝套件即使版本號相同, 也可能無法通過主程式驗證或帶來風險.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者; 主程式使用這些識別碼探索外掛並協商相容性:

```text
application id: io.github.supermonster003.autojs6.plugin.ai.agent
plugin id: ai-agent
engine: ai-agent
variant: default
service action: org.autojs.plugin.AI_AGENT
service category: ai-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.ai.agent.api.IAiAgentPlugin
minimum host build: 5289 (6.8.0)
```

`AiAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: 經身分驗證的宿主連接, 支援任務排隊, 回應, 取消, 查詢與私有步驟記錄; 宿主斷開時任務阻塞, 程序重建後不會自動繼續.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 依階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.2.0

_2026/09/27_

- `提示` 1.2.0 開發版本提供可選 MCP 工具, 原生工具呼叫, 截圖觀察及動態指令碼. AiGoCode gpt-5.6-sol 已通過 P9.2 初始圖片及工具結果圖片測試. P9.1 JSON/原生路徑均已完成 Wi-Fi 開啟及狀態回讀: 測試時暫時關閉目前熱點的自動連線, 模型經行動網路和 VPN 連線. 預設自動連線時 VPN 跨網路切換後的失敗仍未解決. 證據見 ROADMAP.md.
- `新增` 本機或外部 MCP 伺服器的所選工具, 按伺服器設定風險等級, mcp 工具群組預設關閉
- `新增` 開啟 AI Agent 並連接 AutoJs6, 輸入目標並開始任務. 首頁的模型膠囊可選擇線上或本機模型, 或選擇自動 (優先本機模型, 否則使用第一個可用模型). 模型清單支援搜尋, 置頂常用模型及重複使用最近使用的模型, 標籤顯示已宣告的工具呼叫與圖片輸入能力. 任務台與懸浮球的新任務共用這個選擇, 不修改預設或正在執行的任務, 預設也不再包含模型. 可選的預設入口位於任務選項內. 在任務卡片中回答問題並查看進度.
- `新增` 從首頁右上角選單開啟設定. 外觀, 操作權限, 工具群組, 任務限制 (時長以分鐘計), 語音輸入, 懸浮球與資料清理均即時生效, 無需儲存按鈕. 語言, 深色模式與主題色可跟隨 AutoJs6 或獨立設定, 語言與深色模式也可跟隨系統. 版本歷史和法律聲明內建, 可離線閱讀. 手動 GitHub 更新檢查快取成功結果 24 小時. 自動檢查預設關閉, 開啟後僅在應用程式使用期間每 12 小時最多嘗試一次, 失敗或遇到已忽略版本時保持安靜, 不自動下載 APK. 管理已忽略更新可逐項恢復版本提醒. 關於頁面顯示版本, 開發者, 原始碼, 授權條款與第三方聲明.
- `新增` 介面任務需要無障礙時, 先使用 AutoJs6 中已設定的免打擾啟動方式 (Root, 安全設定或 Shizuku). 僅在自動啟動失敗或未設定時, 任務卡片才提示手動開啟並提供無障礙設定入口.
- `新增` 設定中的操作權限新增完全存取: 已啟用的工具 (含付款, 刪除, 指令碼與記憶寫入) 免確認執行. 它不會開啟額外工具群組, 也不放寬預算或宿主權限. 任務台, 懸浮球, 目前任務與歷史詳情以醒目文字標示, 不彈出打擾對話框. 明確要求審慎確認的任務仍依審慎模式執行.
- `新增` 確認卡片新增目前工作階段一律允許: 本次任務結束前, 同一工具的同級風險操作不再重複確認, 參數變更也適用. 付款操作需另行授權; 動態指令碼與記憶提議也可依工作階段允許.
- `修復` AutoJs6 無障礙服務停止時, 模型收到 A11Y_SERVICE_NOT_RUNNING, 而非參數錯誤
- `優化` 模型憑證仍由模型 Provider 保管, 模型呼叫經 AutoJs6. MCP Bearer 權杖使用 Android Keystore 加密後存於私人目錄, 不進入提示詞或歷史匯出. INTERNET 亦用於連接已配置的 MCP 伺服器; Android 17+ 本地網路權限僅從 MCP 設定主動申請. 遠端工具使用使用者為伺服器指定的風險等級, 初始為 SENSITIVE. 取消不回復遠端操作, 呼叫失敗不自動重放.
- `優化` 統一獨立應用程式的導覽, 卡片, 任務時間軸, 分區清晰的設定與明暗外觀, 預算輸入提供即時驗證
- `相依性` 附加 AndroidX AppCompat 1.7.1 與 Material Components for Android 1.13.0 及其 AndroidX 執行時相依套件, 用於 Material 3 介面

#### v1.1.0

_2026/09/26_

- `提示` 原生呼叫需要 AutoJs6 build 5297+ 及具備 tools 能力的目標, 例如 3-Stone AI 1.2.0 開發候選的線上目標. 舊主程式和不支援的目標保留 JSON 決策. 每個原生工作階段保留初始逾時, 上下文/輸出上限及最多 16 個工具輪次; 工具執行後發生錯誤不會改用 JSON 重新啟動
- `提示` 圖片輸入要求相容宿主, observe 工具群組和明確啟用圖片輸入的視覺模型. 實作與確定性測試已完成, 真實線上視覺驗收仍待補測. 舊系統和純文字目標繼續使用文字觀察. 見 ROADMAP.md
- `提示` 生成的指令碼以 AutoJs6 權限執行, 不受 JavaScript 沙箱隔離, 可執行已啟用工具群組以外的操作. 完整原始碼保存在私有步驟中, 仍遵守既有密碼遮蔽及歷史保留規則. 後續密碼遮蔽改變的原始碼無法作為原始指令碼儲存. 分享 .js 前請檢查內容.
- `新增` 經主程式進行原生工具呼叫: 目錄 Schema, 整批參數驗證, 依序執行, 逐項確認, 工具結果接續及步驟記錄共用既有任務規則
- `新增` Android 11+ 經 AutoJs6 截圖觀察: screen_capture 縮放到最長邊 1280, JPEG 品質 70, 搭配視覺提示詞, 圖片 token 准入與原生工具結果圖片
- `新增` 經 script_run_source 執行生成的 JavaScript: script_dynamic 工具群組預設關閉, 每次均展示原始碼摘要及可展開的完整原始碼並逐次確認. 支援逾時, 取消, 結構化結果及私有原始碼記錄. UTF-8 原始碼和其 JSON 字串編碼均限 8 KiB.
- `相依性` 升級三份宿主 API release 製品至 AutoJs6 52ce694f92 / build 5297, 支援協商圖片輸入, 保留 build 5289+ 的基礎連接契約

#### v1.0.0

_2026/09/25_

- `提示` 1.0.0 提供自然語言任務, 已登記指令碼呼叫與分級確認的裝置操作. 已通過案例, 模型限制與待補裝置驗收見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md). 原生工具呼叫, 視覺輸入與動態指令碼產生預計於 1.1.0 支援.
- `提示` 要求 Android 7+, AutoJs6 6.8.0 / build 5293+ 以使用任務 API, 並啟用已設定模型的 3-Stone AI 外掛. OCR 為選用項目. 僅附著協定的最低宿主為 build 5289+.
- `提示` 相容提示: AutoJs6 build 5297 的原生工具代理擴充與本版本相容. 3-Stone AI 1.2.0 開發候選版已實作線上三協定工具續輪; 本 Agent 版本仍使用結構化 JSON 決策, 原生循環接入與實測比較見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).
- `新增` 自然語言任務台支援內嵌詢問, 進度, 停止及結果, 提供選用懸浮輸入, 文字分享, 預設捷徑與語音草稿
- `新增` ai.agent 指令碼 API 支援建立任務, 事件, 查詢, 回應及取消, 包括 detached 任務與登記指令碼的結果/上下文存取
- `新增` project.json / @agent 登記指令碼支援目錄搜尋, 參數驗證及預設值, 缺失值詢問, 執行確認, 有界執行及結構化結果
- `新增` 透過文字節點及選用的已授權 OCR 觀察畫面, 按節點參照點擊, 輸入, 捲動及按鍵, 並檢查畫面變化與完成證據
- `新增` 經 AutoJs6 宿主代理存取線上及本機模型, 不儲存模型憑證; 已選目標缺失時明確失敗, 不會靜默更換模型
- `新增` 步數, 模型呼叫次數, 時長及 token 預算, 工具時限, 每步最多兩次決策修復重試, 以及重複無效動作保護
- `新增` 命名預設與全域設定支援模型選擇, 上下文, 工具組, 預算, 審慎模式, 指令碼目錄及記憶範圍; gesture/files/shell 預設關閉
- `新增` 分範圍偏好記憶支援提議/匯入逐項確認, 編輯, 刪除及 JSON 備份, 上限 500 條 / 256 KiB; 自動注入上限 4 KiB
- `新增` 任務詳情及時間線, 篩選, 重跑草稿與去識別化 JSON 匯出, 私有歷史上限為 200 個任務 / 32 MiB
- `新增` 任務台, 通知及懸浮卡按風險確認操作; 付款與記憶始終逐次批准; 宿主斷開會阻塞任務, 處理程序重啟不會自動繼續
- `新增` 十語言設定, 離線發行歷史及法律聲明; 手動 GitHub 更新檢查支援取消, 每日快取與忽略版本, 不會自動下載 APK
- `修復` 模型將剩餘預算誤判為已用預算而提前結束任務的問題
- `修復` 表單與篩選控制項觸控區域不足, 選項與指令碼參數欄換行, 以及大字體和 Android 7 下懸浮控制項版面的問題
- `修復` 偏好記憶內容中的全形, 零寬字元及部分憑證名稱可繞過憑證驗證的問題
- `修復` 無安全鎖裝置喚醒時螢幕狀態尚未穩定, 導致任務懸浮球無法恢復顯示的問題
- `修復` 外掛程序中斷的任務在重新啟動後正確記錄為失敗; 鎖定畫面觀察阻止後續螢幕操作
- `修復` 檔案工具在確認或呼叫宿主前拒絕路徑穿越, 絕對路徑及非法工作目錄路徑; 任務歷史保留有界的拒絕分類, 不儲存被拒絕的模型正文
- `修復` 通知確認先返回目標應用程式再恢復操作, 頁面停止後仍處理確認回執, 懸浮回覆送出時收起卡片以釋放焦點
- `修復` Android 13 上啟動應用時, 提前讀取尚未建立視窗的系統列控制器導致當機的問題
- `修復` 最近歷史按任務開始時間排序與保留, 避免重新啟動時重寫存檔導致新任務被舊記錄排除
- `修復` 腳本與外掛介面的詢問和確認回應按 interaction 歸屬驗證, 避免腳本代替外掛介面回應
- `修復` 收銀台的確認交易按鈕未辨識為支付動作的問題, 現逐次確認且不可重用整輪授權
- `修復` 畫面外符合項目的空白或倒置邊界導致查詢誤報參數錯誤的問題, 現保留文字並標記座標不可用
- `修復` 節點重新定位時邊界或操作能力不同的巢狀容器被誤判為同一目標的問題
- `修復` 節點目標的模型修復提示明確保留 # 引用前綴, 使用 selector 時省略 snapshotId
- `修復` 任務接入預載訂單意圖規則, 並減少規則初始化開銷
- `修復` 校驗區分不同視窗中的相同節點, 剪貼簿讀取不會解除介面觀察要求, 檔案傳輸不再誤判為支付任務
- `修復` 動作後的介面回讀無回應時, 穩定等待超過截止時間的問題
- `修復` 主控台拆行和裁剪前處理多行參數遮蔽, 避免參數文字與憑證標籤同名時遺漏憑證
- `修復` 連續啟動任務時, 已退出的前景服務不再誤拒絕下一任務的啟動請求
- `優化` 任務上下文裁剪長歷史時重用未變化的提示詞與觀察片段, 減少每步處理時間
- `優化` 指令碼確認描述按 JSON 跳脫後的大小限制, 避免大參數表超過 Binder 事件上限
- `優化` 最低宿主版本為 AutoJs6 6.8.0 / 組建 5289, 用於動作節點檢查以及確認與執行的綁定
- `相依性` 附加同一 AutoJs6 6.8.0 / 5289 release 建置的 common-plugin-api, host-capability-api 與 ai-agent-api (MPL 2.0), 透過 SHA-256 鎖定
- `相依性` 附加 Gson 版本 2.13.2, 用於有界嚴格 JSON 解析與 Schema 資料樹

##### 更多發行歷史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置與驗證

******

本節面向希望從原始碼建置外掛的開發者; 一般使用者直接安裝 Releases 頁面的預建 APK 即可.

建置 Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

執行 JVM 單元測試並建置 instrumentation 測試 APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

建置 Release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

收集發行產物並在檔案名稱後附加版本與 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

驗證多語言文件來源與產生的產物是否同步 (CI 同樣執行此檢查):

```powershell
py .python\generate_markdown.py --check
```

建置需要 JDK 21 或更新版本以及 Android SDK 37; Gradle 與外掛版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 統一管理.

******

### 在地化與文件產生

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

`.readme/` 與 `.changelog/` 下的語言 JSON 檔案是 README, 外掛中心說明與更新日誌的唯一文案來源. 請始終修改這些 JSON 來源檔案並重新執行 `py .python/generate_markdown.py`; 產生的 README, `plugin_instruction.md` 與更新日誌產物不得手動編輯. 執行 `py .python/generate_markdown.py --check` 可驗證全部產生的產物.

******

### 授權條款

******

專案程式碼基於 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE) 授權. 第三方元件及其授權條款列於 [第三方聲明](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- AutoJs6 討論 #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md
