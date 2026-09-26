# Interface captures

Captured on 2026-09-26 from production views in the standalone UI redesign,
version 1.2.0 / build 93. Android API 37.1, 16 KiB disposable emulator,
1080 x 1920, density 420, font scale 1.0. These are interface examples with
synthetic tasks and scripted replies, not real-model acceptance results.

The four README images use English and a supported host appearance snapshot.
The app-* images use Simplified Chinese and the app's indigo theme, in light
and dark mode. The model names explicitly identify fixture models. The home
draft is never submitted. Settings-more captures the bottom of the settings
page after scrolling has finished.

| File | Pixels | SHA-256 |
| --- | --- | --- |
| app-home-dark.png | 1080 x 1920 | 0ff149edd99b0ec79ddde65c270cdcef6be3d271d413d6821c84596c00e7146a |
| app-home-light.png | 1080 x 1920 | 090ae919bf8f83200c88474e694b2e49830a52c89bbc83c80c5cd60789dacb57 |
| app-models-dark.png | 1024 x 1609 | c1791cb1767755ab4deaf07b0ba8575a866e38b3d33a0d7d049426d6f864ebab |
| app-models-light.png | 1024 x 1609 | f8ba8fa9a15a3f1d663ff3de511e4244f303261efe3abb682c575006ffac6e61 |
| app-settings-dark.png | 1080 x 1920 | f9ce24b655f480e6ff7c6a8d4519eb8c3cedddc3d53ce042d8300949cb8a6f35 |
| app-settings-light.png | 1080 x 1920 | b6cc3ced7b456aa3ad52c1f35b806e526d8be1e90ed84b79a80c6b07b32edb1f |
| app-settings-more-dark.png | 1080 x 1920 | 8243acdb5c123621c3d8bd6de18f0d8a680bdda6d90714b8bc4a3c46ec571d66 |
| app-settings-more-light.png | 1080 x 1920 | 4a1a264ff85da570195b2cc8f542c5fce2358ada0f23dc5346f4bde4ed092154 |
| confirmation.png | 1024 x 924 | c22123bd89ca5e979d0ac86dd423d9774d1ce386c1112224fda1d6a8bd365634 |
| detail.png | 1080 x 1920 | 9d0d6918102a1b18b3625f737638938c8b200b3ec160b00d38a21318852be180 |
| floating.png | 945 x 1234 | 40efa0f17986690cb257b49f05bf30103887fa3c1fde8ab01494fc398be2d29d |
| workbench.png | 1080 x 1920 | 00417403073fd5cb90f7fb572f9c24e2ccdf5113c85db1e77d0d21ddbaa53421 |

## Reproduce

1. Create a fresh disposable AI_Agent_Conformance_* emulator without personal
   history. Build Debug and androidTest, then use the guarded
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
