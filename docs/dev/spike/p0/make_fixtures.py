# -*- coding: utf-8 -*-
"""Build the P0.2 spike fixture set from the captured screens plus synthesized variants.

Input : fixtures/<serial>-<screen>.txt (captured by fixture-dump.js)
Output: fixtures-out/<device>/<name>.txt
"""

import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
RAW = HERE / "fixtures"
OUT = HERE / "fixtures-out"

DEVICES = {
    "pad": "968e9f18",
    "sony": "BH900ASK9E",
}

NODE_RE = re.compile(r"^(\s*)#n\d+ (.*)$")


def split(text):
    lines = text.split("\n")
    header = []
    body = []
    for line in lines:
        if NODE_RE.match(line):
            body.append(line)
        elif not body:
            header.append(line)
        else:
            body.append(line)
    return header, body


def renumber(header, body):
    out = []
    n = 0
    for line in body:
        m = NODE_RE.match(line)
        if not m:
            out.append(line)
            continue
        out.append("%s#n%d %s" % (m.group(1), n, m.group(2)))
        n += 1
    fixed_header = []
    for line in header:
        if line.startswith("nodes:"):
            fixed_header.append("nodes: %d" % n)
        else:
            fixed_header.append(line)
    return "\n".join(fixed_header + out)


def load(device, screen):
    return (RAW / ("%s-%s.txt" % (DEVICES[device], screen))).read_text(encoding="utf-8")


def write(device, name, text):
    target = OUT / device / (name + ".txt")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text.rstrip("\n") + "\n", encoding="utf-8")
    print("wrote", target.relative_to(HERE), len(text.encode("utf-8")), "bytes")


CALC_EMPTY = """screen: calculator
package: com.google.android.calculator
activity: com.android.calculator2.Calculator
nodes: 0
#n0 FrameLayout id=content (360,640) [0,0,720,1280]
  #n0 LinearLayout id=main_calculator (360,664) [0,48,720,1280]
    #n0 LinearLayout id=display (360,268) [0,48,720,488]
      #n0 EditText text="" desc="No formula" id=formula (360,214) [0,120,720,308] clickable,long-clickable,editable,focused
      #n0 TextView text="" desc="No result" id=result (360,398) [0,308,720,488]
    #n0 LinearLayout id=pad_numeric (240,884) [0,488,480,1280]
      #n0 Button text="7" desc="7" id=digit_7 (80,587) [0,488,160,686] clickable
      #n0 Button text="8" desc="8" id=digit_8 (240,587) [160,488,320,686] clickable
      #n0 Button text="9" desc="9" id=digit_9 (400,587) [320,488,480,686] clickable
      #n0 Button text="4" desc="4" id=digit_4 (80,785) [0,686,160,884] clickable
      #n0 Button text="5" desc="5" id=digit_5 (240,785) [160,686,320,884] clickable
      #n0 Button text="6" desc="6" id=digit_6 (400,785) [320,686,480,884] clickable
      #n0 Button text="1" desc="1" id=digit_1 (80,983) [0,884,160,1082] clickable
      #n0 Button text="2" desc="2" id=digit_2 (240,983) [160,884,320,1082] clickable
      #n0 Button text="3" desc="3" id=digit_3 (400,983) [320,884,480,1082] clickable
      #n0 Button text="0" desc="0" id=digit_0 (80,1181) [0,1082,160,1280] clickable
      #n0 Button text="." desc="point" id=dec_point (240,1181) [160,1082,320,1280] clickable
      #n0 Button text="=" desc="equals" id=eq (400,1181) [320,1082,480,1280] clickable
    #n0 LinearLayout id=pad_operator (600,884) [480,488,720,1280]
      #n0 Button text="DEL" desc="delete" id=del (600,568) [480,488,720,648] clickable,long-clickable
      #n0 Button text="CLR" desc="clear" id=clr (600,568) [480,488,720,648] clickable
      #n0 Button text="/" desc="divide" id=op_div (600,726) [480,648,720,806] clickable
      #n0 Button text="x" desc="multiply" id=op_mul (600,884) [480,806,720,964] clickable
      #n0 Button text="-" desc="minus" id=op_sub (600,1042) [480,964,720,1122] clickable
      #n0 Button text="+" desc="plus" id=op_add (600,1200) [480,1122,720,1280] clickable
"""


