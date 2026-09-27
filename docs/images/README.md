# Interface captures

Captured on 2026-09-28 from production views after the third UI pass,
version 1.2.0 / build 140. Android API 36.1 (google_apis, x86_64), disposable
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
| app-home-dark.png | 1080 x 2400 | ae4bb556092a4e80508092f562cf2f55959d3bb3aa9d98349d82f89975aaed20 |
| app-home-light.png | 1080 x 2400 | 27f0a57e1ec91573a560e3479cea68e158992234385c928710dbce240562b49d |
| app-models-dark.png | 1080 x 1503 | 69e5bf7ebbc3765c2a5a96ee58cea6ad0f2de4f42c84c5c88426d251208e3b89 |
| app-models-light.png | 1080 x 1503 | fe3fe8c85076cc1f15816f8d74bb3eff59a21318191c5935e4bdc7ce6200ac99 |
| app-settings-dark.png | 1080 x 2400 | 95a3d91a0e57de51437d80d1ca091580ac4a43f50aa32eb5c57c59c9d11429f4 |
| app-settings-light.png | 1080 x 2400 | 95b40aa8cd693b5c35ad77efafd3d374da6194dcaed778d6f28db48da79ec7c6 |
| app-settings-more-dark.png | 1080 x 2400 | 65d8842881a0204233be0c5e4b259b794e786638ea00e9d2840d7cd5861b6998 |
| app-settings-more-light.png | 1080 x 2400 | 9a0df9ae35720af8200760c569e1ca1b319842fcc66a8cd2ed7a7b301baf53f0 |
| confirmation.png | 1024 x 1965 | 8591e4bc2ff5af4b3cec629685e31b9ae9d4ad61d6b3fb30ec47a5b3b0fdf22d |
| detail.png | 1080 x 2400 | 077815d7790ae8fbe0ce193391d9db89cccb85d001fea67c162a5f50da0b7547 |
| floating.png | 945 x 1062 | 203069549a9a874038a54e1f1941a348567e1019d1c922d64b8863954d7bf6bb |
| workbench.png | 1080 x 2400 | e604495df8633ac735d237611923de4eb6caa92dfa92cbc06585fcc586e35122 |

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
