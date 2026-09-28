<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-stove-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>按自然語言目標在 AutoJs6 中選擇已登記指令碼並逐步操作介面完成任務</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hans.md)
- 繁體中文 (香港) [zh-Hant-HK] # 目前
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ar.md)

******

### 簡介

******

3-Stove Agent 把一句自然語言目標變成執行 AutoJs6 的 Android 裝置上的實際操作. 它或者從使用者登記給智能代理使用的指令碼中挑選一個, 補齊參數並執行; 或者透過無障礙節點樹觀察畫面, 按觀察, 決策, 操作, 驗證的循環逐步操作, 直到達成目標, 需要使用者確認, 或預算用盡. 它回應 [AutoJs6 討論 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

3-Stove Agent 既是獨立任務台, 也是透過 ai.agent 呼叫的 AutoJs6 插件. 內置裝置操作及模型呼叫由宿主代理; 可選 MCP 工具只連接用戶配置的伺服器. 不直接繫結模型 Provider, 不申請無障礙權限.

******

### 目前狀態

******

1.2.0 提供可選 MCP 工具, 原生工具呼叫, 截圖觀察及動態指令碼, 並在五部真機與 API 24 / 35 / 36.1 模擬器上完成驗收. 已知限制: 本機小型模型 (Gemma 4 E2B / E4B) 的決策質素有限; 預設自動連線時 VPN 跨網絡切換後的失敗尚未解決; 視覺跨 UID 的完整任務未驗收, AiGoCode gpt-5.6-sol 只通過了初始圖片及工具結果圖片測試. 證據見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md).

******

### 功能

******

目前實現提供以下能力:

- 指令碼選擇: 透過 `project.json` 或 `@agent` 頭部註解登記的指令碼連同描述與參數 Schema 呈現給模型; 智能代理挑選指令碼, 補齊參數, 在需要時請求確認, 在 AutoJs6 中執行並讀取結構化結果.
- 介面逐步操作: 智能代理以緊湊文字形式觀察無障礙節點樹 (安裝了 OCR 外掛時還能讀取畫面文字), 然後經 AutoJs6 能力代理點擊, 輸入, 捲動與按鍵, 直到能夠驗證目標已達成.
- 安全設計: 唯讀工具自動執行; 敏感操作 (付款, 傳送, 刪除, 寫入檔案, shell, 座標手勢, 登記為敏感的指令碼) 預設需要確認; 每次任務都有步數, 模型呼叫次數, 時長與 token 預算. 確認可只允許一次, 也可在本次任務內一律允許; 設定亦提供審慎模式與完全存取, 後者略過確認並有醒目標示. 支付應用列表與敏感關鍵詞表可在設定的風險識別頁擴展, 內置條目不可移除.
- 指令碼 API 與使用者介面: `ai.agent.run(goal, options)` 回傳帶事件, 回應與取消的 `AgentRun` 句柄; 獨立應用程式提供任務台, 歷史, 預設, 偏好記憶, 設定與發行歷史.
- 經宿主進行原生工具呼叫: 目錄 Schema, 整批參數驗證, 順序執行, 逐項確認, 工具結果續輪及步驟記錄共用既有任務規則
- Android 11+ 經 AutoJs6 截圖觀察: screen_capture 縮放到最長邊 1280, JPEG 質素 70, 配套視覺提示詞, 圖片 token 准入與原生工具結果圖片
- 經 script_run_source 執行生成的 JavaScript: script_dynamic 工具組預設關閉, 每次展示原始碼摘要及可展開的完整原始碼, 可允許一次或在本次任務內一律允許; 完全存取略過此審閱. 支援逾時, 取消, 結構化結果及私有原始碼記錄. UTF-8 原始碼和其 JSON 字串編碼均限 8 KiB.
- 本機或外部 MCP 伺服器的所選工具, 按伺服器設定風險等級, mcp 工具組預設關閉
- 獨立應用程式以 Material 3 重新設計: 主頁為任務流, 輸入欄停靠在鍵盤上方; 頂欄提供模型膠囊, 歷史與選單 (新增任務, 預設, 記憶, 腳本目錄, MCP 伺服器, 設定); 僅在未連接 AutoJs6 時顯示連線提示; 步驟時間線按序號逐步更新; 再次執行只填入輸入欄而不直接開始. 設定分區清晰, 明暗外觀一致

### 介面截圖

以下為 Android API 37.1 上的真實英文介面, 使用專用示例任務及預設回應的示範模型. 圖片用於展示介面, 不作為真實模型任務成功的證據, 不含私人帳戶資料. [截圖重現說明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/README.md).

| 任務台 | 任務詳情 |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/workbench.png?raw=true" alt="任務台" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/detail.png?raw=true" alt="任務詳情" width="288" /> |
| 操作確認 | 懸浮任務輸入 |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="操作確認" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/floating.png?raw=true" alt="懸浮任務輸入" width="288" /> |

