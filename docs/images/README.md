# Interface captures

Captured on 2026-09-29 from production views after the P13 additions
(preset import / export, plan mode, share summary), version 1.3.0 / build 168. Android API 36.1 (google_apis, x86_64), disposable
pixel_7 emulator, 1080 x 2400, density 420, font scale 1.0. These are interface
examples with synthetic tasks and scripted replies, not real-model acceptance
results.

The four README images use English and a supported host appearance snapshot.
The app-* images use Simplified Chinese and the app's indigo theme, in light
and dark mode. The model names explicitly identify fixture models. The
app-models images contain only the model bottom sheet, without the dimmed
window behind it. The home draft is never submitted. Settings-more captures
the bottom of the settings page after scrolling has finished.

| File | Pixels | SHA-256 |
| --- | --- | --- |
| app-home-dark.png | 1080 x 2400 | 10b15266291d6c03ed89703342aa6c3a069c2b5c24de8f179ced11c40f9a9ffe |
| app-home-light.png | 1080 x 2400 | 8fa4c689f03d3af162b0398ed0e381f930e40d8881de2a8e5d018c1d51a8a2bd |
| app-models-dark.png | 1080 x 1503 | 69e5bf7ebbc3765c2a5a96ee58cea6ad0f2de4f42c84c5c88426d251208e3b89 |
| app-models-light.png | 1080 x 1503 | fe3fe8c85076cc1f15816f8d74bb3eff59a21318191c5935e4bdc7ce6200ac99 |
| app-settings-dark.png | 1080 x 2400 | 594c3e91fc266397d30eb3b0be8ba1422ca6d7307e25f42e5a84680306ea7b9b |
| app-settings-light.png | 1080 x 2400 | 7b892b3dda69cc85f1e3d6a9d38fc34b9deaa98a609a7530bfb01e0f6c064c78 |
| app-settings-more-dark.png | 1080 x 2400 | 754c4c6ec13d8706f562030d1efd05a7b9a5621f2830a33a8708b89fa794ed59 |
| app-settings-more-light.png | 1080 x 2400 | 92bd8920296a843773a0a34a8bf0a7355d34e0c0e96848b92427a01808aebfeb |
| confirmation.png | 1024 x 1965 | 3df03d92e44f68467bb61312ca52a962659996b263af9b167620e1e995d8d75d |
| detail.png | 1080 x 2400 | 68ecfba199ff2aaddec6b4948beeab4fe7e2b488c3a24db1e47e75e7df09f382 |
| floating.png | 945 x 621 | 0e531e62f1c15e0e79272ff55b37feb1183f3697ba502b98ae1cb9ff519b95df |
| workbench.png | 1080 x 2400 | 2decdde14c9342b6da2f7d93ade213a1af9f81f3b8b8c06749fdc1b1a20c5ede |

## Reproduce

1. Create a fresh disposable Three_Stove_Agent_Conformance_* emulator without
   personal history, or run adb shell pm clear on the plugin package of such an
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

Use the io.github.supermonster003.autojs6.plugin.three.stove.agent.ui package
prefix for each class and the package's ThreeStoveAgentTestRunner
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
