"""Roadmap P18.5 (D90, E3): an Agent task on one phone that hears a sentence through 3-Macaw Speech's plugin tools and reads
it out again.

Usage: py .python/device_plugin_tools_check.py --serial QV770340J7 [--install] [--min-volume 70] [--audio <en.wav>]
       [--target remote|local|auto|<id, model or display name>]

Needs an AutoJs6 debug build with host plugin tools (21117c789 or later), 3-Macaw Speech 1.2.0 or later with its English
recognition and synthesis models, a model target for 3-Stove Agent in 3-Stone AI and an unlocked screen. The run uses a
remote target by default (the host's default target when it is remote): on-device targets get no plugin tools (D91),
because their 4096-token window cannot hold the definitions. With an on-device target (`--target local`, or an id that
names one) the script checks D91 instead: the task asks for one `device_info` call, and it must reach the model (no
`LIMIT_EXCEEDED`) without calling a plugin tool; on the phones tested so far the default groups alone already exceed the
on-device budget (roadmap Q15), so this check waits for Q15. `auto` leaves the choice to 3-Stove Agent, which prefers an
on-device target. `--install` installs this repository's debug APK first (`gradlew :app:assembleDebug`). The script
pushes the test recording of 3-Macaw Speech (`--audio`, by default the sibling checkout's `en.wav`) and a host script,
which starts `ai.agent.run` with the goal below and leaves the tools to 3-Stove Agent's settings (the read-only plugin
group is on by default, D89).
When the plugin's recording starts (`AudioManager` lists one more active recording), the script plays the recording
through the phone's speaker; the media volume is raised to `--min-volume` percent for the run when lower and restored
afterwards. Confirmations of `speech_*` tools are allowed by the script, every other one is refused; questions are
answered with "continue". Nothing here answers a system dialog.
Prints one PASS / FAIL line per item with its key numbers (character counts and error rates, never the recognized text)
and removes the pushed files afterwards.
"""
import argparse
import json
import pathlib
import re
import subprocess
import sys
import tempfile
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
HOST = "org.autojs.autojs6"
PACKAGE = "io.github.supermonster003.autojs6.plugin.three.stove.agent"
MACAW = "io.github.supermonster003.autojs6.plugin.three.macaw.speech"
AUDIO = ROOT.parent / "AutoJs6-Plugin-Three-Macaw-Speech/app/src/androidTest/assets/test-audio/en.wav"
REFERENCE = "AFTER EARLY NIGHTFALL THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE THE SQUALID QUARTER OF THE BROTHELS"
REMOTE = "/sdcard/Download/autojs6-stove-plugin-tools"
GOAL = ("Call the speech_listen tool once with language \"en\" to hear one sentence from the microphone. Then call the "
        "speech_speak tool once with exactly the recognized sentence as text and language \"en\". Then finish. Use no other "
        "tool and ask no questions.")
LOCAL_GOAL = "Call the device_info tool once. Then finish with a one-line summary. Use no other tool and ask no questions."
JOIN_MS = 300_000
# The model's first steps come before the recording; the player gives up after this long without one.
WAIT_MS = 180_000

