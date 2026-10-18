"""Run the main Agent instrumentation suite on the disposable conformance AVD that run_conformance.py guards."""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
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


def main():
    # CI shows the timestamps of these lines, so they must not wait for the end of the run.
    sys.stdout.reconfigure(line_buffering=True)
    parser = argparse.ArgumentParser(description=__doc__,
                                     epilog="Other options (e.g. --three-button-navigation) go to run_conformance.py --prepare-only.")
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--logcat", default="build/main-suite-logcat.txt",
                        help="Main, system, crash and events buffers of the whole run (one -b per buffer for API 24)")
    parser.add_argument("--retry-system-crash", action="store_true",
                        help="Run the suite once more after it reported 'System has crashed'; the first attempt's reports "
                             "stay in build/main-suite-attempt-1")
    args, prepare_options = parser.parse_known_args()
    root = Path(__file__).resolve().parents[2]

    def adb(*command, timeout=60):
        return subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True, text=True,
                              encoding="utf-8", errors="replace", timeout=timeout).stdout

    def prepare():
        # The same guards as the conformance run: disposable AVD only, fake host reinstalled, device awake and unlocked.
        command = [sys.executable, str(root / "test-apps/fake-host/run_conformance.py"), "--adb", args.adb,
                   "--serial", args.serial, "--prepare-only", *prepare_options]
        if subprocess.run(command, cwd=root).returncode:
            raise SystemExit("Preparing the main suite failed")

    def suite():
        gradle = root / ("gradlew.bat" if os.name == "nt" else "gradlew")
        # ANDROID_SERIAL keeps AGP on this emulator when other devices are attached (local runs).
        process = subprocess.Popen([str(gradle), *GRADLE_TASK], cwd=root, env=dict(os.environ, ANDROID_SERIAL=args.serial),
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
        crashed = False
        for line in process.stdout:
            sys.stdout.write(line)
            crashed = crashed or SYSTEM_CRASH.search(line) is not None
        return process.wait(), crashed

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

    log = root / args.logcat
    log.parent.mkdir(parents=True, exist_ok=True)
    adb("logcat", "-G", "16M")
    with log.open("ab") as sink:
        logcat = subprocess.Popen([args.adb, "-s", args.serial, "logcat", "-v", "threadtime",
                                   "-b", "main", "-b", "system", "-b", "crash", "-b", "events"],
                                  stdin=subprocess.DEVNULL, stdout=sink, stderr=subprocess.STDOUT)
    try:
        prepare()
        code, crashed = suite()
        if code and crashed and args.retry_system_crash:
            kept = root / "build/main-suite-attempt-1"
            shutil.rmtree(kept, ignore_errors=True)
            reports = root / "app/build/reports/androidTests"
            if reports.is_dir():
                shutil.copytree(reports, kept / "reports")
            print(f"MAIN_SUITE_RETRY system has crashed in attempt 1; first reports in {kept}, logcat in {log}")
            wait_for_framework()
            # The interrupted attempt leaves the Agent's temporary files and test settings behind (locally the rerun's
            # VisionTransportDeviceTest then found agent-image- files in the cache); clearing its data restores the
            # fresh install the first attempt started from, and prepare() reinstalls and grants as before.
            print(f"MAIN_SUITE_CLEAR {adb('shell', 'pm', 'clear', AGENT_PACKAGE).strip()}")
            prepare()
            code, crashed = suite()
        if code:
            target = root / "build/ci-ui-diagnostics"
            target.parent.mkdir(parents=True, exist_ok=True)
            try:
                adb("pull", UI_DIAGNOSTICS, str(target), timeout=300)
            except subprocess.TimeoutExpired:
                pass
            raise SystemExit(f"Main suite failed{' (system has crashed)' if crashed else ''}; logcat in {log}")
        print("MAIN_SUITE_OK")
    finally:
        logcat.terminate()


if __name__ == "__main__":
    main()
