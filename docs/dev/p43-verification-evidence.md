# P4.3 verification and completion evidence

Date: 2026-09-23. Plugin: 1.0.0 / build 42. Minimum host: AutoJs6 6.8.0 / 5289.
Scope: the three original P4.3 items. No roadmap items were added, split or dropped.
P4.4 real-model acceptance remains pending.

## Loop rules

Each AgentRunner owns its LoopRules. The third consecutive equivalent action
proposal terminates the run as blocked before a third confirmation or execution.
Read-only tools, progress reports and questions cannot erase the action streak.
A different action starts a new streak. Read-only registered scripts are exempt.
Decisions that fail tool preparation still obey the ordinary task/model budgets.

Action identity includes the tool, canonical arguments and trusted inspection
context. JSON key ordering and transport snapshot IDs do not distinguish actions.
Published node references use a fingerprint of the observed window and node plus
bounds, so a changed display ordinal alone cannot reset the streak. A different
observed window is a different node identity. The digest stays private and is
never inserted into model guidance or ordinary logs. Selector aliases are not
proven semantically equivalent; this heuristic does not replace confirmation or
the host's target binding.

Three consecutive screen actions with complete unchanged readbacks set
changeStrategy. An unchanged window with changed contents counts as progress.
Partial, baseline, unknown and failed samples do not prove absence of progress.
Read-only observations do not increment the count. The runtime counters and
observeRequired survive history trimming in the mandatory system prompt. A
clipboard read cannot satisfy a screen observation request.

English and Chinese full/compact prompts require observation before deciding
after an action, references to observed facts in done.evidence, ask for missing
information, and ask(kind: confirm) for consequential work beyond the goal.
The observe/strategy flags guide model decisions; they are not an additional
hard tool-admission gate. Repetition blocking is enforced by the runner.

## Completion and order state

DoneRules converts completed without evidence, or completed with unfinished
work, into partial. It replaces the misleading completion summary with a fixed
localized explanation and retains bounded unfinished entries. Partial results
always include unfinished work, including budget termination after a successful
tool call. The recorded decision and terminal result use the same effective
status; rule feedback retains the proposed status and fixed reason codes.

Order/shopping/payment goal terms and trusted payment inspection require
done.orderStatus. Missing state uses the existing two-repair allowance for the
same step, shared with syntax repairs and still charged to the model budget.
Exhaustion yields DECISION_UNPARSABLE. The runner never invents none, cart,
pending_payment, submitted or paid. Engine-generated failures, cancellation and
loop termination may have no known order state; they do not claim model completion.
none means observed absence of an order, not an unknown state.

Goal terms are packaged in ten languages, separate from the confirmation gate's
payment keywords. Word boundaries avoid matching payload or payment_test;
qualified money-transfer terms avoid treating file transfer or translation as
payment. Literal matching avoids compiling Unicode regexes during synchronous
Binder task admission. Goal matching is a conservative keyword heuristic, not
complete natural-language intent recognition. Prompts also require order state
for payment goals outside the keyword table. Nonempty evidence is a structural
check, not independent proof that a model's natural-language claim is true.

## Validation

| Check | Final result |
|---|---|
| Plugin JVM | 362/362, 29 new cases since P4.2, no failures/errors/skips |
| Plugin instrumentation, API 24 x86 / 4 KiB | 31/31, including four new Android cases |
| Plugin instrumentation, API 37 x86_64 / 16 KiB | 31/31 |
| Host capability broker, each AVD | 5/5 using the saved host APKs |
| Selected host/plugin round trips, each AVD | 17 passed, one optional Wi-Fi skip, two old fixtures excluded as explained below |
| Host initialization fixture, each AVD | One registered-script result/console case passed before the selected round trips |
| Additional host model-broker suite, each AVD | Three passed, five optional fake-Provider conformance cases skipped |
| Builds | debug, androidTest, release/R8 and native-alignment checks passed |
| Lint | 0 errors, 6 existing warnings: NewerVersionAvailable x2, IconDuplicatesConfig x2, StaticFieldLeak x1, UnusedResources x1 |
| Documentation | 10 languages / 36 generated artifacts, freshness check passed |

The host instrumentation command reports 19 discovered tests: 18 passes
(17 round trips plus one initialization fixture) and one conditional skip.
No production confirmation, caller verification or host grant was disabled.

