# Restless

A horror mod for Minecraft 26.3: personal and territorial spirits trigger random events
that grow more dangerous as they age and are angered.

## Structure

| Path | What |
|---|---|
| `apps/mod/core` | Mod logic. Vanilla Minecraft only, no loader API |
| `apps/mod/neoforge` | NeoForge adapter: entry point, `Platform` implementation, final jar assembly |
| `apps/frontend` | Player wiki (Nextra on Next.js, Bun). Pages are Markdown in `apps/frontend/content/<lang>` (`en`, `ru`); `docs/` is for mod developers |

`core` gets everything loader-dependent through `dev.keelfy.restless.platform.Platform`
(ServiceLoader). A Fabric port = a new `apps/mod/fabric` module with its own `Platform` implementation.

## Usage

Requires [mise](https://mise.jdx.dev): `mise install` sets up Java 25, Node 24, Bun 1, uv.

```sh
mise run mod:build    # build the jar → apps/mod/neoforge/build/libs
mise run mod:client   # dev client
mise run mod:server   # dev server
mise run wiki:dev     # wiki locally
mise run wiki:build   # static wiki build
mise run voice:generate  # re-voice the spirit box after its words or answers change (apps/voice)
```

With Java 25, run `./gradlew :neoforge:runGameTestServer` from `apps/mod` to verify altar placement,
dismantling, offerings and salt rings in a dedicated test world. Test code is excluded from the mod jar.

## Release

The version is `mod_version` in `apps/mod/gradle.properties`; changes are listed in [CHANGELOG.md](CHANGELOG.md).
Pushing a tag `v<mod_version>` (for example `v0.1.0-alpha.1`) makes the `release` job in `.github/workflows/mod.yml`
build the jar and attach it to the workflow run as an artifact. Nothing is published anywhere else.

## Licence

Copyright keelfy. All Rights Reserved (`mod_license` in `apps/mod/gradle.properties`), except the third-party sounds
listed under Credits, which keep their own licences.

## Credits

Haunt sounds in `assets/restless/sounds/haunt` are CC0 recordings from [OpenGameArt](https://opengameart.org), cut, mixed down to mono and re-encoded:

| Sound | Source | Author |
|---|---|---|
| `scream_*` | [Female high pitched scream SFX](https://opengameart.org/content/female-high-pitched-scream-sfx) | wuxiascrub |
| `moan_*` | [Ghost / monster voice moaning, growling](https://opengameart.org/content/ghost-monster-voice-moaning-growling) | qubodup |
| `breath` | [Ghost breath](https://opengameart.org/content/ghost-breath) | qubodup |
| `laugh` | [Ghostly cackle laugh 1](https://opengameart.org/content/ghostly-cackle-laugh-1) | nocturnalvanguard (AuraVoice) |
| `screech` | [Scary high pitched ghost](https://opengameart.org/content/scary-high-pitched-ghost) | fupi |
| `horn_*` | [War Horns](https://opengameart.org/node/153572) | Eldritch Grim |
| `chair_creak_*` | [100 CC0 metal and wood SFX](https://opengameart.org/content/100-cc0-metal-and-wood-sfx) | rubberduck |
| `claw_scratch_*` | [Scrapes](https://opengameart.org/content/scrapes) | AntumDeluge |
| `rustle_*` | [Fabric Rustling](https://opengameart.org/content/fabric-rustling) | Iochi Glaucus |

These haunt sounds are cut from a royalty-free pack supplied by the owner (sources to be listed by the owner), trimmed,
levelled and re-encoded by `apps/voice/haunt_pack.py`: `jolt_*`, `sting_roar_*`, `sting_scream_*`, `sting_violin_*`,
`sting_bass_*`, `sting_blast_*`, `blight_bed`, `sanity_bed`, `ritual_fury`, `evolve`, `offering_refused`,
`ritual_leaving`, `requiem`, `laid_to_rest`, `heartbeat_*`, `chain_rattle_*`, `creature_rattle_*`, `laugh_2`,
`screech_2`.

`music_box_note` and `mirror_ring_*` are generated (additive synthesis in `apps/voice/haunt_pack.py`), not recorded.

Spirit box voices in `assets/restless/sounds/spirit_box` are made by `apps/voice` with [Piper](https://github.com/OHF-Voice/piper1-gpl) voices `en_US-joe-medium` and `ru_RU-dmitri-medium`, both trained on CC0 recordings ([OHF-Voice/voice-datasets](https://github.com/OHF-Voice/voice-datasets)). Piper is GPL and only runs at build time; it is not in the mod.
