# Interface captures

Captured on 2026-09-27 from production views after the Material 3 redesign,
version 1.2.0 / build 102. Android API 37.1, 16 KiB disposable emulator,
1080 x 1920, density 420, font scale 1.0. These are interface examples with
synthetic tasks and scripted replies, not real-model acceptance results.

The four README images use English and a supported host appearance snapshot.
The app-* images use Simplified Chinese and the app's indigo theme, in light
and dark mode. The model names explicitly identify fixture models. The
app-models images contain only the model bottom sheet, without the dimmed
window behind it. The home draft is never submitted. Settings-more captures
the bottom of the settings page after scrolling has finished.

| File | Pixels | SHA-256 |
| --- | --- | --- |
| app-home-dark.png | 1080 x 1920 | 525ca968f01821a143ed7adf582fe6418e112955427e4e2975fad5fd7469dcc2 |
| app-home-light.png | 1080 x 1920 | f4008bdf692ad9bdf63ede2d8b057912ae095c7a77477d69fa50445e3fe09d94 |
| app-models-dark.png | 1080 x 1278 | 761bd7af098207f45356506fea171b68ea287d394fbc9d7ed3198de93b48c221 |
| app-models-light.png | 1080 x 1278 | 2607e04cf11c1d361487be1a7842a66e9152a7c129706e346bbfa5beb57aaaee |
| app-settings-dark.png | 1080 x 1920 | bb0b41c5d3f7961c7b7bb48ca5cc83196a4c50b23de5958a24b0484e4aedf21d |
| app-settings-light.png | 1080 x 1920 | ad9d16fc02dcf7f997c94e32200ae27b0fc374039c6840575e8cbea867875a08 |
| app-settings-more-dark.png | 1080 x 1920 | b41b06974cd20faac6b934028bd4f951ed925d12561373b6eaa49451a60dc709 |
| app-settings-more-light.png | 1080 x 1920 | 9071a12a396e0a2369e076265ca498c2182daad13a2a90597c7f2846fc39469f |
| confirmation.png | 1024 x 1715 | d2940940a54b1c58b5fa9b33182216dea964182a32a5c98632e8400b1c4e5489 |
| detail.png | 1080 x 1920 | 5b2340646cb30213fee18d435d8e5a0a1007249fd7972f5f292c9adb0a2954c9 |
| floating.png | 945 x 1234 | f51581e43c7c49794cd3cd6c74e72da4947fff097143d24da0d4bbfcf991f0d2 |
| workbench.png | 1080 x 1920 | 412c0af1b3796902c20d484f25cd258ac40162329cf09bc41e798daadeb8c977 |

## Reproduce

1. Create a fresh disposable AI_Agent_Conformance_* emulator without personal
   history, or run adb shell pm clear on the plugin package of such an
   emulator after earlier test runs, because the home screen lists recent
   tasks. Build Debug and androidTest, then use the guarded
   test-apps/fake-host/run_conformance.py --prepare-only setup on that serial.
   Install the matching app-debug-androidTest.apk. Never install this fake host
   on a real device or an emulator containing real AutoJs6 data.
2. Use font scale 1.0 and an unlocked screen. Grant notification permission on
   API 33+. Overlay permission is temporarily granted and restored by the
   floating-window fixture. No Provider or model credentials are required.
3. Run the following methods with agent.readme.capture=true. The two original
   README methods also take agent.ui.locale=en and agent.ui.dark=false:

- WorkbenchActivityTest#captureReadmeScreens
- FloatingAccessibilityTest#captureReadmeFloating
- WorkbenchActivityTest#captureRedesignedScreens, once with agent.ui.dark=false
  and once with agent.ui.dark=true

Use the io.github.supermonster003.autojs6.plugin.ai.agent.ui package prefix
for each class and the package's androidx.test.runner.AndroidJUnitRunner
instrumentation component. All capture methods are skipped without explicit
opt-in. The original README capture rejects pre-existing task history.

4. Copy cache/readme-captures/*.png using adb exec-out run-as and a binary-safe
   writer. Rename home/models/settings/settings-more false/true to the app-*
   light/dark filenames above. No pixel postprocessing or image generation is
   involved. Only the app's own views are rendered; production security flags
   stay enabled and other applications are never captured.
5. Visually inspect all images, update their hashes, and run the Markdown
   generator and --check. Times and generated task IDs can differ. Stop the
   disposable emulator after capture.
