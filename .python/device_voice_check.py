"""Voice input checks on one device: install the debug and androidTest APKs, then run only the voice related tests.

Usage: py .python/device_voice_check.py --serial QV710AF65F --expect-macaw true [--skip-install]

`--expect-macaw` states whether the 3-Macaw Speech sheet should resolve on that device (installed and its "Use as the
system voice input" switch on). Prints one PASS / FAIL / SKIP line per test and a summary; the device keeps its own
settings because every test restores what it changes. Build the APKs first with
`gradlew :app:assembleDebug :app:assembleDebugAndroidTest`.
"""
import argparse
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
PACKAGE = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
RUNNER = f"{PACKAGE}.test/{PACKAGE}.ThreeStoveAgentTestRunner"
TESTS = [
    f"{PACKAGE}.ui.SpeechInputDeviceTest#realRecognizersMatchTheDeclaredDevice",
    f"{PACKAGE}.ui.SettingsActivityTest#macawPreferenceShowsOnlyWhileItsSheetResolvesAndAppliesAtOnce",
    f"{PACKAGE}.ui.SettingsActivityTest#changesApplyImmediatelyAndSurviveRecreationAndDiskReload",
    f"{PACKAGE}.ui.WorkbenchActivityTest#entryParsingRejectsMalformedSharesAndSpeechFillsWithoutRunning",
]


def adb(serial: str, *args: str) -> str:
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace")
    return result.stdout + result.stderr


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--expect-macaw", required=True, choices=["true", "false"])
    parser.add_argument("--skip-install", action="store_true")
    options = parser.parse_args()
    if not options.skip_install:
        outputs = ROOT / "app/build/outputs/apk"
        for apk in (outputs / "debug").glob("*.apk"), (outputs / "androidTest/debug").glob("*.apk"):
            files = sorted(apk)
            if len(files) != 1:
                print(f"FAIL install: expected one APK, found {len(files)}")
                return 1
            output = adb(options.serial, "install", "-r", "-t", str(files[0]))
            print(("PASS" if "Success" in output else "FAIL") + f" install {files[0].name}")
            if "Success" not in output:
                return 1
    output = adb(options.serial, "shell", "am", "instrument", "-w", "-r", "-e", "class", ",".join(TESTS),
                 "-e", "agent.speech.expectMacaw", options.expect_macaw, RUNNER)
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
    for test in TESTS:
        name = test.rsplit("#", 1)[1]
        status = results.get(name, "MISSING")
        failed += status not in ("PASS", "SKIP")
        reason = results.get(name + ":reason", "") if status == "FAIL" else ""
        print(f"{status} {name}" + (f" ({reason})" if reason else ""))
    summary = re.search(r"OK \((\d+) tests?\)|Tests run: (\d+),\s+Failures: (\d+)", output)
    print(f"SUMMARY serial={options.serial} expectMacaw={options.expect_macaw} failed={failed} " + (summary.group(0) if summary else "no summary"))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
