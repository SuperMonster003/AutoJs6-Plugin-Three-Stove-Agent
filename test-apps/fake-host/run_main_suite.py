"""Run the main Agent instrumentation suite on the disposable conformance AVD that run_conformance.py guards."""
import argparse
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import threading
import time

# Printed by AGP when the instrumentation died with system_server ("INSTRUMENTATION_ABORTED: System has crashed." or
# "Test run failed to complete. ... System has crashed."). In run 39131653130 and its predecessor the API 24 image's
# surfaceflinger took SIGSEGV in GraphicBuffer::flatten while the framework screenshotted a finishing activity, and
# system_server went down with it; the same suite passes on that recipe locally and in most CI runs.
SYSTEM_CRASH = re.compile(r"System has crashed")
GRADLE_TASK = (":app:connectedDebugAndroidTest",
               "-Pandroid.testInstrumentationRunnerArguments.three.stove.agent.ciDiagnostics=true",
               # AGP uninstalls both APKs after the run, which deletes the app-specific external directory holding the
               # CiUiDiagnostics captures before they are pulled below.
               "-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true",
               "--console=plain", "--stacktrace")
AGENT_PACKAGE = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
UI_DIAGNOSTICS = f"/sdcard/Android/data/{AGENT_PACKAGE}/files/ci-ui-diagnostics"
FRAMEWORK_TIMEOUT = 300
BOOT_TIMEOUT = 600
# The adb watchdog: a probe every PROBE_INTERVAL seconds, PROBE_FAILURES misses in a row mean adb is gone. In runs
# 39112487013 (first attempt) and 39137562305 the API 24 guest went silent for 18 seconds right after a test turned
# the screen off; the tests then finished through the instrumentation stream that already existed, but every new adb
# connection timed out (AGP waits 6 minutes for each of its post-run commands), and the job ran into its 45 minute
# limit with all tests passed. The emulator console still answered. One miss covers such a stall; three do not.
PROBE_INTERVAL = 30
PROBE_TIMEOUT = 15
PROBE_FAILURES = 3


