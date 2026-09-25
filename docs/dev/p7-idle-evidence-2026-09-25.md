# P7 idle lifetime evidence, 2026-09-25

The original power/residency item is complete for the implemented lifetime policy. No production behavior, permissions, public API, dependencies or language strings changed. Agent build 66; real host APK 5296; private API 37.1 / 16 KiB AVD, emulator-5586.

- The existing debug-only, unexported WorkbenchFixtureService counts actual FloatingBall main-looper dispatches. The probe is absent from release. A positive assertion first verifies that it observed real window work.
- Collapsed and expanded windows each settled for 3 seconds after wake/animation, then remained idle for 5 seconds. Both recorded 0 FloatingBall dispatches, 0 model calls, an empty queue and no task foreground service. Process CPU deltas were 3 ms / 1 ms in the isolated case. These are short process observations, not whole-device energy or standby-life measurements.
- A real host binding admitted 1 running + 2 queued tasks. Each active task had a foreground service; finishing the last task removed it. Cancellation also removed the service. Queued work is covered as required by AGENTS section 9.
- After completion was durable, detach and unbind removed the foreground service and overlay. Android `am kill` reclaimed the idle plugin without force-stop or SIGKILL. Fresh attachment used a different PID, loaded completed history, made no model calls and successfully executed exactly one explicitly requested new task.
- Initial recovery testing read history before its asynchronous loader had completed. The final test waits for durable terminal state before reclamation and eventual history availability after attachment; it does not block Binder on disk IO or mistake loading for data loss.

Validation:

| Check | Result |
|---|---|
| Agent JVM | 472 passed, 1 opt-in performance test skipped, 0 failures |
| Agent debug + instrumentation APKs | Built |
| Agent lint | 0 errors, 6 existing warnings |
| Isolated idle-window instrumentation | 1/1, 17.748 s |
| Full Agent instrumentation | 74/74, 284.621 s |
| Host foreground/queue/reclamation instrumentation | 3/3, 1.664 s |

Reproduce with `WorkbenchActivityTest#idleCollapsedAndExpandedFloatingWindowsDoNotPollOrStartForegroundWork`. Host methods are `AiAgentPluginRoundTripTest#foregroundServiceExistsOnlyWhileATaskIsActive`, `#foregroundServiceCoversQueuedWorkAndStopsAfterTheLastCompletion` and `#idleUnboundPluginCanBeReclaimedAndAttachedWithoutReplayingHistory`; enable `autojs.agent.plugin=true` and `autojs.agent.lifecycle=true` on a private unlocked AVD. The reclamation case refuses physical devices. Local logs are under ignored `build/p7-idle-*` in each repository.

Source review: FloatingBall has no idle timer. Its screen-wake reconciliation lasts at most 2 seconds per wake generation; presentation refreshes are event-driven. Visible workbench/history screens have bounded refreshes that stop on stop/disconnect. Interaction presentation posts only on changes. The task service returns START_NOT_STICKY, retires when live work is empty and has no boot receiver. This evidence does not close the independent conformance, compatibility, security, UI or release gates.