def calc(formula, formula_desc, result, result_desc):
    text = CALC_EMPTY.replace(
        'EditText text="" desc="No formula"', 'EditText text="%s" desc="%s"' % (formula, formula_desc)
    ).replace('TextView text="" desc="No result"', 'TextView text="%s" desc="%s"' % (result, result_desc))
    header, body = split(text)
    return renumber(header, body)


def main():
    # --- pad -------------------------------------------------------------
    pad_launcher = load("pad", "launcher")
    header, body = split(pad_launcher)
    write("pad", "launcher", renumber(header, body))
    body_calc = list(body)
    for i, line in enumerate(body_calc):
        if 'text="Adobe Acrobat"' in line:
            body_calc.insert(i + 1, '              #n0 TextView text="计算器" desc="计算器" (707,1765) [563,1659,851,1872] clickable,long-clickable')
            break
    else:
        raise SystemExit("pad launcher anchor missing")
    write("pad", "launcher-calc", renumber(header, body_calc))

    header, body = split(load("pad", "settings-home"))
    write("pad", "settings-home", renumber(header, body))

    pad_wifi = load("pad", "wifi")
    header, body = split(pad_wifi)
    write("pad", "wifi", renumber(header, body))
    toggled = []
    for line in body:
        if "CheckBox id=checkbox" in line:
            line = line.replace("unchecked", "checked")
        toggled.append(line)
    for i, line in enumerate(toggled):
        if 'text="网络加速"' in line:
            # HyperOS shows the network list under the switch once WLAN is on.
            insert_at = i - 1
            toggled[insert_at:insert_at] = [
                '                #n0 TextView text="可用 WLAN" id=title (1305,340) [894,320,1716,360]',
                '                #n0 ViewGroup (1305,420) [857,370,1753,470] clickable',
                '                  #n0 TextView text="Xiaomi_5G" id=title (960,405) [894,380,1026,430]',
                '                  #n0 TextView text="已连接" id=summary (940,450) [894,432,986,468]',
                '                #n0 ViewGroup (1305,530) [857,480,1753,580] clickable',
                '                  #n0 TextView text="Neighbor-B" id=title (1010,530) [894,505,1126,555]',
            ]
            break
    else:
        raise SystemExit("pad wifi anchor missing")
    write("pad", "wifi-toggled", renumber(header, toggled))

    # --- sony ------------------------------------------------------------
    sony_launcher = load("sony", "launcher")
    header, body = split(sony_launcher)
    write("sony", "launcher", renumber(header, body))
    body_calc = list(body)
    body_calc.append('    #n0 TextView text="Calculator" desc="Calculator" (200,900) [120,820,280,980] clickable,long-clickable')
    body_calc.append('    #n0 TextView text="Chrome" desc="Chrome" (520,900) [440,820,600,980] clickable,long-clickable')
    write("sony", "launcher-calc", renumber(header, body_calc))

    header, body = split(load("sony", "settings-home"))
    write("sony", "settings-home", renumber(header, body))

    sony_wifi = load("sony", "wifi")
    header, body = split(sony_wifi)
    write("sony", "wifi", renumber(header, body))
    # Shared "networks" fixture for the ambiguous connect round on both devices.
    write("pad", "wifi-networks", renumber(header, body))
    write("sony", "wifi-networks", renumber(header, body))
    toggled = []
    skipping = False
    for line in body:
        if 'Switch text="ON"' in line:
            line = line.replace('text="ON"', 'text="OFF"').replace("clickable,checked", "clickable,unchecked")
        if "RecyclerView id=list" in line:
            toggled.append(line.replace("long-clickable,scrollable,focused", "focused"))
            toggled.append('              #n0 TextView text="To see available networks, turn Wi-Fi on." id=empty (360,500) [64,470,656,530]')
            skipping = True
            continue
        if skipping:
            continue
        toggled.append(line)
    write("sony", "wifi-toggled", renumber(header, toggled))

    # --- calculator (synthetic, both devices) ----------------------------
    header, body = split(CALC_EMPTY)
    for device in DEVICES:
        write(device, "calc-empty", renumber(header, body))
        write(device, "calc-result", calc("12x34", "12 times 34", "408", "408"))


if __name__ == "__main__":
    main()
