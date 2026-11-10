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
| `apps/mod/core/src/main/resources/restless-config` | Bundled tunables (one JSON per group) + lang |
| `apps/mod/neoforge` | NeoForge adapter and jar assembly |
| `apps/frontend` | Player wiki (Nextra), `content/en` and `content/ru` — update both |
| `docs/` | Developer design notes and idea backlogs |

### Packages (`dev.keelfy.restless`)

Packages follow game features; the registered game objects in `content` are split by kind. Client-only code
(screens, overlays, renderers, particles) sits in a `client` subpackage of its feature. A network payload lives next
to the feature that sends it.

| Package | What |
|---|---|
| `haunt` | Haunt engine: `Director` (rolls events), pacing (`Rarity`, `Recent`, `Startle`, `Reactions`), `Scheduler`, `Restore`, `Faults`, `History`, `Location`, `Hardcore` |
| `haunt.event` | Event contract (`HauntEvent`, `HauntEvents`, `HauntTier`, `EventTag`), `Catalog` and the theme groups |
| `haunt.event.kit` | Helpers shared by events: `SimpleEvent`, `Nearby`, `Place`, `Where`, `Walk`, `Swaps`, `PacketEntity`, `Haze`… |
| `haunt.scenario` | Scenarios: scripts of beats |
| `haunt.harm` | Damage bands and lethal pacing (`Band`, `Bands`, `Menace`) |
| `haunt.recall` | What the world remembers of each player for events: sounds, kills, chat, trail, wakes, used lines |
| `haunt.effect` | Screen and view effects sent to a client (dim, vignette, overlays, head turn), door locks |
| `spirit` | Spirit types, traits, story, clues, focus, form |
| `spirit.life` | Birth, deeds, power, anger, evolution |
| `spirit.land` | What spirits leave on the land: corruption, graves |
| `state` | Saved per-world state (`HauntData`) and server/client scopes (`Session`) |
| `divine` | Investigation tools: candle, compass, divination, sensing, clues |
| `journal` | Journal notes, pages, codex, its payloads; `client` draws the book |
| `photo` | Camera pictures of spirits; `client` grabs and shows them |
| `sanity` | Sanity and its overlay |
| `ward` | Wards, salt enclosures, lasting spells |
| `banish` | Altar ritual and the Requiem (`requiem` holds its movements and candles) |
| `content` | Registration (`Content`); `block`, `entity`, `item` (`charm`, `consumable`), `recipe`, `client` |
| `config`, `command`, `debug`, `platform` | Tunables and lang, `/haunt`, debug menu, loader bridge |

## Commands

`mise run mod:build`, `mise run mod:client`, `mise run wiki:build`. Game tests: `./gradlew :neoforge:runGameTestServer`
from `apps/mod`. See [README.md](README.md).
