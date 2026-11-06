"""Roadmap P24 on one device: how long an on-device (3-Stone AI) model takes per phase, cold and warm (host issue #589).

Usage: py .python/device_local_model_check.py --serial 63751f63 [--model E2B] [--limit-ms 300000] [--min-plugin-build 449]
       [--goal TEXT] [--keep-logcat DIR]

Runs the same one-step task twice through the host's ai.agent.run (interaction "script", so no workbench is needed):

- cold: 3-Stone AI is force-stopped first, so the provider process starts, loads the model and the XNNPack weight
  cache, then answers.
- warm: run again at once while the provider process and its engine are still alive.

Each case must complete, its model step must finish inside --limit-ms (default: the 300-second model time limit),
and, with plugin build >= --min-plugin-build, the script must receive the two local-model progress lines (waiting,
then answered). The phase times come from logcat (provider start, model loaded, weight cache, prefill done, decode
done); provider process samples (cpuset, oom_score_adj, RSS) are taken every 2 seconds. Prints the device line, one
PASS / FAIL line per case with the key numbers, and a summary; no logcat text, tokens or model output.

Needs the host, 3-Stove Agent and 3-Stone AI installed, 3-Stone AI enabled in Plugin Center with an imported
Gemma model whose display name contains --model, and the host's accessibility / script permissions set up as for
any ai.agent.run script. The device is left as found except that the provider process keeps running.
"""
import argparse
import json
import pathlib
import re
import subprocess
import sys
import tempfile
import threading
import time

HOST = "org.autojs.autojs6"
PLUGIN = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
STONE = "io.github.supermonster003.autojs6.plugin.threestoneai"
PROVIDER = STONE + ":provider"
REMOTE = "/sdcard/Download/autojs6-stove-local-model-check"
GOAL = "Reply with the single word OK and finish. Do not call any tool and do not ask questions."

CATALOG = """
var out = "%(out)s", r = { targets: [], errors: [] };
files.write(out + ".started", String(Date.now()));
ai.catalog().then(function (c) {
    r.targets = (c.targets || []).map(function (t) {
        return { id: t.id, locality: t.locality, available: t.available === true, displayName: String(t.displayName || "").slice(0, 40) };
    });
}).catch(function (e) { r.errors.push(String((e && e.message) || e).slice(0, 240)); }).finally(function () { files.write(out, JSON.stringify(r)); });
"""

SCRIPT = """
var out = "%(out)s", r = { progress: [], steps: [], states: [], errors: [] }, t0 = Date.now();
files.write(out + ".started", String(t0));
function at() { return Date.now() - t0; }
try {
    var run = ai.agent.run(%(goal)s, %(options)s);
    run.on("state", function (e) { r.states.push(at() + ":" + e.to); });
    run.on("progress", function (e) { r.progress.push({ at: at(), message: String(e.message || "").slice(0, 120) }); });
    run.on("step", function (e) { r.steps.push({ at: at(), index: e.index, kind: e.kind, elapsedMs: e.elapsedMs, error: e.error || null }); });
    run.on("input", function (e) { run.respond(e.requestId, "Continue without asking."); });
    run.on("confirmation", function (e) { run.confirm(e.requestId, true); });
    var result = run.join(%(join)d);
    r.result = { status: result.status, steps: result.steps, durationMs: result.durationMs, usage: result.usage || null,
        error: result.error ? result.error.code : null, summary: String(result.summary || "").slice(0, 200) };
} catch (e) { r.errors.push(String((e && e.code ? e.code + " " : "") + ((e && e.message) || e)).slice(0, 300)); }
r.totalMs = at();
files.write(out, JSON.stringify(r));
"""


def adb(serial, *args, timeout=120):
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, timeout=timeout)
    return (result.stdout + result.stderr).decode("utf-8", "replace").replace("\r\n", "\n").strip()


def shell(serial, command, **kwargs):
    return adb(serial, "shell", command, **kwargs)


def package_info(serial, package):
    dump = shell(serial, f"dumpsys package {package}")
    code = re.search(r"versionCode=(\d+)", dump)
    name = re.search(r"versionName=(\S+)", dump)
    return (int(code.group(1)) if code else 0, name.group(1) if name else "?")


