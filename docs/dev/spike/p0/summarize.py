# -*- coding: utf-8 -*-
"""Summarize a spike results JSONL: metrics table + per-round listing (markdown)."""
import json
import statistics
import sys
from pathlib import Path

# Manual overrides after human review: (round id) -> (reasonable, note)
HUMAN = {
    "W6": (True, "fixture already shows a connected network (HomeRouter_5G / Connected); done:completed with that evidence is defensible, the automatic expectation (ask) was too strict"),
}


def pct(n, d):
    return "%d/%d (%.0f%%)" % (n, d, 100.0 * n / d) if d else "n/a"


def ms(values):
    if not values:
        return "n/a"
    return "%d / %d / %d" % (statistics.mean(values), statistics.median(values), max(values))


def summarize(path):
    rows = [json.loads(l) for l in Path(path).read_text(encoding="utf-8").splitlines() if l.strip()]
    main = [r for r in rows if r["phase"] == "main"]
    pre = [r for r in rows if r["phase"] == "preflight"]
    label = rows[0]["target"]
    n = len(main)
    errors = [r for r in main if r.get("error")]
    json_ok = [r for r in main if r.get("jsonValid")]
    schema_ok = [r for r in main if r.get("schemaValid")]
    auto_ok = [r for r in main if r.get("reasonable")]
    human_ok = 0
    for r in main:
        ok = r.get("reasonable")
        if not ok and r.get("schemaValid") and r["round"] in HUMAN:
            ok = HUMAN[r["round"]][0]
        if ok:
            human_ok += 1
    wall = [r["wallMillis"] for r in main if not r.get("error")]
    dur = [r["usage"]["durationMillis"] for r in main if r.get("usage") and r["usage"].get("durationMillis") is not None]
    tin = [r["usage"]["inputTokens"] for r in main if r.get("usage") and r["usage"].get("inputTokens") is not None]
    tout = [r["usage"]["outputTokens"] for r in main if r.get("usage") and r["usage"].get("outputTokens") is not None]
    strict = [r for r in main if r.get("parseMode") == "strict"]
    variant = main[0]["variant"] if main else "?"
    print("### %s" % label)
    print()
    print("| Metric | Value |")
    print("|---|---|")
    print("| Rounds (main) | %d (preflight %d: A=%s, B=%s) |" % (n, len(pre),
          "ok" if pre and pre[0].get("jsonValid") else "fail", "ok" if len(pre) > 1 and pre[1].get("jsonValid") else "fail"))
    print("| Schema variant used | %s |" % variant)
    print("| Transport errors | %s |" % pct(len(errors), n))
    print("| JSON valid | %s (strict parse %d, lenient %d) |" % (pct(len(json_ok), n), len(strict), len(json_ok) - len(strict)))
    print("| Schema compliant | %s |" % pct(len(schema_ok), n))
    print("| Reasonable (automatic expectation) | %s |" % pct(len(auto_ok), n))
    print("| Reasonable (after human review) | %s |" % pct(human_ok, n))
    print("| Wall latency ms (mean / median / max) | %s |" % ms(wall))
    print("| Provider duration ms (mean / median / max) | %s |" % ms(dur))
    print("| Input tokens (mean / median / max) | %s |" % ms(tin))
    print("| Output tokens (mean / median / max) | %s |" % ms(tout))
    print()
    print("| # | Round | JSON | Schema | Auto | Latency ms | In / out tokens | Decision |")
    print("|---|---|---|---|---|---|---|---|")
    for r in main:
        u = r.get("usage") or {}
        d = r.get("decision")
        if r.get("error"):
            dtxt = "ERROR " + r["error"].split("|")[0].strip()[:80]
        elif d is None:
            dtxt = "unparsed: " + (r.get("text") or "")[:80].replace("\n", " ").replace("|", "\\|")
        else:
            dtxt = json.dumps(d, ensure_ascii=False)[:150].replace("|", "\\|")
        print("| %d | %s | %s | %s | %s | %d | %s / %s | %s |" % (
            r["index"], r["round"], "y" if r.get("jsonValid") else "n", "y" if r.get("schemaValid") else "n",
            "y" if r.get("reasonable") else "n", r["wallMillis"], u.get("inputTokens", "-"), u.get("outputTokens", "-"), dtxt))
    print()
    print("Schema problems:", [(r["index"], r["round"], r.get("schemaProblems")) for r in main if r.get("jsonValid") and not r.get("schemaValid")])
    print("Grade notes for non-matching rounds:", [(r["index"], r["round"], r.get("gradeNote")) for r in main if r.get("schemaValid") and not r.get("reasonable")])
    print()


if __name__ == "__main__":
    for p in sys.argv[1:]:
        summarize(p)