SCRIPT = """
var out = "%(out)s", dir = "%(dir)s";
var r = { steps: [], answers: [], errors: [], states: [] };
files.write(out + ".started", String(Date.now()));
function message(e) { return String((e && e.code ? e.code + " " : "") + ((e && e.message) || e)).slice(0, 240); }
try { var s = ai.agent.status(); r.status = { state: s.state, errorCode: s.errorCode || null }; } catch (e) { r.errors.push("status " + message(e)); }
try {
    r.listed = plugins.tools.list().map(function (t) { return t.name; })
        .filter(function (n) { return n === "speech_listen" || n === "speech_speak"; }).sort().join(",");
} catch (e) { r.errors.push("list " + message(e)); }
var audio = context.getSystemService(android.content.Context.AUDIO_SERVICE);
function recordings() { try { return audio.getActiveRecordingConfigurations().size(); } catch (e) { return -1; } }
r.player = { before: recordings() };
var started = Date.now(), finished = false;
var player = threads.start(function () {
    while (!finished && recordings() <= r.player.before && Date.now() - started < %(wait)d) sleep(100);
    r.player.seen = recordings() > r.player.before;
    r.player.atMs = Date.now() - started;
    if (finished || !r.player.seen) return;
    sleep(300);
    media.playMusic(dir + "/en.wav");
    sleep(12000);
    try { media.stopMusic(); } catch (e) {}
});
var run = null;
try {
    run = ai.agent.run(%(goal)s, %(options)s);
    run.on("state", function (e) { r.states.push(e.to); });
    run.on("step", function (e) {
        var step = { index: e.index, kind: e.kind, tool: e.tool || null, elapsedMs: e.elapsedMs, error: e.error || null };
        if (e.tool === "speech_listen" || e.tool === "speech_speak") {
            try {
                step.observationBytes = String(e.observation || "").length;
                // The step observation wraps the plugin observation: { ok, result: { untrusted, source, tool, result, ... } }.
                var o = JSON.parse(e.observation || "{}"), p = o.result || {}, res = p.result || {};
                step.source = p.source || null;
                step.keys = Object.keys(o).join(",") + " / " + Object.keys(p).join(",") + " / " + Object.keys(res).join(",");
                if (e.tool === "speech_listen") {
                    step.text = res.text || o.text || ""; step.endReason = res.endReason || null; step.durationMillis = res.durationMillis || 0;
                } else {
                    step.text = (e.arguments && e.arguments.text) || ""; step.engine = res.engine || null;
                    step.completed = res.completed === true; step.durationMillis = res.durationMillis || 0;
                }
            } catch (x) { step.unparsed = String(e.observation || "").length; }
        }
        r.steps.push(step);
    });
    run.on("confirmation", function (e) {
        var allowed = String(e.tool || "").indexOf("speech_") === 0;
        r.answers.push("confirmation " + e.tool + (allowed ? " allowed" : " refused"));
        run.confirm(e.requestId, allowed);
    });
    run.on("input", function (e) {
        r.answers.push("input " + e.kind);
        run.respond(e.requestId, e.kind === "plan" ? ["Call speech_listen", "Call speech_speak with the heard sentence", "Finish"] : "Continue without asking.");
    });
    var result = run.join(%(join)d);
    r.result = { status: result.status, steps: result.steps, toolCalls: result.toolCalls, durationMs: result.durationMs,
        modelCalls: (result.usage || {}).modelCalls || 0,
        error: result.error ? result.error.code + (result.error.message ? " (" + String(result.error.message).slice(-80) + ")" : "") : null };
    // P19.1: an over-budget run ends its message with the sizes of the request, ASCII only and without task text.
    var summary = String((result.error || {}).message || ""), sizes = summary.indexOf("(input ");
    if (sizes >= 0) { r.result.context = summary.slice(sizes + 1, summary.lastIndexOf(")")); r.result.error = result.error.code; }
} catch (e) {
    r.errors.push("run " + message(e));
    try { if (run) { r.state = run.state; r.runError = run.error ? run.error.code : null; run.cancel("device-check"); } } catch (x) {}
}
finished = true;
try { player.join(15000); } catch (e) {}
try { media.stopMusic(); } catch (e) {}
files.write(out, JSON.stringify(r));
"""

# Lists the host's model targets without credentials or endpoints.
CATALOG = """
var out = "%(out)s", r = { targets: [], errors: [] };
files.write(out + ".started", String(Date.now()));
ai.catalog().then(function (c) {
    r.defaultTarget = c.defaultTarget || null;
    r.targets = (c.targets || []).map(function (t) {
        return { id: t.id, locality: t.locality, available: t.available === true, displayName: String(t.displayName || "").slice(0, 40),
            model: String(t.model || "").slice(0, 40) };
    });
}).catch(function (e) { r.errors.push(String((e && e.message) || e).slice(0, 240)); }).finally(function () {
    files.write(out, JSON.stringify(r));
});
"""


