"""Run the fake-host APK only on a dedicated, disposable conformance AVD."""
import argparse
from pathlib import Path
import re
import subprocess
import sys
import time

# Framework lines worth keeping from a failed attempt: system_server restarts and their causes, user unlock
# progress, service lookups and process deaths. The full buffers go to the diagnostics file.
FRAMEWORK_EVIDENCE = re.compile(
    r"Watchdog|FATAL EXCEPTION IN SYSTEM PROCESS|SystemServer: Entered|system_server|boot_progress_(?:start|system_run|ams_ready)"
    r"|UserController|ssm\.onUnlock|Unable to (?:start|launch|bind)|bad process|lowmemorykiller|lmkd"
    r"|am_(?:crash|proc_died|kill)|Process .* has died|org\.autojs\.autojs6|three\.stove\.agent")
# `am get-started-user-state` is unknown before this API level; there the boot property is the only signal.
USER_STATE_SDK = 26
# Unexported, not direct-boot aware services that the fake host tests and the main suite bind first.
FAKE_HOST_SERVICE = "org.autojs.autojs6/org.autojs.plugin.three.stove.agent.fakehost.FakeHostService"
AGENT_SERVICE = "io.github.supermonster003.autojs6.plugin.three.stove.agent/.ThreeStoveAgentPluginService"


