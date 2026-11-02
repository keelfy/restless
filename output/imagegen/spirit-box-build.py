"""Build spirit box resources from the saved imagegen sources."""

import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "apps/mod/core/src/main/resources/assets/restless"
SOURCES = Path(__file__).resolve().parent


def save_json(path, value):
    (ASSETS / path).write_text(json.dumps(value, indent=2) + "\n")


# Each 64px texture covers 16 model units: four texels per unit.
panel = Image.open(SOURCES / "spirit-box-front-source.png").convert("RGB")
panel = panel.resize((40, 16), Image.Resampling.NEAREST).quantize(colors=16).convert("RGBA")
atlas = Image.new("RGBA", (64, 64), (39, 42, 40, 255))
atlas.paste(panel, (0, 0))
atlas.save(ASSETS / "textures/block/spirit_box.png")

casing = Image.new("RGBA", (64, 64), (43, 46, 43, 255))
draw = ImageDraw.Draw(casing)
for y in range(0, 64, 8):
    draw.line((0, y, 63, y), fill=(49, 51, 46, 255))
for y in (3, 27):
    for x in range(4, 36, 4):
        draw.line((x, y, x + 1, y), fill=(25, 29, 28, 255))
casing.save(ASSETS / "textures/block/spirit_box_casing.png")

trim = Image.new("RGBA", (64, 64), (135, 112, 70, 255))
draw = ImageDraw.Draw(trim)
for y in range(0, 64, 4):
    draw.line((0, y, 63, y), fill=(156, 132, 87, 255))
trim.save(ASSETS / "textures/block/spirit_box_trim.png")

item = Image.open(SOURCES / "spirit-box-item-source.png").convert("RGBA")
item.thumbnail((32, 32), Image.Resampling.NEAREST)
icon = Image.new("RGBA", (32, 32))
icon.paste(item, ((32 - item.width) // 2, (32 - item.height) // 2))
icon.save(ASSETS / "textures/item/spirit_box.png")


def cuboid(start, end, texture="casing", front=False):
    x, y, z = (end[i] - start[i] for i in range(3))
    sizes = {"up": (x, z), "down": (x, z), "north": (x, y),
             "south": (x, y), "west": (z, y), "east": (z, y)}
    faces = {face: {"uv": [0, 0, width, height], "texture": "#" + texture}
             for face, (width, height) in sizes.items()}
    if front:
        faces["north"]["texture"] = "#front"
    return {"from": start, "to": end, "faces": faces}


elements = [cuboid([3, 0.5, 4], [13, 4.5, 12], front=True)]
for x in (3.5, 11.5):
    for z in (4.5, 10.5):
        elements.append(cuboid([x, 0, z], [x + 1, 0.5, z + 1]))
# Handle, tuning knobs and antenna remain inside the existing selection shape.
for x in (6, 9.5):
    elements.append(cuboid([x, 4.5, 8], [x + 0.5, 5.5, 8.5], "trim"))
elements.append(cuboid([6, 5.5, 8], [10, 6, 8.5]))
for x in (4, 10.5):
    elements.append(cuboid([x, 4.5, 5], [x + 1.5, 5, 6.5], "trim"))
elements.append(cuboid([12, 4.5, 10.5], [12.5, 6, 11], "trim"))

save_json("models/block/spirit_box.json", {
    "parent": "minecraft:block/block",
    "textures": {"particle": "restless:block/spirit_box_casing",
                 "front": "restless:block/spirit_box",
                 "casing": "restless:block/spirit_box_casing",
                 "trim": "restless:block/spirit_box_trim"},
    "elements": elements,
})
save_json("models/item/spirit_box.json", {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "restless:item/spirit_box"},
})
save_json("items/spirit_box.json", {
    "model": {"type": "minecraft:model", "model": "restless:item/spirit_box"},
})
