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

版本 1.0.0 是路線圖的 P0 開發預覽: 外掛身份, AutoJs6 發現契約 (INFO 服務, Wake Activity 與 `org.autojs.plugin.AI_AGENT` 服務佔位) 以及一個顯示主程式狀態的啟動頁. 智能代理循環, 指令碼目錄, `ai.agent` API 與任務台尚未實作; 進度與證據記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md). 外掛將要求 AutoJs6 組建 5283 或更高版本.

******

### 功能

******

1.0.0 計劃提供以下能力:

- 指令碼選擇: 透過 `project.json` 或 `@agent` 頭部註解登記的指令碼連同描述與參數 Schema 呈現給模型; 智能代理挑選指令碼, 補齊參數, 在需要時請求確認, 在 AutoJs6 中執行並讀取結構化結果.
- 介面逐步操作: 智能代理以緊湊文字形式觀察無障礙節點樹 (安裝了 OCR 外掛時還能讀取畫面文字), 然後經 AutoJs6 能力代理點擊, 輸入, 捲動與按鍵, 直到能夠驗證目標已達成.
- 安全設計: 唯讀工具自動執行; 敏感操作 (付款, 傳送, 刪除, 寫入檔案, shell, 座標手勢, 登記為敏感的指令碼) 需要確認; 每次任務都有步數, 模型呼叫次數, 時長與 token 預算.
- 指令碼 API 與使用者介面: `ai.agent.run(goal, options)` 回傳帶事件, 回應與取消的 `AgentRun` 句柄; 獨立應用程式提供任務台, 歷史, 預設, 偏好記憶, 設定與發行歷史.

******

### 使用方法

******

1. 在安裝了 AutoJs6 組建 5283 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `AI Agent` 已被識別並啟用它. 官方發佈套件會自動通過簽名驗證.
3. 從啟動器開啟 AI Agent: 本預覽版的頁面只顯示是否安裝了相容的 AutoJs6 主程式. 任務台, 抽屜項目與 `ai.agent` API 隨後續路線圖階段提供.

> 本預覽版的啟動頁只顯示主程式狀態; AutoJs6 抽屜項目, `ai.agent` API 與任務台分別隨路線圖 P1, P5 與 P6 提供.

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 簽名權限保護, 只有 AutoJs6 能存取; 啟動頁是唯一另外匯出的元件.
- 外掛不持有 API key, 不自行綁定模型提供方, 也不申請無障礙權限: 模型呼叫與裝置操作經 AutoJs6 為單條附着連結借出並在中斷時收回的代理執行, 每個代理都受 grant 約束 (允許的方法, 速率, 體積, 模型配額).
- 外掛不使用網絡. 本預覽版除外掛權限外不宣告任何權限; 前台服務, 通知與懸浮視窗權限將隨需要它們的功能加入並在此說明.
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
minimum host build: 5283 (6.8.0)
```

`AiAgentPluginService` 在 `:agent` 程序中回應 `org.autojs.plugin.AI_AGENT` (category `ai-agent`); 本預覽版在主程式契約模組落地前只公開一個攜帶 descriptor `org.autojs.plugin.ai.agent.api.IAiAgentPlugin` 的佔位 Binder. `AiAgentPluginInfoService` 以 PluginInfo 回應 `org.autojs.plugin.INFO`. `WakeActivity` 供主程式啟動外掛.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 按階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/09/23_

- `提示` P0 開發預覽: 外掛身份, AutoJs6 發現契約與顯示主程式狀態的啟動頁. 智能代理循環, 指令碼目錄, ai.agent API 與任務台尚未實作. 詳見 ROADMAP.md.
- `提示` 宿主端的 AI Agent 契約, 能力與模型代理, 螢幕觀察及腳本登記執行已實作, 插件任務執行功能仍處於開發階段
- `新增` 外掛身份 `ai-agent`, 含 INFO 服務, Wake Activity, 執行於 `:agent` 程序的 `org.autojs.plugin.AI_AGENT` 服務佔位, 以及顯示是否安裝了相容 AutoJs6 主程式的啟動頁
- `新增` 10 種語言的 README, 外掛中心說明與更新日誌
- `依賴` 附加 `common-plugin-api.aar` (AutoJs6 模組 `plugin-api/common-plugin-api`, 主程式組建 6.8.0 / 5282, MPL 2.0) 作為共用外掛契約, 以 SHA-256 鎖定於 `locks/host-api-aars.lock`

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
