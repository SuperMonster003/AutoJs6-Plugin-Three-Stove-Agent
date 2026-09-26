# P9.1 controlled Wi-Fi two-path acceptance (2026-09-26)

The original P9.1 test item is accepted after this JSON/native pair. Earlier fake-Provider
round trips and the G8441 calculator pair remain the other required evidence. No original
roadmap item is added, split or removed. This is Wi-Fi switch/readback acceptance under a
controlled network condition, not a fix or acceptance of VPN underlying-network handoff.

## Device, builds and scope

- QV770340J7, Sony XQ-DQ72, device-reported Android 13 / API 33.
- Exact selected target: AiGoCode / gpt-5.6-sol, using the existing saved profile and credentials.
- Host d58d1eb9c9, 6.8.0 / build 5297; Provider 66cba70, official R8 1.2.0 / build 220;
  Agent official R8 1.2.0 / build 88. Agent source builds 89-92 change documentation and
  commit bookkeeping only. No new APK was built or installed in this follow-up.
- The maintainer explicitly authorized temporarily pausing the existing VPN. Its saved
  profile, always-on configuration and lockdown setting were preserved.

## Direct-network controls

The existing host modelFormatProbe sent the same fixed synthetic JSON prompt in structured
and plain modes, with no device tools, streaming, a 2048-output-token cap and a 60-second
per-mode collection deadline. No prompt, model output or credential is copied here.

| Actual connection | Mode | Result | Probe elapsed time | Input / output tokens |
| --- | --- | --- | --- | --- |
| Wi-Fi, VPN paused | structured | MODEL_FAILED / ONLINE_TIMED_OUT | 30695 ms | No usage reported |
| Wi-Fi, VPN paused | plain | MODEL_FAILED / ONLINE_TIMED_OUT | 30601 ms | No usage reported |
| Cellular, VPN paused | structured | MODEL_FAILED / ONLINE_TIMED_OUT | 31390 ms | No usage reported |
| Cellular, VPN paused | plain | MODEL_FAILED / ONLINE_TIMED_OUT | 30558 ms | No usage reported |
| Wi-Fi, original VPN restored | structured | completed, exact expected 15-byte text | 17332 ms | 42 / 12 |
| Wi-Fi, original VPN restored | plain | completed, exact expected 15-byte text | 19286 ms | 21 / 9 |

Each connection was checked before its probe: actual default physical network had INTERNET
and VALIDATED, no active VPN existed in direct controls, and the restored VPN also validated.
Direct cellular readiness took 6267 ms. All three probes completed collection and their
instrumentation reported OK (1 test). The two direct controls failed the separate model-success
assertion; collection success is not model success. These are six bounded calls, not successful
samples selected from unrecorded retries. Full Wi-Fi tasks were not started on the failing
direct paths. Model profiles, timeouts and retry limits were not changed.

Provider code sets a 30000 ms connection timeout, no fixed read/call timeout, and disables
connection-failure retry. The observed times are compatible with a connection timeout, but
ONLINE_TIMED_OUT alone cannot establish the exact transport stage or exclude HTTP 408/504.
The controls establish reachability through the existing VPN at this time; they do not prove
that all networks or every AiGoCode endpoint universally require a VPN.

## Controlled task environment

The original D32 use case requires opening Settings, switching Wi-Fi and reading its state.
It does not require joining an access point or migrating model traffic to Wi-Fi. Both final
tasks therefore used the same controlled condition: the current saved hotspot's Auto-connect
flag was temporarily disabled through the ordinary Settings UI, while cellular and the
existing VPN carried the model calls. Agent itself only navigated, enabled Wi-Fi and read back
the switch; it did not change saved networks or configure this precondition.

Before changing the flag, the operator recorded its visible true value and the saved network
identity privately. The configured-network dump contained 21 records; exactly one allowAutojoin
value changed true -> false, with the same IDs and the other 20 values unchanged. No SSID,
password, proxy, DNS, IP or credential was edited. Both post-task checks preserved this exact
map. The flag was restored through Settings after both tasks, and all 21 values were compared
with the original map.

An earlier attempt to use the shell's temporary carrier autojoin restriction was refused with
SecurityException / exit 255. A bounded diagnostic confirmed the same denial. The tracker was
empty before and after; no Root, additional privilege or system-file edit was used. This refused
setup is not counted as a model task. The eventual condition uses the visible per-hotspot flag,
not that unavailable shell restriction and not a fixed network binding.