******

### 安裝

******

1. 在安裝了 AutoJs6 組建 5293 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases) 安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `3-Stove Agent` 已被識別並啟用它. 官方發佈套件會自動通過簽名驗證.

安裝並啟用 [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), 在其中設定線上模型或匯入支援的本機模型. 目前宿主模型代理選用 3-Stone AI, 其他 Provider 需要宿主完成整合後才能使用. 在 3-Stove Agent 主頁點按模型膠囊選擇模型. 僅在未連接 AutoJs6 時, 主頁才會顯示連線提示.

### 相容性

支援 Android 7.0+ (API 24). 要求 AutoJs6 6.8.0 / build 5298+ 的宿主, 該版本已包含任務 API (build 5293+) 以及原生工具呼叫與圖片輸入 (build 5297+) 所需的全部宿主改動. 畫面操作需要宿主的無障礙服務; Agent 會先透過 AutoJs6 已設定的免打擾方式 (Root, 安全設定或 Shizuku) 自動啟動, 僅在失敗時提示手動開啟. OCR 為可選能力, 需要安裝並授權 OCR 外掛, 且宿主報告其可用. 3-Stove Agent 本身不儲存模型憑證, 不提供獨立無障礙服務.

### 介面快速開始

開啟 3-Stove Agent 並連接 AutoJs6, 輸入目標並開始任務. 主頁的模型膠囊可選擇線上或本機模型, 或選擇自動 (優先本機模型, 否則使用第一個可用模型). 模型清單支援搜尋, 置頂常用模型及重用最近使用的模型, 標籤顯示已聲明的工具呼叫與圖片輸入能力. 任務台與懸浮球的新任務共用這一選擇, 不修改預設或正在執行的任務, 預設亦不再包含模型. 輸入欄中的預設標籤用於選擇可選的預設. 在任務卡片中回答問題並查看進度.

### 指令碼快速開始

連接 3-Stove Agent 並設定模型後, 在 AutoJs6 執行以下 JavaScript. 詢問與確認由外掛介面處理. 如需使用已儲存的設定, 在選項加入 `preset: "your-preset-name"`. 加入 `plan: true` 可讓任務先給出供你審閱的計劃.

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
if (!context) throw Error('Start this registered script through 3-Stove Agent');
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

