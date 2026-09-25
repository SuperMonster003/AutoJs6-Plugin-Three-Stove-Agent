# README interface captures

Captured on 2026-09-25 from the production views in the P7 UI implementation
(runtime build 70), with the P8 opt-in instrumentation fixtures. Android API 37.1,
16 KiB emulator, 1080 x 2424, density 420, font scale 1.0, English, light theme.
The theme colors are a supported host appearance snapshot. These are interface
examples with synthetic tasks and scripted model replies, not E4 model results.

| File | View | Pixels | SHA-256 |
| --- | --- | --- | --- |
| workbench.png | LauncherActivity waiting for a report-language answer | 1080 x 2424 | 37670a980d8ddf8c29b4492cf6cc2505407eab844a6977143faffb6c3fa70323 |
| detail.png | RunDetailActivity with the answer, outcome and timeline | 1080 x 2424 | a68d350378a27f1de866528fef44be29a6280ca0939b505bc684f1ff763b3b00 |
| confirmation.png | ConfirmationActivity reviewing a memory proposal | 1024 x 856 | 80686358c6caa3d769df1c387de3fb2495510b73f2ce9267db28c38bdf269fa5 |
| floating.png | Expanded FloatingBall with an editable task draft | 945 x 1597 | dd49f33ee8ca5b4c666815d1492f191af04699a1f92555116a62d68d5cb2aeec |

## Reproduce

1. Create a disposable empty emulator. Install a compatible real AutoJs6 APK,
   this plugin's debug APK and its androidTest APK. Do not use a physical device
   or an emulator containing personal tasks. The capture fixture requires an
   emulator and refuses existing plugin history. It never calls device tools.
2. Grant the plugin notification permission on API 33+. Use font scale 1.0,
   an unlocked screen and the dimensions above. No Provider or model credentials
   are needed. Overlay permission is temporarily granted and restored by the
   floating-window fixture.
3. Run the two explicit methods, replacing SERIAL with the disposable emulator:

```powershell
adb -s SERIAL shell am instrument -w -r `
  -e agent.readme.capture true -e agent.ui.locale en `
  -e agent.ui.dark false -e agent.ui.font 1.0 `
  -e class 'io.github.supermonster003.autojs6.plugin.ai.agent.ui.WorkbenchActivityTest#captureReadmeScreens,io.github.supermonster003.autojs6.plugin.ai.agent.ui.FloatingAccessibilityTest#captureReadmeFloating' `
  io.github.supermonster003.autojs6.plugin.ai.agent.test/androidx.test.runner.AndroidJUnitRunner
```

4. Copy `cache/readme-captures/*.png` using `adb exec-out run-as` and a binary-safe
   local writer. Activity decor includes the actual window background; the
   floating capture is the actual overlay root. No pixel postprocessing, image
   generation, security-flag changes or capture of another app is involved.
5. Visually review each image, replace these four assets, update their hashes,
   and run the Markdown generator and `--check`. The clock, task IDs and measured
   durations can differ. Shut down the disposable emulator after capture.

The two methods are skipped unless `agent.readme.capture=true` is explicitly set.
The model is labeled Demo model. The language answer is supplied by the fixture;
the memory proposal is cancelled without approval. The floating task is never
sent. Test services remain debug-only and unexported, and FLAG_SECURE remains
enabled on production windows. No real account, model response or user history
is part of these images.