The goal, selected target, observe/act/user tool access, Settings confirmation allowlist and
budgets remained identical to the prior pair: 30 steps / 40 model calls / 600000 ms and the
default 300000-token budget. Both started at Home with Wi-Fi=0, the device unlocked, AutoJs6
accessibility actually bound, and actual default cellular plus VPN INTERNET/VALIDATED.
Readiness took 6282 ms for JSON and 62 ms for native. Only modelPath=json/auto and caseId differ.
No model response, decision or device observation was substituted by the fixture.

## Completed pair and path evidence

| Path | Steps / tools / model calls | Input / output / total tokens | Estimated | Task duration |
| --- | --- | --- | --- | --- |
| JSON | 10 / 9 / 10 | 98393 / 779 / 99172 | false | 48154 ms |
| Native | 9 / 8 / 9 | 86317 / 558 / 86875 | false | 53137 ms |

Cases are p91-aigocode-cell-vpn-json-wifi-20260926-01 and
p91-aigocode-cell-vpn-native-wifi-20260926-01. Each final.state and result.status is completed,
unfinished is empty, and there is no task error. Counts come from final.result, not snapshot.step
or the number of usage notifications. These are one functional sample per path; they do not
establish general speed, efficiency or success-rate advantages.

- JSON: all 10 decisions are STRICT; started, usage and completed wire events occur 10 times
  each; there are no tool_calls or submitToolResults continuations. Step 7 observes the unchecked
  switch, step 8 performs ui_click successfully, step 9 observes the same switch checked,
  and only step 10 declares done.
- Native: 8 NATIVE_TOOL decisions precede the final STRICT done. One native request has
  8 tool_calls events, rounds 1-8 with one call each, 8 submitToolResults continuations,
  one completed event and no failed/cancelled event. Its 10 usage notifications are cumulative
  session reports, not 10 model calls. Steps 6/7/8 respectively observe unchecked, click,
  then read checked, before step 9 declares done.
- In both paths the observed switch resource is android:id/switch_widget. Before any manual
  recovery, independent system reads report wifi_on=1 and the visible switch checked=true.
  The same checks confirm cellular/VPN validated, no Wi-Fi transport, the expected saved
  autojoin map, and the still-empty temporary carrier restriction tracker.
- JSON has 13 discrete network samples (56-73329 ms); native has 15 (59-85551 ms). Every sample
  retains a validated cellular default and cellular/VPN Internet, with no Wi-Fi transport;
  the Wi-Fi flag changes 0 -> 1. These are sampled observations, not continuous gap-free monitoring.

The Agent's own before/click/after/done sequence was independently reviewed for both cases.
Original raw model events, continuation records, UI XML and network snapshots remain ignored
private evidence. No SSID, screen text, model text or credential enters the committed receipt.

## Recovery and verification

The original hotspot Auto-connect value is true again, all 21 saved-network autojoin values
match the initial map, and the device is connected to the original hotspot (network ID checked
privately). Wi-Fi and the original VPN both have INTERNET/VALIDATED. Wi-Fi=1, mobile_data=1,
font_scale=1.0, screen_off_timeout=120000, always-on VPN app and lockdown value all match their
initial settings. The exact enabled accessibility component set is unchanged and AutoJs6 is
actually bound. The temporary carrier restriction remains empty. No test task is still active.

This follow-up changes documentation only. Ten-language sources and 36 generated artifacts
pass generation and read-only --check; source-field review preserves the Gemma FAQ and historical
changelog entries. No production code, API, timeout, model budget or retry rule changes, so no
Android rebuild or unrelated test suite is repeated. Original failed direct controls, UI
navigation/setup checks and earlier VPN-handoff failures remain in their private evidence.

The original P9.1 item can now close using this pair plus the existing
[fake-Provider/calculator evidence](p91-comparison-evidence-2026-09-25.md). The
[earlier AiGoCode network failures](p91-aigocode-followup-2026-09-26.md) remain unresolved:
this controlled switch/readback result does not cover or repair default automatic Wi-Fi joining
and the ensuing VPN underlying-network transition. All original ROADMAP items are now checked,
with these limits retained; no additional phase is invented. No order, payment, remote push,
release or publication is performed, and concurrent Rhino work is untouched.
