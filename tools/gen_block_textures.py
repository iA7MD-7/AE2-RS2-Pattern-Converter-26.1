import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources"
BLOCK = ROOT / "assets" / "pattern_converter" / "textures" / "block"

FRAMES = 8

STEEL_EDGE = (0x23, 0x24, 0x2C)
STEEL_HI = (0x6B, 0x6E, 0x78)
STEEL_LO = (0x33, 0x35, 0x3C)
STEEL_TOP = (0x5C, 0x5F, 0x69)
STEEL_BOTTOM = (0x42, 0x44, 0x4C)
RIVET_HI = (0x9A, 0x9D, 0xA8)
RIVET_LO = (0x2A, 0x2B, 0x32)
BOARD = (0x14, 0x16, 0x20)

RS_DEEP = (0x0A, 0x4E, 0x80)
RS_MID = (0x00, 0x8C, 0xE0)
RS_BRIGHT = (0x3F, 0xB6, 0xFF)
RS_ICE = (0xD6, 0xF2, 0xFF)

AE_DEEP = (0x2A, 0x26, 0x52)
AE_MID = (0x6C, 0x4C, 0xC0)
AE_BRIGHT = (0x9E, 0x6C, 0xE8)
AE_GLOW = (0xE6, 0xD4, 0xFF)

WHITE = (0xFF, 0xFF, 0xFF)


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def gradient3(a, b, c, t):
    return lerp(a, b, t * 2) if t < 0.5 else lerp(b, c, (t - 0.5) * 2)


def put(img, x, y, rgb, alpha=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), (rgb[0], rgb[1], rgb[2], alpha))


def housing():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            put(img, x, y, lerp(STEEL_TOP, STEEL_BOTTOM, y / 15))
    for i in range(16):
        put(img, i, 0, STEEL_EDGE)
        put(img, i, 15, STEEL_EDGE)
        put(img, 0, i, STEEL_EDGE)
        put(img, 15, i, STEEL_EDGE)
    for i in range(1, 15):
        put(img, i, 1, STEEL_HI)
        put(img, 1, i, STEEL_HI)
        put(img, i, 14, STEEL_LO)
        put(img, 14, i, STEEL_LO)
    for (x, y) in ((2, 2), (12, 2), (2, 12), (12, 12)):
        put(img, x, y, RIVET_HI)
        put(img, x + 1, y, lerp(RIVET_HI, RIVET_LO, 0.5))
        put(img, x, y + 1, lerp(RIVET_HI, RIVET_LO, 0.5))
        put(img, x + 1, y + 1, RIVET_LO)
    return img


def side():
    img = housing()
    for y in (4, 6):
        for x in range(4, 12):
            put(img, x, y, lerp(STEEL_LO, STEEL_EDGE, 0.5))
    for x in range(2, 14):
        t = (x - 2) / 11
        if x <= 7:
            c = gradient3(RS_DEEP, RS_MID, RS_BRIGHT, (x - 2) / 5)
        else:
            c = gradient3(AE_BRIGHT, AE_MID, AE_DEEP, (x - 8) / 5)
        put(img, x, 10, lerp(c, STEEL_EDGE, 0.35))
        put(img, x, 11, c)
    put(img, 7, 11, lerp(RS_ICE, WHITE, 0.5))
    put(img, 8, 11, lerp(AE_GLOW, WHITE, 0.5))
    return img


def bottom():
    img = housing()
    for y in range(5, 11):
        for x in range(5, 11):
            put(img, x, y, lerp(STEEL_LO, STEEL_EDGE, (x + y - 10) / 10))
    return img


def top_frame(f):
    img = housing()
    cx, cy = 7.5, 7.5
    sweep = (f / FRAMES) * 16.0
    for y in range(3, 13):
        for x in range(3, 13):
            if abs(x - cx) + abs(y - cy) > 5.0:
                continue
            depth = 1.0 - (abs(x - cx) + abs(y - cy)) / 5.0
            if x <= 7:
                base = gradient3(RS_DEEP, RS_MID, RS_BRIGHT, depth)
            else:
                base = gradient3(AE_DEEP, AE_MID, AE_BRIGHT, depth)
            dist = min(abs(x - sweep), abs(x - sweep + 16), abs(x - sweep - 16))
            glow = max(0.0, 1.0 - dist / 2.5) * 0.7
            put(img, x, y, lerp(base, WHITE, glow))
    for y in range(3, 13):
        if abs(y - cy) <= 4.5:
            put(img, 7, y, lerp(RS_ICE, WHITE, 0.4))
            put(img, 8, y, lerp(AE_GLOW, WHITE, 0.4))
    return img


