# Spirits and haunt events

Reference for the current content. Source of truth is the code:
`spirit/Spirits.java`, `haunt/HauntEvents.java`, `haunt/event/*`, `haunt/Danger.java`, `haunt/Director.java`, `haunt/Scheduler.java`.
Update this file when any of them changes.

## How it works

- Each player gets 3 spirits (`Director.DEFAULT_SPIRITS`) on first check, each chosen uniformly at random
  (repeats allowed), and keeps them. The list can grow or shrink, down to 0, via `/haunt spirit add|remove`.
- Every spirit has its own cooldown. When it expires, the Director picks one event for that player using that spirit's
  affinities and schedules the spirit's next one. More spirits means proportionally more events.
  Spectators and creative players get no event, but the cooldown still resets.
- The pick is a weighted random choice among events that can run right now (`canRun`):

  `weight = baseWeight × tierFactor(tier, danger) × Π affinity(spirit, tag)`

  `chance = weight / Σ weights of runnable events`. An event that cannot run is excluded and the rest are renormalised.

### Danger

`danger = 0.6 × min(days / 40, 1) + 0.4 × min(advancements / 40, 1)`, range 0..1.

- `days` – overworld clock days. `advancements` – completed advancements that have a toast (recipe unlocks do not count).
- Tier factor: `LIGHT = 1 − 0.7 × danger`, `HARMFUL = danger`. Danger moves weight from light to harmful events;
  harmful events are impossible at danger 0.

### Cooldown

`minutes = (4 − 2.5 × danger) × (1 + roll) / activity`, `roll` uniform in 0..1 (minimum 1 second).

## Spirits

| Id | Description | Activity | Affinity (missing = ×1) |
|---|---|---|---|
| `poltergeist` | Noisy and physical. Mostly messes with doors and belongings; rarely hurts at first. | 1.3 | doors ×4, items ×3, blocks ×1.5, afflictions ×0.5, assaults ×0.5, sounds ×0.5, presence ×0.5 |
| `shade` | Quiet and oppressive. Snuffs lights, whispers and afflicts, turns violent as danger grows. | 0.8 | lights ×3, sounds ×2.5, afflictions ×2, presence ×2, assaults ×1, doors ×0.3, items ×0.5 |

Affinity is a weight multiplier per event tag. Activity 1.3 means 1.3× as often.

## Events

`Idea` is the number in [event-ideas.md](event-ideas.md).

