"""Voice entries of roadmap P17 on one device: install the debug and androidTest APKs, then run only the P17 tests.

Usage: py .python/device_resident_check.py --serial QV710AF65F [--acoustic] [--skip-install]

Runs the VOICE_COMMAND draft entry (E2) and the resident listener checks that need no 3-Macaw Speech (E2). With
`--acoustic` it also runs the E3 case: on a phone with 3-Macaw Speech, its wake word and recognition models and the
microphone granted, the phone plays the pinned test recording through its own speaker (the media volume as set,
raised to 40 % only when lower and then restored)
and the sentence after the wake word must become a draft. Prints one PASS / FAIL / SKIP line per test, the key numbers
the tests log (never draft text) and a summary. Every test restores the switch and the wake word settings it changes.
Build the APKs first with `gradlew :app:assembleDebug :app:assembleDebugAndroidTest`.
"""
import argparse
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
PACKAGE = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
RUNNER = f"{PACKAGE}.test/{PACKAGE}.ThreeStoveAgentTestRunner"
RESIDENT = f"{PACKAGE}.speech.ResidentListenerDeviceTest"
TESTS = [
    f"{PACKAGE}.ui.WorkbenchActivityTest#voiceCommandOpensADraftOnlyAndBorrowsTheApplicationLabel",
    f"{RESIDENT}#theSwitchIsTheComponentStateThatOtherAppsQuery",
    f"{RESIDENT}#theSettingsAliasOpensTheResidentRows",
    f"{RESIDENT}#rebootAndUpdateOnlyRemindAnEnabledListener",
    f"{RESIDENT}#aDraftWithoutTheBallOpensTheWorkbenchOrANotification",
]
ACOUSTIC = f"{RESIDENT}#theWakeWordAndTheNextSentenceBecomeADraft"
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


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--acoustic", action="store_true")
    parser.add_argument("--skip-install", action="store_true")
    options = parser.parse_args()
    if not options.skip_install and not install(options.serial):
        return 1
    tests = TESTS + ([ACOUSTIC] if options.acoustic else [])
    adb(options.serial, "logcat", "-c")
    output = adb(options.serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", ",".join(tests),
                 "-e", "agent.resident.acoustic", "true" if options.acoustic else "false", RUNNER)
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
    for line in adb(options.serial, "logcat", "-d", "-s", "StoveResidentTest:I", "StoveResidentListener:I").splitlines():
        pairs = NUMBERS.findall(line)
        if pairs:
            print("  " + " ".join(f"{key}={value}" for key, value in pairs))
    summary = re.search(r"OK \((\d+) tests?\)|Tests run: (\d+),\s+Failures: (\d+)", output)
    print(f"SUMMARY serial={options.serial} acoustic={options.acoustic} failed={failed} " + (summary.group(0) if summary else "no summary"))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
