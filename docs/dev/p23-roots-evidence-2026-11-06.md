# P23.4 真机证据: 脚本目录被拒时立即回复 (宿主 issue #589)

日期: 2026-11-06. 设备: OPD2413 (OnePlus, ColorOS 15 / Android 15, 系统语言 en-US). 宿主: AutoJs6 6.8.1 debug build 6640, 从宿主提交 `69c5528499` (含 `52ba898be7`) 的干净导出构建, 13:06 安装. 插件: 3-Stove Agent 1.4.1 debug build 446 (提交 `313fd0ca`), 13:02 安装. 宿主工作目录 `/storage/emulated/0/Scripts`.

脚本: `py .python/device_roots_check.py --serial 63751f63 --plugin-build 446`. 每个用例都是任务台冷启动: 先 `am force-stop` 插件, 经 `run-as` 写入 `shared_prefs/script-roots.xml`, 再启动 `LauncherActivity`, 由它在 onStart 自动发出有序广播的连接请求; 横幅文案用 `uiautomator dump` 读取并与 `values/strings.xml` 比对, 宿主日志取 logcat.

## 结果

```
DEVICE serial=63751f63 locale=en-US host=6640 hostUpdated=2026-11-06T13:06:07 plugin=446 pluginUpdated=2026-11-06T13:02:16 workingDir=/storage/emulated/0/Scripts
PASS parent roots=/storage/emulated/0 expect=rejected
  stage=rejected bannerAfterMs=2620 hostAnswerMs=507 polls=1 droppedCodes=ROOTS_REJECTED notCompleted=1 requirementsLogged=true hostBoundToPlugin=false pluginBoundToHost=false top=io.github.supermonster003.autojs6.plugin.three.stove.agent/.ui.LauncherActivity
PASS self roots=/storage/emulated/0/Scripts expect=attached
  stage=hidden bannerAfterMs=5349 hostAnswerMs=n/a polls=2 droppedCodes=none notCompleted=0 requirementsLogged=false hostBoundToPlugin=true pluginBoundToHost=false top=io.github.supermonster003.autojs6.plugin.three.stove.agent/.ui.LauncherActivity
AFTER scriptRoots=removed (none before) top=com.android.launcher/.Launcher
SUMMARY serial=63751f63 cases=2 failed=0
```

- parent (脚本目录设为工作目录的上级 `/storage/emulated/0`): 第一次 UI 读取 (启动后 2.6 s, 含 uiautomator 自身约 1-2 s 的延迟) 横幅已是 `script_roots_rejected`; 宿主在 `LauncherActivity` 的 START 日志后 507 ms 写出 `ThreeStoveAgentAttach: Attach request dropped: ROOTS_REJECTED`, 随后 `ThreeStoveAgentLink: Plugin attach request not completed: ROOTS_REJECTED`, 全局日志里有目录要求 (`... were not accepted: each directory must exist in shared storage ...`). 没有等到 15 秒超时.
- self (脚本目录设为工作目录本身): 启动后 5.3 s 横幅消失 (已连接), 宿主已绑定插件服务, 宿主日志无 ROOTS_REJECTED. 1.4.0 配 6.8.0 时这一配置会被拒.
- 配 6.8.0 宿主 (不回复目录被拒) 的行为未上真机: 与 1.4.0 相同, 超时后才显示, 由 `AttachReplyTest` 覆盖.

脚本结束时删除了它写入的 `script-roots.xml` (此前不存在) 并回到桌面; 宿主 6640 留在设备上, 原宿主 5935 debug 的 APK 备份由会话保留待维护者决定是否装回.