def main():
    # CI shows the timestamps of these lines, so they must not wait for the end of the run.
    sys.stdout.reconfigure(line_buffering=True)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", default="build/p7-fake-host-android.log")
    parser.add_argument("--diagnostics", default="build/p7-fake-host-logcat.txt")
    parser.add_argument("--settle-timeout", type=int, default=300,
                        help="Seconds to wait for a stable system_server before installing and before the tests")
    parser.add_argument("--prepare-only", action="store_true",
                        help="Install the guarded fake host and Agent for the main instrumentation suite")
    parser.add_argument("--release-plugin", action="store_true",
                        help="Exercise the R8 release Agent with this external fake-host test APK; both must use the same test signer")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    version = next(line.split("=", 1)[1].strip() for line in (root / "version.properties").read_text().splitlines() if line.startswith("VERSION_NAME="))

    def adb(*command, timeout=60, attempts=1):
        for remaining in range(attempts - 1, -1, -1):
            result = subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True, text=True,
                                    encoding="utf-8", errors="replace", timeout=timeout)
            if not result.returncode:
                return result.stdout
            if remaining:
                time.sleep(2)
        raise RuntimeError(result.stdout + result.stderr)

    def query(*command):
        # Read-only identity queries. Shortly after boot an API 24 AVD answered them with the right output but a
        # non-zero exit status in two runs in a row, and in CI the runner now starts right after the emulator boots.
        return adb(*command, attempts=5)

    def start_count():
        try:
            return adb("shell", "getprop", "sys.system_server.start_count").strip()
        except (RuntimeError, subprocess.TimeoutExpired):
            return "?"

    def service_lookup(component):
        return adb("shell", "cmd", "package", "query-services", "--components", "-n", component).strip()

    def settle(stage, services=(), quiet=10):
        """Wait until system_server has stayed up for `quiet` seconds with user 0 unlocked and `services` resolvable.

        sys.boot_completed stays 1 across a framework restart (and while user 0 is still locked), so the emulator
        runner's boot check passes while a restarted system_server is still starting. On the API 37.1 16 KB CI image
        installs failed with "Broken pipe" or "before system providers are installed" and the instrumentation died
        with "Process crashed." (runs 2026-09-30 to 2026-10-17). In run 39098321934 every bindService to the fake
        host's own FakeHostService returned false; a lookup that finds no service is one way that happens (a locked
        user hides it, for example), so the tests only start once the lookup succeeds.
        """
        began = time.monotonic()
        last, steady_since = None, None
        while time.monotonic() - began < args.settle_timeout:
            try:
                count = adb("shell", "getprop", "sys.system_server.start_count").strip()
                ready = adb("shell", "getprop", "sys.boot_completed").strip() == "1" and (sdk < USER_STATE_SDK or (
                    adb("shell", "am", "get-started-user-state", "0").strip() == "RUNNING_UNLOCKED"
                    and all("No services found" not in service_lookup(service) for service in services)))
            except (RuntimeError, subprocess.TimeoutExpired):
                count, ready = None, False
            if ready and count == last:
                steady_since = steady_since or time.monotonic()
                if time.monotonic() - steady_since >= quiet:
                    print(f"FAKE_HOST_SETTLED stage={stage} system_server_start_count={count or 'n/a'} waited={time.monotonic() - began:.0f}s")
                    return
            else:
                steady_since = None
            last = count
            time.sleep(2)
        reason = f"system_server did not settle before {stage} within {args.settle_timeout}s (start count {start_count()})"
        diagnostics(f"settle-{stage}", reason)
        raise SystemExit(reason)

    def diagnostics(attempt, reason, instrumentation=None):
        """Keep this disposable emulator's framework evidence for a failed attempt; no real model or user data exists here."""
        sections = {} if instrumentation is None else {"instrumentation output": instrumentation}
        for title, command in (("user 0 state", ("am", "get-started-user-state", "0")),
                               ("fake host service lookup", ("cmd", "package", "query-services", "--components", "-n", FAKE_HOST_SERVICE)),
                               ("processes", ("dumpsys", "activity", "processes")),
                               ("crash buffer", ("logcat", "-d", "-b", "crash")),
                               ("dropbox", ("dumpsys", "dropbox")),
                               ("main, system and events buffers", ("logcat", "-d", "-b", "main,system,events", "-v", "threadtime"))):
            try:
                sections[title] = adb("shell", *command, timeout=120)
            except (RuntimeError, subprocess.TimeoutExpired) as failure:
                sections[title] = f"<unavailable: {failure}>"
        output = root / args.diagnostics
        output.parent.mkdir(parents=True, exist_ok=True)
        with output.open("a", encoding="utf-8") as file:
            file.write(f"==== attempt {attempt}: {reason}\n")
            for title, text in sections.items():
                file.write(f"---- {title}\n{text}\n")
        crashes = [line for line in sections["crash buffer"].splitlines() if "system_server" in line or "FATAL EXCEPTION IN SYSTEM PROCESS" in line]
        bad = [line for line in sections["processes"].splitlines() if "bad" in line.lower()]
        evidence = [line for line in sections["main, system and events buffers"].splitlines() if FRAMEWORK_EVIDENCE.search(line)]
        print(f"FAKE_HOST_DIAGNOSTICS attempt={attempt} {reason}; user0={sections['user 0 state'].strip()[:40]} "
              f"lookup={sections['fake host service lookup'].strip()[:120]!r} system_server_crash_lines={len(crashes)} file={output}")
        for line in bad[:20] + crashes[:40] + evidence[-120:]:
            print("  " + line[:300])

    if not args.serial.startswith("emulator-") or query("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise SystemExit("Fake host requires a disposable emulator, never a physical device")
    if not query("emu", "avd", "name").splitlines()[0].startswith("Three_Stove_Agent_Conformance_"):
        raise SystemExit("Use a disposable AVD named Three_Stove_Agent_Conformance_*, with its own data directory")
    installed = query("shell", "dumpsys", "package", "org.autojs.autojs6")
    if "versionName=" in installed and "versionName=conformance" not in installed:
        raise SystemExit("Refusing to replace a real AutoJs6 installation")
    # The conformance test refuses fingerprints/models that do not look like an emulator; keep the identity in the log.
    print("FAKE_HOST_DEVICE", query("shell", "getprop", "ro.build.fingerprint").strip(), "/", query("shell", "getprop", "ro.product.model").strip())
    sdk = int(query("shell", "getprop", "ro.build.version.sdk").strip())
    variant = "release" if args.release_plugin else "debug"
    apks = [root / "test-apps/fake-host/build/outputs/apk/debug/fake-host-debug.apk",
            root / f"app/build/outputs/apk/{variant}/autojs6-plugin-three-stove-agent-v{version}.apk"]
    if not args.prepare_only:
        apks.append(root / "test-apps/fake-host/build/outputs/apk/androidTest/debug/fake-host-debug-androidTest.apk")
    for apk in apks:
        if not apk.is_file():
            raise SystemExit(f"Build required APK first: {apk}")
    if not args.prepare_only:
        # CI prepares the main suite right after the conformance run; keep both runs' evidence in one file.
        (root / args.diagnostics).unlink(missing_ok=True)

    # A framework restart is an emulator fault, not a test result: retry the whole attempt once after the restart
    # settles, and keep the first attempt's evidence either way. Any failure without a restart fails the run. The
    # new system_server only raises its start count once it runs, so every failure settles before comparing.
    for attempt in (1, 2):
        settle("installation")
        before = start_count()
        try:
            for apk in apks:
                if "Success" not in adb("install", "-r", "-t", str(apk), timeout=120):
                    raise RuntimeError(f"Installation failed: {apk.name}")
        except (RuntimeError, subprocess.TimeoutExpired) as failure:
            settle("evidence")
            after = start_count()
            diagnostics(attempt, f"installation failed; system_server start count {before} -> {after}: {str(failure).strip()[:300]}")
            if before == after or attempt == 2:
                raise SystemExit(f"Installation failed; see {root / args.diagnostics}")
            print(f"FAKE_HOST_RETRY system_server restarted during installation attempt {attempt}; retrying once")
            continue
        # A restart during installation is waited out here, before anything starts in a locked user.
        settle("instrumentation", services=(FAKE_HOST_SERVICE, AGENT_SERVICE))
        if args.prepare_only:
            # Only this disposable AVD is changed; real devices and installed hosts
            # were rejected above. Long CI builds may otherwise leave it locked.
            adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
            adb("shell", "wm", "dismiss-keyguard")
            adb("shell", "settings", "put", "system", "screen_off_timeout", "1800000")
            adb("shell", "svc", "power", "stayon", "true")
            # A slow CI emulator can raise a launcher ANR dialog on top of a test fixture; UiAutomation then reports
            # that system dialog as the active window (run 36323445813, API 35). The disposable AVD hides such dialogs.
            adb("shell", "settings", "put", "global", "hide_error_dialogs", "1")
            if sdk >= 33:
                adb("shell", "pm", "grant", "io.github.supermonster003.autojs6.plugin.three.stove.agent",
                    "android.permission.POST_NOTIFICATIONS")
            print("FAKE_HOST_READY main instrumentation prerequisites installed")
            return
        instrumented = start_count()
        try:
            result = adb("shell", "am", "instrument", "-w", "-r",
                         "org.autojs.plugin.three.stove.agent.fakehost.test/androidx.test.runner.AndroidJUnitRunner", timeout=180)
        except RuntimeError as failure:
            result = str(failure)
        except subprocess.TimeoutExpired:
            # `am instrument -w` never returns when the instrumentation cannot start, e.g. in a locked user.
            result = "am instrument did not finish within 180 s"
        output = root / args.output
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(result, encoding="utf-8")
        if "OK (12 tests)" in result and "FAILURES!!!" not in result:
            print(f"FAKE_HOST_OK tests=12 attempt={attempt} log={output}")
            return
        settle("evidence")
        after = start_count()
        diagnostics(attempt, f"conformance failed; system_server start count {before} -> {instrumented} -> {after}", result)
        if instrumented == after or attempt == 2:
            # Fixture output is deterministic (no real model, device or user data), so it can go straight into CI logs.
            print(result)
            raise SystemExit(f"Conformance failed; see {output} and {root / args.diagnostics}")
        print(f"FAKE_HOST_RETRY system_server restarted during conformance attempt {attempt}; retrying once")


if __name__ == "__main__":
    main()