參數類型支援 string, number, integer 和 boolean, 不支援巢狀物件或陣列. sensitive 指令碼執行前需要確認, 選擇完全存取時除外. 請只登記已審閱的指令碼, 風險聲明不會為 JavaScript 建立沙箱. [完整登記格式](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

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
| `app_installed` | `observe` | `READ_ONLY` | `on` | 检查应用包名是否已安装. |
| `app_list` | `observe` | `READ_ONLY` | `on` | 列出已安装应用, 可按包名或名称片段过滤; 结果有界. |
| `console_tail` | `observe` | `READ_ONLY` | `on` | 读取有界控制台尾部, 其中可能包含无关脚本. |
| `device_info` | `observe` | `READ_ONLY` | `on` | 读取设备信息. |
| `screen_capture` | `observe` | `READ_ONLY` | `auto (vision)` | 文本节点不足时为所选视觉模型捕获已解锁屏幕. 返回缩放后的 JPEG 观察, 不可直接作为设备坐标. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | 读取屏幕是否亮起. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | 在选择动作前观察当前无障碍节点树. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | 查找满足全部选择器条件的节点. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | 在时限内等待选择器目标出现或消失. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | 通过宿主 OCR 插件读取屏幕文字. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | 查找明确登记供智能体使用的脚本. |
| `script_list` | `script` | `READ_ONLY` | `on` | 列出 AutoJs6 中正在运行的脚本执行及其 ID 与状态. |
| `script_run` | `script` | `NORMAL` | `on` | 按 ID 执行登记脚本, 校验参数并采用登记风险. |
| `script_stop` | `script` | `NORMAL` | `on` | 停止所属脚本执行. |
| `script_run_source` | `script_dynamic` | `SENSITIVE` | `off` | 逐次确认源码后以宿主脚本权限运行生成的 Rhino JavaScript. 无沙箱隔离. 源码含 JSON 转义最多 8192 UTF-8 字节. 使用 ai.agent.result(value) 返回结果. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | 确认后执行有时限的非 Root shell 命令. |
| `report_progress` | `user` | `READ_ONLY` | `on` | 报告有界进度, 不声明任务已完成. |

### 預設與記憶

從任務台開啟 "預設" 儲存任務設定. 名稱是腳本與記憶的固定識別碼, 改名請複製預設. 內置 default 可編輯但不能刪除. 預設不包含模型; 早期版本儲存在預設中的模型只對腳本保留. 任務選項只能進一步收緊預設限制. 固定上下文與任務上下文合計最多 8 KiB. 記憶範圍可選全域及目前預設, 僅其中一種或關閉. 編輯或刪除預設不改變已排入佇列的任務. 私有儲存最多 32 個預設 / 1 MiB. 預設可匯出為 JSON, 匯入時逐個審閱; 檔案不含模型, 此裝置沒有的工具組或腳本目錄在匯入時移除. 計劃模式讓模型先提出 3 至 8 步計劃, 你審閱 (可修改) 後再執行, 計劃不再適用時模型會提出新計劃; 預設關閉.

開啟 "記憶" 檢視, 編輯, 刪除或備份偏好. 最多 500 項 / 256 KiB, 保留作用域, 來源任務和時間資訊. memory_propose 與匯入的每項記憶均須單獨確認. 未知預設作用域須先建立對應預設. 自動注入允許範圍內最新的完整項目, 最多 4 KiB; 目前預設的同名 key 覆蓋全域值. memory: false 僅關閉自動注入; 同時禁止查詢和提議請關閉 memory 工具組或選擇無記憶作用域. 匯出包含實際值及來源資訊. 請勿儲存憑據, 可識別的憑據鍵名和權杖格式會被拒絕.

### 使用方法

- 在啟動器的 "指令碼目錄" 中設定附加目錄, 每行一個絕對路徑. 儲存後由宿主校驗並套用; 任務只能縮小已批准的目錄範圍.
- 最多 200 條任務 / 32 MiB. 優先清理最久未查看的已結束任務. 重跑會把原目標和預設填入任務台, 核對後點選開始任務再次執行. 清空歷史會保留執行中的任務. 匯出保留診斷計數, 工具名稱和確認結果. 目標, 參數, 觀察內容及腳本結果會移除. 請選擇檔案儲存位置. 詳情頁的 "分享摘要" 經系統分享面板輸出目標, 狀態, 摘要, 證據與未完成項, 不含觀察內容.
- 前景在任務台回答, 背景從高優先通知開啟對應請求. 確認頁顯示工具, 參數, 風險及剩餘時間. 目前工作階段一律允許會在本次任務結束前放行同一工具的同級風險操作, 亦適用於後續記憶提議或生成原始碼; 付款需另行授權. "記住此答案" 在允許的記憶作用域內產生單獨的 memory_propose 供審閱. 確認通常等待 120 秒, 詢問最多 10 分鐘, 均受任務預算限制. 逾時回傳 USER_TIMEOUT, 由模型決定再次詢問或回報部分完成. 舊請求無法回答新請求. 背景提醒受通知權限與頻道設定影響.
- 從任務台開啟 "設定", 選擇工具組, 預算, 操作權限 (標準, 審慎或完全存取), 語音輸入及預設組態. 每項修改即時儲存, 對新任務生效. 完全存取讓已啟用的工具 (含付款) 免確認執行, 啟用期間任務台, 懸浮球及歷史詳情會顯示警示標示. gesture/files/shell/script_dynamic 初始關閉, OCR 亦需宿主提供可用且獲授權的插件. 任務限制設為自動時沿用初始預設值, 時長以分鐘填寫, 設定值受協議上限約束, 預設與單次參數只能繼續收緊. 資料管理顯示項目數及位元組用量, 按類別清除須確認且不能有執行中的任務; 清除預設後還原內置 default. 預設, 記憶, 腳本目錄與 MCP 伺服器亦可從設定進入. "異常提醒" 可分別開啟通知, 浮動訊息與對話框, 在任務因錯誤, 預算或宿主斷開而停止時提醒. 任務台輸入區的權限標籤可直接切換操作權限, 預設標籤彈出的面板可選擇或管理預設, 右上角選單可開關懸浮球.
- 從主頁右上角選單開啟設定. 語言, 深色模式與主題色可跟隨 AutoJs6 或獨立設定, 語言與深色模式也可跟隨系統. 版本歷史和法律聲明內置, 可離線閱讀. 手動 GitHub 更新檢查快取成功結果 24 小時. 自動檢查預設關閉, 開啟後僅在應用程式使用期間每 12 小時最多嘗試一次, 失敗或遇到已忽略版本時保持安靜, 不自動下載 APK. 管理已忽略更新可逐項恢復版本提醒. 關於頁面顯示版本, 開發者, 原始碼, 授權條款與第三方聲明.
- 在設定中開啟懸浮球, 並授權顯示在其他應用程式上層. 預設關閉, 僅在 AutoJs6 已連線時顯示, 鎖屏或中斷時隱藏, 閒置時不維持前景服務. 可拖動調整位置, 點擊輸入目標並選擇預設, 查看詢問或確認, 停止任務. 收起卡片後恢復背景確認通知. 可將純文字分享至 3-Stove Agent, 使用新增任務捷徑, 或在預設頁將預設及可選固定目標固定至主畫面. 所有入口先顯示可編輯草稿, 點擊開始任務才執行. 預設已刪除時不自動改用其他預設. 語音使用跟隨介面語言的系統識別器, 不可用時隱藏, 結果只填入而不自動傳送.

### 常見問題

**為甚麼需要 AutoJs6?**

外掛負責任務循環與介面, AutoJs6 負責模型存取, 無障礙操作及登記指令碼執行. 未連接相容宿主時可檢視歷史, 無法啟動新的裝置任務. 宿主斷開會阻塞活動任務, 重新連接不會自動重播任務.

**付款甚麼時候需要確認?**

付款是獨立的敏感操作. 批准下單, 指令碼或其他操作不等於批准付款. 預設情況下每次識別到的付款動作均需單獨確認, 逾時視為拒絕. 在付款確認上選擇目前工作階段一律允許, 只會在本次任務內放行該工具的後續付款. 完全存取會略過付款確認, 請僅在信任目標及模型時啟用. 批准前請核對商戶, 商品, 地址及金額.

**本機模型有哪些限制?**

模型能載入不代表任務能成功. 已記錄的 Gemma 4 E2B IT Wi-Fi 決策驗證案例未通過, 該目標保留 JSON 路徑. 原生工具呼叫亦需要相容宿主及目標, 並保留參數, 確認及預算檢查. 請從小任務開始, 檢查 partial/failed 結果. 圖片輸入要求支援圖片的目標, AiGoCode gpt-5.6-sol 已通過初始圖片及工具結果圖片測試, 其他目標須個別驗證. 生成指令碼須明確啟用, 每份原始碼均依確認策略處理.

******

### 權限與安全

******

外掛遵循明確的邊界:

- 權限清單: org.autojs.permission.PLUGIN (宿主契約入口), FOREGROUND_SERVICE 與 FOREGROUND_SERVICE_SPECIAL_USE (任務運行期間的前台服務), POST_NOTIFICATIONS (後台確認與進度通知), INTERNET (手動或自動檢查 GitHub 發行版本, 以及連接用戶配置的 MCP 伺服器), ACCESS_LOCAL_NETWORK (Android 17+ 僅從 MCP 設置主動申請), SYSTEM_ALERT_WINDOW (僅在設置中開啟懸浮球時申請). 不申請無障礙, 存儲或麥克風權限, 模型流量不經過插件.
- Binder 契約入口受 org.autojs.permission.PLUGIN 簽名權限保護. 啟動器 (也用於捷徑) 和 text/plain ACTION_SEND 分享目標為公開入口, 只接受有大小限制的目標/預設草稿. 外部 Intent 不能執行任務, 提交確認或改變授權. 設定, 語音結果與任務控制入口均不匯出.
- 3-Stove Agent 既是獨立任務台, 也是透過 ai.agent 呼叫的 AutoJs6 插件. 內置裝置操作及模型呼叫由宿主代理; 可選 MCP 工具只連接用戶配置的伺服器. 不直接繫結模型 Provider, 不申請無障礙權限.
- 模型憑證仍由模型 Provider 保管, 模型呼叫經 AutoJs6. MCP Bearer 權杖使用 Android Keystore 加密後存於私人目錄, 不進入提示詞或歷史匯出. INTERNET 亦用於連接已配置的 MCP 伺服器; Android 17+ 本地網絡權限僅從 MCP 設定主動申請. 遠端工具使用用戶為伺服器指定的風險等級, 初始為 SENSITIVE. 取消不回復遠端操作, 呼叫失敗不自動重放.
- 任務歷史, 預設與偏好記憶只儲存在外掛私有儲存空間; 備份與裝置轉移已停用.
- 截圖經 AutoJs6 傳送至所選模型, 該模型可能在線上執行. 截圖要求螢幕已解鎖且處於喚醒狀態. 步驟歷史只儲存尺寸和位元組數等中繼資料, 不儲存圖片內容. JSON 決策保留目前圖片, 直到其他觀察或使用者回答取代它; 原生工作階段在每批和工作階段限額內保留已有圖片, 每輪重新預留相應 token.
- 生成的指令碼以 AutoJs6 權限執行, 不受 JavaScript 沙箱隔離, 可執行已啟用工具組以外的操作. 完整原始碼保存在私有步驟中, 仍遵守既有密碼遮蔽及歷史保留規則. 後續密碼遮蔽改變的原始碼無法作為原始指令碼儲存. 分享 .js 前請檢查內容.
- 完全存取只能在插件私有設定中啟用, 模型輸出, 畫面內容, 指令碼請求與外部 Intent 都無法啟用或擴大它. 它對已啟用的工具 (含付款) 免確認, 但不啟用額外工具組, 也不放寬預算與宿主授權. 無障礙由宿主依 AutoJs6 已設定的方式啟動, 插件本身仍不申請無障礙權限.

請只從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases) 頁面或 AutoJs6 外掛中心取得外掛. 來源不明的安裝套件即使版本號相同, 也可能無法通過主程式驗證或帶來風險.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者; 主程式使用這些識別碼發現外掛並協商相容性:

