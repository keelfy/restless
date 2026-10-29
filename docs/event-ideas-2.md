# Event ideas, second batch

Raw backlog. Numbers continue [event-ideas.md](event-ideas.md) (which ends at 102) so the two never collide.
The sketch is only a likely approach; the exact implementation is decided when an idea is picked up.
The `Hardcore` column is what the idea does under [hardcore mode](hardcore.md); `—` means it has no real counterpart
(a sound, a pure presence) and behaves the same.

Tiers as before: **L** light, **H** harmful, **D** deadly (telegraphed). Tags in `code` are the shared pieces below.

## Scale

| Score | Meaning |
|---|---|
| 1 | One call in the style of the current events, existing infra only |
| 2–3 | A new event on existing infra with some logic (search, schedule, restore) |
| 4–5 | Needs a new shared piece (below) or a multi-step mechanic; new blocks with art |
| 6–7 | Several systems at once, or client render code |
| 8–10 | A new subsystem, a mixin into rendering, or an unclear technical path |

A score includes the event's share of a shared piece only the first time; later events on the same piece are cheaper.

## Shared pieces (built once)

| Tag | What | Difficulty |
|---|---|---|
| `fake-block` | Per-player block view: send `ClientboundBlockUpdatePacket` to one player, server unchanged; resend the real state when due. A click on it shows the real block (acceptable "glitch"). Server-only. | 3 |
| `phantom` | Packet-only entity: `ClientboundAddEntityPacket` with a free id to one player, move and remove packets, no entity on the server. Attacks on an unknown id are already ignored by the server. Server-only. | 4 |
| `npc` | "Another player": the vanilla `mannequin` with a profile, already used by `DoubleEvent` and moved by `Walk`. Add a pool of default skins (Steve, Alex and the other defaults), arm swing (`ClientboundAnimatePacket`), place and break actions. | 3 |
| `sounds` | Custom sound events: `assets/restless/sounds.json` and `.ogg` files in `core`. Sources must be CC0 (freesound.org filter) or CC-BY with credit in the README. | 2 + asset hunt |
| `twin-block` | Custom look-alike blocks swapped in by `Restore`: burnt-out torch and lantern, static stonecutter, walk-through log and leaves. Each family needs models and textures. | 3 per family |
| `menu` | Read-only menus: a `CraftingMenu` / furnace menu subclass whose clicks, shift-clicks and `removed()` do nothing, prefilled from a container nobody owns. | 3 |
| `stash` | Items a spirit holds: `ItemStack` list in the spirit's saved data (codec with registries), given back by the banishing ritual. | 3 |

## Blocks and world

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 103 | H | Chest, crafting table, furnace or bed moves | `Place` finds a free spot nearby; move the state and the block entity through its save format (as the furnace fuel event does); a bed is two halves. `Restore.revert` moves it back if both spots are unchanged. | stays moved | 5 |
| 104 | L | Block substitution by a mapping (planks → stripped log of that wood, stone bricks → stone, glass colour cycle) | Extend `Variants` with a data-driven table in `spirits.json`; full blocks first, stairs and slabs later (copy properties). `Restore.swap`. | stays | 3 |
| 105 | L | Glass nearby becomes air, fences, or the block around it | Scan a radius for glass; per pane pick air, a fence (state from `getStateForPlacement` so it connects) or the most common neighbour. `fake-block` keeps it per player and harmless. | real swap | 3 |
| 106 | L | Texture swaps: crafting table looks like a furnace, furnace like a chest | `fake-block` with a look-alike mapping, resent after N ticks. | real swap via the mapping; blocks with inventory only if contents can move | 4 |
| 107 | L | Fake tree, walk-through | `twin-block` log and leaves with no collision. Sent through `fake-block` to one player (client collision follows the block it was told about, so it stays walk-through), or placed for real with `revert`. | the ghost tree stays (still walk-through) | 4 |
| 108 | L | A tree moves a few blocks | Flood-fill logs and leaves (cap the size), check the target volume and ground, move block by block keeping offsets so leaves do not decay. `revert` moves it back. | stays | 6 |
| 109 | L | A block breaks and comes back, once or several times | `Scheduler`: set air with level event 2001 (particles and sound), put the state and block entity data back; no drops. | the last break stays (no drop) | 3 |
| 110 | L | A growing plant loses all growth | Set `age` to 0 on crops, stems, berries, cocoa; event 73 already withers one stage permanently. | same | 1 |
| 111 | L | A torch or lantern goes out without breaking | `twin-block` burnt-out torch, wall torch, lantern, soul variants; redstone lamp set unlit with no neighbour update. | stays out | 4 |
| 112 | L | Torch type swaps (torch ↔ soul ↔ copper), same for lanterns | Map types, keep `facing` / `hanging`. `Restore.swap`. | stays | 2 |
| 113 | L | A block's animation stops (the stonecutter blade freezes) | Animated textures are global, so per block it is a `twin-block` with a static texture swapped in, keeping `facing`. A generic client-side approach would be 8. | stays frozen | 4 |
| 114 | H | A nether portal goes out | Flood-find the portal blocks the player is near, set air; the frame stays and can be relit. Only on the side the player is not trapped on, or only if they carry flint and steel. | same | 2 |
| 115 | L | A trail of items leads to a sign or object | Line along surface heights from the player to a spot; item entities that cannot be picked up; at the end a sign (story or journal text), a grave or a chest. Cleaned up by `Restore.after`. | items can be picked up and the sign stays | 5 |
| 116 | H | The player is walled in and gets mining fatigue | Shell of the surrounding material on air positions only, mining fatigue, `revert` removes it. Telegraphed. | walls stay (the player digs out); timer kept if there is no air (rules) | 3 |
| 117 | H | Explosion damage on opening a chest | The `chest` reaction exists: a short hiss, then `explode` with no block interaction, or damage plus particles. | still no block damage (rules: contents) | 2 |
| 118 | L | A block above seems to break (no sound) | `BlockParticleOption` particles of the ceiling block falling, plus the crack progress `ClientboundBlockDestructionPacket` on it, as if mined from above. | ceiling block breaks for real, no drop, only if it cannot drop the player | 2 |

