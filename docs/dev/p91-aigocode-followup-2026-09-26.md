# P9.1 AiGoCode Wi-Fi comparison and safe failure diagnostics (2026-09-26)

This follow-up uses the maintainer-authorized AIGoCode / gpt-5.6-sol target on QV770340J7. It preserves the original P9.1 comparison requirement and all failed samples. P9.2 synthetic-image results are recorded separately.

## Preconditions

- Actual device: Sony XQ-DQ72, Android 13 / API 33. The device is unlocked, AutoJs6 accessibility is bound, mobile data is enabled and the test SIM is ready. Some historical receipts called this device API 35; this run uses the current device-reported API 33.
- Initial host: 6.8.0 / build 5297. Agent upgraded with retained data to official signed R8 1.2.0 / build 88, CRC32 47afd9ea; source build 89 differed only in documentation. Initial Provider is R8 1.2.0 / build 216.
- The public catalog reports the selected profile as configured and available with structured-json and tools. The exact model ID was independently confirmed through the Provider's credential-free catalog as gpt-5.6-sol.
- Provider metered-network permission was already true and was not changed. The user's existing VPN remained enabled. No proxy, APN, DNS, saved Wi-Fi network, credential or default model was changed.
- Before each Wi-Fi task, the device starts at Home with Wi-Fi off. The actual default cellular network and existing VPN over cellular both have INTERNET and VALIDATED, and no active Wi-Fi transport remains. Readiness took 6374 ms and 6550 ms respectively, with bounded polling rather than a fixed sleep treated as proof.
- Both paths use the same goal, target, 30 steps / 40 model calls / 600000 ms / 300000 token budget, observe/act/user tools and system-settings confirmation allowlist. JSON selects modelPath=json; native selects modelPath=auto. No vision, dynamic script or MCP tool participates.

## Minimal format control

The explicit host modelFormatProbe uses only the fixed synthetic expected JSON, no device tools, streaming, a 2048-output-token cap and a 60-second per-mode deadline. Case: p91-aigocode-format-20260926-01.

| Mode | Exact expected output | Input / output / total tokens | Time |
| --- | --- | --- | --- |
| Structured | matched, 15 bytes | 42 / 12 / 54 | 5168 ms |
| Plain | matched, 15 bytes | 21 / 9 / 30 | 4890 ms |

Both modes completed, collectionComplete=true, instrumentation OK (1 test). Unlike the previous Model8 minimal probe, this target returned usable text. Since the device and target differ from the earlier XQ-AT72 run, this is not a controlled single-variable provider benchmark.

## First comparison, before diagnostic changes

| Path / case suffix | Result | Steps / tools / model calls | Input / output tokens | Time |
| --- | --- | --- | --- | --- |
| JSON / json-wifi-20260926-01 | failed / MODEL_FAILED, reason PROVIDER_FAILED | 10 / 9 / 10 | 98248 / 773, estimated=true | 135631 ms |
| Native / native-wifi-20260926-01 | failed / MODEL_FAILED, reason PROVIDER_FAILED | 9 / 8 / 9 | 62994 / 387, estimated=false | 148668 ms |

Case names have prefix p91-aigocode-. Counts use result.steps, not the zero-based/current snapshot step. JSON has 9 completed decisions with STRICT parsing. Native has 8 tool_calls events and 8 submitToolResults continuations, and all 8 decisions use NATIVE_TOOL. This is a real native round trip, not merely an auto-path configuration.

Both runs actually enable Wi-Fi through Settings. Before any manual recovery, an independent system read reports wifi_on=1 and the visible Android switch is checked=true; current Wi-Fi and the preserved VPN over Wi-Fi are validated. The next model request fails, so neither run completes final model confirmation. Neither result is counted as a full pass, and failed durations are not a speed comparison.

Raw instrumentation, model events, UI XML and screenshots remain in ignored private evidence. No raw model text, credentials or UI content is copied into this receipt.

## Safe error classification

Provider previously discarded the backend Throwable category at the session boundary; the host then reduced failures to PROVIDER_FAILED. The follow-up preserves only closed ONLINE_* categories in the existing AiError.providerCode and Agent failure reason. Main public error codes, generic messages and retry policy remain unchanged; arbitrary provider codes, raw exceptions, URLs and credentials are never forwarded. No AIDL or bundled SDK changes are needed. No model or device action is automatically replayed.

The network access policy checks active network, INTERNET and persisted metered consent. It does not reject networks for lacking VALIDATED. The observations therefore do not establish such a validation gate as the failure cause.

## Diagnostic follow-up

The Provider classification is in commit 40ac023 / build 219. Provider build 220 changes only P9.2 documentation and version bookkeeping; its signed R8 arm64 candidate is installed for this follow-up. Host commit d58d1eb9c9 / 6.8.0 / build 5297 was rebuilt with the complete callback-to-broker classification path; its main APK SHA-256 is e08a1c963c80e985e2fd862afcbef0324078c58b450dd2fe8a8e79c47c78da97, and matching androidTest SHA-256 is 9483d0f451830455d104d04ae162a0d53018e391351d7d520f71706ec05525a0.