```text
application id: io.github.supermonster003.autojs6.plugin.three.stove.agent
plugin id: three-stove-agent
engine: three-stove-agent
variant: default
service action: org.autojs.plugin.THREE_STOVE_AGENT
service category: three-stove-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentPlugin
minimum host build: 5298 (6.8.0)
```

`ThreeStoveAgentPluginService` / `IThreeStoveAgentPlugin` / `IThreeStoveAgentLink`: 經身份驗證的宿主連接, 支援任務排隊, 回應, 取消, 查詢與私有步驟記錄; 宿主斷開時任務阻塞, 程序重建後不會自動繼續.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 按階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.3.0

_2026/09/28_

- `新增` 風險識別可配置: 設定頁新增風險識別子頁, 可在內置支付應用列表 (支付寶, 支付寶香港, 雲閃付, PayPal, Google 錢包, Samsung Pay, 華為錢包, Mi Pay) 與十語言敏感關鍵詞表之外添加自訂套件名稱和關鍵詞, 只增不刪且即時生效; 命中的屏幕操作提升為敏感操作並進入確認
- `新增` 懸浮球改版: 最小化時顯示任務描述與當前步驟兩行, 點擊文字打開步驟時間線卡; 展開的控制頁與主頁一致地壓縮為兩行 (預設 / 模型 / 訪問權限, 輸入欄 / 語音 / 開始), 模型與訪問權限直接在懸浮窗內切換並與主應用共用同一選擇; 時間線在讀者位於底部時自動跟隨新步驟, 上滑查看歷史時暫停, 回到底部後恢復, 任務詳情頁同樣如此
- `新增` 預設匯入 / 匯出: 預設頁新增 "匯入 JSON" 與 "匯出 JSON", 匯出檔案包含全部預設的設定但不含模型; 匯入時逐個審閱, 同名預設顯示取代提示, 此裝置沒有的工具組或腳本目錄在匯入時移除, 審閱中途離開後可恢復
- `新增` 觀察工具補齊: 新增唯讀工具 app_list (列出已安裝應用, 可按套件名稱或名稱片段過濾, 最多回傳 200 條), app_installed (檢查套件名稱是否已安裝) 與 script_list (列出 AutoJs6 中正在執行的腳本執行及其 ID 與狀態, 配合 script_stop 使用); 三者分別對應宿主 grant 早已允許的 package_manager.listApps, app.isInstalled 與 engines.list, 歸入觀察與腳本工具組並預設開啟
- `新增` 計劃模式 (預設開關, 預設關閉; 腳本可用 options.plan 覆蓋): 開啟後模型先給出 3 至 8 步計劃, 任務台 / 懸浮球 / 確認頁顯示可編輯的計劃審閱卡, 批准後運行時把計劃隨每次提示傳給模型並要求按序執行, 計劃不再適用時模型提出新計劃再次審閱; 決策 Schema 新增僅計劃模式接受的 plan 分支, 時間線與歷史記錄計劃步驟
- `新增` 任務結果分享: 任務詳情頁選單新增 "分享摘要", 經系統分享面板輸出目標, 狀態與摘要, 證據和未完成項的純文字; 觀察內容, 參數, 腳本結果與錯誤詳情不會離開私有歷史, 任務未結束時該項不可用
- `修復` 輸入欄多於一行時發送按鈕不再停在首行, 與麥克風按鈕一樣貼底對齊
- `優化` 去除換個模型重試按鈕 (模型統一在任務台頂部或懸浮球內切換); 懸浮球的任務歷史與打開任務台收進更多選單; 歷史圖標改為標準樣式
- `優化` MCP 伺服器發出 tools/list_changed 後, 下一次呼叫前重新核對已凍結的工具定義: 定義未變則繼續執行, 變了才以 MCP_CATALOG_CHANGED 失敗並提示模型請用戶在 MCP 設定中重新整理工具選擇後重新開始任務; 端點說明補充不支援 OAuth 登入與舊的 HTTP+SSE 傳輸

