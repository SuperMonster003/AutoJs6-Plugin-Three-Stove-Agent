"""Voice entries of roadmap P17 on one device: install the debug and androidTest APKs, then run only the P17 tests.

Usage: py .python/device_resident_check.py --serial QV710AF65F [--acoustic] [--min-volume 40] [--grant-overlay] [--tests a,b]
       [--skip-install]

Runs the VOICE_COMMAND draft entries (workbench and floating card, E2), the resident listener checks that need no
3-Macaw Speech and the conflict with another app's resident listener (a disabled service of the test APK, E2). With
`--acoustic` it also runs the E3 cases: on a phone with 3-Macaw Speech, its wake word and recognition models and the
microphone granted, the phone plays the pinned test recordings through its own speaker (the media volume as set, raised
to 40 % only when lower and then restored; `--min-volume` raises that floor for a phone whose microphone needs more, for
example 70 on the XQ-AT72, and only when the maintainer agreed); the sentence after the wake word must become a draft from the background
(workbench or notification) and on the floating card, and a round without speech must give the card its earlier draft
back. The floating card cases need the overlay permission: they skip without it, unless `--grant-overlay` lets them
allow it through appops for their duration (restored afterwards; the dialog is never touched). `--tests` limits the run
to the named test methods. Prints one PASS / FAIL / SKIP line per test, the key numbers the tests log (never draft
text), what 3-Macaw Speech's microphone activity did on API 34+, the state the run leaves behind, and a summary. Every
test restores the switch, the wake word settings, the floating ball settings and the media volume it changes.
Build the APKs first with `gradlew :app:assembleDebug :app:assembleDebugAndroidTest`.
"""
import argparse
import pathlib
import re
import subprocess
import sys
import tempfile
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
PACKAGE = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
MACAW = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
RUNNER = f"{PACKAGE}.test/{PACKAGE}.ThreeStoveAgentTestRunner"
RESIDENT = f"{PACKAGE}.speech.ResidentListenerDeviceTest"
TESTS = [
    f"{PACKAGE}.ui.WorkbenchActivityTest#voiceCommandOpensADraftOnlyAndBorrowsTheApplicationLabel",
    f"{RESIDENT}#theSwitchIsTheComponentStateThatOtherAppsQuery",
    f"{RESIDENT}#theSettingsAliasOpensTheResidentRows",
    f"{RESIDENT}#rebootAndUpdateOnlyRemindAnEnabledListener",
    f"{RESIDENT}#aDraftWithoutTheBallOpensTheWorkbenchOrANotification",
    f"{RESIDENT}#voiceCommandFillsTheFloatingCardAndReplacesItsDraft",
    f"{RESIDENT}#anotherResidentListenerKeepsThisOneOff",
]
ACOUSTIC = [
    f"{RESIDENT}#theWakeWordAndTheNextSentenceBecomeADraft",
    f"{RESIDENT}#theFloatingCardListensThenRestoresOrTakesTheDraft",
]
# Only "key=value" numbers and enums of the tests' own log lines are shown.
NUMBERS = re.compile(r"\b(\w+)=([\w.:-]+)")


def adb(serial: str, *args: str) -> str:
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace")
    return result.stdout + result.stderr


def install(serial: str) -> bool:
    outputs = ROOT / "app/build/outputs/apk"
    for apk in (outputs / "debug").glob("*.apk"), (outputs / "androidTest/debug").glob("*.apk"):
        files = sorted(apk)
        if len(files) != 1:
            print(f"FAIL install: expected one APK, found {len(files)}")
            return False
        output = adb(serial, "install", "-r", "-t", str(files[0]))
        print(("PASS" if "Success" in output else "FAIL") + f" install {files[0].name}")
        if "Success" not in output:
            return False
    return True


