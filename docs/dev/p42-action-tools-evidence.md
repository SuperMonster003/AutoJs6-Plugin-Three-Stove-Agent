# P4.2 action tools evidence

Date: 2026-09-23. Final plugin: 1.0.0 / 39. Minimum host: AutoJs6 6.8.0 / 5289.
Scope is the three original P4.2 items. P4.3 verification rules and P4.4 real-model
acceptance remain separate roadmap items.

## Action path

Each run owns ActionTools and its observation registry. Node actions first call
the real host's read-only accessibility.inspectNode. The host resolves a bounded
selector or published node reference, checks the actual eligible recipient and
returns bounded risk metadata plus a private preparation token. If it climbs
from a label to a clickable parent, both identities are retained. Execution checks
the same window, method, unique full identity, bounds, enabled/password/action
flags and the original label's presence inside its recipient. Changed or evicted
targets fail with NODE_REF_STALE; there is no selector or coordinate fallback.

The plugin binds the preparation to its task, keeps its token out of model input
and step records, and uses inspected text for the existing confirmation gate.
Uncertain/clipped/container targets and gestures require individual confirmation.
Password input is redacted from recorded arguments, events and subsequent model
history. Host-side append uses the actual field value, with a combined 64 KiB
UTF-8 limit. Password append is rejected because accessibility may expose masks;
password replacement is supported.

Implemented tools are ui_click, ui_long_click, ui_set_text, ui_scroll, ui_press_key,
app_launch, clipboard_get, clipboard_set, ui_click_xy, ui_swipe and ui_gesture.
Scrolling respects direction and the requested bound, stopping at the first false
receipt. Press keys cover back/home/recents/notifications/quick_settings. Launch
accepts package or application name. Gesture remains disabled by default; only an
explicit initial host grant enables it, and per-task/config updates cannot widen
that grant. Clipboard reads remain read-only.

Replies preserve ok, actionResult and windowChanged (null if unobserved), plus the
actual attempt count and bounded changes. Clipboard's void write acknowledgement
is normalized to success; false action receipts remain false. A receipt does not
establish completion of the user's goal.

## Stability and cancellation

After an action, the adapter samples the bounded compact tree every 250 ms until
it remains unchanged for 500 ms, with a 3 s stabilization limit and the operation's
own remaining deadline. Identity, state, position, window and truncation changes
reset the quiet interval; newly generated snapshot IDs do not. Missing observation
waits the remaining 500 ms and reports unknown/partial stability. Truncated samples
are explicitly partial and do not prove that the entire application is stable.

The next ui_dump includes changes since the pre-action baseline, even when hidden
stabilization dumps intervened. Explicit ui_wait_for refreshes that summary at its
own condition/deadline completion without adding another quiet interval. Hidden
dumps never redefine an implicit model node reference. Cancellation and deadlines
fence pending callbacks, polling and further actions; an acknowledged action is
never retried by the stability adapter. An independent stabilization timer also
ends a stalled host read at 3 s, preserving the receipt with unknown/unstable
observation and discarding its late callback. A regression failed before this
fix and passes with the timer.

## Validation record

| Check | Result |
| --- | --- |
| Plugin JVM | 333/333, including 22 new action/grant/stability/cancellation cases |
| Host JVM | 3188 discovered, 3182 passed, 6 existing conditional skips |
| Plugin Android, each API 24 / API 37 | 27/27 |
| Host capability broker Android, each AVD | 5/5 |
| Real host/plugin round trip, each AVD | 19 passed, 1 existing optional Wi-Fi skip; 20 discovered |
| Plugin build | debug, androidTest, release/R8 and lint; 0 errors, 6 existing lint warnings |
| Host build | app debug, androidTest and three release API modules together |
| Documentation | 10 languages / 36 generated artifacts, freshness check passed |

Device tests use private read-only AVDs and a debug-only synthetic activity, the
real host UID, the installed plugin and the production capability broker, with
scripted model decisions. Initial functional runs used plugin build 37 and host
5289. Build 39 adds the independent stalled-read deadline and its regression;
final build/device results use that version together with host 5289. No physical
device, real model service, shopping app, account or payment is involved.

The node-action run performs 11 model calls. The global-action matrix has 12
independent action/done runs (24 model calls), including both launch modes and all
five press keys. The ancestor-label regression takes 3 model calls. The disabled
gesture case verifies rejection without any coordinate dispatch. These counts
describe deterministic correctness fixtures, not real-model task performance.

An initial regression exposed a fixture configuration error: setting single-line
mode after the password input type overwrote its password transformation. The
fixture now sets the input type last and asserts Android's password flag before
exercising redaction. A separate regression changes a child label after confirming
its parent action and verifies refusal without a second click. On API 37 the first
button was initially behind the system bar; applying window insets keeps the
synthetic controls visible on edge-to-edge targets. Both cases pass on both AVDs.

Useful commands (Temurin JDK 21 selected for Gradle):

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:lintDebug
py .python/generate_markdown.py --check
adb -s <private-avd> shell am instrument -w -r io.github.supermonster003.autojs6.plugin.ai.agent.test/androidx.test.runner.AndroidJUnitRunner
adb -s <private-avd> shell am instrument -w -r -e autojs.agent.plugin true -e autojs.agent.observe true -e autojs.agent.actions true -e class org.autojs.autojs.core.plugin.agent.AiAgentPluginRoundTripTest org.autojs.autojs6.test/androidx.test.runner.AndroidJUnitRunner
```