## The other player (`npc`)

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 119 | L | A fake nametag floats under the player or far away | Text display entity styled like a nameplate (or `phantom`), billboard, cleaned up. | — | 2 |
| 120 | L | Steve or another default skin walks in the distance | `npc` with a default skin, `Walk` along a line 30–60 blocks out, gone when looked at or out of sight (logic of `PresenceFigure`). | — | 3 |
| 121 | L | Steve pillars up on cobblestone | `npc`: jump (motion), place cobble under, arm swing, repeat N times, vanish; pillar removed by `revert`. | pillar stays | 4 |
| 122 | L | Steve harvests wheat | Find ripe crops near the player's farm, `npc` walks to each and breaks it (no drops: "stolen"), maybe replants. Normal mode puts the crop back at its age. | crops stay gone | 5 |
| 123 | L | Steve watches from afar | `npc` on high ground 40–80 blocks out, head turned to the player every tick, vanishes when approached. Close to 94/95. | — | 2 |
| 124 | L | More "other player" events | Fake join or leave message; a fake entry in the tab list (`ClientboundPlayerInfoUpdatePacket`); a fake death message; an `npc` asleep in the player's bed, opening their door or chest; mining sounds "from a player". Each separate. | — | 2–5 |

## Mobs and hallucinations

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 125 | L | Mob disguise (cow looks like a sheep, chicken like a wolf) | Re-adding the entity under another type by packet breaks on mismatched entity data, so it needs either an outgoing packet filter through `Platform` or a client renderer override (mixin). | real conversion (`convertTo`) | 7 |
| 126 | L | Hallucinated mobs (an iron golem standing in a field) | `phantom`: render-only, nothing on the server. | the mob is spawned for real | 5 |
| 127 | L | A dragon or wither flies over, or only its sounds | Sounds alone: 1. Flyover: `phantom` dragon on a path high up, optionally a fake boss bar (`ClientboundBossEventPacket`). | stays visual (rules) | 1 / 5 |
| 128 | H | Armor stands as mannequins: appear, follow, take threatening poses | Real armor stand behind the player; moves only while unobserved (the `ShadowEvent` logic), changes pose from presets; drops nothing when broken. | stays in the world | 4 |
| 129 | L | Cats are afraid to approach a territorial spirit | Every few seconds cats within reach of the anchor get `getNavigation().moveTo(away)` and hiss. Also a clue to where the anchor is. | — | 3 |
| 130 | H | A spirit steals a dropped item and returns it when banished | Item entity near the player removed and kept in the spirit's `stash`; the ritual drops it at the altar. | open: return less or nothing | 4 |
| 131 | L | Mob sounds at different distances | Pick a mob sound and a distance band (2–40); volume from distance. | — | 1 |
| 132 | L | Hostile sounds from passive mobs | Zombie or creeper sound bound to a nearby cow (`ClientboundSoundEntityPacket`, follows the mob). | — | 1 |