def state(serial: str) -> dict:
    """Device facts as numbers and enums only: API level, 3-Macaw Speech version, media volume and Stove's overlay mode."""
    volume = re.search(r"volume is (\d+) in range \[0\.\.(\d+)\]", adb(serial, "shell", "cmd", "media_session", "volume", "--stream", "3", "--get"))
    overlay = re.search(r"SYSTEM_ALERT_WINDOW: (\w+)", adb(serial, "shell", "appops", "get", PACKAGE, "SYSTEM_ALERT_WINDOW"))
    macaw = re.search(r"versionCode=(\d+)", adb(serial, "shell", "dumpsys", "package", MACAW))
    return {
        "api": adb(serial, "shell", "getprop", "ro.build.version.sdk").strip(),
        "macawVersion": macaw.group(1) if macaw else "missing",
        "volume": f"{volume.group(1)}/{volume.group(2)}" if volume else "unknown",
        "overlay": overlay.group(1) if overlay else "default",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--acoustic", action="store_true")
    parser.add_argument("--min-volume", type=int, default=40, help="lowest media volume in percent for the acoustic cases")
    parser.add_argument("--grant-overlay", action="store_true")
    parser.add_argument("--tests", default="", help="comma separated test method names")
    parser.add_argument("--skip-install", action="store_true")
    options = parser.parse_args()
    if not options.skip_install and not install(options.serial):
        return 1
    tests = TESTS + (ACOUSTIC if options.acoustic else [])
    if options.tests:
        wanted = {name.strip() for name in options.tests.split(",") if name.strip()}
        tests = [test for test in tests if test.rsplit("#", 1)[1] in wanted]
        if len(tests) != len(wanted):
            print("FAIL unknown or excluded test names (acoustic cases need --acoustic)")
            return 1
    before = state(options.serial)
    print("DEVICE " + " ".join(f"{key}={value}" for key, value in before.items()))
    adb(options.serial, "logcat", "-c")
    # Streamed while the tests run: a run of several minutes overflows the device's log buffer.
    with tempfile.TemporaryFile() as captured:
        stream = subprocess.Popen(["adb", "-s", options.serial, "logcat", "-v", "brief", "-s", "StoveResidentTest:I",
                                   "StoveResidentListener:I", "ActivityTaskManager:V"], stdout=captured, stderr=subprocess.DEVNULL)
        try:
            output = adb(options.serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", ",".join(tests),
                         "-e", "agent.resident.acoustic", "true" if options.acoustic else "false",
                         "-e", "agent.resident.overlay", "appops" if options.grant_overlay else "as-set",
                         "-e", "agent.resident.minVolume", str(options.min_volume), RUNNER)
            time.sleep(2)
        finally:
            stream.terminate()
            stream.wait(timeout=10)
        captured.seek(0)
        log = captured.read().decode("utf-8", errors="replace")
    # Instrumentation status blocks: test name, then INSTRUMENTATION_STATUS_CODE (0 ok, -1 error, -2 failure, -3 ignored, -4 assumption).
    results = {}
    current = None
    for line in output.splitlines():
        if line.startswith("INSTRUMENTATION_STATUS: test="):
            current = line.split("=", 1)[1].strip()
        elif line.startswith("INSTRUMENTATION_STATUS_CODE:") and current:
            code = int(line.split(":", 1)[1])
            if code != 1:
                results[current] = {0: "PASS", -3: "SKIP", -4: "SKIP"}.get(code, "FAIL")
        elif line.startswith("INSTRUMENTATION_STATUS: stack=") and current:
            results.setdefault(current + ":reason", line.split("=", 1)[1].strip()[:200])
    failed = 0
    for test in tests:
        name = test.rsplit("#", 1)[1]
        status = results.get(name, "MISSING")
        failed += status not in ("PASS", "SKIP")
        reason = results.get(name + ":reason", "") if status in ("FAIL", "SKIP") else ""
        print(f"{status} {name}" + (f" ({reason})" if reason else ""))
    for line in log.splitlines():
        if "StoveResidentTest" in line or "StoveResidentListener" in line:
            pairs = NUMBERS.findall(line)
            if pairs:
                print("  " + " ".join(f"{key}={value}" for key, value in pairs))
    # API 34+: 3-Macaw Speech starts its microphone service through a transparent activity while Stove is visible (D74).
    starts = sum(1 for line in log.splitlines() if "START" in line and "MicrophoneStartActivity" in line)
    blocked = sum(1 for line in log.splitlines() if MACAW in line and re.search(r"[Bb]ackground activity (launch|start)|Abort background activity", line))
    print(f"  macawMicrophoneActivityStarts={starts} macawBackgroundStartsBlocked={blocked}")
    after = state(options.serial)
    services = adb(options.serial, "shell", "dumpsys", "activity", "services", f"{PACKAGE}/.speech.ResidentListenerService")
    # Only enabled components answer: Stove's switch and the test APK's rival must both be off again.
    listeners = adb(options.serial, "shell", "cmd", "package", "query-services", "--brief", "-a", "org.autojs.plugin.speech.action.RESIDENT_LISTENER")
    left = len(re.findall(rf"pkg={re.escape(PACKAGE)} id=55[2-5]\b", adb(options.serial, "shell", "dumpsys", "notification")))
    unchanged = after["volume"] == before["volume"] and after["overlay"] == before["overlay"]
    print(f"AFTER volume={after['volume']} overlay={after['overlay']} unchanged={str(unchanged).lower()} "
          f"listenerRunning={str('app=ProcessRecord' in services).lower()} enabledListeners={listeners.count(PACKAGE)} "
          f"residentNotifications={left}")
    failed += not unchanged
    summary = re.search(r"OK \((\d+) tests?\)|Tests run: (\d+),\s+Failures: (\d+)", output)
    print(f"SUMMARY serial={options.serial} acoustic={options.acoustic} minVolume={options.min_volume} grantOverlay={options.grant_overlay} failed={failed} "
          + (summary.group(0) if summary else "no summary"))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