Independent review caught that the first host candidate had covered persistent sessions but omitted the ordinary Attempt path and its earlier inbound summary. No model request was made with that incomplete diagnostic candidate. The final implementation retains a typed closed category at first decode, then forwards it through ordinary, streaming and native-tool Attempt dispatch. The old per-path timeout mapping is deliberately preserved.

Host verification before the model rerun: 72 related JVM tests passed, including real encoded failure callbacks and public ask compatibility. AiPluginAskFailureDispatchAndroidTest passed both methods on this device, exercising the actual private Attempt dispatch and resource release for ASK, STREAM, TOOLS and STREAM_TOOLS without binding or model requests. Provider verification: 30 related JVM tests and 12 native-session Android tests passed; full Debug lint has 0 errors / 97 existing warnings. Full host AppDebug lint passed with 0 errors / 2406 warnings (1487 tasks, 3 executed / 1484 up-to-date, 10m 2s). The report total is not presented as a newly introduced warning count.

Case p91-aigocode-native-diagnostic-20260926-01 starts from the Settings Wi-Fi page to reduce navigation while collecting the failure category. Target, goal and budgets are unchanged. Before starting, AutoJs6 is bound, the device is unlocked, Wi-Fi is off, and cellular plus the existing VPN over cellular are validated; readiness took 6536 ms. This different initial foreground is kept separate from the first comparison and is not a performance sample against it.

The category is read from the host E4 model wire event. Agent build 88 keeps its existing final MODEL_FAILED result and does not expose arbitrary provider reasons in public task results. A successful instrumentation run only establishes successful evidence collection, not successful task completion.

## Build environment recovery

The first full host lint attempt exhausted the original 4 GiB heap and spent nearly all CPU
time in garbage collection. Only that owned build process tree was stopped. A lint-only rerun
used a temporary command-line 8 GiB / G1 heap in the same private cache; it completed successfully.
No repository JVM setting, shared cache lock or unrelated Java process was changed. The already
completed 72 JVM tests, main/test APK build and real Android tests were not rerun merely for this
environment adjustment. Failed/incomplete initial build logs are retained privately.

## Final status

The diagnostic failed after actually enabling Wi-Fi. It used 4 steps / 3 tools / 4 model calls,
16974 input / 174 output / 17148 total tokens (estimated=false), and 24053 ms. The model wire
contains 3 native tool-call events and the final MODEL_FAILED / ONLINE_NETWORK_UNAVAILABLE.
The successful actions were app_current, ui_dump and ui_click. Before recovery, independent
reads again confirmed wifi_on=1 and android:id/switch_widget checked=true. Raw XML, screenshot,
connectivity snapshot and model events are retained privately.

This establishes a network-related failure category in this controlled follow-up. It does not
retroactively prove the exact category of the first two generic failures, identify an HTTP
transport stage, or distinguish policy denial from an IOException. It also does not establish
that the VPN itself or pooled connections caused the failure. The device's existing VPN moves
from cellular to Wi-Fi with the default network; it was neither stopped nor reconfigured.

The 24-second diagnostic, low output count and preserved ONLINE_NETWORK_UNAVAILABLE reason do
not support a fixed 120/150-second timeout or output-token exhaustion as its cause. No deadline,
context budget, output quota or retry limit was increased. No tool or model action was replayed.

The original P9.1 checkbox remains pending because the model did not complete final confirmation.
A targeted next comparison would temporarily pause the existing VPN, first check the selected
model's reachability and then run a bounded Wi-Fi task, restoring the VPN afterward. Because this
changes the user's existing routing, the user was asked before doing so. Pending that answer,
no VPN setting or additional paid model call is changed. No extra device or image model is needed.
The independent P9.2 vision gate is already accepted and is not affected by this Wi-Fi failure.


## Device recovery and installed artifacts

The temporary screen timeout was restored from 600000 to the original 120000 ms. Wi-Fi=1,
mobile_data=1 and font_scale=1.0 are preserved. The exact set of originally enabled accessibility
components is unchanged; the already-enabled AutoJs6 service was rebound after instrumentation
and independently confirmed bound. Metered-network consent remains its original true value,
and the existing VPN remains configured and connected. No profile, default target, saved Wi-Fi
network, APN, DNS or credential was changed. No order or payment operation was performed.

Installed APK bytes were read back with device sha256sum and match the following verified files:

| Component | Installed build | SHA-256 |
| --- | --- | --- |
| Host 6.8.0 | 5297, corrected Debug | e08a1c963c80e985e2fd862afcbef0324078c58b450dd2fe8a8e79c47c78da97 |
| 3-Stone AI 1.2.0 | 220, official R8 | 408312dab105422949260e8b9aa3f6c322825d8b6b1024665ec481e0eae06db5 |
| AI Agent 1.2.0 | 88, official R8 | f1395767880420bb36a96bc083ffb82973bb60086e3aa02778e50b1b689ee310 |

Agent builds 89-91 change documentation and commit bookkeeping, not runtime code; no new Agent
APK was built for these receipts. Provider's build 220 image documentation is commit 66cba70.
All ten Agent language sources and 36 generated artifacts pass the read-only Markdown check.
Original private evidence and generated APKs remain ignored. No remote publication or push is
performed, and the concurrent Rhino synchronization files are untouched.
