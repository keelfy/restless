# Event ideas, third batch

Raw backlog toward about 500 single events. Numbers continue [event-ideas-2.md](event-ideas-2.md) (which ends at 206).
Same tiers (**L** light, **H** harmful, **D** deadly, telegraphed), the same 1–10 [scale](event-ideas-2.md#scale) and
the same `Hardcore` column (`—`: no real counterpart). The sketch is only a likely approach.

## Target

About 213 single events exist (the entries in `restless-config/*.json` other than `haunt`, `spirits` and
`scenarios`). Scenarios do not count toward 500: they reuse the vocabulary, they do not grow it. This batch holds 330
ideas, so about 40 can be dropped and the target is still met.

**What counts.** An idea counts as an event only if the player can tell it apart from the others and it gets its own
name in the journal. One mechanic over a parameter list (an effect for N seconds, a mob sound at a distance) is one
event, not twenty. The same mechanic in a clearly different place (desert, End, a village) may count.

| Section | Ideas | Built on |
|---|---|---|
| [Blocks and mechanisms](#blocks-and-mechanisms) | 40 | existing infra |
| [Mobs](#mobs) | 35 | existing infra |
| [Per-player blocks](#per-player-blocks) | 30 | `fake-block` (built: `Swaps`, `Illusions`) |
| [Sounds](#sounds) | 30 | vanilla sounds |
| [Packet lies and the camera](#packet-lies-and-the-camera) | 25 | packets, new `camera` |
| [The other player](#the-other-player) | 25 | `npc` (built: `Figure`, `Walk`) |
| [Hallucinated mobs](#hallucinated-mobs) | 25 | `phantom` (built: `PacketEntity`) |
| [Items and inventory](#items-and-inventory) | 25 | components, slot packets |
| [Nether and End](#nether-and-end) | 25 | a dimension condition |
| [Biomes and weather](#biomes-and-weather) | 25 | a biome condition |
| [Custom content](#custom-content) | 30 | new assets |
| [The mod's own things](#the-mods-own-things) | 15 | existing mod blocks and items |

## New shared pieces

| Tag | What | Difficulty |
|---|---|---|
| `camera` | `ClientboundSetCameraPacket` to one player: the view jumps to another entity (a real mob, or a `phantom` with a free id), then back to the player. Vanilla applies the creeper, spider and enderman post shaders by itself. Needs a check that movement and chunk loading stay sane while the view is away, and a forced reset on death, logout and dimension change. | 3 |
| `where` | Biome and dimension conditions on an event (a tag list in its config), so the director only rolls it there. Also needed by scenarios ("Where beyond the first beat"). | 2 |
| `decal` | Thin wall and glass overlays as `twin-block`s (handprints, frost, claw marks), placed on air faces next to a block and reverted. Shares the model approach of `muddy_footprints`. | 3 + art |
| `hud` | One HUD layer for full-screen textures with a fade curve (handprint, frost, face), driven by a payload like `DimPayload` and drawn through `EdgeFade`. | 3 + art |
| `template` | Small structure templates (`.nbt`) placed by `Place` and removed by `Restore.revert` block by block. | 3 |

## Blocks and mechanisms

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 207 | L | The enchanting table's book turns to an empty spot | The client turns the book toward the nearest player, so it needs an invisible packet player there; add enchanting glyph particles flowing to that spot. | — | 4 |
| 208 | L | A potion in a brewing stand changes type | Swap `potion_contents` in one slot; `revert` if the slot is untouched. | stays | 2 |
| 209 | L | An empty brewing stand bubbles | Brewing sound and particles at a stand with no fuel. | — | 1 |
| 210 | L | An anvil lands at night | Level event for the anvil land sound at a real anvil. | the anvil gets one damage stage | 1 |
| 211 | L | Someone ate the cake | `bites` +1 and an eating sound; `revert` puts the slice back. | stays | 1 |
| 212 | L | A composter empties or fills | Set `level`; `revert`. | stays | 1 |
| 213 | L | A skull pattern appears on a banner | Append to `banner_patterns` on the block entity; `revert`. | stays | 2 |
| 214 | H | A beacon beam goes out | `fake-block` an opaque block above the beacon for one player (a black stained glass instead turns the beam black). | a real block above until dawn | 2 |
| 215 | H | "Your home bed was missing or obstructed" | The vanilla message on its own, spawn untouched. | the respawn point really resets | 1 |
| 216 | L | A crafter crafts by itself | Its crafting animation and sound, then ejects a bone, a skull or a paper named after the player from its face. | consumes its real contents | 3 |
| 217 | L | A piston in a wall twitches | Block events extend then retract, only with air in front (23 looked wrong because it pushed). | — | 2 |
| 218 | H | A hopper swallows a dropped item | A dropped item near a hopper slides into it. | same | 2 |
| 219 | L | A tripwire hook clicks with no string | Click sound at a real hook. | — | 1 |
| 220 | L | TNT hisses where TNT is stored | Primed sound at a chest or block of TNT, nothing primed. | — | 1 |
| 221 | L | A daylight detector inverts, lamps light at noon | Toggle `inverted`; `revert`. | stays | 1 |
| 222 | L | A copper golem statue changes pose while unwatched | The statue's pose property, changed only out of view (the `ShadowEvent` check); `revert`. | stays | 2 |
| 223 | L | A copper golem puts an item into the wrong chest | A live golem takes a stack from its copper chest and drops it into another chest nearby. | same | 3 |
| 224 | L | A lightning rod sparks with no storm | Set `powered` briefly and spawn electric spark particles. | — | 1 |
| 225 | L | Sculk sensors trigger one by one toward the player | Activate real sensors in order of distance, farthest first. | — | 2 |
| 226 | L | Note blocks play the spirit's tune | A fixed melody on real note block pitches near the player; the tune is stored per spirit. | — | 2 |
| 227 | L | A campfire cooks something nobody put there | Rotten flesh or a rabbit foot in a slot; `revert` removes it. | stays | 2 |
| 228 | L | The jukebox goes quiet and its disc turns up in a chest | Take the playing disc, put it into a random chest nearby. | same | 3 |
| 229 | L | A spawner shows another creature inside | Block entity data packet with fake spawn data for one player (a villager, the player's double); the real spawner is unchanged. | — | 2 |
| 230 | L | A trial spawner turns ominous for a moment | `fake-block` with `ominous=true`. | — | 2 |
| 231 | L | A vault shows the player's own head as its reward | Block entity data packet with that display item. | — | 2 |
| 232 | D | A scaffolding column collapses under the player | Creaking sound first, then break the scaffolding below, only if the fall is under 4 blocks. | same | 3 |
| 233 | L | A bed turns black | Swap both halves to the black bed, keep `facing` and `part`; `revert`. | stays | 2 |
| 234 | L | Carpets in the room turn black | Same swap for carpets in the room (`Enclosure`). | stays | 2 |
| 235 | L | Glazed terracotta turns | Rotate `facing` on nearby glazed terracotta; `revert`. | stays | 1 |
| 236 | L | Stairs in the house flip upside down | `half=top` on stairs out of view; `revert`. | stays | 2 |
| 237 | L | An end rod turns to face the player | Set `facing` toward the player; `revert`. | stays | 1 |
| 238 | L | A map on the wall shows another place | Swap the framed map for a map of the anchor area; `revert`. Doubles as a clue. | stays | 3 |
| 239 | L | A water cauldron turns to lava | Swap with a sizzle; `revert`. | stays | 1 |
| 240 | L | A redstone torch burns out | The vanilla burnout smoke and fizz, `lit=false` for a while. | stays out until updated | 1 |
| 241 | H | An observer pulses and runs a contraption | Toggle `powered` a few times. | — | 2 |
| 242 | L | A lectern's book is swapped for one the spirit wrote | Written book signed by the spirit; `revert` returns the original. | stays | 2 |
| 243 | L | Every chest in the room opens at once, then shuts | Open block events on all chests in the `Enclosure`. | — | 1 |
| 244 | L | The ender chest holds a note that is not the player's | Slot packet when the menu opens; gone on any click. | a real note | 2 |
| 245 | L | A dry cauldron fills with water in a closed room | Set the level with drip particles; `revert`. | stays | 1 |
| 246 | L | A decorated pot holds something new | Put a bone or a shard into the pot's slot; `revert`. | stays | 1 |

## Mobs

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 247 | L | A parrot repeats a whisper or the player's hurt sound | Sound bound to the parrot (`ClientboundSoundEntityPacket`). | — | 1 |
| 248 | L | An allay brings a bone | Put a bone in its hand, navigate to the player, drop it. | — | 2 |
| 249 | H | A fox snatches the held item at night | A real fox nearby paths to the player and takes the stack into its mouth slot. | same | 2 |
| 250 | L | An armadillo rolls up near the anchor | Its scared state; a clue to where the anchor is. | — | 1 |
| 251 | L | Sheep in the pen turn black one by one | Set the colour; `revert` to the old one. | stays | 1 |
| 252 | H | A horse throws its rider and bolts toward the anchor | Eject the passenger, navigate away. | — | 2 |
| 253 | L | An axolotl plays dead with no danger near | Its playing-dead flag. | — | 1 |
| 254 | L | The cat's morning gift is something the player lost | After sleep, a cat by the bed drops an item that vanished earlier (or a bone). | — | 2 |
| 255 | L | Chickens all stop and stare up | Fixed look target in the sky for a few seconds. | — | 2 |
| 256 | L | Cows face the anchor at night | Same look lock toward the anchor; a clue. | — | 2 |
| 257 | L | Villagers wake at night and stand at their windows | Wake sleeping villagers, path to a window, look at the player. | — | 3 |
| 258 | L | A villager becomes a nitwit | Set villager data; `revert` restores profession and trades. | the trades are lost | 2 |
| 259 | L | Villagers raise their prices | A minor negative gossip about the player that decays. | major gossip | 1 |
| 260 | L | A wandering trader comes at midnight with strange trades | Spawn a trader with custom offers (a bone for the player's head, essence); gone when unseen or after 2 minutes. | — | 4 |
| 261 | L | An iron golem walks into the house and stands in a corner | Path in, fixed yaw toward the wall. | — | 3 |
| 262 | H | Bees leave the hive and will not go back | Release the bees, block re-entry for a while. | — | 2 |
| 263 | L | Fish in an aquarium all face the player | Look lock on tropical fish. | — | 2 |
| 264 | H | The snow golem melts on a cold day | Damage it to death with drip particles. | same | 1 |
| 265 | L | Frogs croak together, then all go silent | Sounds bound to each frog in a short wave. | — | 1 |
| 266 | L | An ink cloud in still water with no squid | Squid ink particles. | — | 1 |
| 267 | L | Dolphins lead the player toward the anchor | Their treasure-leading goal with the anchor as target. | — | 3 |
| 268 | L | Rabbits stream away from the house | Navigate every rabbit nearby away from the player. | — | 2 |
| 269 | H | A witch laughs out of sight and a splash of darkness hits | Giggle, splash particles, the darkness effect. | a real witch | 2 |
| 270 | L | Hostile mobs turn and walk away from the player | Navigation away, as if something worse were behind them. | — | 2 |
| 271 | L | A creeper next to the player calms down and walks off | Reset its swell, navigate away. | — | 2 |
| 272 | L | Spiders gather on the wall under a window | Path to the wall, hold there. | — | 3 |
| 273 | L | Endermen turn and stare in a circle, not hostile | Look lock, teleport away when looked at; no aggro. | — | 2 |
| 274 | L | A llama spits at empty air | Spit entity in that direction. | — | 1 |
| 275 | L | A goat screams in the mountains at night | Screaming goat sound far away. | — | 1 |
| 276 | L | Bats hang from the ceiling of a dark room | Spawn resting bats under the ceiling; gone at dawn. | — | 2 |
| 277 | L | A sniffer digs up a head | Override the dig result (player head, a bone) near the anchor. | — | 2 |
| 278 | L | A glow squid stops glowing | Set its dark ticks. | — | 1 |
| 279 | H | The tamed cat leaves home and is found at the anchor | Teleport it there, sitting; a clue. | same | 2 |
| 280 | L | A wind burst in the house with no breeze | The wind charge burst level event and a small push on loose items. | — | 1 |
| 281 | H | Every mob within 16 blocks goes silent and still | No AI and no sounds for 10 s, then back. | — | 2 |

## Per-player blocks

All on `fake-block`: one player sees it, the world is unchanged, a click shows the truth.

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 282 | L | A door in the house is bricked up for a moment | The door's two blocks shown as the wall material. | — | 2 |
| 283 | L | The windows look boarded up from outside | Planks over the panes while the player is outside. | real planks with `revert` | 2 |
| 284 | L | Blocks behind the player turn to netherrack and back | Swap only blocks out of view, resend when turned. | — | 3 |
| 285 | L | A staircase leads into a wall | Fill the space above the last step. | — | 2 |
| 286 | L | A pit opens in the yard | Air over solid ground, only 8+ blocks away so the client never walks into it. | — | 2 |
| 287 | L | The base's torches all look unlit | The burnt-out `twin-block` torches sent to one player. | real burnt-out torches | 2 |
| 288 | L | A diamond ore in the cave wall that is only stone | Mining it shows the stone. | the real ore turns to stone when mined | 2 |
| 289 | L | The roof is gone, open sky above | Roof blocks shown as air while inside. | — | 3 |
| 290 | L | A ladder ends halfway up | Upper ladder blocks shown as air. | — | 2 |
| 291 | L | A second door appears in a wall | A door in a plain wall section. | — | 2 |
| 292 | L | Crops look ripe, harvested they were green | Fake max `age`. | — | 2 |
| 293 | L | The pond turns to lava for a moment | Surface water shown as lava for one second. | — | 2 |
| 294 | D | A lava pool looks like water | Only while the player is more than 8 blocks away; resent as lava at 8. Telegraphed by a sizzle. | same | 2 |
| 295 | L | Snow lies only on the player's house in summer | Snow layers on the roof. | — | 2 |
| 296 | L | The house glass turns black | Tinted glass for panes. | — | 1 |
| 297 | L | The wool and carpets turn red | Red variants. | — | 1 |
| 298 | L | A sign shows text only this player sees | Sign block entity data packet. | the text is real | 2 |
| 299 | L | Flowers around the house turn to wither roses | Same swap for flowers. | real wither roses | 1 |
| 300 | L | The path behind the player rearranges | Swap blocks of the walked path out of view. | — | 3 |
| 301 | L | One block type in view flickers to another for a tick | A subliminal block, two packets one tick apart. | — | 2 |
| 302 | L | A gravestone stands in the yard for one player only | The mod's `Gravestone` sent as a fake block. | a real one (185) | 2 |
| 303 | L | A landmark tree near home is gone | The tree shown as air until the player gets near. | — | 3 |
| 304 | L | The nether portal frame looks broken | Some obsidian shown as air. | — | 1 |
| 305 | L | The tunnel the player just dug looks filled in | Track the last mined blocks, show them as stone behind the player. | — | 3 |
| 306 | L | The room ahead is a copy of the room behind | Copy a room volume onto the next one. | — | 6 |
| 307 | L | A lit circle in a dark cave goes dark when entered | Fake `minecraft:light` blocks, removed as the player steps in. | — | 2 |
| 308 | L | Gaps open in the pen fence | Some fence posts shown as air. | real gaps (an animal may escape) | 1 |
| 309 | L | Furniture on the ceiling | Upside-down stairs, slabs and a bed shown under the ceiling. | — | 3 |
| 310 | L | Cobwebs in every corner of the room | Fake cobwebs on air corners. | real cobwebs with `revert` | 2 |
| 311 | L | A trail of redstone dust leads to the player | Fake wire line on the floor. | — | 2 |

## Sounds

All 1 unless noted: one sound at a chosen point, as the existing ones.

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 312 | L | A warden roars far away, a sonic boom | Far point, low volume. | — | 1 |
| 313 | L | The ender dragon growls in the overworld | Dragon growl from the sky. | — | 1 |
| 314 | L | A wither spawns somewhere | The global wither spawn sound. | — | 1 |
| 315 | L | An end portal opens somewhere | Level event 1038. | — | 1 |
| 316 | L | A ghast screams over the overworld | Ghast scream from above. | — | 1 |
| 317 | L | A fox screeches at night | Fox screech in the woods. | — | 1 |
| 318 | L | A rabbit's death cry in the grass | Rabbit death near. | — | 1 |
| 319 | L | The player's own hurt sound, no damage | Hurt sound at the player. | — | 1 |
| 320 | L | A tool breaks, the tool is whole | Item break sound at the player. | the tool loses durability | 1 |
| 321 | L | Elytra wind passes overhead | Elytra loop moving overhead. | — | 2 |
| 322 | L | Nether ambience in the overworld | Soul sand valley or basalt delta mood for 10 s. | — | 1 |
| 323 | L | Someone puts on armor behind the player | Equip sounds in a row. | — | 1 |
| 324 | L | A fishing splash and reel where there is no water | Bobber splash and retrieve. | — | 1 |
| 325 | L | A zombie breaks at the door | Wood door attack sounds at the player's door. | — | 1 |
| 326 | L | A respawn anchor charges or depletes | Anchor sounds. | — | 1 |
| 327 | L | A bucket of water emptied in the house | Bucket empty sound inside. | — | 1 |
| 328 | L | A villager's "hmm" far from any village | Villager ambient behind the player. | — | 1 |
| 329 | L | An arrow hits the wall next to the player | Arrow hit sound at the wall. | a real arrow stuck in the wall | 1 |
| 330 | L | A window breaks, the glass is whole | Glass break at a real pane. | — | 1 |
| 331 | L | Death cries from the pen, animals fine | Pig, cow or sheep death at the pen. | — | 1 |
| 332 | L | Sniffing right behind the player | Sniffer sniff. | — | 1 |
| 333 | L | A goat horn calls far away | Horn sound from the distance. | — | 1 |
| 334 | L | A raid horn, no raid | Raid horn. | — | 1 |
| 335 | L | Something big falls into the water nearby | Heavy splash at nearby water. | — | 1 |
| 336 | L | A ravager roars far away | Ravager roar. | — | 1 |
| 337 | L | Heavy iron golem steps around the house | Step sounds on a moving point circling the house. | — | 2 |
| 338 | L | A burp behind the player | Player burp sound. | — | 1 |
| 339 | L | Bubble columns rise from a dry floor | Bubble column sounds. | — | 1 |
| 340 | L | Lava pops near the player, no lava | Lava pop and ambient. | — | 1 |
| 341 | L | Chains rattle through the house | Chain step sounds on a moving point. | — | 2 |

## Packet lies and the camera

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 342 | L | A carved pumpkin on the head (the blur overlay) | Slot packet for the head slot. | a real pumpkin if the slot is empty | 1 |
| 343 | L | The experience bar drops to zero | `ClientboundSetExperiencePacket`, truth resent. | — | 1 |
| 344 | L | The player is on fire on screen, no burning | The on-fire flag in the player's own entity data. | a real one-second fire | 1 |
| 345 | L | Air bubbles show and drain on land | Air supply in the player's entity data. | — | 1 |
| 346 | L | Frost creeps over the screen, no freezing | Frozen ticks in entity data. | real freezing | 1 |
| 347 | L | Through the eyes of a mob | `camera` to a nearby spider, creeper or enderman for 2–3 s; the vanilla shader comes with it. | — | 3 |
| 348 | L | Out of body: the player sees themselves from behind | `camera` to a `phantom` behind the player. | — | 3 |
| 349 | L | One second at the spirit's spot | `camera` to a `phantom` at the anchor; the player sees where it is. A clue. | — | 3 |
| 350 | L | The moon changes phase | Time packet with another day, real time resent. | — | 1 |
| 351 | L | The world border closes in on screen | Per-player border shrinking around the player, reset after. | — | 2 |
| 352 | L | A boss bar with the spirit's name, no boss | `ClientboundBossEventPacket`, fills and empties. | — | 1 |
| 353 | L | Action bar: "Don't turn around" | Action bar text, a few ticks. | — | 1 |
| 354 | L | "???" repeats the player's last chat line | Hook on the player's chat, echo later as a system message. | — | 2 |
| 355 | L | "New recipe unlocked": a bone and skull recipe | A hidden recipe in the mod's data, added then removed. | — | 2 |
| 356 | L | Golden hearts appear and drain | Absorption in the player's entity data. | — | 1 |
| 357 | L | The armor bar reads empty | Attribute packet with armor 0. | — | 1 |
| 358 | L | The world narrows (FOV pinch) | Movement speed attribute lie for one second. | — | 2 |
| 359 | L | The hurt tilt with no damage | `ClientboundHurtAnimationPacket` to the player. | — | 1 |
| 360 | L | The player's own hand swings by itself | Animate packet on the player's own entity. | — | 1 |
| 361 | L | The eyes adjust: a flash of night vision, then darkness | Effect packets only. | — | 1 |
| 362 | L | Dead silence | Stop every sound category at once. | — | 1 |
| 363 | L | Every item is on cooldown for two seconds | `ClientboundCooldownPacket` per held item. | — | 1 |
| 364 | L | The portal swirl with no portal | `fake-block` portal at the player's feet for a moment. | — | 2 |
| 365 | L | The sky is lit by a full moon at noon | Time packet to midnight with a full moon, sun resent. | — | 1 |
| 366 | L | The camera lingers on a door left open | `camera` to a `phantom` in the doorway, looking in. | — | 3 |

## The other player

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 367 | L | A figure fishes at the nearest lake | `npc` with a rod and a `phantom` bobber; gone when approached. | — | 3 |
| 368 | L | Someone stands in the player's window, inside | `npc` inside the house facing out, while the player is outside. | — | 3 |
| 369 | L | A figure sits on the roof | `npc` sitting pose on the roof. | — | 2 |
| 370 | L | Someone follows 30 blocks back and stops when the player stops | `Walk` mirroring the player. | — | 3 |
| 371 | L | A figure waves from a hilltop | Arm swing loop. | — | 2 |
| 372 | L | Someone mines toward the player through the wall | Crack progress on the wall from the other side, mining sounds. | the wall block breaks | 2 |
| 373 | L | A figure stands at the player's crafting table | Looking down at it, gone when approached. | — | 2 |
| 374 | L | Torches the player never placed, deeper in the cave | Real torches along a tunnel; `revert`. | stay | 2 |
| 375 | L | An abandoned camp: bed, chest with a book, a campfire | `template`; the book in the spirit's voice. | stays | 4 |
| 376 | L | A figure swims past underwater | `npc` in the swimming pose. | — | 3 |
| 377 | L | A figure in the player's own skin walks far away | Like 120 with the player's profile. | — | 2 |
| 378 | L | A figure drops an item and runs | A note or a torch that can be picked up. | — | 3 |
| 379 | L | A figure sprints across the path in the dark | Fast `Walk` across the view. | — | 2 |
| 380 | L | Six figures on the horizon, all facing the player | Several `npc`s; all gone at once on a blink. | — | 3 |
| 381 | L | Someone built a dirt hut next to the house overnight | Real blocks; `revert`. | stays | 4 |
| 382 | L | A figure sneaks up behind with a sword | Sneak pose; freezes when looked at, gone. | — | 3 |
| 383 | L | A figure rides a boat across the lake | `phantom` boat with an `npc` passenger. | — | 4 |
| 384 | L | Someone sleeps in a village bed | `npc` in the sleeping pose. | — | 2 |
| 385 | L | A figure crouches by a shrieker in the deep dark | `npc` sneaking, gone when the shrieker shrieks. | — | 2 |
| 386 | L | A figure falls from a cliff, a death cry, no body | `npc` walked off a ledge, removed mid-fall. | — | 3 |
| 387 | L | "<Stranger> hello?" in chat | System message styled as chat from a made-up name. | — | 1 |
| 388 | L | "Teleported ??? to <player>" | Fake command feedback. | — | 1 |
| 389 | L | A figure stands where the player last died | The player's last death location. | — | 2 |
| 390 | L | A figure next to the bed on waking | `npc` by the bed for a second after sleep. | — | 2 |
| 391 | L | "<Stranger> has made the advancement [Who's There?]" | Fake advancement chat line. | — | 1 |

## Hallucinated mobs

All on `phantom`: seen by one player, nothing on the server.

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 392 | L | A creeper right behind the player when they turn | Spawned behind, removed after a few ticks in view. | a real creeper, telegraphed | 2 |
| 393 | L | A skeleton on the roof draws its bow, no arrow | Aiming pose. | — | 2 |
| 394 | L | A drowned rises out of the pond and sinks | Moved up, then down. | — | 2 |
| 395 | L | A riderless horse gallops past | Moved along a line. | — | 2 |
| 396 | L | A cat sits on the bed facing the wall | Fixed pose. | — | 2 |
| 397 | L | A villager child runs through the house | Fast path through rooms. | — | 2 |
| 398 | L | An enderman stands in the doorway | Gone when looked at, with the teleport sound. | — | 2 |
| 399 | L | Wolves watch from the forest edge | Several, all facing the player. | — | 3 |
| 400 | L | A spider on the room's ceiling | Upside down under the ceiling. | — | 2 |
| 401 | L | A swarm of bats flies through the player | No collision, through a wall. | — | 2 |
| 402 | L | A zombie pressed against the window | Outside a pane, facing in. | — | 2 |
| 403 | L | A ghast drifts over the forest | High path, slow. | — | 2 |
| 404 | L | A guardian beam on the player from the water | Hidden guardian with its target set to the player, so the beam renders. | — | 3 |
| 405 | L | A vex flies through the wall | Path through blocks. | — | 2 |
| 406 | L | A warden emerges far away and digs back down | Emerging and digging poses. | — | 3 |
| 407 | L | A witch in the swamp hut window | Fixed in the window. | — | 2 |
| 408 | L | A shulker peeks out of the house wall | Shulker opening in a wall. | — | 3 |
| 409 | L | A phantom swoops at the player and vanishes on contact | Dive path. | — | 3 |
| 410 | L | An allay carries the player's item into the woods | Item in its hand, path away. | — | 3 |
| 411 | L | A creaking in an ordinary forest, moving only while unwatched | `ShadowEvent` logic on a harmless `phantom`. | — | 3 |
| 412 | L | A whole herd vanishes in a blink | Fake cows in a field, removed during `blinking`. | — | 3 |
| 413 | L | A dead mob lies on the path | Death animation entity event, then gone. | — | 2 |
| 414 | L | Llamas without their trader | A small caravan crossing the view. | — | 2 |
| 415 | L | A huge slime bounces behind a hill | A big slime size. | — | 2 |
| 416 | L | A minecart with a figure rolls through the cave | `phantom` cart with an `npc` passenger. | — | 3 |

## Items and inventory

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 417 | L | A figure seen only through the spyglass | Shown while the player is scoping, removed when they stop. | — | 3 |
| 418 | H | The fishing line pulls the player toward the water | While fishing: bobber pulled under, motion toward the water. | — | 2 |
| 419 | L | The catch is the player's own head or a bone | Override the fishing loot in the spirit's area. | — | 2 |
| 420 | H | A thrown ender pearl lands somewhere else | Redirect the pearl to a safe `Place` spot. | — | 3 |
| 421 | L | Elytra durability flickers to almost broken | Slot packet lie. | real wear, only high above ground | 1 |
| 422 | L | A lodestone compass points at the anchor | Change its target; `revert`. A clue. | stays | 2 |
| 423 | L | A book "written by" the player appears in the inventory | A written book signed with the player's name. | — | 2 |
| 424 | H | A stew is eaten, only the bowl is left | Swap the stew for a bowl. | same | 1 |
| 425 | L | A potion in the inventory changes type | Swap `potion_contents`; `revert`. | stays | 2 |
| 426 | H | Arrows vanish from the inventory one by one | Remove into `stash`. | same | 2 |
| 427 | L | A water bucket holds a pufferfish | Swap the bucket. | same | 1 |
| 428 | H | A stack of torches burns down | Shrink the stack. | same | 1 |
| 429 | L | A bundle gains a bone | Add to its contents. | — | 1 |
| 430 | L | The shield blocks a hit from nothing | Shield block sound and entity event. | — | 1 |
| 431 | H | A loaded crossbow fires by itself | Shoot upward from the player. | — | 2 |
| 432 | L | The map in hand shows the anchor's area | Swap the map id; `revert`. | stays | 2 |
| 433 | L | A dropped item is thrown back at the player | Item velocity toward them. | — | 1 |
| 434 | L | An item on the ground slides away when approached | Item motion away. | — | 2 |
| 435 | L | Experience orbs fly away from the player | Orb motion away. | — | 2 |
| 436 | L | A goat horn in the inventory plays by itself | Its instrument sound at the player. | — | 1 |
| 437 | L | Leather armor is dyed black | `dyed_color`; `revert`. | stays | 1 |
| 438 | L | A plain item glints with enchantment, then fades | `enchantment_glint_override`; `revert`. | — | 1 |
| 439 | L | A lore line: "it remembers you" | Lore component; `revert`. | stays | 1 |
| 440 | H | A tool shows full durability, then nearly broken | Slot lie, then the truth. | real wear | 1 |
| 441 | L | A spinning clock | `item_model` set to a custom spinning clock; `revert`. Needs one model. | — | 3 |

## Nether and End

All need `where`.

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 442 | L | Piglins stop and stare together | Look lock. | — | 1 |
| 443 | L | A piglin barters the player's head or a bone | Override the barter result in the spirit's area. | — | 2 |
| 444 | L | A ghast cries out of sight, a far fireball blast | Two sounds. | — | 1 |
| 445 | L | A figure stands on the lava sea | `npc` on lava. | — | 2 |
| 446 | L | A strider shivers on lava | Its cold flag. | — | 1 |
| 447 | L | Soul fire leans toward the player | Soul fire flame particles streaming. | — | 2 |
| 448 | H | A hoglin herd charges and stops a block short | Target, then clear. | — | 3 |
| 449 | L | Wither skulls on fences turn toward the player | Rotate real skulls. | — | 1 |
| 450 | L | A blaze stands still in a corridor, no fire | `phantom` blaze. | — | 2 |
| 451 | L | Magma cubes all jump at once | Jump impulse on all nearby. | — | 2 |
| 452 | L | A respawn anchor charges itself | Charge +1; `revert`. | stays | 1 |
| 453 | L | Weeping vines grow toward the player | Real growth. | stays | 2 |
| 454 | L | The compass holds steady in the Nether, at the anchor | Lodestone target set to the anchor. | — | 2 |
| 455 | L | End: endermen all turn to the player, not hostile | Look lock. | — | 1 |
| 456 | L | End: a city shulker opens toward the player and shuts | Peek. | — | 1 |
| 457 | L | End: an end crystal on an empty pillar | `phantom` crystal. | — | 2 |
| 458 | H | End: a void wind drifts the player toward the edge | Slow motion; telegraphed by sound, never near the edge. | — | 2 |
| 459 | L | End: a gateway beam fires with no one near | The gateway's beam event. | — | 1 |
| 460 | L | End: chorus plants shift | `fake-block`. | — | 2 |
| 461 | L | End: a dragon egg sits where none is | `fake-block` egg. | — | 1 |
| 462 | L | End: ender chests open everywhere | Ender chest sounds around the player. | — | 1 |
| 463 | L | Overworld: a lit nether portal near home that leads nowhere | `fake-block` portal in a frame of obsidian. | — | 2 |
| 464 | H | Soul sand drags and whispers | Extra slowness and a whisper only on soul sand. | — | 1 |
| 465 | H | Piglins act as if the player wears no gold | Make nearby piglins angry for a while. | — | 2 |
| 466 | L | End: an enderman carries a block from the player's home | A block taken from the base's palette. | — | 2 |

## Biomes and weather

All need `where`.

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 467 | L | Desert: a husk in the heat haze | Far `phantom` husk. | — | 2 |
| 468 | L | Desert: sand trickles from the ceiling | Falling dust particles. | — | 1 |
| 469 | L | Desert: a temple plate clicks underfoot | Plate sound only. | — | 1 |
| 470 | L | Swamp: lily pads drift toward the player | Move pads block by block; `revert`. | stays | 2 |
| 471 | L | Swamp: the witch hut cauldron bubbles | Sound and particles. | — | 1 |
| 472 | L | Swamp: fireflies go dark around the player | `fake-block` firefly bushes as plain bushes. | — | 2 |
| 473 | L | Snow: drifts pile against the door overnight | Real snow layers; `revert`. | stay | 2 |
| 474 | H | Snow: a snowball from nowhere | Snowball entity. | — | 1 |
| 475 | H | Snow: powder snow on the path | One block deep; `revert`. | stays | 2 |
| 476 | L | Mountains: an avalanche | Rumble and snow particles. | — | 1 |
| 477 | H | Mountains: wind howls and pushes near a cliff | Sound, slight push. | — | 2 |
| 478 | L | Ocean: a ship's bell from the open sea | Bell far over water. | — | 1 |
| 479 | L | Ocean: lights moving under the water | `fake-block` sea lanterns on a path. | — | 3 |
| 480 | H | Ocean: something grabs the boat | Boat velocity zero, drowned sounds. | — | 2 |
| 481 | L | Jungle: all parrots fly off at once | Flight away. | — | 2 |
| 482 | L | Jungle: vines grow over the windows | Real vines; `revert`. | stay | 2 |
| 483 | L | Mushroom fields: mooshrooms turn brown | Variant swap; `revert`. | stays | 1 |
| 484 | L | Pale garden: eyeblossoms open while unwatched | Swap closed and open; out of view only. | — | 2 |
| 485 | L | Creaking heart sounds in an ordinary forest | Sound. | — | 1 |
| 486 | L | Resin clumps grow on the house walls | Real; `revert`. | stay | 2 |
| 487 | L | Cherry grove: petals fall black | Ash particles in petal motion. | — | 1 |
| 488 | L | Dark forest: the canopy closes above | `fake-block` leaves in gaps. | — | 2 |
| 489 | L | Deep dark: sculk creeps toward the base | Real sculk; `revert`. | stays | 2 |
| 490 | L | Lush caves: glow berries go dark one by one | `berries=false`; `revert`. | stay | 1 |
| 491 | L | Storm: in each flash a figure stands closer | `phantom` shown only on the flash tick of `silent_lightning`. | — | 3 |

## Custom content

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 492 | L | Horror paintings: the spirit's portrait, the house at night | Data-driven painting variants used by `new_painting`. | — | 2 + art |
| 493 | L | A painting that darkens every night | A chain of 4 variants, advanced each night. | — | 3 + art |
| 494 | L | The pet turns pale at night | Data-driven wolf and cat variant; `revert` at dawn. | stays | 2 + art |
| 495 | L | Farm animals turn pale | Cow, pig and chicken variants. | stays | 2 + art |
| 496 | L | The spirit's song plays from a jukebox | Jukebox song + `.ogg`, used by `phantom_jukebox`. | — | 2 + sound |
| 497 | L | The spirit's horn call | Instrument + `.ogg`. | — | 1 + sound |
| 498 | L | The spirit's sigil on banners | Banner pattern + texture. | stays | 2 + art |
| 499 | H | "was taken by <spirit>" | Damage types for harmful events, with death messages. | — | 1 |
| 500 | L | Curse of the Restless: the item whispers when held | Enchantment in data, a tick hook for whispers. | — | 3 |
| 501 | L | A hollow armor trim appears | Trim pattern + texture; `revert`. | stays | 2 + art |
| 502 | L | Frost handprints on the windows, from outside | `decal`. | stay | 4 |
| 503 | L | Frost writing on the glass: the name, "LET ME IN" | `decal` glyph set. | stays | 5 |
| 504 | L | Claw marks inside the door | `decal`. | stay | 4 |
| 505 | L | A doll moves from room to room and turns its head | Block with model and `facing`, moved while unwatched. | — | 4 |
| 506 | L | A music box plays by itself | Block + `.ogg`. | — | 4 |
| 507 | L | A rocking chair rocks | Block with animated model states. | — | 5 |
| 508 | L | The planks in the house rot | Rotten `twin-block` planks, spreading; pale oak as a vanilla stand-in. | stays | 4 |
| 509 | L | A black candle lights itself | Candle block with a soul flame. | — | 3 |
| 510 | L | A cracked mirror on the wall shows a figure | Block with two textures, swapped. | — | 3 |
| 511 | L | Eyes in the dark that blink and go out | Particle texture. | — | 3 |
| 512 | L | A handprint on the screen | `hud`. | — | 3 |
| 513 | L | Frost on the screen edges | `hud`. | — | 3 |
| 514 | L | A face for two ticks (rare, off by default) | `hud`. | — | 3 |
| 515 | L | Black ash falls inside the house | Particle. | — | 2 |
| 516 | L | Children count to ten, then search the house | `.ogg` + steps through rooms. | — | 3 |
| 517 | L | Scratching inside the walls, moving along | `.ogg` on a moving point. | — | 2 |
| 518 | L | Knocking from inside a chest | `.ogg` at a chest. | — | 2 |
| 519 | L | A lullaby hummed in the bedroom | `.ogg`. | — | 2 |
| 520 | L | A shrine built overnight near the house | `template`: candles, skulls, a sign. | stays | 4 |
| 521 | L | A copy of the house far away, a light in its window | Copy the house volume as fake blocks, or real with `revert`. | — | 6 |

Not numbered, a subsystem each (8+): a thing crawling on the ceiling, a faceless villager, a black silhouette with
glowing eyes as the shade and wraith figure.

## The mod's own things

| # | Tier | Idea | Sketch | Hardcore | Diff |
|---|---|---|---|---|---|
| 522 | L | The scarecrow turns toward the house, or stands closer | `facing` or position; `revert`. | stays | 1 |
| 523 | L | The ouija board spells "HI" unasked | The ouija letter scheduler without a question. | — | 2 |
| 524 | L | The spirit box says a word unasked | One word from its pool. | — | 1 |
| 525 | H | A gap in the salt line | Remove one cell, on the anchor's side; a clue. | stays | 2 |
| 526 | L | Ash on the floor forms a circle or the spirit's initial | `ash_line` placed; `revert`. | stays | 3 |
| 527 | L | A spirit candle goes out by itself | Unlit with the candle warning sound. | — | 1 |
| 528 | L | The gravestone reads the player's name | Change its text; `revert`. | stays | 1 |
| 529 | L | A photo shows a figure that was not there | A flag on the photo data and an overlay texture. | — | 4 + art |
| 530 | L | A journal page is torn out until dawn | Hide one entry. | — | 2 |
| 531 | L | The dreamcatcher fails: it swings and the dream comes anyway | Swing, then `dream` despite the ward. | — | 2 |
| 532 | L | The hallowed lantern burns with soul fire when the spirit is near | A state swap; a clue. | — | 2 |
| 533 | H | The door charm falls off | Drop the charm as an item. | same | 1 |
| 534 | H | Holy water turns cloudy, useless until dawn | Component on the bottle. | stays | 1 |
| 535 | L | The mirror shows the spirit's name once | Next use of the mirror shows it. | — | 2 |
| 536 | L | The altar candles light themselves at midnight | Light the candles around the altar stone. | — | 2 |

## Signatures

Five fitting ideas per spirit type, for the `spirit` field and `signature_boost`. The numbers are from this batch.

| Type | Ideas |
|---|---|
| banshee | 318, 331, 386, 519, 226 |
| demon | 220, 294, 340, 445, 452 |
| domovoi | 211, 227, 254, 473, 522 |
| hoarder | 218, 223, 249, 410, 426 |
| lost_one | 285, 303, 367, 370, 375 |
| mara | 233, 257, 384, 390, 505 |
| mimic | 247, 306, 319, 348, 377 |
| nix | 394, 404, 418, 478, 480 |
| phantom | 368, 371, 380, 387, 391 |
| poltergeist | 217, 236, 241, 243, 433 |
| revenant | 279, 382, 389, 413, 499 |
| shade | 287, 307, 472, 490, 511 |
| trickster | 216, 260, 288, 292, 438 |
| wraith | 281, 346, 406, 502, 503 |

## Cheapest first

1. Cost 1 on existing infra: sounds (312–341), packet lies (342–346, 350, 352, 353, 356, 357, 359–363, 365), items
   (421, 424, 427–430, 433, 436–440), and 210, 211, 219–221, 224, 235, 237, 239, 240, 243, 245, 246. About 80 events.
2. `fake-block` and `phantom` are built, so 282–311 and 392–416 cost 1–3 each.
3. `where` (2): unlocks 442–491.
4. `camera` (3): 347–349, 366.
5. Data-driven content (492–501): JSON and textures, no new code paths.
6. `decal`, `hud`, `template`, then the custom blocks (502–510, 520, 521).

Check the director before the count passes about 300: every spirit type needs enough events for its affinities, and
`rarity_memory` and `rarity_boost_max` must still bring rare events back.