def adb(serial: str, *args: str) -> str:
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace",
                            stdin=subprocess.DEVNULL)
    return (result.stdout + result.stderr).strip()


def version(serial: str, package: str) -> str:
    match = re.search(r"versionCode=(\d+)", adb(serial, "shell", "dumpsys", "package", package))
    return match.group(1) if match else "missing"


def install(serial: str) -> bool:
    files = sorted((ROOT / "app/build/outputs/apk/debug").glob("*.apk"))
    if len(files) != 1:
        print(f"FAIL install: expected one debug APK, found {len(files)}")
        return False
    output = adb(serial, "install", "-r", "-t", str(files[0]))
    print(("PASS" if "Success" in output else "FAIL") + f" install {files[0].name}")
    return "Success" in output


class Volume:
    """Raises the music volume to `percent` for the run when it is lower, then restores it (both shell commands, since
    HyperOS ignores the first)."""

    def __init__(self, serial: str, percent: int):
        self.serial, self.percent = serial, percent
        self.command, self.current, self.maximum = None, None, None
        for command in (["cmd", "media_session", "volume"], ["media", "volume"]):
            match = re.search(r"volume is (\d+) in range \[(\d+)\.\.(\d+)\]", adb(serial, "shell", *command, "--stream", "3", "--get"))
            if match:
                self.command, self.current, self.maximum = command, int(match.group(1)), int(match.group(3))
                break

    def set(self, index: int):
        adb(self.serial, "shell", *self.command, "--stream", "3", "--set", str(index))
        adb(self.serial, "shell", "cmd", "audio", "set-volume", "3", str(index))

    def __enter__(self):
        if self.command and self.current < self.target():
            self.set(self.target())
        return self

    def __exit__(self, *exc):
        if self.command and self.current < self.target():
            self.set(self.current)

    def target(self) -> int:
        return max(1, self.maximum * self.percent // 100)

    def describe(self) -> str:
        if not self.command:
            return "media volume unknown"
        return f"media volume {self.current}/{self.maximum} (raised to {max(self.current, self.target())} for the run)"


def launcher(serial: str) -> str:
    """AutoJs6's own launcher activity (a debug host with LeakCanary lists a second launcher entry)."""
    listed = adb(serial, "shell", "cmd", "package", "query-activities", "--brief", "-a", "android.intent.action.MAIN",
                 "-c", "android.intent.category.LAUNCHER", HOST)
    own = [line.strip() for line in listed.splitlines() if line.strip().startswith(HOST + "/") and "leakcanary" not in line]
    return own[0] if own else adb(serial, "shell", "cmd", "package", "resolve-activity", "--brief", "-c",
                                  "android.intent.category.LAUNCHER", HOST).splitlines()[-1].strip()


def start(serial: str, name: str, script: str) -> bool:
    """Runs `script` as `<name>.js` through AutoJs6; the script writes `<name>.json.started` first."""
    with tempfile.TemporaryDirectory() as directory:
        local = pathlib.Path(directory) / f"{name}.js"
        local.write_text(script, encoding="utf-8")
        adb(serial, "push", str(local), f"{REMOTE}/{name}.js")
    adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(serial, "shell", "am", "start", "-n", launcher(serial))
    time.sleep(2)
    adb(serial, "shell", "am", "start", "-a", "android.intent.action.VIEW", "-t", "application/x-javascript",
        "-d", f"file://{REMOTE}/{name}.js", "-n", f"{HOST}/org.autojs.autojs.external.open.RunIntentActivity")
    for _ in range(15):
        time.sleep(1)
        if adb(serial, "shell", "ls", f"{REMOTE}/{name}.json.started").endswith(".started"):
            return True
    return False


def result(serial: str, name: str, seconds: int):
    deadline = time.time() + seconds
    while time.time() < deadline:
        raw = adb(serial, "shell", "cat", f"{REMOTE}/{name}.json")
        if raw.startswith("{"):
            return json.loads(raw)
        time.sleep(3)
    return None


def choose(serial: str, wanted: str):
    """Returns (target id or None for 3-Stove Agent's automatic choice, description, locality or None). `remote` takes
    the host's default target when it is remote, otherwise the first available remote one; Automatic prefers an
    on-device target."""
    if wanted == "auto":
        return None, "automatic (on-device first)", None
    if not start(serial, "catalog", CATALOG % {"out": f"{REMOTE}/catalog.json"}):
        return None, None, None
    catalog = result(serial, "catalog", 30) or {}
    targets = [target for target in catalog.get("targets", []) if target.get("available")]
    if wanted in ("remote", "local"):
        matching = [target for target in targets if target.get("locality") == wanted]
        matching.sort(key=lambda target: target.get("id") != catalog.get("defaultTarget"))
    else:
        matching = [target for target in targets if wanted in (target.get("id"), target.get("model"), target.get("displayName"))]
    if not matching:
        return None, None, None
    target = matching[0]
    return target["id"], f"{target['locality']} {target['model']} ({target['displayName']})", target["locality"]


def cer(reference: str, hypothesis: str) -> float:
    """Character error rate over upper-case letters and digits."""
    r, h = ("".join(ch for ch in text.upper() if ch.isalnum()) for text in (reference, hypothesis or ""))
    previous = list(range(len(h) + 1))
    for i, rc in enumerate(r, 1):
        current = [i] + [0] * len(h)
        for j, hc in enumerate(h, 1):
            current[j] = min(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + (rc != hc))
        previous = current
    return previous[-1] / max(1, len(r))


def report(r: dict, local: bool) -> bool:
    verdicts = []

    def item(name: str, ok: bool, detail: str):
        verdicts.append(ok)
        print(f"    {name}: {'PASS' if ok else 'FAIL'} {detail}")

    status = r.get("status") or {}
    item("tools listed", r.get("listed") == "speech_listen,speech_speak",
         f"{r.get('listed') or '-'}; ai.agent.status {status.get('state')} {status.get('errorCode') or ''}".rstrip())
    outcome = r.get("result") or {}
    errors = [error.split(" ")[0] + " " + error.split(" ")[1] if " " in error else error for error in r.get("errors", [])]
    error = outcome.get("error") or r.get("runError")
    agent = (f"{outcome.get('status') or r.get('state') or '-'}, {outcome.get('steps', '-')} steps, {outcome.get('toolCalls', '-')} tool calls, "
             f"{outcome.get('modelCalls', '-')} model calls, {outcome.get('durationMs', '-')} ms"
             + (f", error {error}" if error else "") + (f", script errors {errors}" if errors else ""))
    if outcome.get("context"):
        print(f"    context: {outcome['context']}")
    tools = [step for step in r.get("steps", []) if step.get("tool")]
    names = [step["tool"] for step in tools]
    if local:
        # D91: the prompt fits once the plugin tools stay out; a small model may still decide poorly, so the status is
        # reported but not required.
        plugin = [name for name in names if name.startswith("speech_")]
        item("agent reaches the model", (outcome.get("modelCalls") or 0) >= 1 and "LIMIT_EXCEEDED" not in (error or ""), agent)
        item("no plugin tools", not plugin, f"{' -> '.join(names) or 'no tool steps'}; plugin tool calls {len(plugin)}")
        print(f"    answers: {', '.join(r.get('answers') or []) or 'none'}; states {' -> '.join(r.get('states') or []) or '-'}")
        return all(verdicts)
    item("agent", outcome.get("status") == "completed", agent)
    listen = next((step for step in tools if step["tool"] == "speech_listen"), None)
    speak = next((step for step in tools if step["tool"] == "speech_speak"), None)
    others = [name for name in names if name not in ("speech_listen", "speech_speak")]
    item("order", listen is not None and speak is not None and listen["index"] < speak["index"],
         f"{' -> '.join(names) or 'no tool steps'}; other tools {len(others)}")
    player = r.get("player") or {}
    capture = f"recording {'seen' if player.get('seen') else 'not seen'} after {player.get('atMs')} ms (active before {player.get('before')})"
    if listen is None:
        item("listen", False, f"no speech_listen step; {capture}")
    else:
        heard = listen.get("text") or ""
        item("listen", bool(heard.strip()) and not listen.get("error"),
             f"{len(heard)} chars, cer {cer(REFERENCE, heard):.3f} against en.wav, end {listen.get('endReason')}, "
             f"{listen.get('durationMillis')} ms heard, step {listen.get('elapsedMs')} ms, source {listen.get('source')}, {capture}"
             + (f", error {listen['error']}" if listen.get("error") else "")
             + ("" if heard.strip() else f", observation {listen.get('observationBytes')} bytes, keys [{listen.get('keys')}]"
                + (f", unparsed {listen['unparsed']}" if listen.get("unparsed") is not None else "")))
    if speak is None:
        item("speak", False, "no speech_speak step")
    else:
        spoken = speak.get("text") or ""
        match = f", cer {cer(listen.get('text') or '', spoken):.3f} against the heard text" if listen and (listen.get("text") or "").strip() else ""
        item("speak", speak.get("completed") is True and (speak.get("durationMillis") or 0) > 0 and bool(spoken.strip()) and not speak.get("error"),
             f"engine {speak.get('engine')}, completed {speak.get('completed')}, {speak.get('durationMillis')} ms spoken, "
             f"text {len(spoken)} chars{match}, step {speak.get('elapsedMs')} ms" + (f", error {speak['error']}" if speak.get("error") else ""))
    print(f"    answers: {', '.join(r.get('answers') or []) or 'none'}; states {' -> '.join(r.get('states') or []) or '-'}")
    return all(verdicts)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--install", action="store_true", help="install this repository's debug APK first")
    parser.add_argument("--min-volume", type=int, default=70, help="lowest media volume in percent for the run")
    parser.add_argument("--audio", type=pathlib.Path, default=AUDIO, help="the English test recording of 3-Macaw Speech")
    parser.add_argument("--target", default="remote",
                        help="remote (default), local, auto (3-Stove Agent's Automatic), or a target id, model or display name")
    options = parser.parse_args()
    serial = options.serial
    if not options.audio.is_file():
        print(f"FAIL audio: {options.audio.name} not found (pass --audio)")
        return 1
    if options.install and not install(serial):
        return 1
    model = adb(serial, "shell", "getprop", "ro.product.model")
    api = adb(serial, "shell", "getprop", "ro.build.version.sdk")
    with Volume(serial, options.min_volume) as volume:
        print(f"{model} ({serial}, API {api}): host {version(serial, HOST)}, 3-Stove Agent {version(serial, PACKAGE)}, "
              f"3-Macaw Speech {version(serial, MACAW)}; {volume.describe()}")
        try:
            adb(serial, "shell", "rm", "-rf", REMOTE)
            adb(serial, "shell", "mkdir", "-p", REMOTE)
            target, described, locality = choose(serial, options.target)
            if described is None:
                print(f"    model target: FAIL no available target for {options.target}")
                return 1
            local = locality == "local"
            print(f"    model target: {described}" + ("; checks D91 (no plugin tools on-device)" if local else ""))
            run = {"locale": "en", "interaction": "script", "budget": {"maxSteps": 8, "maxModelCalls": 12, "maxDurationMs": JOIN_MS}}
            if target:
                run["target"] = target
            adb(serial, "push", str(options.audio), f"{REMOTE}/en.wav")
            script = SCRIPT % {"out": f"{REMOTE}/agent.json", "dir": REMOTE, "goal": json.dumps(LOCAL_GOAL if local else GOAL),
                               "options": json.dumps(run), "join": JOIN_MS, "wait": WAIT_MS}
            if not start(serial, "agent", script):
                print("    script: FAIL did not start (unlock the phone; AutoJs6 needs all-files access)")
                return 1
            r = result(serial, "agent", JOIN_MS // 1000 + 60)
        finally:
            adb(serial, "shell", "rm", "-rf", REMOTE)
    if r is None:
        print("    script: FAIL no result")
        return 1
    ok = report(r, local)
    print(f"summary: {'PASS' if ok else 'FAIL'}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