def front_frame(f, lit):
    img = housing()
    for y in range(2, 14):
        for x in range(2, 14):
            put(img, x, y, BOARD)
    pulse = (f / FRAMES) * 12.0
    for y in range(2, 14):
        for x in range(2, 14):
            d = (x - 2) + (y - 2)
            if d < 11:
                base = gradient3(RS_DEEP, RS_MID, RS_BRIGHT, d / 10)
            elif d > 11:
                base = gradient3(AE_BRIGHT, AE_MID, AE_DEEP, (d - 12) / 10)
            else:
                base = None
            k = x - 2
            dist = min(abs(k - pulse), abs(k - pulse + 12), abs(k - pulse - 12))
            if d == 11:
                core = WHITE if lit else lerp(RS_ICE, AE_GLOW, k / 11)
                glow = max(0.0, 1.0 - dist / (3.0 if lit else 2.0))
                put(img, x, y, lerp(lerp(core, BOARD, 0.25), WHITE, glow))
            elif d in (10, 12):
                spill = max(0.0, 1.0 - dist / (3.0 if lit else 2.0)) * (0.8 if lit else 0.5)
                put(img, x, y, lerp(base, WHITE, spill))
            else:
                put(img, x, y, base)
    ink = WHITE if lit else RS_ICE
    for (bx, by) in ((3, 3), (6, 3), (3, 6)):
        for dy in range(2):
            for dx in range(2):
                put(img, bx + dx, by + dy, ink)
    put(img, 6, 6, ink)
    glow = WHITE if lit else AE_GLOW
    for (x, y) in ((10, 8), (9, 9), (10, 9), (11, 9), (9, 10), (10, 10), (11, 10), (10, 11), (10, 12)):
        put(img, x, y, glow if (x, y) in ((10, 9), (10, 10)) else lerp(glow, AE_BRIGHT, 0.45))
    return img


def strip(frames):
    img = Image.new("RGBA", (16, 16 * len(frames)))
    for i, fr in enumerate(frames):
        img.paste(fr, (0, 16 * i))
    return img


def write_animated(name, frames, frametime):
    strip(frames).save(BLOCK / f"{name}.png")
    (BLOCK / f"{name}.png.mcmeta").write_text(
        json.dumps({"animation": {"frametime": frametime, "interpolate": True}}, indent=2) + "\n", encoding="utf-8")


def main():
    import sys
    force = "--force" in sys.argv
    BLOCK.mkdir(parents=True, exist_ok=True)

    def want(name):
        exists = (BLOCK / f"{name}.png").exists()
        if exists and not force:
            print(f"kept   {name}.png (hand-edited or already generated; --force to overwrite)")
            return False
        return True

    if want("pattern_converter_side"):
        side().save(BLOCK / "pattern_converter_side.png")
    if want("pattern_converter_bottom"):
        bottom().save(BLOCK / "pattern_converter_bottom.png")
    for stale in ("pattern_converter_side.png.mcmeta", "pattern_converter_bottom.png.mcmeta"):
        (BLOCK / stale).unlink(missing_ok=True)
    if want("pattern_converter_top"):
        write_animated("pattern_converter_top", [top_frame(f) for f in range(FRAMES)], 4)
    if want("pattern_converter_front"):
        write_animated("pattern_converter_front", [front_frame(f, False) for f in range(FRAMES)], 4)
    if want("pattern_converter_front_on"):
        write_animated("pattern_converter_front_on", [front_frame(f, True) for f in range(FRAMES)], 2)
    lit = Image.open(BLOCK / "pattern_converter_front_on.png").convert("RGBA").crop((0, 0, 16, 16))
    logo = Image.new("RGBA", (160, 160))
    logo.paste(lit.resize((128, 128), Image.NEAREST), (16, 16))
    logo.save(ROOT / "logo.png")
    print("logo written from the current front_on frame 0")


if __name__ == "__main__":
    main()
