<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>按自然語言目標在 AutoJs6 中選擇已登記指令碼並逐步操作介面完成任務</p>

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
- 繁體中文 (香港) [zh-Hant-HK] # 目前
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-TW.md)
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

AI Agent 把一句自然語言目標變成執行 AutoJs6 的 Android 裝置上的實際操作. 它或者從使用者登記給智能代理使用的指令碼中挑選一個, 補齊參數並執行; 或者透過無障礙節點樹觀察畫面, 按觀察, 決策, 操作, 驗證的循環逐步操作, 直到達成目標, 需要使用者確認, 或預算用盡. 它回應 [AutoJs6 討論 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

外掛既是 AutoJs6 外掛, 也是獨立應用程式. 指令碼透過 AutoJs6 的 `ai.agent` API 使用它; 使用者透過它自己的任務台, AutoJs6 抽屜項目, 懸浮球, 系統分享面板, 應用程式捷徑和語音輸入使用它. 模型呼叫與裝置操作始終經 Binder 交給 AutoJs6: 主程式借給外掛一個模型代理 (主程式已知的 AI Provider 外掛, 例如 3-Stone AI) 和一個帶有限 grant 的能力代理. 外掛從不持有憑證, 從不自行綁定模型提供方, 也不申請無障礙權限.

******

### 目前狀態

******

開發預覽: P6.1-P6.4 已提供任務台, 歷史, 預設與偏好記憶. ai.agent API 要求 AutoJs6 構建編號不低於 5293. 其餘介面繼續按 P6.5-P6.7 實施, 可靠性與發佈門檻仍在 P7/P8. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

******

### 功能

******

1.0.0 計劃提供以下能力:

- 指令碼選擇: 透過 `project.json` 或 `@agent` 頭部註解登記的指令碼連同描述與參數 Schema 呈現給模型; 智能代理挑選指令碼, 補齊參數, 在需要時請求確認, 在 AutoJs6 中執行並讀取結構化結果.
- 介面逐步操作: 智能代理以緊湊文字形式觀察無障礙節點樹 (安裝了 OCR 外掛時還能讀取畫面文字), 然後經 AutoJs6 能力代理點擊, 輸入, 捲動與按鍵, 直到能夠驗證目標已達成.
- 安全設計: 唯讀工具自動執行; 敏感操作 (付款, 傳送, 刪除, 寫入檔案, shell, 座標手勢, 登記為敏感的指令碼) 需要確認; 每次任務都有步數, 模型呼叫次數, 時長與 token 預算.
- 指令碼 API 與使用者介面: `ai.agent.run(goal, options)` 回傳帶事件, 回應與取消的 `AgentRun` 句柄; 獨立應用程式提供任務台, 歷史, 預設, 偏好記憶, 設定與發行歷史.

### 工具目錄

開發預覽: 登記指令碼, 畫面操作與 ai.agent 任務 API 已接通. 任務 API 需要 AutoJs6 組建 5293 或更高版本; 完整任務台繼續按 P6 實施, 穩定性驗收繼續按 P7 實施.

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
| `screen_state` | `observe` | `READ_ONLY` | `on` | 读取屏幕是否亮起. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | 在选择动作前观察当前无障碍节点树. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | 查找满足全部选择器条件的节点. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | 在时限内等待选择器目标出现或消失. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | 通过宿主 OCR 插件读取屏幕文字. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | 查找明确登记供智能体使用的脚本. |
| `script_run` | `script` | `NORMAL` | `on` | 按 ID 执行登记脚本, 校验参数并采用登记风险. |
| `script_stop` | `script` | `NORMAL` | `on` | 停止所属脚本执行. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | 确认后执行有时限的非 Root shell 命令. |
| `report_progress` | `user` | `READ_ONLY` | `on` | 报告有界进度, 不声明任务已完成. |

******

### 使用方法

******

1. 在安裝了 AutoJs6 組建 5289 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `AI Agent` 已被識別並啟用它. 官方發佈套件會自動通過簽名驗證.
3. 開啟 AI Agent 並連接 AutoJs6, 輸入目標, 選擇預設後開始. 在任務卡片中回答詢問或確認操作, 點擊最近任務查看詳情.
4. 在啟動器的 "指令碼目錄" 中設定附加目錄, 每行一個絕對路徑. 儲存後由宿主校驗並套用; 任務只能縮小已批准的目錄範圍.
5. 最多 200 條任務 / 32 MiB. 優先清理最久未查看的已結束任務. 重跑會把原目標和預設填入任務台, 核對後點選開始任務再次執行. 清空歷史會保留執行中的任務. 匯出保留診斷計數, 工具名稱和確認結果. 目標, 參數, 觀察內容及腳本結果會移除. 請選擇檔案儲存位置.
6. 從任務台開啟 "預設" 儲存任務設定. 名稱是腳本與記憶的固定識別碼, 改名請複製預設. 內置 default 可編輯但不能刪除. 模型從宿主清單選擇, 也可保留自動選擇; 指定模型失效時失敗, 不自動換目標. 任務選項只能進一步收緊預設限制. 固定上下文與任務上下文合計最多 8 KiB. 記憶範圍可選全域及目前預設, 僅其中一種或關閉. 編輯或刪除預設不改變已排入佇列的任務. 私有儲存最多 32 個預設 / 1 MiB.
7. 開啟 "記憶" 檢視, 編輯, 刪除或備份偏好. 最多 500 項 / 256 KiB, 保留作用域, 來源任務和時間資訊. memory_propose 與匯入的每項記憶均須單獨確認. 未知預設作用域須先建立對應預設. 自動注入允許範圍內最新的完整項目, 最多 4 KiB; 目前預設的同名 key 覆蓋全域值. memory: false 僅關閉自動注入; 同時禁止查詢和提議請關閉 memory 工具組或選擇無記憶作用域. 匯出包含實際值及來源資訊. 請勿儲存憑據, 可識別的憑據鍵名和權杖格式會被拒絕.

> 開發預覽: P6.1-P6.4 已提供任務台, 歷史, 預設與偏好記憶. ai.agent API 要求 AutoJs6 構建編號不低於 5293. 其餘介面繼續按 P6.5-P6.7 實施, 可靠性與發佈門檻仍在 P7/P8.

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 簽名權限保護, 只有 AutoJs6 能存取; 啟動頁是唯一另外匯出的元件.
- 外掛不持有 API key, 不自行綁定模型提供方, 也不申請無障礙權限: 模型呼叫與裝置操作經 AutoJs6 為單條附着連結借出並在中斷時收回的代理執行, 每個代理都受 grant 約束 (允許的方法, 速率, 體積, 模型配額).
- 不申請網絡權限. FOREGROUND_SERVICE 與 FOREGROUND_SERVICE_SPECIAL_USE 用於執行中的任務, POST_NOTIFICATIONS 用於顯示進度與停止操作. 不申請無障礙或懸浮視窗權限.
- 任務歷史, 預設與偏好記憶只儲存在外掛私有儲存空間; 備份與裝置轉移已停用.

請只從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 頁面或 AutoJs6 外掛中心取得外掛. 來源不明的安裝套件即使版本號相同, 也可能無法通過主程式驗證或帶來風險.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者; 主程式使用這些識別碼發現外掛並協商相容性:

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

`AiAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: 經身份驗證的宿主連接, 支援任務排隊, 回應, 取消, 查詢與私有步驟記錄; 宿主斷開時任務阻塞, 程序重建後不會自動繼續.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 按階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/09/24_

- `提示` 開發預覽: P6.1-P6.4 已提供任務台, 歷史, 預設與偏好記憶. ai.agent API 要求 AutoJs6 構建編號不低於 5293. 其餘介面繼續按 P6.5-P6.7 實施, 可靠性與發佈門檻仍在 P7/P8.
- `新增` 偏好記憶支援逐次提議確認, 作用域查詢, 衝突保護, 按項目持久化, 編輯, 刪除及逐項確認匯入的 JSON 備份
- `新增` 命名預設的新增, 編輯, 複製, 刪除與預設值選擇; 宿主模型清單顯示執行位置與結構化 JSON 支援, 可設定固定上下文, 收緊工具群組和預算, 確認策略, 已批准腳本目錄與記憶作用域
- `新增` 完整步驟時間線與任務結果, 狀態/預設/日期篩選, 重跑草稿, 刪除與脫敏 JSON 匯出, 以及支援遷移和 LRU 清理的版本化私有歷史 (200 條 / 32 MiB)
- `新增` 任務台支援統一任務發起, 宿主外觀, 內聯互動, 預算進度及中斷連接後仍可讀取的最多 20 條最近任務
- `新增` 完成狀態要求結果證據, 部分完成結果列出未完成項, 下單或支付任務必須報告觀察到的訂單狀態
- `新增` 任務校驗在上下文裁剪後保留無變化計數, 並在執行前阻斷連續第 3 次相同動作請求
- `新增` 介面動作後有界等待螢幕樣本穩定, 並在後續觀察中附帶自上一動作以來的變化摘要
- `新增` 介面動作將確認綁定至宿主檢查的節點, 支援文字追加與有界捲動, 並回報執行結果和視窗變化
- `新增` 宿主回報已授權 OCR 插件可用時提供螢幕 OCR, 識別結果合併為帶座標的有界文字行
- `新增` 介面觀察保留宿主快照參照, 提供有界節點與主控台回饋, 並彙整可見文字和節點狀態變化
- `新增` 單腳本任務在模型收尾後保留腳本 ID, 路徑, 執行 ID 及上報結果, 區分明確的 null 並標記過大結果截斷
- `新增` 登記腳本執行接入確認清單校驗, 結構化觀察, 主控台尾部遮蔽及逾時或任務取消時的所屬腳本停止
- `新增` 按作用域注入偏好記憶供腳本填參, 支援 4 KiB 上限, 截斷標記與任務級關閉
- `新增` 登記腳本參數校驗與預設值補全, 缺參詢問, 目前清單風險檢查及確認參數表
- `新增` `ai-agent`: `AiAgentPluginInfoService`, `WakeActivity`, `AiAgentPluginService`, `ui.LauncherActivity`
- `新增` 10 種語言的 README, 外掛中心說明與更新日誌
- `新增` Agent 核心工具目錄, 含 30 個工具, 分組准入, 參數 Schema, bridge 呼叫準備, 有界觀察與敏感風險提升
- `新增` Agent 決策核心, 含協定 Schema 變體, 嚴格及擷取式 JSON 解析, 工具/分支驗證, 最多兩次修復重試與中英文提示範本
- `新增` Agent 任務預算, 統一限制步數, 模型呼叫, 時長和 token, 並提供工具/互動時限, usage 估算及輸出 token 准入
- `新增` Agent 確認閘, 支援預設/審慎策略, 同任務同工具同風險授權, 支付逐次確認與 10 語言支付關鍵詞
- `新增` Agent 私有步驟日誌, 限制為 200 步和 1 MiB, 支援密碼文字遮蔽, 終態裁剪保留狀態和計數
- `新增` 經身份驗證的宿主連接, 支援任務排隊, 回應, 取消, 查詢與私有步驟記錄; 宿主斷開時任務阻塞, 程序重建後不會自動繼續
- `新增` Agent 確定性上下文裝箱, 支援位元組上限, 最近完整步驟對, 中英文提示與節點優先保留; 本機模型使用 3000 token 輸入預算和精簡工具簽名
- `新增` 宿主模型用戶端核心, 驗證事件順序並支援 usage 記帳, 取消, 逾時和有限格式降級; 每次降級計入模型呼叫且保留決策修復額度
- `新增` 啟動器支援請求宿主連接, 15 秒逾時後引導在 AutoJs6 啟用 AI Agent 並授權
- `新增` 僅在任務存續期間顯示前景通知, 提供進度, 停止和查看操作; 可從啟動器完成輸入與逐次操作確認
- `新增` 已登記指令碼目錄支援任務開始時重新整理, 連結內 60 秒快取, 按關鍵詞確定性排序最多 24 個候選, 有界參數摘要及 script_catalog 查詢
- `修復` 最近歷史按任務開始時間排序與保留, 避免重新啟動時重寫存檔導致新任務被舊記錄擠掉
- `修復` 指令碼與外掛介面的詢問和確認回應按 interaction 歸屬驗證, 避免指令碼代替外掛介面回應
- `修復` 收銀台的確認交易按鈕未識別為支付動作的問題, 現逐次確認且不可重用整輪授權
- `修復` 畫面外匹配項的空白或倒置邊界導致查詢誤報參數錯誤的問題, 現保留文字並標記座標不可用
- `修復` 節點重新定位時邊界或操作能力不同的巢狀容器被誤判為同一目標的問題
- `修復` 節點目標的模型修復提示明確保留 # 引用前綴, 使用 selector 時省略 snapshotId
- `修復` 任務接入預載訂單意圖規則, 並減少規則初始化開銷
- `修復` 校驗區分不同視窗中的相同節點, 剪貼簿讀取不會解除介面觀察要求, 檔案傳輸不再誤判為支付任務
- `修復` 動作後的介面回讀無回應時, 穩定等待超過截止時間的問題
- `修復` 主控台拆行和裁剪前處理多行參數遮蔽, 避免參數文字與憑據標籤同名時遺漏憑據
- `修復` 連續啟動任務時, 已退出的前景服務不再誤拒絕下一任務的啟動請求
- `優化` 腳本確認描述按 JSON 轉義後的大小限制, 避免大參數表超過 Binder 事件上限
- `優化` 最低宿主版本為 AutoJs6 6.8.0 / 組建 5289, 用於動作節點檢查以及確認與執行的綁定
- `依賴` 附加同一 AutoJs6 6.8.0 / 5289 release 建置的 common-plugin-api, host-capability-api 與 ai-agent-api (MPL 2.0), 透過 SHA-256 鎖定
- `依賴` 附加 Gson 版本 2.13.2, 用於有界嚴格 JSON 解析與 Schema 資料樹

##### 更多發行歷史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

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

專案程式碼基於 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE) 授權. 第三方元件及其授權條款列於 [第三方聲明](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- AutoJs6 討論 #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md
