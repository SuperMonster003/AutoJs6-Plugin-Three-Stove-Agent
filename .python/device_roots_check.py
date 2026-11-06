"""Roadmap P23.4 on one device: script directories the host turns down are reported at once (host issue #589).

Usage: py .python/device_roots_check.py --serial 63751f63 [--min-host-build 6640] [--plugin-build 446]
       [--working-dir /storage/emulated/0/Scripts] [--values values]

Needs a debuggable host (`run-as` reads its working directory unless `--working-dir` is given) and the debuggable
plugin APK installed (`gradlew :app:assembleDebug`, then `adb install -r`). Two cases, each one a cold start of the
workbench (the plugin is force-stopped, `shared_prefs/script-roots.xml` is written through `run-as`, then
LauncherActivity starts and sends its automatic attach request):

- parent: the configured directory is the parent of the AutoJs6 working directory. The connection banner must show
  `script_roots_rejected` well before the 15-second deadline (the host answered the ordered broadcast with
  ROOTS_REJECTED), the host log must say "Attach request dropped: ROOTS_REJECTED", and the host's global log line
  (the directory requirements) must be in logcat.
- self: the configured directory is the working directory itself. The banner must disappear (link attached) and the
  two apps must be bound to each other; no ROOTS_REJECTED anywhere.

Prints the device line, one PASS / FAIL line per check with the key numbers (never a UI dump), and a summary. The
plugin's original script-roots file (usually none) is put back and the device returns to the home screen; the host
stays as installed. Texts are matched against the plugin's own string resources of `--values` (default `values`,
English), so the device locale must match that directory.
"""
import argparse
import html
import pathlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
PLUGIN = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
HOST = "org.autojs.autojs6"
LAUNCHER = f"{PLUGIN}/.ui.LauncherActivity"
PREFS = "shared_prefs/script-roots.xml"
UI_DUMP = "/sdcard/.aj6-p23-ui.xml"
DEADLINE_MS = 15000
PLACEHOLDER = re.compile(r"%\d\$[sd]")


def adb(serial, *args, data=None, timeout=60):
    result = subprocess.run(["adb", "-s", serial, *args], input=data, capture_output=True, timeout=timeout)
    return (result.stdout + result.stderr).decode("utf-8", "replace").replace("\r\n", "\n").strip()


def shell(serial, command, **kwargs):
    return adb(serial, "shell", command, **kwargs)


def run_as(serial, package, command, **kwargs):
    return shell(serial, f"run-as {package} sh -c '{command}'", **kwargs)


def package_info(serial, package):
    dump = shell(serial, f"dumpsys package {package}")
    code = re.search(r"versionCode=(\d+)", dump)
    updated = re.search(r"lastUpdateTime=(\S+ \S+)", dump)
    return (int(code.group(1)) if code else 0, updated.group(1) if updated else "?")


def device_time(serial):
    return shell(serial, "date '+%m-%d %H:%M:%S.000'")


def fragments(values):
    """Longest placeholder-free part of each banner text the checks tell apart, unescaped like Android does."""
    tree = ET.parse(ROOT / "app" / "src" / "main" / "res" / values / "strings.xml")
    wanted = {
        "rejected": "script_roots_rejected",
        "connecting": "launcher_link_connecting",
        "failed": "launcher_link_failed",
        "timeout": "launcher_link_timeout",
        "timeout_accepted": "launcher_link_timeout_accepted",
        "not_reached": "launcher_link_not_reached",
        "host_ready": "launcher_host_ready",
    }
    texts = {node.get("name"): "".join(node.itertext()) for node in tree.getroot() if node.tag == "string"}
    out = {}
    for key, name in wanted.items():
        text = texts[name].replace('\\"', '"').replace("\\'", "'")
        out[key] = max(PLACEHOLDER.split(text), key=len).strip()
    return out


