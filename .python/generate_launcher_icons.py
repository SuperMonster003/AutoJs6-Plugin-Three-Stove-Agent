# -*- coding: utf-8 -*-
"""Compose the Three Stove Agent launcher icons from the maintainer's source art.

Inputs (transparent-background PNG, 1254 x 1254, glyph centered):
  .python/icons/three-stove-ic-launcher-light.png   dark glyph for the light background
  .python/icons/three-stove-ic-launcher-dark.png    light glyph for the night background

Outputs (all RGBA PNG, regenerated deterministically from this script):
  app/src/main/res/mipmap/ic_launcher.png              432 x 432 legacy icon: glyph only, transparent
  app/src/main/res/mipmap/ic_launcher_round.png        432 x 432 legacy round icon: glyph on a filled disc
  app/src/main/res/mipmap/ic_launcher_foreground.png   432 x 432 adaptive foreground (glyph inside the 66 dp safe zone)
  app/src/main/res/mipmap/ic_launcher_monochrome.png   432 x 432 adaptive monochrome (black silhouette)
  app/src/main/res/mipmap-night/*.png                  same set with the night glyph and background

Rules (roadmap D50, AGENTS.md 11.1): the legacy icon keeps a transparent background and its glyph has
exactly the size and position of the round icon's glyph; the round icon and the adaptive background use
values/ic_launcher_background.xml (#D8D8D8) and values-night/ic_launcher_background.xml (#272727). The
adaptive XML references these mipmaps directly, as the 3-Stone AI plugin does.

Usage: py .python/generate_launcher_icons.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
ICONS = ROOT / ".python" / "icons"
SIZE = 432
SCALE = 4

DAY_BACKGROUND = (0xD8, 0xD8, 0xD8, 255)
NIGHT_BACKGROUND = (0x27, 0x27, 0x27, 255)
TRANSPARENT = (0, 0, 0, 0)
BLACK = (0, 0, 0, 255)

# Glyph width as a fraction of the canvas. The legacy and round icons share one value so the
# legacy icon equals the round icon minus its background. The adaptive layers are drawn smaller:
# the launcher shows the central 72 dp of the 108 dp layer and masks it, so a 44% glyph appears at
# 66% and its corners (aspect 850:695) stay inside the 66 dp safe circle.
LEGACY_GLYPH = 0.66
ADAPTIVE_GLYPH = 0.44


def load_glyph(name: str) -> Image.Image:
    path = ICONS / name
    if not path.is_file():
        raise SystemExit(f"Missing icon source: {path.relative_to(ROOT).as_posix()}")
    image = Image.open(path).convert("RGBA")
    box = image.getchannel("A").getbbox()
    if box is None:
        raise SystemExit(f"Icon source is fully transparent: {path.name}")
    return image.crop(box)


def place_glyph(canvas: Image.Image, glyph: Image.Image, width_ratio: float) -> None:
    scaled = canvas.width * SCALE
    target_width = int(round(scaled * width_ratio))
    target_height = max(1, int(round(glyph.height * target_width / glyph.width)))
    resized = glyph.resize((target_width, target_height), Image.LANCZOS)
    layer = Image.new("RGBA", (scaled, scaled), TRANSPARENT)
    layer.alpha_composite(resized, ((scaled - target_width) // 2, (scaled - target_height) // 2))
    canvas.alpha_composite(layer.resize((canvas.width, canvas.height), Image.LANCZOS))


def render(glyph: Image.Image, width_ratio: float, background: tuple[int, int, int, int] | None, monochrome: bool = False) -> Image.Image:
    canvas = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
    if background is not None:
        scaled = SIZE * SCALE
        disc = Image.new("RGBA", (scaled, scaled), TRANSPARENT)
        ImageDraw.Draw(disc).ellipse((0, 0, scaled - 1, scaled - 1), fill=background)
        canvas.alpha_composite(disc.resize((SIZE, SIZE), Image.LANCZOS))
    if monochrome:
        silhouette = Image.new("RGBA", glyph.size, BLACK)
        silhouette.putalpha(glyph.getchannel("A"))
        glyph = silhouette
    place_glyph(canvas, glyph, width_ratio)
    return canvas


def write(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)
    print(f"Generated {path.relative_to(ROOT).as_posix()} {image.size[0]}x{image.size[1]}")


def main() -> None:
    for directory, source, background in (
        ("mipmap", "three-stove-ic-launcher-light.png", DAY_BACKGROUND),
        ("mipmap-night", "three-stove-ic-launcher-dark.png", NIGHT_BACKGROUND),
    ):
        glyph = load_glyph(source)
        target = RES / directory
        write(render(glyph, LEGACY_GLYPH, None), target / "ic_launcher.png")
        write(render(glyph, LEGACY_GLYPH, background), target / "ic_launcher_round.png")
        write(render(glyph, ADAPTIVE_GLYPH, None), target / "ic_launcher_foreground.png")
        write(render(glyph, ADAPTIVE_GLYPH, None, monochrome=True), target / "ic_launcher_monochrome.png")


if __name__ == "__main__":
    main()