def start_script(serial, name, script):
    with tempfile.TemporaryDirectory() as directory:
        local = pathlib.Path(directory) / f"{name}.js"
        local.write_text(script, encoding="utf-8")
        adb(serial, "push", str(local), f"{REMOTE}/{name}.js")
    shell(serial, "input keyevent KEYCODE_WAKEUP")
    # The host has several launcher aliases, so monkey picks one; a frozen or cold host would otherwise stall the run.
    shell(serial, f"monkey -p {HOST} -c android.intent.category.LAUNCHER 1")
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/{name}.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    for _ in range(15):
        time.sleep(1)
        if shell(serial, f"ls {REMOTE}/{name}.json.started").endswith(".started"):
            return True
    return False


def wait_result(serial, name, seconds):
    deadline = time.time() + seconds
    while time.time() < deadline:
        raw = shell(serial, f"cat {REMOTE}/{name}.json")
        if raw.startswith("{"):
            return json.loads(raw)
        time.sleep(2)
    return None


class Sampler(threading.Thread):
    def __init__(self, serial):
        super().__init__(daemon=True)
        self.serial, self.samples, self.stop = serial, [], threading.Event()

    def run(self):
        while not self.stop.is_set():
            pid = shell(self.serial, f"pidof {PROVIDER}")
            if pid:
                raw = shell(self.serial, f"grep -E 'cpuset' /proc/{pid}/cgroup; cat /proc/{pid}/oom_score_adj; grep VmRSS /proc/{pid}/status")
                cpuset = re.search(r"cpuset:(\S+)", raw)
                adj = re.search(r"^(-?\d+)$", raw, re.M)
                rss = re.search(r"VmRSS:\s+(\d+)", raw)
                self.samples.append((cpuset.group(1) if cpuset else "?", int(adj.group(1)) if adj else None, int(rss.group(1)) // 1024 if rss else None))
            self.stop.wait(2)


def stamp(line):
    match = re.match(r"\d\d-\d\d (\d\d):(\d\d):(\d\d)\.(\d\d\d)", line)
    if not match:
        return None
    h, m, s, ms = (int(part) for part in match.groups())
    return ((h * 60 + m) * 60 + s) * 1000 + ms


PHASES = [
    ("providerStart", re.compile(r"ActivityManager: Start proc \d+:" + re.escape(PROVIDER))),
    ("modelLoaded", re.compile(r"magic_number_utils\.cc:\d+\] Loaded: num_subgraphs=\d{3,}")),
    ("weightCache", re.compile(r"XNNPack weight cache")),
    ("prefillDone", re.compile(r"prefill profile summary")),
    ("decodeDone", re.compile(r"decode profile summary")),
]


def phases(log, base_ms):
    found = {}
    for line in log.split("\n"):
        for key, pattern in PHASES:
            if key not in found and pattern.search(line):
                at = stamp(line)
                found[key] = (at - base_ms) if at is not None and base_ms is not None else None
    return found


def run_case(serial, name, target_id, goal, limit_ms, expect_progress, keep):
    if name == "cold":
        shell(serial, f"am force-stop {STONE}")
        time.sleep(2)
    provider_before = shell(serial, f"pidof {PROVIDER}") or "none"
    marker = shell(serial, "date '+%m-%d %H:%M:%S.000'")
    sampler = Sampler(serial)
    sampler.start()
    options = {"locale": "en", "interaction": "script", "target": target_id,
               "budget": {"maxSteps": 2, "maxModelCalls": 2, "maxDurationMs": limit_ms + 40_000}}
    started = time.monotonic()
    # Warm case: no provider start in the log, so the phases count from the moment the script is launched.
    script_started_ms = stamp(shell(serial, "date '+%m-%d %H:%M:%S.000'"))
    ok_start = start_script(serial, name, SCRIPT % {"out": f"{REMOTE}/{name}.json", "goal": json.dumps(goal),
                                                     "options": json.dumps(options), "join": limit_ms + 40_000})
    result = wait_result(serial, name, limit_ms // 1000 + 60) if ok_start else None
    sampler.stop.set()
    sampler.join(10)
    log = shell(serial, f"logcat -d -v threadtime -T '{marker}'", timeout=180)
    if keep:
        (pathlib.Path(keep) / f"{name}-logcat.txt").write_text(log, encoding="utf-8")
    if result is None:
        return False, f"startOk={str(ok_start).lower()} noResult after {int(time.monotonic() - started)}s providerBefore={provider_before}"
    outcome = result.get("result") or {}
    steps = result.get("steps") or []
    model_step_ms = steps[0]["elapsedMs"] if steps else None
    progress = [p["message"] for p in result.get("progress") or []]
    start_line = next((line for line in log.split("\n") if PHASES[0][1].search(line)), None)
    base = stamp(start_line) if start_line else None
    timeline = phases(log, base if base is not None else script_started_ms)
    numbers = (f"status={outcome.get('status')} error={outcome.get('error')} totalMs={result.get('totalMs')} modelStepMs={model_step_ms} "
               f"providerBefore={provider_before} providerStarted={str(start_line is not None).lower()} "
               + " ".join(f"{key}Ms={timeline.get(key)}" for key, _ in PHASES[1:])
               + f" tokens={((outcome.get('usage') or {}).get('inputTokens'))}/{((outcome.get('usage') or {}).get('outputTokens'))}"
               f" progressLines={len(progress)} waitingLine={str(bool(progress) and 'model' in progress[0].lower()).lower()}"
               f" answeredLine={str(len(progress) >= 2 and progress[-1].endswith(' s)')).lower()}"
               f" providerSamples={len(sampler.samples)} cpusets={','.join(sorted({s[0] for s in sampler.samples})) or 'none'}"
               f" oomAdjMax={max((s[1] for s in sampler.samples if s[1] is not None), default=None)}"
               f" rssMaxMb={max((s[2] for s in sampler.samples if s[2] is not None), default=None)}"
               f" errors={len(result.get('errors') or [])}")
    ok = (outcome.get("status") == "completed" and model_step_ms is not None and model_step_ms < limit_ms and not result.get("errors")
          and (not expect_progress or (len(progress) >= 2 and progress[-1].endswith(" s)"))))
    return ok, numbers


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--model", default="E2B", help="substring of the local target's display name (default E2B)")
    parser.add_argument("--limit-ms", type=int, default=300_000, help="model step must finish inside this (default: the 300 s model time limit)")
    parser.add_argument("--min-plugin-build", type=int, default=449, help="plugin build from which the two progress lines are required")
    parser.add_argument("--goal", default=GOAL)
    parser.add_argument("--keep-logcat", default="", help="directory for the two logcat captures (default: not kept)")
    options = parser.parse_args()
    serial = options.serial
    host_code, host_name = package_info(serial, HOST)
    plugin_code, plugin_name = package_info(serial, PLUGIN)
    stone_code, stone_name = package_info(serial, STONE)
    print(f"DEVICE serial={serial} model={shell(serial, 'getprop ro.product.model')} android={shell(serial, 'getprop ro.build.version.release')} "
          f"host={host_name}/{host_code} plugin={plugin_name}/{plugin_code} stone={stone_name}/{stone_code}")
    if not (host_code and plugin_code and stone_code):
        print("FAIL host, 3-Stove Agent and 3-Stone AI must all be installed")
        return 1
    if options.keep_logcat:
        pathlib.Path(options.keep_logcat).mkdir(parents=True, exist_ok=True)
    shell(serial, f"rm -rf {REMOTE}; mkdir -p {REMOTE}")
    failed = 0
    try:
        if not start_script(serial, "catalog", CATALOG % {"out": f"{REMOTE}/catalog.json"}):
            print("FAIL catalog script did not start")
            return 1
        catalog = wait_result(serial, "catalog", 60) or {}
        local = [t for t in catalog.get("targets", []) if t.get("locality") == "local" and t.get("available")]
        target = next((t for t in local if options.model.lower() in t.get("displayName", "").lower()), None)
        if not target:
            print(f"FAIL no available local target containing {options.model!r}: localTargets={len(local)} errors={len(catalog.get('errors') or [])}")
            return 1
        print(f"INFO target={target['id']} displayName={target['displayName']} limitMs={options.limit_ms}")
        expect_progress = plugin_code >= options.min_plugin_build
        for name in ("cold", "warm"):
            ok, numbers = run_case(serial, name, target["id"], options.goal, options.limit_ms, expect_progress, options.keep_logcat)
            failed += not ok
            print(f"{'PASS' if ok else 'FAIL'} {name} expectProgress={str(expect_progress).lower()}")
            print("  " + numbers)
    finally:
        shell(serial, f"rm -rf {REMOTE}")
        print(f"AFTER providerPid={shell(serial, f'pidof {PROVIDER}') or 'none'} remoteDir=removed")
    print(f"SUMMARY serial={serial} cases=2 failed={failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
