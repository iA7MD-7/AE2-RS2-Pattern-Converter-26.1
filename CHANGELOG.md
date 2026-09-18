# Changelog

## 1.0.0 — 2026-09-18

First release, for Minecraft 1.21.1 (NeoForge 21.1) and Minecraft 26.1.2 (NeoForge 26.1),
each in its own repository.

- Pattern Converter block: 3×3 input grid, 3×3 output grid, blank-pattern supply and return slots.
- Converts crafting, processing (items and fluids), stonecutter and smithing patterns between
  Refined Storage 2 and Applied Energistics 2 in both directions, through each mod's own API.
- Direction detected per pattern, lockable with the arrow button.
- Per-slot status tint and tooltip explaining exactly why a pattern is waiting or refused.
- Addon support: AdvancedAE processing patterns, Expanded AE / ExtendedAE, Refined Types ↔
  Applied Flux (FE) and Ars Énergistique (Source) with a configurable unmappable-resource policy.
- Automation: sided item handler, batch conversion per work cycle, redstone pulse on completion,
  comparator output, lit front face while working.
- JEI and EMI categories (EMI on 1.21.1 only).
- Translations: English, German, French, Spanish, Portuguese (BR), Russian, Simplified Chinese.