#### v1.2.0

_2026/09/28_

- `提示` 應用已更名為 3-Stove Agent: 應用 ID 改為 io.github.supermonster003.autojs6.plugin.three.stove.agent, 倉庫改為 AutoJs6-Plugin-Three-Stove-Agent, 插件 ID 與 engine 改為 three-stove-agent, 服務 action 改為 org.autojs.plugin.THREE_STOVE_AGENT, 契約版本升為 2. 不相容舊名稱: 需先卸載舊的 AI Agent 再安裝, 歷史, 預設與記憶不遷移; 最低宿主版本提升為 AutoJs6 6.8.0 / build 5298, 更早的宿主不再識別本插件
- `提示` 1.2.0 提供可選 MCP 工具, 原生工具呼叫, 截圖觀察及動態指令碼, 並在五部真機與 API 24 / 35 / 36.1 模擬器上完成驗收. 已知限制: 本機小型模型 (Gemma 4 E2B / E4B) 的決策質素有限; 預設自動連線時 VPN 跨網絡切換後的失敗尚未解決; 視覺跨 UID 的完整任務未驗收, AiGoCode gpt-5.6-sol 只通過了初始圖片及工具結果圖片測試. 證據見 ROADMAP.md.
- `新增` 任務台輸入區顯示當前操作權限 (標準 / 審慎 / 完全存取, 僅完全存取為紅色), 點擊即可切換; 右上角選單新增 "懸浮球" 核取項並與設定同步; 懸浮球展開時點擊其他位置即收起, 卡片按內容定高, 新增 "更多" 按鈕提供最小化與退出; 輸入框單行時游標與語音, 發送按鈕垂直置中, 多行時按鈕保持底部對齊
- `新增` 任務台預設面板: 點擊預設標籤彈出底部面板, 可選擇預設, 也可就地新建, 編輯, 複製, 設為預設或刪除 (刪除需確認), 並可進入完整的預設管理頁
- `新增` 異常提醒: 設定頁新增 "異常提醒", 可分別開啟通知 (預設開), 浮動訊息與對話框; 任務因錯誤, 預算上限或宿主斷開而停止時由後台進程提醒, 通知可直接開啟任務詳情, 對話框在 Android 10+ 需懸浮窗權限, 否則改用通知; 已完成與已取消的任務不提醒
- `新增` 本機或外部 MCP 伺服器的所選工具, 按伺服器設定風險等級, mcp 工具組預設關閉
- `新增` 開啟 3-Stove Agent 並連接 AutoJs6, 輸入目標並開始任務. 主頁的模型膠囊可選擇線上或本機模型, 或選擇自動 (優先本機模型, 否則使用第一個可用模型). 模型清單支援搜尋, 置頂常用模型及重用最近使用的模型, 標籤顯示已聲明的工具呼叫與圖片輸入能力. 任務台與懸浮球的新任務共用這一選擇, 不修改預設或正在執行的任務, 預設亦不再包含模型. 輸入欄中的預設標籤用於選擇可選的預設. 在任務卡片中回答問題並查看進度.
- `新增` 從主頁右上角選單開啟設定. 外觀, 操作權限, 工具組, 任務限制 (時長以分鐘計), 語音輸入, 懸浮球與資料清理均即時生效, 無需儲存按鈕. 語言, 深色模式與主題色可跟隨 AutoJs6 或獨立設定, 語言與深色模式也可跟隨系統. 版本歷史和法律聲明內置, 可離線閱讀. 手動 GitHub 更新檢查快取成功結果 24 小時. 自動檢查預設關閉, 開啟後僅在應用程式使用期間每 12 小時最多嘗試一次, 失敗或遇到已忽略版本時保持安靜, 不自動下載 APK. 管理已忽略更新可逐項恢復版本提醒. 關於頁面顯示版本, 開發者, 原始碼, 授權條款與第三方聲明.
- `新增` 介面任務需要無障礙時, 先使用 AutoJs6 中已設定的免打擾啟動方式 (Root, 安全設定或 Shizuku). 僅在自動啟動失敗或未設定時, 任務卡片才提示手動開啟並提供無障礙設定入口.
- `新增` 設定中的操作權限新增完全存取: 已啟用的工具 (含付款, 刪除, 指令碼與記憶寫入) 免確認執行. 它不會開啟額外工具組, 也不放寬預算或宿主權限. 任務台, 懸浮球, 目前任務與歷史詳情以醒目文字標示, 不彈出打擾對話框. 明確要求審慎確認的任務仍依審慎模式執行.
- `新增` 確認卡片新增目前工作階段一律允許: 本次任務結束前, 同一工具的同級風險操作不再重複確認, 參數變更也適用. 付款操作需另行授權; 動態指令碼與記憶提議也可依工作階段允許.
- `修復` 連續讀取較大頁面或進行多輪原生工具呼叫後任務中途停止的問題; 現在保留已完成步驟並壓縮舊上下文後繼續, 確認策略, 預算與逾時限制保持有效
- `修復` 宿主連接橫幅在 360 dp 寬的手機上放大到 2 倍字號時, "連接 AutoJs6" 按鈕被擠成每行一個字 (Redmi Note 12 與 Xperia XZ1 Compact 實測); 橫幅的兩個操作按鈕並排放不下時改為上下排列
- `修復` 預設編輯器的預算說明仍寫着放寬前的預設值 (40 步, 60 次模型調用, 600000 毫秒, 300000 token); 現在與自動預算一致 (60 步, 90 次, 15 分鐘, 託管任務 30 分鐘, 500000 token), 時長以分鐘表示
- `修復` 任務台的操作權限標籤在首次狀態到達前沒有無障礙名稱, 讀屏器只能讀到一個無名按鈕 (遠端 API 35 版面審計暴露); 現在先以 "操作權限" 命名, 狀態到達後再改為具體權限值
- `修復` 任務停止原因更具體: 終態摘要附帶方括號說明, 預算類給出維度與已用/上限 (如步數 60/60, 任務時長 900 s/900 s), 超限類說明是模型回覆過大, 上下文超出模型輸入上限還是工具結果批次過大, 其他錯誤附帶錯誤碼與宿主回傳的固定失敗原因 (如 MODEL_FAILED: ONLINE_NETWORK_UNAVAILABLE); 此前除 REQUEST_REJECTED 外的宿主原因被丟棄
- `修復` 記憶列表中較長的範圍名稱不再把條目鍵擠出行外: 範圍徽章單行省略並保留完整名稱的無障礙描述 (API 24 / 360 dp 寬度下的 CI 佈局審計暴露)
- `修復` 內部異常導致任務失敗時, 步驟記錄保留異常類名 (不含訊息) 以便診斷; 工具超時終止時結果註明工具時長上限維度; MCP 工具發現限時 8 秒, 不再擠佔 15 秒的任務準備窗口
- `修復` 插件能力聲明補齊 native-tools 與 vision, 運行上限常量直接綁定宿主契約, MCP 客戶端版本號取自安裝包資訊, 分散的超時與體積字面量統一引用契約常量
- `修復` AutoJs6 無障礙服務停止時, 模型收到 A11Y_SERVICE_NOT_RUNNING, 而非參數錯誤
- `優化` 管理頁細節統一: 預設編輯器的固定上下文獨立成 "上下文" 一節; MCP 伺服器編輯器改為與預設, 記憶編輯器一致的底部操作欄 (刪除 / 儲存); 任務歷史的保留說明與其他頁面同一樣式; 懸浮卡頭部與正文之間留出間距
- `優化` 任務詳情的模型, 預設, 耗時與任務預算改為對齊的鍵值兩欄, 長值在標籤欄旁換行, 與確認卡的參數表同一風格
- `優化` 任務台目前任務卡改為單行狀態: 狀態按色調著色 (執行中強調色, 完成綠, 失敗紅, 部分完成琥珀), 同行顯示模型與預設, 預算改為緊湊的 "步驟 n/m · 模型呼叫 · 分鐘 · token" 一行; 任務詳情的 "再次執行" 為整行主操作, "換個模型重試" 單獨一行不再換行
- `優化` 自動任務預算放寬: 步數 40 -> 60, 模型呼叫 60 -> 90, 時長 10 -> 15 分鐘, token 300k -> 500k; 設定, 預設與單次任務仍只能收緊
- `優化` 啟動器圖標改為維護者提供的 Three Stove 圖案: 亮色模式為淺灰底深色圖案, 暗色模式為深灰底淺色圖案, 圓形與自適應圖標由同一源圖合成
- `優化` 懸浮球步驟標籤按可截斷角色提供完整文本給讀屏器, 模型置頂已滿的提示改為頁內提示條, 啟動器聲明圓形圖標; 界面工具集移除未使用的成員並統一正文與說明文本構建
- `優化` 工具目錄以 confirmAlways 屬性聲明記憶提議與生成腳本的強制確認, 內置工具名統一經 ToolNames 常量引用並由快照測試與目錄對齊
- `優化` 模型憑證仍由模型 Provider 保管, 模型呼叫經 AutoJs6. MCP Bearer 權杖使用 Android Keystore 加密後存於私人目錄, 不進入提示詞或歷史匯出. INTERNET 亦用於連接已配置的 MCP 伺服器; Android 17+ 本地網絡權限僅從 MCP 設定主動申請. 遠端工具使用用戶為伺服器指定的風險等級, 初始為 SENSITIVE. 取消不回復遠端操作, 呼叫失敗不自動重放.
- `優化` 獨立應用程式以 Material 3 重新設計: 主頁為任務流, 輸入欄停靠在鍵盤上方; 頂欄提供模型膠囊, 歷史與選單 (新增任務, 預設, 記憶, 腳本目錄, MCP 伺服器, 設定); 僅在未連接 AutoJs6 時顯示連線提示; 步驟時間線按序號逐步更新; 再次執行與換個模型重試只填入輸入欄而不直接開始. 設定分區清晰, 明暗外觀一致
- `優化` 確認卡片以可讀表格顯示風險等級, 工具組和全部參數, 不再顯示原始 JSON; 允許一次, 目前工作階段一律允許與拒絕三個操作清晰區分. 懸浮球採用相同的 Material 設計, 預設在卡片內直接選擇, 模型一行可開啟共用的模型切換器
- `優化` 任務歷史新增搜尋, 狀態標籤, 預設與日期範圍篩選, 可從選單清除已結束的任務. 任務詳情顯示所用模型, 附參數表格與可展開觀察內容的步驟時間線, 提供再次執行與換個模型重試, 選單中可匯出診斷, 刪除記錄或將該任務的模型用於新任務
- `優化` 預設, 記憶, MCP 伺服器與腳本目錄採用統一設計: 預設以卡片呈現並提供列選單, 編輯器為整頁 (時長以分鐘計, 儲存按鈕固定在底部); 記憶支援搜尋與作用域標籤; MCP 提供啟用開關, 風險選擇與工具清單; 離開未儲存的修改前會先確認
- `依賴` 升級三份宿主 API release 製品至 AutoJs6 86d9bfa26b / build 5298: ai-agent-api 改為 three-stove-agent-api (AIDL 套件 org.autojs.plugin.three.stove.agent.api, 契約版本 2), common-plugin-api 與 host-capability-api 從同一建置一併換鎖
- `依賴` 升級三份宿主 API release 製品至 AutoJs6 3cdf7de13c / build 5297 (P10 的 mcp 工具組選項與 TOOL_FAILED 常量), 基礎契約仍為 V1
- `依賴` 附加 AndroidX AppCompat 1.7.1 與 Material Components for Android 1.13.0 及其 AndroidX 執行時依賴, 用於 Material 3 介面