def ui_texts(serial):
    output = shell(serial, f"uiautomator dump {UI_DUMP}")
    if "dumped" not in output:
        return None
    raw = shell(serial, f"cat {UI_DUMP}")
    try:
        root = ET.fromstring(raw)
    except ET.ParseError:
        return None
    return [html.unescape(node.get("text") or "") for node in root.iter("node") if node.get("text")]


def stage_of(texts, frags):
    joined = "\n".join(texts)
    for key in ("rejected", "timeout_accepted", "timeout", "not_reached", "failed", "connecting", "host_ready"):
        if frags[key] and frags[key] in joined:
            return key
    return "hidden"


def resumed(serial):
    dump = shell(serial, "dumpsys activity activities")
    match = re.search(r"topResumedActivity=ActivityRecord\{\S+ u0 (\S+)", dump) or re.search(r"ResumedActivity: ActivityRecord\{\S+ u0 (\S+)", dump)
    return match.group(1) if match else "?"


def bound_between(serial):
    """Whether any service binding joins the two apps (either direction)."""
    plugin_dump = shell(serial, f"dumpsys activity services {PLUGIN}")
    host_dump = shell(serial, f"dumpsys activity services {HOST}")
    return (HOST in plugin_dump), (PLUGIN in host_dump)


def write_roots(serial, roots):
    items = ",".join("&quot;" + root + "&quot;" for root in roots)
    xml = ("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
           f'    <string name="configuration">{{&quot;scriptRoots&quot;:[{items}]}}</string>\n</map>\n')
    run_as(serial, PLUGIN, f"cat > {PREFS} && chmod 660 {PREFS}", data=xml.encode("utf-8"))
    return run_as(serial, PLUGIN, f"cat {PREFS}").count("scriptRoots") == 1


def logcat_since(serial, marker):
    lines = shell(serial, f"logcat -d -v threadtime -T '{marker}'", timeout=120).split("\n")
    return [line for line in lines if any(tag in line for tag in ("ThreeStoveAgentAttach", "ThreeStoveAgentLink", "LauncherActivity", "were not accepted", "GlobalConsole"))]


def log_time(line):
    match = re.match(r"\d\d-\d\d (\d\d):(\d\d):(\d\d)\.(\d\d\d)", line)
    if not match:
        return None
    h, m, s, ms = (int(part) for part in match.groups())
    return ((h * 60 + m) * 60 + s) * 1000 + ms


