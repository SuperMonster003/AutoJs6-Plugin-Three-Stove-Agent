# P6.1 task workbench evidence (2026-09-24)

## Scope

Plugin 1.0.0 / 54 implements the original P6.1 task workbench. Host source and
installed test host remain AutoJs6 6.8.0 / 5293 (aeed8edcb9). No host, provider,
Rhino, declaration or documentation-repository changes are needed for this UI.

The launcher now accepts a goal and the existing default preset, shows link and
selected model information, current state, step progress and budget limits, and
offers stop and inline replies. Recent tasks are limited to 20 visible entries.
Their read-only detail destination displays the result and bounded step history.
Ordering and retention use task start times, preventing restart-time file rewrites
from promoting old entries or evicting newer tasks.
History remains readable through the private service while the host is detached.

Full history retention, filters, deletion, rerun and export remain P6.2. Custom
presets remain P6.3. Background confirmation routing, countdown and run-scoped
allowance remain P6.5. The task input already uses the system speech recognizer,
hides that button when unavailable and only fills the draft; the complete
floating/share/shortcut/voice acceptance remains P6.7. No roadmap stage was added,
split or discarded.

## Implementation boundaries

- Host `startRun` and private UI starts converge on `RunLauncher`. Link, preset,
  options and budget validation precede admission to the existing queue. Broker
  preparation still waits for successful foreground-service promotion.
- Only the private status snapshot gains the selected model display name. The
  catalog is queried through the supplied model broker; the plugin never binds a
  Provider or acquires model credentials. Public AIDL is unchanged.
- `RunQueries` shares bounded validation/projection between the host and the
  same-UID private endpoint. Offline UI queries do not grant execution access.
- Script-owned interactions remain read-only in the launcher. Inline replies
  preserve request IDs and use the existing single-claim response path. Sensitive
  confirmations use one-time scope; registered-script parameter rendering keeps
  the existing redaction table.
- Visible screens bind and poll off the main thread; polling stops on `onStop`.
  Closing a screen does not cancel a plugin-owned task. Draft goals are stored in
  private preferences; an unanswered inline text draft survives Activity recreation.
  Reopening the UI reads state and never resubmits a task automatically.
- Host appearance uses the official V1 settings snapshot over an unstable provider
  client on a worker thread. Unavailable/unknown settings fall back to the system.
  Language, explicit light/dark themes and primary/accent colors follow the host.
  Buttons choose black or white text by background luminance. System bars retain
  contrast on API 28. Layout direction uses the wrapped configuration rather than
  Android's process-wide `Locale.getDefault()` direction.
- `WorkbenchFixtureService` is present only in `src/debug`, is unexported, checks
  same UID and injects scripted brokers into the real `:agent` runtime. Release
  manifest and R8 mapping contain no fixture component/class. No extra permission
  was introduced. Speech discovery only adds an intent query.

## Validation

| Check | Evidence |
| --- | --- |
| JVM suite | 375 tests, zero failures/errors/skips |
| AVD API 37, x86_64, 16 KiB | Full plugin instrumentation: 39/39, 14.345 s |
| Sony G8441, API 28, arm64 | Full plugin instrumentation: 39/39, 11.689 s |
| Workbench coverage | Goal -> running -> inline question -> answer draft recreation -> reply -> completed; pending model cancellation; absent-host guidance; offline details; confirmation ownership/one reply; snapshot validation; Arabic RTL/night/2.0 font layout; restart ordering and retention with reversed file timestamps |
| Build | Temurin 21, debug + androidTest + release R8; native alignment checks pass |
| Lint | Zero errors; six existing dependency/runtime/icon warnings remain |
| Localization | 10 languages, 36 generated artifacts, generator `--check` passes |
| Manual UI inspection | API 37 and API 28 task screen, button/system-bar contrast, scrolling and bounded history |
| Actual host connection | API 37 plugin-center enable -> protected attach request -> connected, displaying imported Gemma 4 E2B model catalog name |

The final suite includes the system-bar styling adjustment and a retention test
that loads 21 terminal tasks with reversed file modification times and checks
that the newest 20 tasks remain in chronological order. The no-host UI test supplies an absent package snapshot; it does
not uninstall or disable the user's host. RTL/night/large-font coverage inflates
and measures the actual localized layout in the corresponding themed context.
This is E2/E3 UI/integration evidence with deterministic brokers, not a new
real-model E4 task benchmark or completion of the P7 compatibility matrix.

The AVD required enabling the installed AI Agent entry in the host's plugin
center for its first normal UI connection; it remains enabled for subsequent use.
Its temporary plugin notification grant is restored after instrumentation, and
the headless AVD started for this session is shut down afterward. No
model configuration, accessibility setting, Wi-Fi state or screen timeout was
changed for these checks. G8441's existing services are preserved. No purchase,
order submission or payment is performed in this phase.

Build logs, raw UI dumps, screenshots and test logs remain in ignored `build/p61-*`
artifacts. `p61-complete-emulator-5586.log` and `p61-complete-BH900ASK9E.log` are the
full-suite records. The committed evidence contains no account data or raw model
credentials. This is a local implementation commit, with no push or release.
