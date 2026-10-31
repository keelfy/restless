# Restless

A horror mod for Minecraft 26.3: personal and territorial spirits trigger random events
that grow more dangerous as they age and are angered.

## Structure

| Path | What |
|---|---|
| `apps/mod/core` | Mod logic. Vanilla Minecraft only, no loader API |
| `apps/mod/neoforge` | NeoForge adapter: entry point, `Platform` implementation, final jar assembly |
| `apps/frontend` | Player wiki (Nextra on Next.js, Bun). Pages are Markdown in `apps/frontend/content`; `docs/` is for mod developers |

`core` gets everything loader-dependent through `dev.keelfy.restless.platform.Platform`
(ServiceLoader). A Fabric port = a new `apps/mod/fabric` module with its own `Platform` implementation.

## Usage

Requires [mise](https://mise.jdx.dev): `mise install` sets up Java 25, Node 24, Bun 1.

```sh
mise run mod:build    # build the jar → apps/mod/neoforge/build/libs
mise run mod:client   # dev client
mise run mod:server   # dev server
mise run wiki:dev     # wiki locally
mise run wiki:build   # static wiki build
```

With Java 25, run `./gradlew :neoforge:runGameTestServer` from `apps/mod` to verify altar placement,
dismantling, offerings and salt rings in a dedicated test world. Test code is excluded from the mod jar.

## Credits

Haunt sounds in `assets/restless/sounds/haunt` are CC0 recordings from [OpenGameArt](https://opengameart.org), cut, mixed down to mono and re-encoded:

| Sound | Source | Author |
|---|---|---|
| `scream_*` | [Female high pitched scream SFX](https://opengameart.org/content/female-high-pitched-scream-sfx) | wuxiascrub |
| `moan_*` | [Ghost / monster voice moaning, growling](https://opengameart.org/content/ghost-monster-voice-moaning-growling) | qubodup |
| `breath` | [Ghost breath](https://opengameart.org/content/ghost-breath) | qubodup |
| `laugh` | [Ghostly cackle laugh 1](https://opengameart.org/content/ghostly-cackle-laugh-1) | nocturnalvanguard (AuraVoice) |
| `screech` | [Scary high pitched ghost](https://opengameart.org/content/scary-high-pitched-ghost) | fupi |
| `whisper_*` | [Voices of Madness](https://opengameart.org/content/voices-of-madness) | eugeneloza |
