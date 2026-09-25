# P7 independent peer conformance, 2026-09-25

The original P7 fake Agent / fake host item is complete. Agent build 67, production host APK 5296. Both directions use published AIDL; no runtime implementation, public contract/AAR, model provider, credentials or device-action permission changed.

## Host against an independent fake Agent

Host repository `test-apps:ai-agent-conformance` now supplies a real IAiAgentPlugin/IAiAgentLink over its signature-protected, host-package-checked driver. It runs as its own UID in `:agent`. A separate test-only control AIDL, mirrored byte-for-byte under host androidTest, selects deliberately hostile events. Worker dispatch avoids inheriting the caller's Binder identity. No published AIDL changed.

`AiAgentPeerConformanceTest` injects only discovery/transport into the production AiAgentLinkController and AiAgentRunRegistry. The installed-plugin inspector remains pinned to the production package; it was not relaxed for a fake package. Each acquired binding has a real unbind lease. Tests assert remote Binder, different PID/UID, plus callback sender identity on the capability reply.

Six cases passed on the private API 37.1 / 16 KiB AVD emulator-5586, 5.551 s:

1. An echo callback emitted before the synchronous startRun reply reaches the host handle. Detach unbinds and the old capability grant rejects further dispatch.
2. Illegal status, unknown event, wrong Bundle type, unsupported version, oversized body and excessive queue count cannot change the valid attachment. Retired-generation callbacks cannot detach or publish into the replacement.
3. Wrong run ID, lower/repeated sequence and post-terminal events cannot overwrite the accepted result or emit duplicate completion.
4. Invalid run events fail only their handle with INVALID_REQUEST; a later valid task still completes.
5. Link-status/link-event/run-event callbacks carrying unexpected descriptors are rejected. A real pipe reaches EPIPE after both sender-side copies close, proving receiver-side closure, not just a mock close call.
6. Actual fake-Agent process death fails the old host handle, releases its binding and permits reattachment without starting any task; only an explicit new task runs.

The existing grant suite also passed 9/9, 3.419 s, including unauthorized UID entries and three foreign attach-broadcast identities. The host instrumentation build, test-source lint, and fake-Agent APK/lint passed. Fake-Agent lint: 0 errors / 2 existing manifest warnings. Whole-host lint remains pending.

## Independent fake host against the real Agent

New `test-apps:fake-host` is debug-only and testOnly. Its package is the real `org.autojs.autojs6`, version 5289, signed like the plugin. This preserves the production package/version/signer verifier. Its private `:broker` service holds model/capability/callback Binders, separately from instrumentation; the plugin is another UID/process. It has no network, storage, accessibility or device-control permission.

The fake host ran only in a freshly created `AI_Agent_Conformance_P7` AVD with its own ignored build-directory data, emulator-5588. No real host installation or configured model data was replaced. `run_conformance.py` refuses physical devices, other AVD names and existing real AutoJs6 installs before installing anything.

Four cases passed, 1.288 s:

- Production attach/start/cancel/detach; attached grant widening is rejected and detached start is refused.
- A disabled shell tool produces a validation observation and never reaches the capability broker; corrected output finishes.
- An allowed device-info request receives an explicit broker grant denial; CAPABILITY_DENIED reaches the next model call and the task can finish.
- Actual broker-process death blocks one running plus two queued tasks with HOST_UNAVAILABLE. The plugin remains alive. New attachment preserves blocked history, makes zero model calls until a new task is explicitly started, and then completes it once.

Debug/test APKs and fake-host lint passed, 0 errors / 2 test-only manifest warnings (backup-rule recommendation and no icon). The fixture's release variant is disabled and its APK is excluded from app release artifacts. CI builds/lints the new fixture; GitHub CI execution itself was not run locally. Existing dependency versions and locked API AARs are reused. Agent source was unchanged from the idle item except build number and build/test infrastructure; its prior same-turn 472 passing JVM / 1 performance skip and 74/74 full instrumentation remain applicable.

Reproduction is documented in `test-apps/fake-host/README.md` and the host fixture README. Ignored logs: Agent `build/p7-fake-host-*`, host `build/p7-peer-*` and `build/p7-conformance-build-final.log`. These deterministic checks do not count as real-model E4 acceptance or the six-device compatibility matrix. P1.3 has gained its missing independent attach/detach/death evidence; its remaining positive production attach-broadcast acceptance is kept explicit rather than inferred from a foreign-package rejection.

Cleanup: the original AVD's timeout, accessibility settings and keyguard flag were restored and read back against `build/p7-idle-baseline.json`. Both emulators were shut down. The disposable AVD data remains under ignored `build/p7-fake-host-avd-home`; the local execution policy rejected recursive cleanup. Its generated hardware-path evidence is retained separately. Physical devices and their model configuration were not changed. The installation runner's refusal on the original configured AVD was also verified before any installation attempt.