| Id | Idea | Tier | Tag | Base weight | Runs when | Effect |
|---|---|---|---|---|---|---|
| `extinguish_light` | | LIGHT | LIGHTS | 1.0 | A lit candle or a torch / soul torch (floor or wall) is within 10 blocks | Snuffs a lit candle; a torch is knocked off and drops as an item |
| `door` | | LIGHT | DOORS | 1.0 | A wooden door is within 12 blocks | Opens a closed door or slams an open one |
| `blindness` | | LIGHT | AFFLICTION | 0.6 | Player is not already blind | Blindness for 6 s and a low cave ambient sound |
| `cold_touch` | | HARMFUL | ASSAULT | 1.0 | Player health > 1 heart | 1–2 hearts of magic damage (2–4 HP), never lethal (leaves at least 0.5 heart), freezing sound |
| `candles_out` | 1 | LIGHT | LIGHTS | 0.8 | A lit candle within 10 blocks | Every lit candle within 10 blocks goes out |
| `torch_chain` | 2 | LIGHT | LIGHTS | 0.8 | A torch within 10 | Up to 8 torches knock off one by one, every 0.3 s, nearest to the player last |
| `lamp_off` | 4 | LIGHT | LIGHTS | 0.8 | A lit redstone lamp within 10 | Switches it off for 1 s, back on if still powered |
| `campfire_out` | 6 | LIGHT | LIGHTS | 0.8 | A lit campfire within 10 | Snuffs one campfire |
| `candles_light` | 8 | LIGHT | LIGHTS | 0.8 | An unlit candle within 10 | Lights one candle |
| `new_face` | 9 | LIGHT | LIGHTS | 0.6 | A jack o'lantern within 10 | Turns it to face another way |
| `burning_hand` | 11 | HARMFUL | LIGHTS | 0.6 | Torch or lantern in main hand | The item drops out of the hand |
| `breathing_door` | 12 | LIGHT | DOORS | 0.7 | A wooden door within 12 | Toggles once a second, six times, ending as it began |
| `trapdoor` | 13 | LIGHT | DOORS | 0.7 | A wooden trapdoor within 12 | Toggles it with a slam |
| `fence_gate` | 14 | LIGHT | DOORS | 0.7 | A fence gate within 12 | Toggles it |
| `button` | 15 | LIGHT | DOORS | 0.7 | An unpressed button within 12 | Presses it |
| `lever` | 16 | LIGHT | DOORS | 0.7 | A lever within 12 | Flips it |
| `pressure_plate` | 17 | LIGHT | DOORS | 0.7 | An unpressed pressure plate within 12 | Triggers it for a second |
| `iron_door` | 18 | LIGHT | DOORS | 0.7 | An iron door within 12 | Toggles it, no redstone |
| `open_house` | 20 | HARMFUL | DOORS | 0.7 | Night, and a closed wooden door within 20 | Opens every such door |
| `chest_lid` | 21 | LIGHT | DOORS | 0.7 | A chest or trapped chest within 10 | Lid opens and closes with sound, contents untouched |
| `barrel_opens` | 22 | LIGHT | DOORS | 0.7 | A closed barrel within 10 | Opens for 1.5 s, then closes |
| `footsteps` | 24 | LIGHT | SOUND | 0.4 | Always | A step sound 2 blocks behind the player |
| `creeper_hiss` | 25 | LIGHT | SOUND | 0.4 | Always | Creeper fuse hiss nearby |
| `three_knocks` | 26 | LIGHT | SOUND | 0.4 | Always | Three knocks, on the nearest wooden door within 12 or else from nowhere nearby |
| `crying_child` | 27 | LIGHT | SOUND | 0.4 | Always | High-pitched villager hurt sound |
| `block_breaking` | 28 | LIGHT | SOUND | 0.4 | Always | Stone / wood / glass / gravel break sound nearby |
| `eating_behind_wall` | 29 | LIGHT | SOUND | 0.4 | Always | Eating sound 3 blocks away |
| `enderman_hiss` | 30 | LIGHT | SOUND | 0.4 | Always | Enderman stare sound |
| `chest_opens` | 31 | LIGHT | SOUND | 0.4 | Always | Chest open sound nearby |
| `breathing` | 32 | LIGHT | SOUND | 0.4 | Always | Five breaths 1 block from the player, each louder |
| `distant_bell` | 33 | LIGHT | SOUND | 0.4 | Always | Loud bell from ~40 blocks away |
| `phantom_jukebox` | 34 | LIGHT | SOUND | 0.4 | Always | Music disc 11 from a point nearby, no jukebox |
| `whisper` | 35 | LIGHT | SOUND | 0.4 | Always | Chat line `<???> <player name>`, only to the player |
| `death_cry` | 37 | LIGHT | SOUND | 0.4 | Always | Player death sound nearby |
| `dripping` | 38 | LIGHT | SOUND | 0.4 | Always | Water drip nearby |
| `nausea` | 39 | LIGHT | AFFLICTION | 0.6 | No nausea | Nausea 5 s |
| `heaviness` | 40 | LIGHT | AFFLICTION | 0.6 | No slowness | Slowness II 8 s |
| `fatigue` | 41 | LIGHT | AFFLICTION | 0.6 | No mining fatigue | Mining fatigue II 10 s |
| `hunger_pang` | 42 | LIGHT | AFFLICTION | 0.5 | Food > 3 bars | −3 food points |
| `darkness` | 43 | LIGHT | AFFLICTION | 0.6 | No darkness | Darkness 6 s |
| `shudder` | 44 | LIGHT | AFFLICTION | 0.5 | Always | Small random shove |
| `poison` | 46 | HARMFUL | AFFLICTION | 0.6 | Not poisoned | Poison 5 s (cannot kill) |
| `weakness` | 47 | HARMFUL | AFFLICTION | 0.6 | No weakness | Weakness 30 s |
| `levitation` | 48 | HARMFUL | AFFLICTION | 0.6 | No levitation | Levitation 2 s, then the fall |
| `frostbite` | 49 | HARMFUL | AFFLICTION | 0.6 | Health > 3 hearts, not freezing | Powder-snow freeze for about 3 s |
| `drained` | 50 | HARMFUL | AFFLICTION | 0.6 | Player has XP | Loses 5–14 XP points |
| `bad_luck` | 51 | HARMFUL | AFFLICTION | 0.6 | No bad luck | Bad luck 5 min |
| `trip` | 53 | HARMFUL | ASSAULT | 0.7 | Not slowed | Stops dead for 1 s |
| `scorch` | 54 | HARMFUL | ASSAULT | 0.6 | Health > 3 hearts, dry, not burning | On fire for 1 s |
| `wrath` | 55 | HARMFUL | ASSAULT | 0.6 | Health > 4 hearts | 2–3.5 hearts of magic damage, never lethal |
| `snatch` | 56 | HARMFUL | ASSAULT | 0.7 | Item in main hand | The item is thrown ~5 blocks away |
| `suffocation` | 57 | HARMFUL | ASSAULT | 0.5 | Health > 4 hearts, not in water, light level < 8 | Heartbeat, 2 s later the air bar drains (about 7 s to empty), then 1 HP a second. Stops in light, in water, at 3 hearts, or after 15 s |
| `thunder_knocks` | 58 | HARMFUL | ASSAULT | 0.5 | Health > 5 hearts | Three knocks; 3 s later a bolt strikes nearby; 2 s after that a second bolt hits the player if they have moved less than 2 blocks since (6 HP, never lethal). Bolts set nothing on fire |
| `embrace` | 60 | HARMFUL | ASSAULT | 0.5 | Health > 5 hearts | Growing damage once a second (0.5, 1, 1.5 HP…) until the player is 10 blocks from where it began. Stops at 3 hearts or after 20 hits |
| `swap_slots` | 62 | LIGHT | ITEMS | 0.5 | Inventory not empty | Two inventory slots swap |
| `hotbar_shift` | 63 | LIGHT | ITEMS | 0.5 | Always | Selected hotbar slot changes |
| `strange_name` | 64 | LIGHT | ITEMS | 0.5 | An item without a custom name | Renamed to a creepy line |
| `book_page` | 65 | LIGHT | ITEMS | 0.5 | A book and quill or a written book | One creepy page is appended (nothing is erased) |
| `shuffle_chest` | 66 | LIGHT | ITEMS | 0.5 | A non-empty chest within 10 | Its contents are shuffled |
| `wear` | 67 | HARMFUL | ITEMS | 0.5 | A damageable tool with durability to spare | Loses 10% durability, never breaks |
| `rot` | 68 | HARMFUL | ITEMS | 0.5 | Food in the inventory | One food stack becomes rotten flesh |
| `vanishing_item` | 69 | HARMFUL | ITEMS | 0.5 | Two chests (not halves of one) within 10, one non-empty, one with space | One stack moves to the other chest |
| `false_compass` | 70 | HARMFUL | ITEMS | 0.5 | A plain compass in the inventory | Points at a spot 30–60 blocks away (stays that way) |
| `withering_crops` | 73 | LIGHT | BLOCKS | 0.6 | A grown crop within 6 | Every crop within 6 loses one growth stage |
| `turning_frame` | 76 | LIGHT | BLOCKS | 0.6 | A filled item frame within 12 | Rotates its item |
| `dry_cauldron` | 78 | LIGHT | BLOCKS | 0.6 | A water cauldron within 10 | Empties it |
| `dead_ground` | 79 | LIGHT | BLOCKS | 0.6 | Player on grass | Grass in a 3-block radius becomes dirt |
| `broken_bed` | 81 | HARMFUL | BLOCKS | 0.6 | A bed within 16 | Breaks it; it drops as an item |
| `animals_stare` | 85 | LIGHT | MOBS | 0.6 | Two animals within 16 | They stop and face the player for 3 s |
| `cat_hiss` | 86 | LIGHT | MOBS | 0.6 | A cat within 16 | The cat hisses |
| `dog_barks` | 87 | LIGHT | MOBS | 0.6 | A wolf within 16 | Turns away and makes its ambient sound |
| `villager_stares` | 88 | LIGHT | MOBS | 0.6 | A villager within 16 | One villager stops and faces the player for 5 s |
| `bats` | 89 | LIGHT | MOBS | 0.6 | Free air 4 blocks away | Five bats appear |
| `open_pen` | 90 | HARMFUL | MOBS | 0.6 | A closed gate with an animal within 5 of it | Opens the gate |
| `wild_pet` | 91 | HARMFUL | MOBS | 0.6 | A tamed pet of the player within 32 | Loses its owner and targets the player |
| `copycat` | 92 | HARMFUL | MOBS | 0.6 | Not peaceful, standing room 3 blocks behind | A zombie in a copy of the player's armor, targeting them |
| `footprints` | 96 | LIGHT | PRESENCE | 0.6 | Standing on or in sand or snow | For 6 s, puffs of the ground and a step sound appear where the player stood a second ago. Particles only, nothing is placed |
| `cold_breath` | 97 | LIGHT | PRESENCE | 0.6 | Warm biome | A breath puff at the face |
| `local_rain` | 98 | LIGHT | PRESENCE | 0.6 | Sky visible, not raining | Rain particles and sound over the player for 6 s |
| `lost_time` | 99 | HARMFUL | PRESENCE | 0.5 | Night | The overworld clock jumps 1000 ticks (affects everyone) |

