"""Run the fake-host APK only on a dedicated, disposable conformance AVD."""
import argparse
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", default="build/p7-fake-host-android.log")
    parser.add_argument("--prepare-only", action="store_true",
                        help="Install the guarded fake host and Agent for the main instrumentation suite")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]

    def adb(*command, timeout=60):
        result = subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True, text=True,
                                encoding="utf-8", errors="replace", timeout=timeout)
        if result.returncode:
            raise RuntimeError(result.stdout + result.stderr)
        return result.stdout

    if not args.serial.startswith("emulator-") or adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise SystemExit("Fake host requires a disposable emulator, never a physical device")
    if not adb("emu", "avd", "name").splitlines()[0].startswith("AI_Agent_Conformance_"):
        raise SystemExit("Use a disposable AVD named AI_Agent_Conformance_*, with its own data directory")
    installed = adb("shell", "dumpsys", "package", "org.autojs.autojs6")
    if "versionName=" in installed and "versionName=conformance" not in installed:
        raise SystemExit("Refusing to replace a real AutoJs6 installation")
    apks = [root / "test-apps/fake-host/build/outputs/apk/debug/fake-host-debug.apk",
            root / "app/build/outputs/apk/debug/autojs6-plugin-ai-agent-v1.0.0.apk"]
    if not args.prepare_only:
        apks.append(root / "test-apps/fake-host/build/outputs/apk/androidTest/debug/fake-host-debug-androidTest.apk")
    for apk in apks:
        if not apk.is_file():
            raise SystemExit(f"Build required APK first: {apk}")
    for apk in apks:
        if "Success" not in adb("install", "-r", "-t", str(apk), timeout=120):
            raise RuntimeError(f"Installation failed: {apk.name}")
    if args.prepare_only:
        # Only this disposable AVD is changed; real devices and installed hosts
        # were rejected above. Long CI builds may otherwise leave it locked.
        adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
        adb("shell", "wm", "dismiss-keyguard")
        adb("shell", "settings", "put", "system", "screen_off_timeout", "1800000")
        adb("shell", "svc", "power", "stayon", "true")
        if int(adb("shell", "getprop", "ro.build.version.sdk").strip()) >= 33:
            adb("shell", "pm", "grant", "io.github.supermonster003.autojs6.plugin.ai.agent",
                "android.permission.POST_NOTIFICATIONS")
        print("FAKE_HOST_READY main instrumentation prerequisites installed")
        return
    result = adb("shell", "am", "instrument", "-w", "-r",
                 "org.autojs.plugin.ai.agent.fakehost.test/androidx.test.runner.AndroidJUnitRunner", timeout=180)
    output = root / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(result, encoding="utf-8")
    if "OK (4 tests)" not in result or "FAILURES!!!" in result:
        raise SystemExit(f"Conformance failed; see {output}")
    print(f"FAKE_HOST_OK tests=4 log={output}")


if __name__ == "__main__":
    main()
