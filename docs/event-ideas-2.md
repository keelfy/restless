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
| `sound-remap` | Client-side sound swap: a payload "sounds of X play as Y for N ticks", applied in the client through a `Platform` hook on the loader's play-sound event (NeoForge `PlaySoundEvent`). Unlocks 206 and similar: footsteps on grass sounding like glass, placing sounds, a cow mooing like a ghast. | 4 |
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
| 205 | L | Someone climbs a ladder or walks up the stairs nearby | `Scheduler` repeating: ladder step sounds moving up or down a real ladder column nearby (a shaft, a tower); for stairs, step sounds of the stair's material rising along a real staircase. No ladder or stairs near: from the spirit's point. | — | 2 |
| 206 | L | Blocks the player breaks sound wrong (stone breaks with the glowstone or glass sound) | The breaker's own break and hit sounds are played by their client, not sent by the server, so this needs `sound-remap`. | — | 4 |

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

## Packet lies

The server tells one client something untrue and resends the truth a moment later. Server-only, no client code, so
most cost 1.

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 153 | L | Health drops for a moment | `ClientboundSetHealthPacket` with lower health, the real value resent after 1–2 s. | real damage, never below one heart | 1 |
| 154 | L | Black wither (or green poison) hearts with no effect | `ClientboundUpdateMobEffectPacket` to the client only, a remove packet later; the server has no effect. | the effect is real, short | 1 |
| 155 | L | The hunger bar empties for a moment | Same health packet, it carries food too. | real hunger loss | 1 |
| 156 | L | The elder guardian face, far from any monument | `ClientboundGameEventPacket` `GUARDIAN_ELDER_EFFECT`. A built-in vanilla jump scare. | plus real mining fatigue, as vanilla gives | 1 |
| 157 | L | Totem of undying animation with no totem | Entity event 35 to that player only; the client shows a default totem when the inventory has none. | — | 1 |
| 158 | L | A subliminal title for 2–3 ticks: "BEHIND YOU", the spirit's nickname | Title packets with zero fade. | — | 1 |
| 159 | L | Night falls at noon, for one player | Time packet with a night time and a frozen cycle, real time resent after. | real jump to night (like 99) | 2 |
| 160 | L | Red vignette at the screen edges | Per-player world border warning distance set huge so the tint shows, reset after. | — | 1 |
| 161 | L | A slot shows an item that is not there (a skull, a bone, the player's own head) | `ClientboundContainerSetSlotPacket`; any click resyncs. | the item really appears (worthless) | 2 |
| 162 | L | The player's own chat line they never wrote: "<name> help me" | System message formatted like chat, sent only to them. | — | 1 |
| 163 | L | Their own death message in chat: "<name> was slain by ???" | Same, only to them. | — | 1 |
| 164 | L | Fake subtitles: "Something breathes behind you" | `sounds` event with a silent file and a custom subtitle. Only seen with subtitles on, which is the point. | — | 2 |
| 165 | L | Advancement toast: "Who are you?", "Not alone" | Hidden advancements in the mod's data, granted then revoked. | — | 2 |
| 166 | H | Something turns the player's head | `ClientboundPlayerRotationPacket` turns the camera toward a figure, a sound or the anchor. | — | 2 |
| 167 | L | Rain or thunder starts for one player, then stops | Game event packets for rain and thunder level. | — | 1 |
| 168 | L | Lightning near the house, visual only | `LightningBolt` with `setVisualOnly(true)`. | real lightning (fire) | 1 |

## Sleep

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 169 | L | "This bed is occupied", nobody in it | Set the bed's `occupied` property for a while; vanilla shows the message itself. | — | 2 |
| 170 | L | "You may not rest now, there are monsters nearby", no monsters | `onUseBlock` on a bed cancels and sends the vanilla message. | — | 2 |
| 171 | H | Insomnia: phantoms come the next night | After waking, the `time_since_rest` stat is set past three days. | — | 1 |
| 172 | H | Sleepwalking: the player wakes up somewhere else | After a slept night, move the player to a safe spot (`Place`) outside, by the door, or toward the anchor. | — | 3 |
| 173 | L | A dream: "You dreamed of ..." on waking | Line in chat and the journal from the spirit's `Story` or one trait. Doubles as a clue. | — | 2 |

## Home and mechanisms

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 174 | L | A lectern turns pages by itself | Set the page, page sound. | — | 1 |
| 175 | L | A decorated pot wobbles, a bell swings | Block events, real animation and sound. | — | 1 |
| 176 | L | Books in a chiseled bookshelf rearrange into a pattern | Change slots; `revert` puts them back. | stays | 2 |
| 177 | H | A dispenser near the player clicks, then shoots an arrow at them | Click as the telegraph, arrow spawned from its face, no ammo used. | uses its real contents | 2 |
| 178 | H | A repeater's delay or a comparator's mode changes silently | Set the property; `revert` sets it back. | stays (breaks the contraption) | 1 |
| 179 | L | An empty minecart rolls along rails toward the player | Give it velocity along the rail every few ticks. | — | 3 |
| 180 | L | A boat drifts away from the shore after the player gets out | A slow push away from land. | — | 2 |
| 181 | H | A leash snaps and the animal is loose | Drop the fence knot. | — | 1 |
| 182 | L | The furnace smelted the wrong thing: iron came out as charcoal or rotten flesh | Swap the output slot; `revert` swaps it back if it was not taken. | stays | 2 |
| 183 | H | A leak: water drips from the ceiling inside the house | Water source under the roof; `revert` removes it. | stays, the player cleans up | 3 |
| 184 | L | Muddy footprints indoors lead to the bed or the chest | `twin-block` floor decals, `revert`. | stay | 4 |
| 185 | L | A gravestone appears by the house with the player's name and tomorrow's date | The existing `Gravestone` block; `revert`. | stays | 3 |
| 186 | L | Gifts on the doorstep: flowers, a bone, raw rabbit, a letter signed by the spirit | Items at the door; the letter is a written book signed with the `Story` nickname. | — (already real) | 2 |

## Mines and caves

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 187 | H | Torches behind the player in a tunnel go out one by one, so the way back is lost | Burnt-out `twin-block` torches (111) or removal, `revert`. | stay out | 3 |
| 188 | H | The tunnel behind the player is sealed with stone | `fake-block`, or real with `revert`. | real, the player digs out | 3 |
| 189 | L | Signs on the cave walls point deeper or say "go back" | Signs on walls along the tunnel, `revert`. | stay | 3 |
| 190 | L | A line of torches in a dark forest leads somewhere and is gone when approached | `fake-block` torches along a line. | real torches | 3 |
| 191 | H | Stone the player is about to mine is infested: silverfish | Swap stone ahead of the player to its infested variant. | same | 2 |

## Mobs

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 192 | L | A villager shakes its head and will not trade | The unhappy animation (its synced counter), turn away. | — | 2 |
| 193 | L | An iron golem holds out a poppy to empty air | Its flower-offer entity event. | — | 1 |
| 194 | L | A dog sits and growls at the empty space next to the player, will not follow | Sit, look at the spot, growl sound. | — | 1 |
| 195 | L | Farm animals walk to one corner of the pen and face the wall | Navigation to the corner, then fixed yaw. | — | 3 |
| 196 | H | Bees get angry at the player for no reason | Set the hive's bees angry at the player. | — | 1 |
| 197 | H | A goat out of sight rams the player | A real goat nearby given the player as its ram target, or a `phantom` goat plus knockback. | — | 2 |
| 198 | L | An enderman carries a block taken from the player's house | Take a block from the build, give it to an enderman nearby; `revert` puts it back. | the block is gone for good | 3 |
| 199 | L | A sculk shrieker nearby shrieks with no vibration; a warden heartbeat with no warden | Trigger the shrieker; play the heartbeat. | — | 1 |

## Multiplayer

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 200 | L | Another real player looks frozen in place while they walk | Hide the real one and show a `phantom` copy at their last spot; the clean way, filtering their movement packets, needs a packet filter through `Platform`. | — | 5 |
| 201 | L | A friend is in two places at once | `npc` with their skin and nametag far away while they stand next to the player. | — | 3 |
| 202 | L | A whisper "from" a real online player who never sent it | System message styled like `/msg`. Impersonates a real person, so it should be off by default. | — | 1 |

## Structure

| # | Idea | Sketch | Diff |
|---|---|---|---|
| 203 | Scenes: events chained into one scripted sequence (knocks, the door opens, steps on the stairs, a figure at the top) | A `scene` piece on top of `Scheduler`: a list of events with delays and conditions (the player is still near, has not looked). Chosen as one event by the Director. | 5 |
| 204 | The spirit writes in the journal in the first person and its `Story` leaks through | New journal entries; tone from type and traits. | 2 |

## Cheapest first

1. Packet lies (153–168), sounds (131, 132, 136, 138, 139, 140) and 110: 1 each, no new infra.
2. Hardcore seam (`Restore.revert` and the setting): about 3, and every later block event gets its hardcore branch for free.
3. `npc` (119–124, 148): unlocks the whole "other player" group.
4. `fake-block` and `phantom` (105–107, 126, 127): unlock most visual tricks without client code.
5. Last: 125 (disguise, client or packet filter), 108 (moving trees), 149 (water spirits).