## Chances

Share of each tag per roll of one spirit, assuming every event can run (in practice many cannot, and the rest renormalise,
so a spirit near no doors never rolls a door). Computed from the weights above.

| Spirit | Danger | Lights | Doors | Sound | Affliction | Assault | Items | Blocks | Mobs | Presence |
|---|---|---|---|---|---|---|---|---|---|---|
| `poltergeist` | 0 | 9.4% | 49.1% | 5.7% | 4.7% | 0% | 17.7% | 8.5% | 4.2% | 0.7% |
| | 0.5 | 8.2% | 42.0% | 4.4% | 6.2% | 2.5% | 22.2% | 7.9% | 5.8% | 0.9% |
| | 1 | 6.3% | 31.5% | 2.5% | 8.4% | 6.3% | 28.8% | 6.9% | 8.2% | 1.2% |
| `shade` | 0 | 29.8% | 3.9% | 29.8% | 19.9% | 0% | 3.1% | 6.0% | 4.5% | 3.0% |
| | 0.5 | 25.0% | 3.2% | 22.5% | 25.3% | 5.2% | 3.8% | 5.4% | 6.0% | 3.7% |
| | 1 | 18.4% | 2.3% | 12.3% | 32.8% | 12.3% | 4.7% | 4.5% | 8.0% | 4.6% |

Cooldown per spirit, minutes: `(4 − 2.5 × danger) × (1 + roll) / activity`. Poltergeist: 3.1–6.2 at danger 0, 1.2–2.3 at 1.
Shade: 5.0–10.0 at danger 0, 1.9–3.8 at 1.

## Scheduled events

Multi-step events run through `haunt/Scheduler.java`: `later(player, delay, action)` and
`repeat(player, delay, interval, step)` (the step returns false to stop). Tasks are in memory only and are dropped when
the player dies, leaves or the world closes, so no step may leave something broken behind. Events that hurt
(`suffocation`, `thunder_knocks`, `embrace`) announce themselves with a sound first and stop at 3 hearts.

## Admin command

Permission level: gamemaster. `[player]` defaults to the caller.

- `/haunt info [player]`: danger, days, advancements, and the numbered spirit list with time to each spirit's next event.
- `/haunt force <event> [player]`: runs an event now (fails if it cannot run).
- `/haunt spirit add <spirit> [player]`: binds one more spirit.
- `/haunt spirit remove <n> [player]`: unbinds spirit number `n` (1-based, as listed by `info`).