The added JVM cases cover repeat detection across read-only turns, canonical
arguments, reference renumbering, window identity, missing/partial observations,
content progress, context trimming, task isolation, cancellation, evidence
downgrades, bounded unfinished lists, order requirements, shared repair budgets
and language matching. Android tests cover packaged rule assets, the cached
runtime policy used at Binder admission, actual Android scheduling, missing
evidence, the third action block, information questions, payment denial and
order-state repair. Local prompt packing remains within 7500 input bytes and a
combined 4096 estimated input/output tokens in the Android regression fixture.

Integration found a missing order-intent asset in AgentRuntime's preloaded
policy map. Without it, startRun returned INVALID_REQUEST. The cache and an
Android regression now cover this entry point. An intermediate concurrent run
also hit the existing 200 ms startRun assertion. Per-admission Unicode regex
compilation was removed; final selected round trips passed the original timing
assertion. This does not replace the separate P7 cold-start/load performance gate.

An isolated currentWindow test initially returned an empty package because the
host's lazily initialized activity information provider missed the opening
Settings event. The final test command first executes the existing
AgentRegisteredScriptExecutionTest#noReportPreservesNullResultAndConsoleFallback
in the same instrumentation process, then runs the selected round trips from
Home. Both AVDs pass without modifying host code or relaxing assertions.

## Host isolation and deferred fixture alignment

Another process was synchronizing the host's upstream Rhino engine. This task
did not edit, build or commit the host repository. It copied the existing P4.2
debug/test APKs into ignored build/p43-host-baseline before testing. Those APKs
come from host 0d1c7cc788 / build 5289; this is not validation of the in-progress
Rhino changes. The three API AARs, lock, required host version and public JS/AIDL
contracts are unchanged.

| Saved host artifact | SHA-256 |
|---|---|
| host-x86.apk | f3b20df8ed529e02ac529a9e69d488e5329b69e55de27ca649a7d8ca14f81d07 |
| host-x86_64.apk | 6a60601e4e48e42bdca14460e7b02df007d763e0a8e984299e7da493e11f9b08 |
| host-tests.apk | ed91c3305aa07df38d917f7fba5c2d519179f5ea99c457be15af6cd072d6353a |

Two saved AiAgentPluginRoundTripTest fixtures inspect a synthetic Pay button but
their scripted done response omits orderStatus:

- inspectedNodeActionsAppendPasswordsAndStaleConfirmationUseTheRealHost
- changedChildLabelCannotAuthorizeThePreviouslyInspectedParent

They were excluded from this baseline run, not counted as passed. Their old
model responses are incompatible with P4.3's stricter order-state rule. The
fixture-only [patch](p43-host-test-fixtures.patch) adds explicit none for these
synthetic, no-order screens and preserves all existing assertions. git apply
--check passes against the current host test file. The patch has not been applied,
compiled or executed; it must be committed and the two fixtures rerun in the
host repository after the concurrent synchronization is finished. It contains no
host production changes and no bypass of P4.3. On a checkout that converts patch
files to CRLF, use git apply --ignore-space-change --check before applying it.

Reproduction commands use the usual JDK 21 / Temurin vendor arguments:

```text
gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:lintDebug
py .python/generate_markdown.py --check
adb -s <owned-avd> shell am instrument -w -r io.github.supermonster003.autojs6.plugin.ai.agent.test/androidx.test.runner.AndroidJUnitRunner
adb -s <owned-avd> shell am instrument -w -r -e class org.autojs.autojs.core.plugin.hostbroker.HostCapabilityBrokerStubTest org.autojs.autojs6.test/androidx.test.runner.AndroidJUnitRunner
```

For round trips, enable autojs.agent.plugin, autojs.agent.observe and
autojs.agent.actions, and select the initialization fixture followed by all
AiAgentPluginRoundTripTest methods except the two listed above. Keep the optional
real Wi-Fi toggle disabled. Local command selections, full logs, APK hashes and
machine-readable counts are in ignored build/p43-device/.

Only the owned read-only emulator instances on ports 5584 and 5586 were operated.
No physical device, real model, real shopping application or payment was used.
The instances are shut down after verification. These are E0/E1 checks; P4.4 still
requires the stated online/local model targets and physical-device E4 evidence.
