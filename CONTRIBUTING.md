# Contributing

Bug reports and feature requests belong in the
[issue tracker](https://github.com/iA7MD-7/AE2-RS2-Pattern-Converter-FB-Bug-Reports/issues).

## Pull requests

- Two repositories: [AE2-RS2-Pattern-Converter](https://github.com/iA7MD-7/AE2-RS2-Pattern-Converter) (Minecraft 1.21.1 / NeoForge 21.1) and
  [AE2-RS2-Pattern-Converter-26.1](https://github.com/iA7MD-7/AE2-RS2-Pattern-Converter-26.1) (Minecraft 26.1.2 / NeoForge 26.1). Open the PR against
  the one you tested on; if a fix applies to both, say so and it will be ported.
- Set up with `python tools/fetch_libs.py`, then `gradlew build`. `gradlew runClient` gives you a
  client with the runtime mods staged; `gradlew runGameTestServer` runs the end-to-end conversion
  tests headlessly and must stay green.
- Keep `api/` and `convert/` free of NeoForge-specific classes; loader plumbing lives in
  `registry/`, `block/`, `menu/`, `client/` and `compat/`.
- New user-facing text goes into `en_us.json` **and** `tools/gen_lang.py` (which writes the other
  locales and checks that every locale has every key).
- After editing `textures/gui/pattern_converter.png`, run `python tools/gui_sheet_extras.py`.
