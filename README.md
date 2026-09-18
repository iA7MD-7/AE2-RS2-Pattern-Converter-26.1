# AE2 & RS2 Pattern Converter

A bridge block that re-encodes autocrafting patterns between **Refined Storage 2** and
**Applied Energistics 2**, in both directions, through each mod's own pattern API.

- **Minecraft 26.1.2 · NeoForge 26.1.2.109+ · Java 25** (this branch; the 1.21.1 build lives on the `1.21.1` branch)
- Requires Refined Storage 3.2.1+ and Applied Energistics 2 26.1.11-beta+
- Optional: AdvancedAE, ExtendedAE, Refined Types (FE), Applied Flux, JEI

## 26.1 port notes

Ported from the `1.21.1` branch on 2026-09-17. The conversion core (`api/`, `convert/`) needed
only the `ResourceLocation → Identifier` rename and recipe ids becoming `ResourceKey<Recipe<?>>`;
everything else that changed was Minecraft/NeoForge plumbing:

- recipes are server-only (`ServerLevel.recipeAccess()`), `assemble(input)` lost its registry
  argument, stonecutter results come from `assemble`;
- block entities save through `ValueOutput`/`ValueInput`, drop contents in `preRemoveSideEffects`;
- the item capability is NeoForge's transfer API (`ResourceHandler<ItemResource>`), so the
  inventory is an `ItemStacksResourceHandler` and the menu uses `ResourceHandlerSlot`;
- screens "extract render state" (`GuiGraphicsExtractor`, `RenderPipelines.GUI_TEXTURED`) — the
  whole screen class was rewritten, same layout;
- items need an `items/<name>.json` model definition, recipe ingredients are plain strings;
- game tests are registered through `RegisterGameTestsEvent` instead of `@GameTestHolder`.

