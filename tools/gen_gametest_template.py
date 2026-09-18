import gzip
import io
import struct
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "data" / "pattern_converter" / "structure"
DATA_VERSION = 3955
SIZE = 3


def w_str(buf, s):
    b = s.encode("utf8")
    buf.write(struct.pack(">H", len(b)))
    buf.write(b)


def w_named(buf, tag, name, writer):
    buf.write(bytes([tag]))
    w_str(buf, name)
    writer(buf)


def w_int_payload(v):
    return lambda b: b.write(struct.pack(">i", v))


def w_list_int(buf, vals):
    buf.write(bytes([3]))
    buf.write(struct.pack(">i", len(vals)))
    for v in vals:
        buf.write(struct.pack(">i", v))


def main():
    palette = [("minecraft:stone", {}), ("minecraft:air", {})]
    blocks = []
    for x in range(SIZE):
        for z in range(SIZE):
            blocks.append((x, 0, z, 0))
            for y in range(1, SIZE):
                blocks.append((x, y, z, 1))

    buf = io.BytesIO()
    buf.write(b"\x0a")
    w_str(buf, "")
    w_named(buf, 9, "size", lambda b: w_list_int(b, [SIZE, SIZE, SIZE]))
    w_named(buf, 9, "entities", lambda b: (b.write(b"\x00"), b.write(struct.pack(">i", 0))))

    def wblocks(b):
        b.write(bytes([10]))
        b.write(struct.pack(">i", len(blocks)))
        for (x, y, z, s) in blocks:
            w_named(b, 9, "pos", lambda bb, x=x, y=y, z=z: w_list_int(bb, [x, y, z]))
            w_named(b, 3, "state", w_int_payload(s))
            b.write(b"\x00")

    w_named(buf, 9, "blocks", wblocks)

    def wpal(b):
        b.write(bytes([10]))
        b.write(struct.pack(">i", len(palette)))
        for (nm, _) in palette:
            w_named(b, 8, "Name", lambda bb, nm=nm: w_str(bb, nm))
            b.write(b"\x00")

    w_named(buf, 9, "palette", wpal)
    w_named(buf, 3, "DataVersion", w_int_payload(DATA_VERSION))
    buf.write(b"\x00")

    OUT.mkdir(parents=True, exist_ok=True)
    with gzip.open(OUT / "empty.nbt", "wb") as f:
        f.write(buf.getvalue())
    print("wrote", OUT / "empty.nbt")


if __name__ == "__main__":
    main()
