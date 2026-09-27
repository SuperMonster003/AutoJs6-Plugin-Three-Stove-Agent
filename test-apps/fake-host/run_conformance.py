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
    parser.add_argument("--release-plugin", action="store_true",
                        help="Exercise the R8 release Agent with this external fake-host test APK; both must use the same test signer")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    version = next(line.split("=", 1)[1].strip() for line in (root / "version.properties").read_text().splitlines() if line.startswith("VERSION_NAME="))

    def adb(*command, timeout=60):
        result = subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True, text=True,
                                encoding="utf-8", errors="replace", timeout=timeout)
        if result.returncode:
            raise RuntimeError(result.stdout + result.stderr)
        return result.stdout

    if not args.serial.startswith("emulator-") or adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise SystemExit("Fake host requires a disposable emulator, never a physical device")
    if not adb("emu", "avd", "name").splitlines()[0].startswith("Three_Stove_Agent_Conformance_"):
        raise SystemExit("Use a disposable AVD named Three_Stove_Agent_Conformance_*, with its own data directory")
    installed = adb("shell", "dumpsys", "package", "org.autojs.autojs6")
    if "versionName=" in installed and "versionName=conformance" not in installed:
        raise SystemExit("Refusing to replace a real AutoJs6 installation")
    # The conformance test refuses fingerprints/models that do not look like an emulator; keep the identity in the log.
    print("FAKE_HOST_DEVICE", adb("shell", "getprop", "ro.build.fingerprint").strip(), "/", adb("shell", "getprop", "ro.product.model").strip())
    variant = "release" if args.release_plugin else "debug"
    apks = [root / "test-apps/fake-host/build/outputs/apk/debug/fake-host-debug.apk",
            root / f"app/build/outputs/apk/{variant}/autojs6-plugin-three-stove-agent-v{version}.apk"]
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
        # A slow CI emulator can raise a launcher ANR dialog on top of a test fixture; UiAutomation then reports
        # that system dialog as the active window (run 36323445813, API 35). The disposable AVD hides such dialogs.
        adb("shell", "settings", "put", "global", "hide_error_dialogs", "1")
        if int(adb("shell", "getprop", "ro.build.version.sdk").strip()) >= 33:
            adb("shell", "pm", "grant", "io.github.supermonster003.autojs6.plugin.three.stove.agent",
                "android.permission.POST_NOTIFICATIONS")
        print("FAKE_HOST_READY main instrumentation prerequisites installed")
        return
    result = adb("shell", "am", "instrument", "-w", "-r",
                 "org.autojs.plugin.three.stove.agent.fakehost.test/androidx.test.runner.AndroidJUnitRunner", timeout=180)
    output = root / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(result, encoding="utf-8")
    if "OK (12 tests)" not in result or "FAILURES!!!" in result:
        # Fixture output is deterministic (no real model, device or user data), so it can go straight into CI logs.
        print(result)
        raise SystemExit(f"Conformance failed; see {output}")
    print(f"FAKE_HOST_OK tests=12 log={output}")


if __name__ == "__main__":
    main()