## Sounds

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 133 | L | Sustained footsteps nearby (now only one step) | `Scheduler` repeating: step sound of the block under a moving point circling or approaching the player, walking or running pace. | — | 2 |
| 134 | L | Mining sounds outside a mine | Rhythmic stone hits and breaks below the player on the surface, minecart rail sounds, cave ambience. | — | 2 |
| 135 | L | Custom horror sounds: whispers, scraping | `sounds` piece, played like the existing ones. | — | 2 + assets |
| 136 | L | A spider overhead | Spider ambient and steps at +2..3 above the player, only with a solid ceiling. | — | 1 |
| 137 | L | Many blocks breaking (6 glass in a row, 10 stone) | `Scheduler`: N break sounds in quick succession at real blocks of that material nearby. | those blocks actually break, no drops | 2 |
| 138 | H | A block breaks under the feet, with effects | Level event 2001 on the block below, no change. | it breaks for real, only if the drop is one block (D, telegraphed) | 1 |
| 139 | L | A bottle shatters nearby | Level event 2002 (splash potion break, coloured particles). | — | 1 |
| 140 | L | A note block plays something that is not a note | Any sound (mob, scream, whisper) at a real note block nearby, like a head on top would make. | — | 1 |

## Afflictions and assaults

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 141 | H | More harmful events with varied effects and durations | One data-driven event over a config list of (effect, amplifier, duration range, tier): wither, hunger, weakness with slowness, infested, oozing, weaving... | — | 2 |
| 142 | H | More freezing (like the hantu) | Cold zones around an anchor (freeze ticks while inside), water near the player turning to ice (`Restore.swap`), freeze with slowness, breath (97). | ice stays | 2–3 |
| 143 | H | More pushes | Toward water or lava, into a mob, off a ladder or scaffolding, while bridging, a slow drift; motion packets as in 52 and 100. | — | 2 each |

## Client

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 144 | L | Dimming with different durations and speeds (blinking) | `DimPayload` gets fade in, hold, fade out, repeats; `Dimming` follows the curve. | see dimming in [hardcore.md](hardcore.md) | 2 |
| 145 | L | Screen static | A HUD layer like `Dimming` with an animated noise texture, payload with duration and strength; could follow sanity. | — | 3 |
| 146 | L | Tool textures change in the inventory | Server-only: set the `item_model` component on the stack (looks different, works the same), marked with custom data so `revert` finds it again wherever it moved. | the item really becomes what it looked like | 3 |
| 147 | L | A crafting table or furnace opens a dead UI with a strange recipe laid out | `onUseBlock` hook opens a `menu` instead: grid prefilled (bones in a cross, rotten flesh, a head), odd result, furnace arrow stuck. Items vanish on close. | — | 4 |

## Spirit types and behaviour

| # | Idea | Sketch | Diff |
|---|---|---|---|
| 148 | Phantom: likes to show itself, as other players | New type in `spirits.json` with high affinity for a new `PLAYER` tag (119–124) and presence; lang, clues, docs. Needs the `npc` events first. | 2 |
| 149 | Water spirits: keep the player from surfacing, pull toward water, kill squid and fish | New type, territorial on a water body; a new `WATER` tag: pull down while swimming and push down near the surface (motion packets as in 100), pull toward water nearby, squid and fish die with particles, a boat breaks. | 6 |
| 150 | Signature sound events per type (banshee scream) | An event allowed only for one type (catalog filter by type), with a `sounds` asset. | 3 + asset |
| 151 | A spirit that grows calmer instead | A trait or evolution branch where power falls with age after maturity and anger decays; maybe it asks to be banished. | 3 |
| 152 | Twins: two events at once, as if two spirits | A paired spirit: one record, two locations; each cooldown picks two events of different tags. Banishing one could anger the other. | 4 |

## Cheapest first

1. Sounds (131, 132, 136, 138, 139, 140) and 110: 1 each, no new infra.
2. Hardcore seam (`Restore.revert` and the setting): about 3, and every later block event gets its hardcore branch for free.
3. `npc` (119–124, 148): unlocks the whole "other player" group.
4. `fake-block` and `phantom` (105–107, 126, 127): unlock most visual tricks without client code.
5. Last: 125 (disguise, client or packet filter), 108 (moving trees), 149 (water spirits).
