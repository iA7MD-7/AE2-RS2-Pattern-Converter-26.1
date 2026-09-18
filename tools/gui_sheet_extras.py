from pathlib import Path

from PIL import Image

SHEET = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "pattern_converter" / "textures" / "gui" / "pattern_converter.png"

BG = (0xC6, 0xC6, 0xC6, 255)
HI = (0xFF, 0xFF, 0xFF, 255)
LO = (0x55, 0x55, 0x55, 255)
OUTLINE = (0, 0, 0, 255)
CLEAR = (0, 0, 0, 0)

ICON_V, ICON_HOVER_V = 80, 98
ICON_SWITCH_U, ICON_ISSUES_U = 208, 226
ICON_INK = (0x20, 0x20, 0x20, 255)
ICON_HOVER = (0x2A, 0x8E, 0xD8, 255)
BUTTON_U, BUTTON_V = 200, 0
BUTTON_FACE, BUTTON_FACE_HOVER = (0xA8, 0xA8, 0xA8, 255), (0xBC, 0xC8, 0xFF, 255)
BLANK_BUTTON_U, BLANK_BUTTON_V, BLANK_BUTTON_HOVER_V = 208, 128, 146

BUG = [
    "..................",
    "..................",
    "......#....#......",
    ".......#..#.......",
    "......######......",
    ".....########.....",
    "...#.########.#...",
    "..#..########..#..",
    "..#..########..#..",
    ".....########.....",
    "...#.########.#...",
    "..#..########..#..",
    "..#..########..#..",
    ".....########.....",
    "......######......",
    ".......####.......",
    "..................",
    "..................",
]


def fix_right_blank_slot(px):
    for x in range(92, 110):
        px[x, 34] = HI


def clear_retired_sprites(px):
    for y in range(80, 124):
        for x in range(176, 203):
            px[x, y] = CLEAR


def draw_blank_buttons(px):
    for (src_v, dst_v, face) in ((BUTTON_V, BLANK_BUTTON_V, BUTTON_FACE), (BUTTON_V + 18, BLANK_BUTTON_HOVER_V, BUTTON_FACE_HOVER)):
        for y in range(18):
            for x in range(18):
                c = px[BUTTON_U + x, src_v + y]
                if c[:3] == ICON_INK[:3] or c[3] == 0:
                    c = face if 0 < x < 17 and 0 < y < 17 else HI
                px[BLANK_BUTTON_U + x, dst_v + y] = c


def draw_icons(px):
    def clear(u, v):
        for y in range(18):
            for x in range(18):
                px[u + x, v + y] = CLEAR
    for (u, v) in ((ICON_SWITCH_U, ICON_V), (ICON_SWITCH_U, ICON_HOVER_V), (ICON_ISSUES_U, ICON_V), (ICON_ISSUES_U, ICON_HOVER_V)):
        clear(u, v)
    for y in range(1, 17):
        for x in range(1, 17):
            if px[BUTTON_U + x, BUTTON_V + y][:3] == ICON_INK[:3]:
                px[ICON_SWITCH_U + x, ICON_V + y] = ICON_INK
                px[ICON_SWITCH_U + x, ICON_HOVER_V + y] = ICON_HOVER
    for y, row in enumerate(BUG):
        for x, ch in enumerate(row):
            if ch == "#":
                px[ICON_ISSUES_U + x, ICON_V + y] = ICON_INK
                px[ICON_ISSUES_U + x, ICON_HOVER_V + y] = ICON_HOVER


def fix_button_left_edge(px):
    for y in range(1, 17):
        if px[BUTTON_U, BUTTON_V + y][3] == 0:
            px[BUTTON_U, BUTTON_V + y] = HI


def main():
    img = Image.open(SHEET).convert("RGBA")
    px = img.load()
    fix_right_blank_slot(px)
    fix_button_left_edge(px)
    clear_retired_sprites(px)
    draw_icons(px)
    draw_blank_buttons(px)
    img.save(SHEET)
    print("sheet updated:", SHEET)


if __name__ == "__main__":
    main()
