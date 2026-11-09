# Restless — agent instructions

## Mandatory workflow

- **Caveman**: use the `caveman` skill for every reply. Terse, answer first, keep every technical fact.
- **Lean build**: use the `lean-build` skill for every feature or change. Narrow complete outcome, reuse existing
  seams, no speculative modes/config/extensibility, run focused proof, stop when acceptance passes.
- **Subagents**: complex tasks may be delegated to subagents. Every subagent prompt must tell it to use `caveman`
  (and `lean-build` for changes).
- **Wiki**: keep the player wiki (`apps/frontend/content/{en,ru}`) current. Every code change that alters player-visible
  behavior (events, items, spirits, mechanics, numbers) updates the matching wiki pages in both languages, including
  counts in index pages, in the same change.
- **Images**: image generation (item art, textures, icons) is delegated to the `chatgpt-6.1-sol` harness, never drawn
  by hand or by other tools.

## What the mod is

Horror mod for Minecraft 26.3 (NeoForge; core is loader-free). Spirits are born around players and places (personal
and territorial). Each spirit has a procedural type, traits and story, and rolls random haunt events: scares first,
real harm as it ages, gains power and gets angry. Player loop: notice → investigate (clues, journal, divination) →
protect (wards, salt, offerings) or banish (Requiem ritual) → rewards (essences, relics, charms) → face stronger
spirits. Design: [docs/design.md](docs/design.md), spirits: [docs/spirits.md](docs/spirits.md).

## Client and server

The mod is required on both the client and the server. Never add fallbacks, guards or `canReceive` checks for a
player without the mod; custom entities, payloads, particles and sounds may be used freely.

## Events

**Index of every event id → defining class: [docs/events-index.md](docs/events-index.md).** Read it before touching,
adding or searching for an event. Keep it in sync when events are added, moved or removed.

## Layout

| Path | What |
|---|---|
| `apps/mod/core` | Mod logic, vanilla Minecraft only. Loader calls go through `dev.keelfy.restless.platform.Platform` |
| `apps/mod/core/.../haunt` | Spirits' haunting: director, events, scenarios, requiem, journal, wards |
| `apps/mod/core/.../spirit` | Spirit types, traits, story |
| `apps/mod/core/src/main/resources/restless-config` | Bundled tunables (one JSON per group) + lang |
| `apps/mod/neoforge` | NeoForge adapter and jar assembly |
| `apps/frontend` | Player wiki (Nextra), `content/en` and `content/ru` — update both |
| `docs/` | Developer design notes and idea backlogs |

## Commands

`mise run mod:build`, `mise run mod:client`, `mise run wiki:build`. Game tests: `./gradlew :neoforge:runGameTestServer`
from `apps/mod`. See [README.md](README.md).
