# AE2 & RS2 Pattern Converter

One block that converts autocrafting patterns between **Refined Storage 2** and **Applied Energistics 2**,
in both directions, using each mod's own pattern API — no more re-encoding every pattern by hand when a
pack has both, or when you move your base from one to the other.

- Minecraft **26.1.2** · NeoForge **26.1.2.109+** · Java 25
- Requires Refined Storage 3.2.1+ and Applied Energistics 2 26.1.11-beta+
- Optional: JEI for the recipe category (EMI does not exist on 26.1 yet)

NeoForge only: AE2 has no Fabric or Forge builds for these Minecraft versions, so there is nothing to
convert to on other loaders.

## What it converts

| Pattern type | RS2 → AE2 | AE2 → RS2 |
|---|---|---|
| Crafting (3×3 grid, result, substitution flag) | ✔ | ✔ |
| Processing (items and fluids) | ✔ up to 81 inputs / 27 outputs | ✔ up to 81 / 81 |
| Stonecutter | ✔ | ✔ |
| Smithing table | ✔ | ✔ |

Crafting patterns are re-resolved against the recipes on the other side, so the converted pattern is one
the destination mod would have encoded itself.

## Using the block

1. Put encoded patterns from either mod in the **left 3×3 grid**. A mixed batch is fine — the direction is
   detected per pattern.
2. Put **blank patterns of the other mod** in the slot **under the arrow**.
3. Converted patterns appear in the **right 3×3 grid**. The emptied originals come back as blanks in the
   slot **beside the output grid**; that slot is filled by the machine only, and when it is full the block
   waits until you empty it.

Nothing is created or destroyed: one encoded pattern plus one blank in, one converted pattern plus one
blank out.

- The **arrow** between the grids shows the direction and is also the direction button: click it to cycle
  **Auto → RS2 → AE2 → AE2 → RS2**. "Auto" / "Locked" is shown at the bottom right, and while locked,
  patterns of the other kind are refused.
- Empty slots show a **ghost icon** of the pattern that belongs there (in Auto it alternates between the two).
- An input slot turns **red** when its pattern cannot be converted and **amber** while it waits for blanks
  or space; hover it for the reason (recipe missing on the other side, too many outputs for AE2, a resource
  the destination cannot hold, direction locked…).
- Details that cannot be carried over exactly (Refined Storage tag alternatives, AE2 fluid substitution)
  are dropped and listed on the arrow tooltip instead of failing the pattern.
- The **bug button** to the left of the GUI opens the issue tracker.

## Automation

- Pipes, conduits, hoppers and the storage mods' own exporters can push patterns (→ input grid) and blanks
  (→ supply slot) in from any side, and pull converted patterns and returned blanks out.
- The whole grid is converted every work cycle.
- A **redstone pulse** fires after every cycle that converted something, a comparator reads the output
  grid, and the front face lights up while the block is working.

## Recipe viewers

AE2 on 26.1 has no recipe-viewer integration of its own yet, and Refined Storage's `+` button in the
Pattern Grid comes from its separate *Refined Storage – JEI Integration* mod. This mod's own category
shows up in JEI.

## Config

`config/pattern_converter-common.toml`

| Key | Default | Meaning |
|---|---|---|
| `machine.cycleTicks` | 10 | Ticks between work cycles. |
| `machine.patternsPerCycle` | 9 | Patterns converted per cycle (9 = the whole grid). |
| `machine.redstonePulseTicks` | 10 | Length of the redstone pulse after a successful cycle. |
| `conversion.unmappableResources` | `BRIDGE_IF_AVAILABLE` | What to do with a resource that is not an item or fluid: convert it when the other side can hold it, `ALWAYS_REJECT` the pattern, or `DROP_UNMAPPABLE_OUTPUTS` (outputs only — inputs are never dropped). |
| `conversion.dropAlternativeInputs` | true | Convert RS2 patterns with tag alternatives by keeping the first choice (false = refuse them). |
| `conversion.dropFluidSubstitution` | true | Convert AE2 patterns with fluid substitution by dropping the flag (false = refuse them). |

## Building

```
python tools/fetch_libs.py     # downloads the exact dependency jars into libs/ and the run directories
gradlew build                  # jar in build/libs
gradlew runClient              # dev client
gradlew runGameTestServer      # 12 end-to-end conversion tests; the exit code is the result
```

Java 25. The project compiles against release jars in `libs/` instead of mod mavens, so every commit builds
against the same bytes; `tools/fetch_libs.py` is the list of exactly which versions those are (it downloads
them from Modrinth, verifies the checksums and extracts the nested API jars). `libs/*.jar` and `run/` are
git-ignored; `python tools/fetch_libs.py --libs-only` is all a build needs and is what CI runs.

`gradlew runClient` starts a client with Refined Storage, AE2, JEI and the Refined Storage JEI integration
already in `run/mods`. The script parks three jars in `run/mods_disabled` whose current 26.1 builds crash the
creative inventory (an upstream bug); the game-test server still loads them.

Other scripts in `tools/`: `gen_lang.py` writes the non-English lang files and checks every locale against
`en_us.json`; `gui_sheet_extras.py` re-applies the code-drawn sprites to the hand-edited GUI sheet (run it
after editing `textures/gui/pattern_converter.png`); `gen_block_textures.py` renders the animated block
textures; `gen_gametest_template.py` writes the structure the game tests run in.

## Versions

| Repository | Minecraft | NeoForge | Pack |
|---|---|---|---|
| [AE2-RS2-Pattern-Converter](https://github.com/iA7MD-7/AE2-RS2-Pattern-Converter) | 1.21.1 | 21.1.194+ | All the Mods 10 |
| [AE2-RS2-Pattern-Converter-26.1](https://github.com/iA7MD-7/AE2-RS2-Pattern-Converter-26.1) | 26.1.2 | 26.1.2.109+ | All the Mods 11 |

Same mod, same features; fixes are ported between the two.

## Contributing

Bug reports and feature requests go to the [issue tracker](https://github.com/iA7MD-7/AE2-RS2-Pattern-Converter-26.1/issues) — the bug button beside the GUI opens
the same page. Pull requests are welcome; see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE). Refined Storage and Applied Energistics 2 are separate projects under their own licenses;
this mod compiles against their public APIs and does not redistribute them.