def main():
    # CI shows the timestamps of these lines, so they must not wait for the end of the run.
    sys.stdout.reconfigure(line_buffering=True)
    parser = argparse.ArgumentParser(description=__doc__,
                                     epilog="Other options (e.g. --three-button-navigation) go to run_conformance.py --prepare-only.")
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--logcat", default="build/main-suite-logcat.txt",
                        help="Main, system, crash and events buffers of the whole run (one -b per buffer for API 24)")
    parser.add_argument("--retry-once", action="store_true",
                        help="Run the suite once more after it reported 'System has crashed' or after adb stopped answering; "
                             "the first attempt's reports stay in build/main-suite-attempt-1")
    parser.add_argument("--relaunch", metavar="COMMAND",
                        help="Emulator command line that starts this AVD again (same port) when adb stays lost after an adb "
                             "server restart; without it such a run only fails fast")
    parser.add_argument("--emulator-log", default="build/main-suite-emulator.log")
    args, prepare_options = parser.parse_known_args()
    root = Path(__file__).resolve().parents[2]

    def adb(*command, timeout=60):
        return subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True, text=True,
                              encoding="utf-8", errors="replace", timeout=timeout).stdout

    def adb_server(*command):
        subprocess.run([args.adb, *command], capture_output=True, timeout=60)

    def probe():
        try:
            return "ok" in adb("shell", "echo", "ok", timeout=PROBE_TIMEOUT)
        except subprocess.TimeoutExpired:
            return False

    def prepare():
        # The same guards as the conformance run: disposable AVD only, fake host reinstalled, device awake and unlocked.
        command = [sys.executable, str(root / "test-apps/fake-host/run_conformance.py"), "--adb", args.adb,
                   "--serial", args.serial, "--prepare-only", *prepare_options]
        if subprocess.run(command, cwd=root).returncode:
            raise SystemExit("Preparing the main suite failed")

    def kill(process):
        # gradlew execs the JVM on POSIX; gradlew.bat (local runs) starts it as a child, so Windows takes the tree.
        if os.name == "nt":
            subprocess.run(["taskkill", "/F", "/T", "/PID", str(process.pid)], capture_output=True)
        else:
            process.kill()
        process.wait()

    def gradle():
        return str(root / ("gradlew.bat" if os.name == "nt" else "gradlew"))

    def suite():
        """Returns (exit code, system crashed, adb lost); on a lost adb Gradle is killed, since AGP would otherwise wait
        six minutes per adb command until the job timeout, and the run's result cannot be collected anyway."""
        # ANDROID_SERIAL keeps AGP on this emulator when other devices are attached (local runs).
        process = subprocess.Popen([gradle(), *GRADLE_TASK], cwd=root, env=dict(os.environ, ANDROID_SERIAL=args.serial),
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
        lost, stop = threading.Event(), threading.Event()

        def watchdog():
            misses = 0
            while not stop.wait(PROBE_INTERVAL):
                misses = 0 if probe() else misses + 1
                if misses >= PROBE_FAILURES:
                    lost.set()
                    print(f"MAIN_SUITE_ADB_LOST no answer to {misses} probes over {misses * PROBE_INTERVAL}s; stopping Gradle")
                    kill(process)
                    return

        thread = threading.Thread(target=watchdog, daemon=True)
        thread.start()
        crashed = False
        for line in process.stdout:
            sys.stdout.write(line)
            crashed = crashed or SYSTEM_CRASH.search(line) is not None
        code = process.wait()
        stop.set()
        thread.join()
        if code and not crashed and not lost.is_set():
            # A run that failed between two probes (adb erroring out at once rather than hanging): still the device's
            # fault, not the tests', when adb does not answer afterwards.
            for _ in range(PROBE_FAILURES):
                if probe():
                    break
                time.sleep(5)
            else:
                lost.set()
                print("MAIN_SUITE_ADB_LOST adb does not answer after the failed run")
        if lost.is_set():
            # A build cancelled by its client's death may keep its daemon in AGP's adb waits; the rerun needs a free one.
            subprocess.run([gradle(), "--stop"], cwd=root, capture_output=True, timeout=180)
        return code, crashed, lost.is_set()

    def wait_for_framework():
        """sys.boot_completed stays 1 across a framework restart, so wait until the boot animation has stopped again and
        the activity and package services answer, unchanged for 10 seconds."""
        began, steady_since = time.monotonic(), None
        while time.monotonic() - began < FRAMEWORK_TIMEOUT:
            try:
                ready = (adb("shell", "getprop", "init.svc.bootanim").strip() in ("stopped", "")
                         and "activity: found" in adb("shell", "service", "check", "activity")
                         and adb("shell", "pm", "path", "android").startswith("package:"))
            except subprocess.TimeoutExpired:
                ready = False
            if ready:
                steady_since = steady_since or time.monotonic()
                if time.monotonic() - steady_since >= 10:
                    print(f"MAIN_SUITE_FRAMEWORK_READY waited={time.monotonic() - began:.0f}s")
                    return
            else:
                steady_since = None
            time.sleep(2)
        raise SystemExit(f"system_server did not come back within {FRAMEWORK_TIMEOUT}s after the crash")

    def recover_adb():
        """First the host side: a fresh adb server reconnects to the emulator's adb port. Then, with --relaunch, the
        emulator itself: the console (independent of the guest's adbd) kills it, the same command starts the AVD again
        (its data image persists, prepare() reinstalls the APKs) and the emulator runner's own post-boot settings are
        applied again. Returns False when adb stayed lost."""
        adb_server("kill-server")
        adb_server("start-server")
        began = time.monotonic()
        while time.monotonic() - began < 45:
            if probe():
                print(f"MAIN_SUITE_ADB_REVIVED by an adb server restart after {time.monotonic() - began:.0f}s")
                return True
            time.sleep(5)
        if not args.relaunch:
            print("MAIN_SUITE_ADB_LOST stays lost after an adb server restart; no --relaunch given")
            return False
        try:
            adb("emu", "kill", timeout=30)
        except subprocess.TimeoutExpired:
            pass
        began = time.monotonic()
        while time.monotonic() - began < 60 and args.serial in subprocess.run(
                [args.adb, "devices"], capture_output=True, text=True, timeout=30).stdout:
            time.sleep(2)
        emulator_log = root / args.emulator_log
        emulator_log.parent.mkdir(parents=True, exist_ok=True)
        with emulator_log.open("ab") as sink:
            subprocess.Popen(shlex.split(args.relaunch), stdin=subprocess.DEVNULL, stdout=sink, stderr=subprocess.STDOUT)
        print(f"MAIN_SUITE_RELAUNCH {args.relaunch}")
        began = time.monotonic()
        while time.monotonic() - began < BOOT_TIMEOUT:
            try:
                if adb("shell", "getprop", "sys.boot_completed", timeout=PROBE_TIMEOUT).strip() == "1":
                    break
            except subprocess.TimeoutExpired:
                pass
            time.sleep(3)
        else:
            print(f"MAIN_SUITE_RELAUNCH the emulator did not boot within {BOOT_TIMEOUT}s; see {emulator_log}")
            return False
        # What reactivecircus/android-emulator-runner does after the boot: unlock, no animations.
        adb("shell", "input", "keyevent", "82", timeout=30)
        for setting in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
            adb("shell", "settings", "put", "global", setting, "0.0", timeout=30)
        print(f"MAIN_SUITE_BOOTED waited={time.monotonic() - began:.0f}s")
        return True

    log = root / args.logcat
    log.parent.mkdir(parents=True, exist_ok=True)

    def start_logcat(attempt):
        with log.open("ab") as sink:
            sink.write(f"==== main suite attempt {attempt}".encode() + os.linesep.encode())
            adb("logcat", "-G", "16M")
            return subprocess.Popen([args.adb, "-s", args.serial, "logcat", "-v", "threadtime",
                                     "-b", "main", "-b", "system", "-b", "crash", "-b", "events"],
                                    stdin=subprocess.DEVNULL, stdout=sink, stderr=subprocess.STDOUT)

    prepare()
    logcat = start_logcat(1)
    try:
        code, crashed, lost = suite()
        if code and (crashed or lost) and args.retry_once:
            kept = root / "build/main-suite-attempt-1"
            shutil.rmtree(kept, ignore_errors=True)
            reports = root / "app/build/reports/androidTests"
            if reports.is_dir():
                shutil.copytree(reports, kept / "reports")
            reason = "adb was lost" if lost else "system has crashed"
            print(f"MAIN_SUITE_RETRY {reason} in attempt 1; first reports in {kept}, logcat in {log}")
            logcat.terminate()
            if lost:
                if not recover_adb():
                    raise SystemExit(f"Main suite failed (adb was lost and not recovered); logcat in {log}")
            else:
                wait_for_framework()
            # The interrupted attempt leaves the Agent's temporary files and test settings behind (locally the rerun's
            # VisionTransportDeviceTest then found agent-image- files in the cache); clearing its data restores the
            # fresh install the first attempt started from, and prepare() reinstalls and grants as before.
            print(f"MAIN_SUITE_CLEAR {adb('shell', 'pm', 'clear', AGENT_PACKAGE).strip()}")
            prepare()
            logcat = start_logcat(2)
            code, crashed, lost = suite()
        if code:
            if not lost:
                target = root / "build/ci-ui-diagnostics"
                target.parent.mkdir(parents=True, exist_ok=True)
                try:
                    adb("pull", UI_DIAGNOSTICS, str(target), timeout=300)
                except subprocess.TimeoutExpired:
                    pass
            why = " (adb was lost)" if lost else " (system has crashed)" if crashed else ""
            raise SystemExit(f"Main suite failed{why}; logcat in {log}")
        print("MAIN_SUITE_OK")
    finally:
        logcat.terminate()


if __name__ == "__main__":
    main()
