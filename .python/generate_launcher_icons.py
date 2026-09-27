# -*- coding: utf-8 -*-
"""Render the Three Stove Agent launcher icons.

Outputs (all RGBA PNG, regenerated deterministically from this script):
  app/src/main/res/mipmap/ic_launcher.png              432 x 432 legacy icon (rounded square)
  app/src/main/res/mipmap/ic_launcher_round.png        192 x 192 legacy round icon
  app/src/main/res/mipmap/ic_launcher_foreground.png   432 x 432 adaptive foreground (white glyph)
  app/src/main/res/mipmap/ic_launcher_monochrome.png   432 x 432 adaptive monochrome (black glyph)
  app/src/main/res/mipmap-night/*.png                  same set on the night background

The glyph is a speech bubble (the natural-language goal) holding the letters AI, with a task
check mark in its lower right corner (the completed task), so the icon reads as "an agent that
takes instructions and gets tasks done" at launcher and plugin-center sizes. It deliberately
shares neither the node frame of the MCP Server plugin nor the colors of the AI provider
plugins. Background colors match values/ic_launcher_background.xml (indigo) and
values-night/ic_launcher_background.xml.

Usage: py .python/generate_launcher_icons.py
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
SIZE = 432
ROUND_SIZE = 192
SCALE = 4

DAY_BACKGROUND = (0x4F, 0x46, 0xE5, 255)
NIGHT_BACKGROUND = (0x37, 0x30, 0xA3, 255)
GLYPH_WHITE = (255, 255, 255, 255)
GLYPH_BLACK = (0, 0, 0, 255)
TRANSPARENT = (0, 0, 0, 0)

FONT_CANDIDATES = (
    "C:/Windows/Fonts/segoeuib.ttf",
    "C:/Windows/Fonts/arialbd.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
)


def load_font(size: int) -> ImageFont.FreeTypeFont:
    for candidate in FONT_CANDIDATES:
        if Path(candidate).is_file():
            return ImageFont.truetype(candidate, size)
    raise SystemExit("No bold TrueType font found; add a candidate path to FONT_CANDIDATES")


def inset_triangle(points: list[tuple[float, float]], inset: float) -> list[tuple[float, float]]:
    """Move every edge of a triangle inwards by `inset` (scaling about the incenter)."""
    (ax, ay), (bx, by), (cx, cy) = points
    a = math.dist((bx, by), (cx, cy))
    b = math.dist((ax, ay), (cx, cy))
    c = math.dist((ax, ay), (bx, by))
    perimeter = a + b + c
    ix = (a * ax + b * bx + c * cx) / perimeter
    iy = (a * ay + b * by + c * cy) / perimeter
    s = perimeter / 2
    area = math.sqrt(max(s * (s - a) * (s - b) * (s - c), 0.0))
    inradius = area / s
    factor = max(inradius - inset, 0.0) / inradius
    return [(ix + (x - ix) * factor, iy + (y - iy) * factor) for x, y in points]


def draw_glyph(draw: ImageDraw.ImageDraw, center: tuple[float, float], box: float, color: tuple[int, int, int, int]) -> None:
    cx, cy = center
    half = box / 2
    stroke = box * 0.075
    radius = box * 0.22

    # Speech bubble = rounded body + tail, drawn as one filled silhouette whose interior is
    # cleared afterwards, so the outline is a single continuous stroke of uniform width.
    body_top = cy - half
    body_bottom = cy + half * 0.62
    body_left = cx - half
    body_right = cx + half
    tail = [
        (body_left + box * 0.18, body_bottom - stroke),
        (body_left + box * 0.40, body_bottom - stroke),
        (body_left + box * 0.14, cy + half),
    ]
    draw.rounded_rectangle((body_left, body_top, body_right, body_bottom), radius=radius, fill=color)
    draw.polygon(tail, fill=color)
    draw.rounded_rectangle(
        (body_left + stroke, body_top + stroke, body_right - stroke, body_bottom - stroke),
        radius=max(radius - stroke, 1.0),
        fill=TRANSPARENT,
    )
    draw.polygon(inset_triangle(tail, stroke), fill=TRANSPARENT)

    font = load_font(int(box * 0.36))
    text = "AI"
    left, top, right, bottom = draw.textbbox((0, 0), text, font=font)
    text_width = right - left
    text_height = bottom - top
    text_x = cx - box * 0.08 - text_width / 2 - left
    text_y = (body_top + body_bottom) / 2 - text_height / 2 - top
    draw.text((text_x, text_y), text, font=font, fill=color)

    # Task check mark in the lower right corner of the bubble.
    check_stroke = int(round(box * 0.07))
    x0 = body_right - box * 0.34
    y0 = body_bottom - box * 0.30
    p1 = (x0, y0 + box * 0.06)
    p2 = (x0 + box * 0.09, y0 + box * 0.15)
    p3 = (x0 + box * 0.24, y0 - box * 0.04)
    draw.line((p1, p2), fill=color, width=check_stroke)
    draw.line((p2, p3), fill=color, width=check_stroke)
    for point in (p1, p2, p3):
        r = check_stroke / 2 - 0.5
        draw.ellipse((point[0] - r, point[1] - r, point[0] + r, point[1] + r), fill=color)


def render(
    size: int,
    background: tuple[int, int, int, int] | None,
    mask: str | None,
    glyph_color: tuple[int, int, int, int],
    glyph_ratio: float,
) -> Image.Image:
    scaled = size * SCALE
    canvas = Image.new("RGBA", (scaled, scaled), TRANSPARENT)
    draw = ImageDraw.Draw(canvas)
    if background is not None:
        if mask == "circle":
            draw.ellipse((0, 0, scaled - 1, scaled - 1), fill=background)
        elif mask == "rounded":
            draw.rounded_rectangle((0, 0, scaled - 1, scaled - 1), radius=scaled * 0.2, fill=background)
        else:
            draw.rectangle((0, 0, scaled - 1, scaled - 1), fill=background)
    glyph = Image.new("RGBA", (scaled, scaled), TRANSPARENT)
    draw_glyph(ImageDraw.Draw(glyph), (scaled / 2, scaled / 2 - scaled * 0.02), scaled * glyph_ratio, glyph_color)
    canvas.alpha_composite(glyph)
    return canvas.resize((size, size), Image.LANCZOS)


def write(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)
    print(f"Generated {path.relative_to(ROOT).as_posix()} {image.size[0]}x{image.size[1]}")


def main() -> None:
    for directory, background in (("mipmap", DAY_BACKGROUND), ("mipmap-night", NIGHT_BACKGROUND)):
        target = RES / directory
        write(render(SIZE, background, "rounded", GLYPH_WHITE, 0.66), target / "ic_launcher.png")
        write(render(ROUND_SIZE, background, "circle", GLYPH_WHITE, 0.60), target / "ic_launcher_round.png")
        # Adaptive layers: the glyph stays inside the 66% safe zone of the 108 dp canvas.
        write(render(SIZE, None, None, GLYPH_WHITE, 0.52), target / "ic_launcher_foreground.png")
        write(render(SIZE, None, None, GLYPH_BLACK, 0.52), target / "ic_launcher_monochrome.png")


if __name__ == "__main__":
    main()
