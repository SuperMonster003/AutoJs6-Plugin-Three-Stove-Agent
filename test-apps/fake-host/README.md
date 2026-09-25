# Independent fake host conformance APK

This debug-only, `testOnly` module is a minimal host with the **real host application ID** `org.autojs.autojs6`, version 5289 and the same optional local signer as the Agent. It exercises the production installed-package/version/signer verifier without a debug bypass or any production API change. It has no network, accessibility, storage or device-action permission. The test control service is unexported and runs in `:broker`, separately from instrumentation.

Install only in a new disposable AVD named `AI_Agent_Conformance_*` with a separate data directory. Do not install over AutoJs6 or on a physical device. `run_conformance.py` checks the emulator identity, AVD name and installed host version before any install, and refuses a real host. The runner does not uninstall or clear applications. The module disables release variants and is not part of plugin distribution.

Build using the repository JDK and vendor flags:

```text
gradlew :app:assembleDebug :test-apps:fake-host:assembleDebug :test-apps:fake-host:assembleDebugAndroidTest :test-apps:fake-host:lintDebug
python test-apps/fake-host/run_conformance.py --adb PATH_TO_ADB --serial emulator-5588
```

The eight tests cover real attach/start/cancel/detach, refusal to widen an attached grant, a disabled tool never reaching the broker, capability rejection reaching the next model observation, and actual broker-process death blocking one running plus two queued tasks. They also cover a native batch/result round trip with cumulative usage, whole-batch rejection before execution, a tools-capable target on a legacy broker retaining JSON, and cancellation while a native tool waits. Fresh attachment preserves blocked history, does not replay tasks and accepts an explicitly requested new task. UID/PID and remote-Binder assertions prevent same-process mocks from being mistaken for this evidence. Models and tool results are deterministic; no real model/device action is performed.

For R8 validation, assemble the release Agent and use `run_conformance.py --release-plugin` with the same AVD checks. Build both APKs in an isolated checkout using the standard Android test key, never the production signer for the fake host. The external instrumentation targets the fake-host APK and drives the release Agent through Binder; it does not load debug tests into an R8 application. The APK version is read from `version.properties`. This mode leaves all production package, version and signer checks enabled.

GitHub Actions also uses a fresh `AI_Agent_Conformance_CI_*` AVD for the full
Agent instrumentation suite. After assembling the fake host and Agent debug
APKs, `run_conformance.py --prepare-only` applies the same installation guards,
installs the two APKs, keeps the disposable device awake and unlocked, and
grants the Agent notification permission on API 33+. This mode prepares the
real package/version checks used by the workbench; it does not bypass them or
run the eight independent broker tests. The main suite then runs normally.

After testing, shut down the disposable AVD. Keep logs if needed and remove only that newly created AVD data directory. The main Agent instrumentation remains in `app/src/androidTest`; this module complements it and the host repository's independent fake Agent, rather than replacing those suites.
