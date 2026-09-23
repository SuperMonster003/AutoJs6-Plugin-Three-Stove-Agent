"""Run/collect the opt-in host E4 instrumentation without printing private evidence.

Build and install AiAgentRealModelE4Test from the companion host repository first.
Configurations, replies and collected output belong in an ignored local directory.
No credentials are read or moved. Model targets must already exist on each device.
"""
import argparse
import json
import pathlib
import re
import subprocess

TEST = "org.autojs.autojs.core.plugin.agent.AiAgentRealModelE4Test"
RUNNER = "org.autojs.autojs6.test/androidx.test.runner.AndroidJUnitRunner"
CONTROL = "/sdcard/autojs6-agent-e4"
EVIDENCE = ("started.json", "snapshot.json", "pending.json", "final.json",
            "harness.json", "events.jsonl", "model-events.jsonl", "capability-errors.jsonl", "node-queries.jsonl")


def case_id(value):
    if not re.fullmatch(r"[a-z0-9][a-z0-9_-]{0,63}", value):
        raise ValueError("Invalid case ID")
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", required=True, type=pathlib.Path)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("run").add_argument("config", type=pathlib.Path)
    commands.add_parser("collect").add_argument("case", type=case_id)
    reply = commands.add_parser("reply")
    reply.add_argument("case", type=case_id)
    reply.add_argument("response", type=pathlib.Path)
    commands.add_parser("cancel").add_argument("case", type=case_id)
    commands.add_parser("catalog")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    def adb(*parts, check=True):
        return subprocess.run([args.adb, "-s", args.serial, *parts], check=check,
                              capture_output=True, timeout=60)

    def collect(case):
        destination = args.output / case
        destination.mkdir(exist_ok=True)
        for name in EVIDENCE:
            value = adb("exec-out", "run-as", "org.autojs.autojs6", "cat",
                        f"files/agent-e4/{case}/{name}", check=False)
            # Some adb/Android versions report remote cat failures with exit code 0.
            try:
                text = value.stdout.decode("utf-8")
                if name.endswith(".jsonl"):
                    if not text.strip():
                        continue
                    for line in text.splitlines():
                        json.loads(line)
                else:
                    json.loads(text)
            except (UnicodeDecodeError, ValueError):
                continue
            (destination / name).write_bytes(value.stdout)
        snapshot = destination / "snapshot.json"
        if snapshot.exists():
            data = json.loads(snapshot.read_text(encoding="utf-8"))
            run_id = data.get("runId", "")
            if re.fullmatch(r"[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}", run_id):
                private = adb("exec-out", "run-as", "io.github.supermonster003.autojs6.plugin.ai.agent", "cat",
                              f"files/agent-runs/{run_id}.json", check=False)
                try:
                    archive = json.loads(private.stdout)
                    if archive.get("runId") == run_id:
                        (destination / "full-run.json").write_bytes(private.stdout)
                        # A late operator reply can outlive the harness. Prefer its durable terminal record.
                        if archive.get("state") in ("completed", "partial", "failed", "blocked", "cancelled") and archive.get("result"):
                            data = archive
                except (UnicodeDecodeError, ValueError):
                    pass
            result = data.get("result", {})
            pending = data.get("pending", {})
            print(json.dumps({"case": case, "state": data["state"],
                              "step": data.get("step"), "usage": result.get("usage"),
                              "durationMs": result.get("durationMs"),
                              "error": result.get("error", {}).get("code"),
                              "pending": {k: pending[k] for k in ("requestId", "type", "tool", "risk") if k in pending}},
                             ensure_ascii=True))

    if args.command == "collect":
        collect(args.case)
    elif args.command in ("reply", "cancel"):
        if args.command == "reply":
            response = json.loads(args.response.read_text(encoding="utf-8"))
            if not response.get("runId") or not response.get("requestId"):
                raise ValueError("A reply must identify its run and pending request")
            current = json.loads(adb("exec-out", "run-as", "org.autojs.autojs6", "cat",
                                     f"files/agent-e4/{args.case}/snapshot.json").stdout)
            pending = current.get("pending", {})
            if current.get("runId") != response["runId"] or pending.get("requestId") != response["requestId"]:
                raise ValueError("The reply does not match the current pending request; collect again")
            if pending.get("kind") == "choice" and response.get("value") not in pending.get("choices", []):
                raise ValueError("A choice reply must exactly match one offered choice")
            if pending.get("type") == "input":
                value = response.get("value")
                if pending.get("kind") == "confirm" and type(value) is not bool:
                    raise ValueError("A confirm answer must be a JSON boolean")
                if pending.get("kind") in ("text", "choice") and not isinstance(value, str):
                    raise ValueError("A text or choice answer must be a JSON string")
            elif pending.get("type") == "confirmation":
                if type(response.get("allowed")) is not bool or response.get("scope", "once") != "once":
                    raise ValueError("This E4 driver requires a boolean decision with once scope")
            remote = f"{CONTROL}/reply-{args.case}.json"
            # Publish atomically; the device polls while adb is still copying a file.
            adb("push", str(args.response.resolve()), remote + ".tmp")
            adb("shell", "mv", remote + ".tmp", remote)
        else:
            adb("shell", "touch", f"{CONTROL}/cancel-{args.case}")
    else:
        if args.command == "run":
            config = json.loads(args.config.read_text(encoding="utf-8"))
            case = case_id(config["caseId"])
            if (args.output / case).exists() or (args.output / f"{case}-instrumentation.txt").exists():
                raise FileExistsError("This case already has local evidence; choose a new caseId")
            method = "runRealGoal"
            adb("shell", "mkdir", "-p", CONTROL)
            adb("push", str(args.config.resolve()), f"{CONTROL}/config.json")
        else:
            case = "catalog"
            method = "publicCatalog"
        with (args.output / f"{case}-instrumentation.txt").open("wb") as log:
            subprocess.run([args.adb, "-s", args.serial, "shell", "am", "instrument", "-w", "-r",
                            "-e", "autojs.agent.e4", "true", "-e", "class", f"{TEST}#{method}", RUNNER],
                           stdout=log, stderr=subprocess.STDOUT, check=True)
        if args.command == "run":
            collect(case)
        else:
            value = adb("exec-out", "run-as", "org.autojs.autojs6", "cat", "files/agent-e4/catalog/catalog.json")
            data = json.loads(value.stdout)
            (args.output / "catalog.json").write_bytes(value.stdout)
            print(json.dumps({"state": data["type"], "targetCount": len(data.get("targets", []))}))


if __name__ == "__main__":
    main()