Known upstream issue on 26.1 (not this mod): **ExtendedAE 26.1-1.0.3 and Applied Flux 26.1-1.0.1
crash the client when the creative inventory is opened** ("Stack […] has already been added to the
tab" from their Glodium registration). They are therefore left out of the dev client's `run/mods`
(`tools/fetch_libs.py` stages them in `run/mods_disabled`) but stay in the game-test server. Pack devs on 26.1 should
watch for a fixed release of those two before shipping them.

## Why NeoForge only

A Fabric/Forge/NeoForge multiloader build was the original plan. The release matrices (checked
against Modrinth on 2026‑09‑15) make that moot:

| | Forge 1.21.1 | Fabric 1.21.1 | NeoForge 1.21.1 |
|---|---|---|---|
| Refined Storage 2 | — | ✔ | ✔ |
| Applied Energistics 2 | — | — (Fabric support ended at 1.20.1) | ✔ |
| AdvancedAE / Expanded AE / ExtendedAE / Refined Types | — | — | ✔ |

NeoForge 1.21.1 is the only loader/version on which both storage mods and all four addons
exist. A Fabric build would have nothing to convert *to*, and Forge has neither mod.
1.21.1 (rather than the newer 26.1.x builds of RS2/AE2) is where the addon ecosystem tops out.

The code is still split so an Architectury port is mechanical if AE2 ever returns to Fabric:

- `api/` and `convert/` are loader-agnostic — they use only Minecraft, RS2's `common` API and
  AE2's `appeng.api` surface, which are identical on every loader.
- `registry/`, `block/`, `menu/`, `client/` and `PCConfig` are the NeoForge layer
  (DeferredRegister, capabilities, ModConfigSpec, `IMenuTypeExtension`).

## The block

```
┌─────────────────────────────────────────────┐
│   INPUT PATTERNS      OUTPUT (converted)     │
│   [0][0][0]    ⇉      [X][X][X]              │
│   [0][0][0]    ⇇      [X][X][X]              │
│   [0][0][0]    ●      [X][X][X]              │
│  Auto / Locked   [ blank buffer ]     [⇕]    │
│  RS2 → AE2                                   │
│  status line (hover: recent notes)           │
└─────────────────────────────────────────────┘
```

- **Input grid** — encoded RS2 or AE2 patterns (any addon pattern AE2/RS2 can decode).
- **Blank buffer** — blank patterns of the *destination* format, one consumed per conversion.
- **Output grid** — the converted pattern **and** the emptied original (an AE2 pattern comes
  back as an AE2 blank, an RS2 pattern as an RS2 blank). Nothing is created or destroyed:
  `1 encoded A + 1 blank B → 1 encoded B + 1 blank A`.
- **Direction** is detected per pattern. The ⇕ button cycles *Automatic → Locked RS2→AE2 →
  Locked AE2→RS2*; while locked, patterns of the other kind are refused with a reason.
- **Batch** — every work cycle (`cycleTicks`, default 10) converts up to `patternsPerCycle`
  patterns (default 9 = the whole grid).
- **Redstone** — a pulse (`redstonePulseTicks`) after every cycle that converted something;
  the front inlay lights up for its duration. A comparator reads the output grid.
- **Automation** — the item handler accepts patterns into the input grid and blanks into the
  buffer from any side, and only lets the output grid be extracted.
- **Validation feedback** — every input slot is tinted (red = refused, amber = waiting,
  blue = queued) and its tooltip carries the reason. The status line shows the last event;
  hovering it lists recent notes (things that were dropped because the destination cannot
  express them).

## Conversion logic

1. A `PatternTranslator` reads the source item into a mod-agnostic `UniversalPattern`
   (`Crafting` 3×3 grid, `Processing` inputs/outputs, `Stonecutting`, `Smithing`).
2. Keys are mapped through `KeyBridge`s onto `UniversalKey` (item, fluid, or a named custom
   type such as Forge Energy).
3. A `PatternEncoder` writes the destination item through that mod's API:
   - AE2: `PatternDetailsHelper.encode*Pattern(...)` — never hand-written NBT.
   - RS2: the same data components its Pattern Grid writes (`PatternState` +
     `Crafting/Processing/Stonecutter/SmithingTablePatternState`).
4. Shapes the destination cannot express fail with a translated reason (too many AE2 outputs,
   recipe not found, unmappable resource…). Lossy-but-safe details become **notes** instead
   (RS2 tag alternatives dropped for AE2, AE2 fluid substitution dropped for RS2, AdvancedAE
   push directions dropped, large AE2 amounts split across RS2 slots).

| Detail | RS2 → AE2 | AE2 → RS2 |
|---|---|---|
| Crafting grid layout + result | ✔ (recipe re-resolved, id preserved when it still matches) | ✔ |
| Fuzzy / substitutes flag | ✔ | ✔ |
| Fluid substitution (AE2) | n/a | note (or reject via config) |
| Processing inputs/outputs | ✔ (81 in / 27 out; same keys merged) | ✔ (81 / 81; per-slot limits split) |
| Tag alternatives (RS2) | note (or reject via config) | n/a |
| Stonecutter / smithing | ✔ | ✔ |
| FE / Source (via addons) | ✔ when bridge present | ✔ when bridge present |
| IF Souls | policy (see below) | — |

## The Refined Types decision (unmappable resources)

Refined Types adds FE, Ars Nouveau Source and Industrial Foregoing Souls to RS2. On the AE2
side, **Applied Flux** holds FE and **Ars Énergistique** holds Source; nothing holds Souls.
Rather than picking one of lossy / reject / conditional globally, the policy is a config
value, applied to *any* non item/fluid resource on either side (so it also covers e.g.
Applied Mekanistics chemicals in AE2 patterns):

```
conversion.unmappableResources = BRIDGE_IF_AVAILABLE   (default)
```

- `BRIDGE_IF_AVAILABLE` — convert when a matching key type exists on the other side
  (Refined Types FE ↔ Applied Flux FE, Source ↔ Ars Énergistique); otherwise refuse and say
  which resource and which addon is missing.
- `ALWAYS_REJECT` — refuse any pattern with a non item/fluid resource, bridge or not.
- `DROP_UNMAPPABLE_OUTPUTS` — like the default, but an unmappable **output** is dropped
  with a note. **Inputs are never dropped**: a pattern that stops paying its Source cost is
  an exploit, not a conversion. A pattern whose outputs would all be dropped is refused.

## Addon compatibility

Primary path is API-first: anything AE2's decoder registry (`PatternDetailsHelper`) or RS2's
`PatternProviderItem` can decode converts without a dedicated translator — it is read through
`IPatternDetails` / `RefinedStorageApi.getPattern` and written as a processing pattern, with
a note saying so. Dedicated translators/bridges override that by priority:

| Addon | What the module does |
|---|---|
| **AdvancedAE** | Translator for `advanced_ae:adv_processing_pattern` (Pattern Encoder output). Inputs/outputs carried exactly; per-input push directions reported as dropped (RS2 routes by provider side). Vanilla AE2 processing patterns already work in Advanced Pattern Providers, so the reverse direction needs nothing special. |
| **Expanded AE** | Its Expanded/Giga Pattern Providers hold stock AE2 patterns — covered by the core translator. Detected and listed in the JEI info page; no separate translator exists because there is no separate pattern format. |
| **ExtendedAE** | Same: the 36-slot provider and the Pattern Modifier's batch-edited/cloned patterns are stock AE2 patterns. Amounts above stack size (a Pattern Modifier product) are split across RS2 matrix slots by the core encoder. |
| **Refined Types** | RS2-side key bridge naming FE / Source / Souls as custom universal keys. |
| **Applied Flux** | AE2-side key bridge for FE (`FluxKey`, FE variant only). |
| **Ars Énergistique** | AE2-side key bridge for Source (`SourceKey`). |

Each module is guarded by a `ModList` check and a try/catch; a broken addon version logs an
error and disables only itself. Per-addon config toggles live under `addons.*`.

## For other mods

`com.ia7md.patternconverter.api.PatternConverterApi` is the extension point:

```java
PatternConverterApi.registerTranslator(myTranslator);   // read your pattern item
PatternConverterApi.registerRs2KeyBridge(myBridge);      // map an RS2 ResourceKey
PatternConverterApi.registerAe2KeyBridge(myBridge);      // map an AEKey
```

Register during `FMLCommonSetupEvent` (the mod itself registers in `enqueueWork`; order
`AFTER pattern_converter` to be safe). Custom resource types meet in the middle through
`UniversalKey.Custom(ResourceLocation type)` — use the ids in `CustomKeyTypes` or your own.

## Recipe viewers (JEI / EMI)

A *Pattern Conversion* category (one entry per direction, block as workstation/catalyst) is
provided natively for **both JEI and EMI**, plus a JEI info page on the block listing the
active addon modules.

Which viewer to run matters more for the two storage mods than for this one:

- **AE2 19.x (1.21) integrates only with EMI** — it dropped JEI/REI. Without EMI, JEI cannot
  see items in ME terminals and there is no `+` transfer button in the Pattern Encoding
  Terminal. This is AE2's choice, not something this mod can fix.
- **RS2's viewer support is separate**: install *Refined Storage – JEI Integration* and/or
  *Refined Storage – EMI Integration* for the Pattern Grid's `+` button.

The dev client (`run/mods`) ships EMI + JEI + both RS integrations, which is the ATM-style
setup: EMI drives the overlay and bridges JEI plugins through JEMI.

## Building

```
python tools/fetch_libs.py     # downloads the exact dependency jars into libs/ and the run dirs
gradlew build                  # jar in build/libs
gradlew runClient              # dev client with every supported addon loaded from run/mods
gradlew runGameTestServer      # headless end-to-end conversion tests (12); exit code is the result
```

Java 25. The project compiles against release jars in `libs/` rather than mod mavens, so a
given commit always builds against the same bytes; `tools/fetch_libs.py` is the single source of
truth for which versions those are (it downloads them from Modrinth, verifies the checksums, and
extracts the nested API jars the addons ship). `libs/*.jar` and `run/` are git-ignored;
`python tools/fetch_libs.py --libs-only` is all a build needs and is what CI runs.

The dev client leaves ExtendedAE, Applied Flux and Glodium in `run/mods_disabled` because of the
creative-tab crash described above; the game-test server still loads them.

### Tools

| Script | Purpose |
|---|---|
| `tools/fetch_libs.py` | Dependency jars (see above). |
| `tools/gen_lang.py` | Writes the six non-English lang files and checks every locale against `en_us.json`. |
| `tools/gui_sheet_extras.py` | Re-applies the code-drawn sprites (icons, button backgrounds) to the hand-edited GUI sheet; run after every edit of `textures/gui/pattern_converter.png`. |
| `tools/gen_block_textures.py` | Animated block textures; never overwrites existing files without `--force`. |
| `tools/gen_gametest_template.py` | The empty structure template the game tests run in. |

## Versions

| Repository | Minecraft | NeoForge | Pack |
|---|---|---|---|
| [AE2-RS2-Pattern-Converter](https://github.com/iA7MD-7/AE2-RS2-Pattern-Converter) (branch `1.21.1`) | 1.21.1 | 21.1.194+ | All the Mods 10 |
| [AE2-RS2-Pattern-Converter-26.1](https://github.com/iA7MD-7/AE2-RS2-Pattern-Converter-26.1) (branch `26.1`) | 26.1.2 | 26.1.2.109+ | All the Mods 11 |

Same mod, same features (minus addons that do not exist on 26.1 yet); fixes are ported between them.

## Contributing

Bug reports and feature requests go to the [issue tracker](https://github.com/iA7MD-7/AE2-RS2-Pattern-Converter-FB-Bug-Reports/issues) — the bug button beside the
GUI opens the same page. Pull requests are welcome on either repository; see
[CONTRIBUTING.md](CONTRIBUTING.md) for the setup and the ground rules.

## License

[MIT](LICENSE). Refined Storage, Applied Energistics 2 and the supported addons are separate
projects under their own licenses; this mod compiles against their public APIs and does not
redistribute them.
