# P7 lifecycle evidence

This completes the original P7 lifecycle item without adding, splitting or removing roadmap stages. Environment: private API 37.1 AVD, 16 KiB pages, host `cde1fbcf9c` / 5296, production Agent 1.0.0 / 64. Deterministic model replies exercise the real plugin Binder, scheduler, private history and Android windows. These results do not substitute for E4 or the pending six-device/full conformance matrices.

## Changes

- `RunArchive` converts every unfinished persisted state, including queued and waiting interactions, to `failed` with historical reason `process-died`. It drops stale pending requests and never repopulates the runtime queue. Previously terminal records keep their original results, including `blocked / HOST_UNAVAILABLE` after host death. JVM coverage checks every state and idempotent recovery; Android checks the durable per-record file and deletion without resurrection.
- The host now returns `SCREEN_LOCKED` for Agent screen operations while the display is off or keyguard is showing. `ActionTools` propagates that error from the pre-action sample instead of discarding it and attempting the action. Its JVM regression verifies zero action dispatch while locked and a successful explicit subsequent operation after unlocking.
- The floating ball reconciles screen/keyguard state for at most two seconds following screen-on or user-present broadcasts. A new screen-off event invalidates that work, and closing the ball removes its callbacks. There is no recurring idle poll. Android coverage checks three consecutive sleep/wake cycles, remembered position and the still-pending confirmation notification.
- Host startup now initializes context references before providers can serve appearance queries. The actual host-death setup reproduced the previous `Pref` initializer crash; a debug-only startup probe and the repaired external-driver round trip verify the fix. Host source and evidence are in `docs/dev/evidence/ai-agent-p7-lifecycle-20260925.md` in the host repository.

The public JS/AIDL signatures, API AARs and dependency set are unchanged. History still uses its existing versioned format; the host already normalizes a historical string error into a typed result. Ten-language changelogs and generated Markdown describe the behavior changes. Rhino synchronization sources were not edited.

## Cross-process matrix

| Scenario | Verified outcome |
| --- | --- |
| Real host SIGKILL | Plugin PID survives. One running and two queued tasks persist as `blocked / HOST_UNAVAILABLE`. New host attachment has no queued work, no task service and zero model replay. An explicitly new task completes. |
| Real plugin `:agent` SIGKILL | Real Binder death, new plugin PID, three unfinished records recovered as `failed / process-died`, no stale interaction or queued work. Only the explicitly new run calls the replacement model. |
| Plugin force-stop while host holds handles | All three host handles settle as `failed` with `PLUGIN_UNAVAILABLE`. |
| Separate callback process death | Run observer death does not cancel the owned run. Link callback death blocks all admitted tasks and rejects further admission. |
| Cancel during model/tool/confirmation | Duplicate cancellation, two late model/tool replies and a stale approval cannot change the single terminal result or execute subsequent work. JVM tests also cover preparation and scheduler ordering. Cancellation does not undo a device operation already performed. |
| Actual screen-off and visible keyguard | Both return `SCREEN_LOCKED` to the model. The model asks; unlocking alone does not answer the request. Explicit input permits a new real observation and completion. |
| User switches foreground app | A real dump reports `windowChanged`. Separate deterministic decisions either launch the target again and verify its package, or ask while Settings stays foreground. |
| Service lifetime | Queue/detach/cancel regression and absence of task FGS after recovery are checked; explicit fresh work can start its task service normally. |

The host-death driver is `test-apps/ai-agent-conformance/run_host_death.py` in the host repository. It verifies the AVD identity and exact host PID before SIGKILL. Killing that process intentionally aborts its preparation instrumentation. The independent durable-history checks and a passing recovery phase provide acceptance, rather than counting the abort as success.

## Results and failed attempts

- Agent JVM: 469 passed, no failures/errors/skips. Debug and instrumentation APKs assemble, native alignment checks pass, and lint reports 0 errors / 6 existing warnings.
- Final full Agent instrumentation: **73/73 passed in 256.097 s** on the API 37.1 / 16 KiB AVD. This includes private history, hostile observations, UI/notification confirmation, the real 120-second timeout, floating controls and the three wake cycles.
- Host JVM: 22/22 passed. Final host lifecycle/startup suite: **11/11 passed in 17.874 s**. The external real host-death recovery phase: **1/1 passed in 0.211 s**, after verifying the surviving plugin and three durable blocked records.
- First Agent full run: 73 tests, one failure in wake-position restoration, 278.539 s. The isolated old case also failed when the returned frame disappeared after wake. After bounded state reconciliation, the three-cycle isolated case passed in 15.005 s.
- The next full run had two fixture failures, 279.543 s: the positive-control injection test could read a closing previous Activity, and a floating-card collapse tap could use coordinates from before the IME repositioned the window. The fixture now waits for real Activity closure, and the latter activates the visible labeled control. Assertions still require actual canary deletion, frame changes, retained draft, task start and stop. The final 73-test run passed with both changes; no tests were skipped or assertions removed.
- The first host-death preparation failed before the checkpoint because of early `Pref` initialization. After the host context fix, the startup probe and real SIGKILL/recovery flow passed. Its deliberate post-checkpoint `Process crashed` output is distinct from that earlier unexpected failure.
- Markdown generation/check: 10 languages, 36 artifacts. Host changelog rendering is idempotent. Host debug/instrumentation builds and Android-test-source lint analysis pass; whole-host lint remains incomplete from the prior grant session and is not claimed as passed. No runtime dependency changed, so release/R8 was not rerun in this item.

Ignored local evidence uses `build/p7-lifecycle-*` in both repositories. Only summaries and repeatable test sources are committed, not private window dumps, model data or checkpoint task bodies.

## Scope and next item

Only the private AVD was operated. Its original screen timeout, enabled accessibility services, accessibility flag and unsecured-keyguard setting are restored before shutdown. No real phone was locked or reconfigured, and no real model inference, order or payment was performed.

The next original item is the performance baseline: context/parse/validation cost, 200-node round-trip time, peak task memory and per-record store writes. Idle power, full fake Agent/fake host conformance, all six devices (including QV770340J7), security audit, UI acceptance and P8 release gates remain open. P1.3's independent conformance evidence is not closed by these production-plugin tests.

No additional data or manual operation is currently required. Redmi carrier service is unnecessary for this item and the next deterministic performance work. A later real online-model Wi-Fi-off case needs an independent network only during that case; SIM, USB or Ethernet can provide it.