def run_case(serial, name, roots, frags, wait_ms, expect):
    shell(serial, f"am force-stop {PLUGIN}")
    time.sleep(1)
    if not write_roots(serial, roots):
        return False, f"could not write {PREFS}"
    marker = device_time(serial)
    started = time.monotonic()
    shell(serial, f"am start -W -n {LAUNCHER}")
    stage, first_ms, polls = "unknown", None, 0
    while (time.monotonic() - started) * 1000 < wait_ms:
        texts = ui_texts(serial)
        polls += 1
        if texts is None:
            time.sleep(0.3)
            continue
        stage = stage_of(texts, frags)
        if stage not in ("connecting", "unknown"):
            first_ms = int((time.monotonic() - started) * 1000)
            break
        time.sleep(0.5)
    if first_ms is None:
        first_ms = int((time.monotonic() - started) * 1000)
    top = resumed(serial)
    time.sleep(1)
    log = logcat_since(serial, marker)
    dropped = [line for line in log if "Attach request dropped" in line]
    rejected_codes = sorted({re.search(r"dropped: (\S+)", line).group(1) for line in dropped})
    not_completed = [line for line in log if "attach request not completed" in line]
    requirements = any("were not accepted" in line for line in log)
    start_line = next((line for line in log if "LauncherActivity" in line and ("START u0" in line or "Displayed" in line)), None)
    host_ms = None
    if start_line and dropped:
        t0, t1 = log_time(start_line), log_time(dropped[0])
        if t0 is not None and t1 is not None:
            host_ms = t1 - t0
    plugin_bound, host_bound = bound_between(serial)
    numbers = (f"stage={stage} bannerAfterMs={first_ms} hostAnswerMs={host_ms if host_ms is not None else 'n/a'} polls={polls} "
               f"droppedCodes={','.join(rejected_codes) or 'none'} notCompleted={len(not_completed)} requirementsLogged={str(requirements).lower()} "
               f"hostBoundToPlugin={str(plugin_bound).lower()} pluginBoundToHost={str(host_bound).lower()} top={top}")
    if expect == "rejected":
        ok = (stage == "rejected" and first_ms < DEADLINE_MS - 3000 and rejected_codes == ["ROOTS_REJECTED"] and requirements
              and top.startswith(PLUGIN))
    else:
        ok = (stage == "hidden" and not rejected_codes and not not_completed and (plugin_bound or host_bound) and top.startswith(PLUGIN))
    return ok, numbers


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--min-host-build", type=int, default=6640)
    parser.add_argument("--plugin-build", type=int, default=0, help="expected plugin versionCode (0: any)")
    parser.add_argument("--working-dir", default="", help="host working directory on the device (default: read from the host's preferences)")
    parser.add_argument("--values", default="values", help="plugin string directory matching the device locale")
    options = parser.parse_args()
    serial = options.serial
    host_code, host_updated = package_info(serial, HOST)
    plugin_code, plugin_updated = package_info(serial, PLUGIN)
    locale = shell(serial, "getprop persist.sys.locale")
    working = options.working_dir
    if not working:
        prefs = run_as(serial, HOST, f"cat shared_prefs/{HOST}_preferences.xml")
        match = re.search(r'name="key_\$_working_directory">([^<]*)<', prefs)
        working = "/storage/emulated/0" + (match.group(1) if match else "")
    print(f"DEVICE serial={serial} locale={locale} host={host_code} hostUpdated={host_updated.replace(' ', 'T')} "
          f"plugin={plugin_code} pluginUpdated={plugin_updated.replace(' ', 'T')} workingDir={working}")
    failed = 0
    if host_code < options.min_host_build:
        print(f"FAIL host build {host_code} is older than {options.min_host_build} (needs the issue #589 fix)")
        return 1
    if options.plugin_build and plugin_code != options.plugin_build:
        print(f"FAIL plugin build {plugin_code}, expected {options.plugin_build}")
        return 1
    if shell(serial, f"ls -d {working}") != working:
        print(f"FAIL working directory {working} is not on the device")
        return 1
    if not shell(serial, f"pidof {HOST}"):
        # The host has several launcher aliases, so monkey picks one instead of resolve-activity.
        shell(serial, f"monkey -p {HOST} -c android.intent.category.LAUNCHER 1")
        time.sleep(6)
        shell(serial, "input keyevent KEYCODE_HOME")
        time.sleep(1)
        print(f"INFO host started pid={shell(serial, f'pidof {HOST}') or 'none'}")
    original = run_as(serial, PLUGIN, f"cat {PREFS} 2>/dev/null")
    frags = fragments(options.values)
    parent = working.rsplit("/", 1)[0] or "/"
    cases = [
        ("parent", [parent], "rejected", 10000),
        ("self", [working], "attached", 25000),
    ]
    try:
        for name, roots, expect, wait_ms in cases:
            ok, numbers = run_case(serial, name, roots, frags, wait_ms, expect)
            failed += not ok
            print(f"{'PASS' if ok else 'FAIL'} {name} roots={','.join(roots)} expect={expect}")
            print("  " + numbers)
    finally:
        shell(serial, f"am force-stop {PLUGIN}")
        if original:
            run_as(serial, PLUGIN, f"cat > {PREFS}", data=original.encode("utf-8"))
            restored = "restored"
        else:
            run_as(serial, PLUGIN, f"rm -f {PREFS}")
            restored = "removed (none before)"
        shell(serial, f"rm -f {UI_DUMP}")
        shell(serial, "input keyevent KEYCODE_HOME")
        print(f"AFTER scriptRoots={restored} top={resumed(serial)}")
    print(f"SUMMARY serial={serial} cases={len(cases)} failed={failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