#### v1.1.0

_2026/09/26_

- `提示` 1.1.0 未單獨發佈, 其全部內容隨 1.2.0 一併發佈
- `提示` 原生呼叫需要 AutoJs6 build 5297+ 及具備 tools 能力的目標, 例如 3-Stone AI 1.2.0 開發候選的線上目標. 舊宿主和不支援的目標保留 JSON 決策. 每個原生工作階段保留初始逾時, 上下文/輸出上限及最多 16 個工具輪次; 工具執行後發生錯誤不會改用 JSON 重新啟動
- `提示` 圖片輸入要求兼容宿主, observe 工具組和明確啟用圖片輸入的視覺模型. 實作與確定性測試已完成, 真實線上視覺驗收仍待補測. 舊系統和純文字目標繼續使用文字觀察. 見 ROADMAP.md
- `提示` 生成的指令碼以 AutoJs6 權限執行, 不受 JavaScript 沙箱隔離, 可執行已啟用工具組以外的操作. 完整原始碼保存在私有步驟中, 仍遵守既有密碼遮蔽及歷史保留規則. 後續密碼遮蔽改變的原始碼無法作為原始指令碼儲存. 分享 .js 前請檢查內容.
- `新增` 經宿主進行原生工具呼叫: 目錄 Schema, 整批參數驗證, 順序執行, 逐項確認, 工具結果續輪及步驟記錄共用既有任務規則
- `新增` Android 11+ 經 AutoJs6 截圖觀察: screen_capture 縮放到最長邊 1280, JPEG 質素 70, 配套視覺提示詞, 圖片 token 准入與原生工具結果圖片
- `新增` 經 script_run_source 執行生成的 JavaScript: script_dynamic 工具組預設關閉, 每次均展示原始碼摘要及可展開的完整原始碼並逐次確認. 支援逾時, 取消, 結構化結果及私有原始碼記錄. UTF-8 原始碼和其 JSON 字串編碼均限 8 KiB.
- `依賴` 升級三份宿主 API release 製品至 AutoJs6 52ce694f92 / build 5297, 支援協商圖片輸入, 保留 build 5289+ 的基礎連接契約

##### 更多發行歷史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

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

收集發佈產物並在檔案名稱後附加版本與 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

驗證多語言文件來源與生成產物是否同步 (CI 同樣執行此檢查):

```powershell
py .python\generate_markdown.py --check
```

建置需要 JDK 21 或更高版本以及 Android SDK 37; Gradle 與外掛版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 統一管理.

******

### 本地化與文件生成

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

`.readme/` 與 `.changelog/` 下的語言 JSON 檔案是 README, 外掛中心說明與更新日誌的唯一文案來源. 請始終修改這些 JSON 來源檔案並重新執行 `py .python/generate_markdown.py`; 生成的 README, `plugin_instruction.md` 與更新日誌產物不得手動編輯. 執行 `py .python/generate_markdown.py --check` 可驗證全部生成產物.

******

### 授權條款

******

專案程式碼基於 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/LICENSE) 授權. 第三方元件及其授權條款列於 [第三方聲明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- AutoJs6 討論 #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/THIRD_PARTY_NOTICES.md
