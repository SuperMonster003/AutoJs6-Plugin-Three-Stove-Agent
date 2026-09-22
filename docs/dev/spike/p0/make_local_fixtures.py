# -*- coding: utf-8 -*-
"""Compaction level 2 for local (4096-token) targets: drop bounds, drop container-only lines, cap lines.
Node refs are preserved (lines are removed, never renumbered)."""
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
OUT = HERE / "fixtures-out"
NODE_RE = re.compile(r"^(\s*)#n(\d+) (\S+)(.*)$")
BOUNDS_RE = re.compile(r" \[-?\d+,-?\d+,-?\d+,-?\d+\]")
MAX_LINES = 70

for device_dir in sorted(OUT.iterdir()):
    for f in sorted(device_dir.glob("*.txt")):
        if f.name.endswith(".local.txt"):
            continue
        header, body, dropped = [], [], 0
        for line in f.read_text(encoding="utf-8").split("\n"):
            m = NODE_RE.match(line)
            if not m:
                if line.strip():
                    header.append(line)
                continue
            rest = BOUNDS_RE.sub("", m.group(4))
            has_text = 'text="' in rest or 'desc="' in rest
            has_flags = re.search(r"\) (clickable|long-clickable|scrollable|checked|unchecked|editable|disabled|selected|focused)", rest) is not None
            if not has_text and not has_flags:
                dropped += 1
                continue
            body.append("%s#n%s %s%s" % (m.group(1), m.group(2), m.group(3), rest))
        omitted = 0
        if len(body) > MAX_LINES:
            omitted = len(body) - MAX_LINES
            body = body[:MAX_LINES]
            body.append("... (%d more nodes omitted)" % omitted)
        text = "\n".join(header + body) + "\n"
        target = f.with_name(f.stem + ".local.txt")
        target.write_text(text, encoding="utf-8")
        print("%-28s %6d -> %5d bytes, kept %d, dropped %d container lines, omitted %d" % (
            str(target.relative_to(OUT)), f.stat().st_size, len(text.encode("utf-8")), len(body), dropped, omitted))
