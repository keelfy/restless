# Spirits and haunt events

Reference for the current content. Source of truth is the code:
`spirit/Spirits.java`, `haunt/HauntEvents.java`, `haunt/event/*`, `haunt/Power.java`, `haunt/Reactions.java`, `haunt/Anger.java`, `spirit/Traits.java`, `spirit/Story.java`, `haunt/Director.java`, `haunt/Scheduler.java`.
Update this file when any of them changes. The numbers below are the shipped defaults; see [Config](#config).

## How it works

- A spirit is **personal** (follows one player) or **territorial** (stays at an anchor); the kind is `kind` in `spirits.json`.
  `shade` is personal, `poltergeist` is territorial.
- Each player gets 3 personal spirits (`director.default_spirits`) on first check, each chosen uniformly at random
  (repeats allowed), and keeps them. The list can grow or shrink, down to 0, via `/haunt spirit add|remove`. A poltergeist
  in an older save is dropped.
- A territorial spirit has an anchor, a dimension and a radius (`director.territorial_radius`, 48 blocks), saved per world.
  When its cooldown expires it acts on one random player (survival or adventure) inside the radius, and does nothing, but
  still reschedules, if nobody is there.
- Every spirit has a location. A personal spirit roams in a ring `location.min_distance` to `location.max_distance`
  blocks from its player, drifting `location.step` blocks per second, and is forgotten on restart; a territorial spirit
  stays at its anchor. Sound events with no fixed source (creeper hiss, crying child, eating behind a wall, enderman
  hiss, chest opening, distant bell, phantom jukebox, death cry, dripping, block breaking, and the knocks when no door
  is near) come from that point, so a territorial spirit's sounds are only heard near its anchor. Events started by hand
  with `/haunt force` have no spirit and keep a random point around the player. `/haunt info` shows a personal spirit's
  current point as `at=`.
- Every spirit has its own cooldown. When it expires, the Director picks one event for that player using that spirit's
  affinities and schedules the spirit's next one. More spirits means proportionally more events.
  Spectators and creative players get no event, but the cooldown still resets.
- The pick is a weighted random choice among events that can run right now (`canRun`):

  `weight = baseWeight × tierFactor(tier, power) × Π affinity(spirit, tag) × Π affinity(trait, tag)`

  `chance = weight / Σ weights of runnable events`. An event that cannot run is excluded and the rest are renormalised.

### Power

Each spirit has an identity: `uuid`, `seed`, `bornAt` (game time it appeared) and `anger` 0..1. Player actions raise anger (see [Anger](#anger)). Power depends on the spirit alone, never on the world:

`power = min(1, 0.6 × min(age / 30 days, 1) + 0.4 × anger)`, range 0..1.

- A new spirit starts at 0 and reaches 0.6 after 30 in-game days (`power.maturity_days`). Spirits from saves that predate identity
  count as born at tick 0.
- Tier factor: `LIGHT = 1 − 0.7 × power`, `HARMFUL = power`. Power moves weight from light to harmful events;
  harmful events are impossible at power 0.

### Traits

Besides its type, every spirit has 3–5 traits (`director.traits_min`/`traits_max`) drawn from the `traits` catalog in
`spirits.json` by its seed: the same seed gives the same traits, and the ids are saved with the spirit. A save from before
traits gets them from its seed on load. Ids the catalog no longer has are skipped. So two spirits of one type differ in
what they pick and when they act. A trait has any of:

- `affinity`: tag multipliers, multiplied into the weight with the type's. `light_eater` (lights ×2.5), `door_slammer`,
  `restless_hands`, `whisperer`, `cold_touch`, `vengeful`, `meddler`.
- `activity` + `when` (`rain`, `night`, `dark`; light level below `director.dark_light_level`): the spirit acts that many
  times as often while the condition holds at the moment it schedules its next event. `rain_walker`, `night_stalker`,
  `dark_dweller`.
- `noise`: ticks the next event comes sooner for every sound the player makes (`noise_feeder`: 6 per sound, so sprinting
  hastens it most). Sneaking instead delays it by `director.sneak_delay_ticks` per check. Sounds are the ones `Echoes` hears.
- `reacts`: after the player opens a chest or barrel (`chest`), uses a bed (`bed`), goes below y 0 (`deep`) or kills a mob
  (`kill`), the spirit has a `director.react_chance` to act on the next check. The event is still picked and capped as
  usual. `chest_watcher`, `sleep_thief`, `deep_caller`, `grudge`.

Noise and reactions apply to personal spirits only. The debug screen shows each spirit's traits and, for the selected
event, the chance each spirit rolls it next.

### Anger

Anger is saved per spirit and only goes up for now (offerings that lower it come later). The amounts are the `anger` config:

| Source | Amount | Who takes it |
|---|---|---|
| Killing a cat | `kill_cat` 0.15 | all the player's spirits; territorial ones if it happened in their radius |
| Breaking a grave (`graves`: skeleton and wither skeleton skull, bone block) | `break_grave` 0.2 | same |
| Lighting a log or leaves with flint and steel or a fire charge | `burn_forest` 0.1 per ignition | same |
| Breaking a block within `anchor_reach` (3) of a territorial spirit's anchor | `break_anchor` 0.3 | that spirit only |

Anger is capped at 1. A territorial spirit takes nothing from a player outside its radius, and a player's actions never
anger another player's personal spirits. Fire is counted when the player lights it, not when a forest burns down.

### Story

Every spirit was a player once: `Story.of(seed)` gives a nickname, how many days it had been dead when it woke (between
`days_min` and `days_max`) and a cause of death, all drawn from the `story` lists in `spirits.json`. Nothing is saved, the
same seed always tells the same story, and only `/haunt info` shows it for now. The nicknames are built from two word lists (`name_first`, `name_second`) with an optional `_` and number, which gives
over a hundred thousand different ones. They are made up: there is no way to ask Mojang for random real ones, and a name
taken from the server's own players would change the story whenever they do.

### Cooldown

`minutes = (4 − 2.5 × power) × (1 + roll) × quiet / activity`, `roll` uniform in 0..1 (minimum 1 second).
`activity` is the type's activity times every trait's active modifier.

`quiet = 1.2^n`, where `n` counts the true conditions among: it is day, the player sees the sky, there is no thunderstorm.
So a player in the open on a clear day waits up to 1.73× longer; in a mine, a house or at night nothing changes.

## Detection and protection

Everything is crafted (shapeless) and listed in the **Restless** creative tab. Salt: sand and dried kelp (makes 2).

### Clues

A trait lists which of four clues it leaves (`clues` in `spirits.json`), so a spirit leaves the union of its traits'
clues. A tool that reads a clue the spirit does not leave finds nothing. The tools read the player's own spirits and the
territorial ones whose radius holds them.

| Clue | Tool | What it shows |
|---|---|---|
| footprints | **Ash powder** (charcoal, makes 4) | Placed flat like salt. After `ash.reveal_ticks` (30 s), if a spirit that leaves footprints is within `ash.reach` of it, it takes one of four prints (bare foot, paw, hoof, drag), picked by the spirit's seed: the same spirit always leaves the same print. |
| silhouette | **Camera obscura** (iron, glass pane, black dye) | Use it: a **photograph** with the spirit's type and one trait's hint, `camera.cooldown_ticks` apart. |
| voice | **Spirit box** (block: iron, redstone, note block) | Say something in chat within `spirit_box.radius` of it: a moment later it answers with a word of one trait. |
| planchette | **Ouija board** (block: planks, black dye, salt) | Use it: the planchette spells one trait's hint letter by letter on the action bar. |

A spirit with the `liar` trait lies through the spirit box and the ouija board half the time (`clues.lie_chance`): it
names a trait it does not have. A photograph and footprints never lie.

Two tools do not depend on traits: the **spirit candle** (candle, soul sand) and the **spirit compass** (compass,
amethyst shard, soul sand).

- The candle in a hand shows a flame that leans toward the spirit whose event comes first, and burns blue for the last
  `candle.warning_ticks` before it. It reads the timer the director runs, so the warning is never wrong.
- The compass in a hand points at the nearest spirit. It uses the game's lodestone needle: the server writes the spirit's
  point into the compass, so the client has no code of its own. With no spirit in reach it spins.

A personal spirit has no point until the director's first check, so until then the flame stands straight and the compass spins.

### Protection

Checked when a spirit's event is about to run on a player. A ward stops that one event and shows ash and a chime; the
spirit's cooldown still runs, so the event is lost, not postponed.

| Ward | Stops | Wears out |
|---|---|---|
| **Salt line** | Any event on a player standing inside a closed loop of salt. | No |
| **Hallowed lantern** (lantern, salt, glowstone dust) | Any event within `wards.lantern_radius` (6). It gives light. | No |
| **Dreamcatcher** (string, feather, stick, salt) | Any event on a player sleeping within `wards.dreamcatcher_radius` (5). | No |
| **Door charm** (salt, paper, string) | Door events within `wards.charm_radius` (8). | Breaks after 5 |
| **Scarecrow** (hay, carved pumpkin, stick, salt) | Any event within `wards.scarecrow_radius` (16). | Breaks after 10 |
| **Bell** (vanilla) | Ringing it pushes every spirit within `bell.radius` back by `bell.delay_ticks`. | No |

Salt is placed like redstone dust, one flat tile per block, walkable. The loop is checked on the floor level the player
stands on, with walls closing the gaps, through four-way neighbours only (a diagonal line closes), and the shut-in area
may not exceed `salt.max_cells`. Walls alone do not ward a room; at least one salt tile has to border it. The first
ward found decides; the lantern, then the dreamcatcher, the door charm and the scarecrow.

Not built yet: salt from a cauldron, and the traits that hate a ward (the lantern raising anger of light-hating
traits, the bell of noise-hating ones). The salt, the charm and the scarecrow have no wall-hanging form; place them on a floor.

## Banishing

Every trait names a **weakness** (`weakness` in `spirits.json`): iron, glowstone, holy water or a mirror. A spirit is
weak to the one most of its traits name; a tie goes to the first in that order. A photograph shows the weakness of the
trait it names, so a few photos of one spirit tell what it is weak to.

One **altar** item (chiseled stone bricks, salt, candle) places a 3×3 platform centered on the clicked space:
a worn charcoal-stone offering slab, raised tuff paving with recessed joints, a broken chalk circle and four iron
candle stands. The offering rests flat on a red clay inset. Place ordinary candles on top of the corner stands and
light them: they count toward the ritual and spirits can extinguish them. Breaking the altar also releases these
candles. All nine spaces must be clear, dry,
and supported by solid top faces. Breaking any part dismantles the platform and returns one altar plus its held
item; creative mode does not drop the altar. Any part accepts or returns the offering. Existing single-block altars
must be broken and placed again to assemble the platform.
Place the salt outside the platform, for example as the perimeter of a 5×5 square.

The altar needs at least `ritual.candles` (4) lit candles within
`ritual.candle_radius` (3) and a closed salt loop around it, checked like a salt ward on the altar's own level. Using it
with a focus (an iron ingot, glowstone dust, **holy water**, a **mirror** from a glass pane and two iron nuggets) takes the
focus and calls the first spirit near the player that is weak to it:

- The ritual runs `ritual.waves` (12) waves of `ritual.wave_ticks` (5 s). Each wave the spirit may snuff one of the
  candles (`ritual.snuff_chance`) and may act on the player at once, past every ward (`ritual.fury_chance`).
- At the next wave every candle must be lit again, the focus still on the altar and the player within `ritual.radius`
  (16). Otherwise the ritual breaks: the focus is lost and the spirit's anger rises by `ritual.fail_anger`.
- After the last wave the spirit is gone for good and drops its **essence** (`<type>_essence`).
- No spirit near is weak to the focus: one of them eats it, its anger goes to 1 and it strikes at once with a harmful event.

Altars with nothing to answer, or missing candles or salt, refuse the focus. Empty-handed use takes the item back.
**Holy water** is a water bottle left on the altar for `ritual.bless_ticks` (half a day).

| Essence | Artifact | Use |
|---|---|---|
| Poltergeist | **Telekinesis amulet** (essence, gold ingot, string) | Pulls every dropped item within `artifacts.amulet_radius` to you. |
| Shade | **Dark eye** (essence, eye of ender) | Night vision, and every living thing within `artifacts.eye_radius` glows, for `artifacts.eye_ticks`. |

Not built yet: offerings, throwing holy water to end a running event, the essence lure, and the pact enchantment and
mimic mask (there are no spirit types for them yet). A ritual that its player leaves by dying, logging out or a restart simply stops; the focus stays on the altar.

## Unfinished business, sanity, journal

### Graves

Every spirit a player in the overworld can sense gets a **gravestone** `grave.min_distance`..`grave.max_distance`
(100..400) blocks from where that player stood. It is built on the surface once its chunk loads, and built again
(where the ground now is) if it goes; breaking it angers spirits like any grave (`anger.graves`) and drops nothing.

Using the gravestone reads it: who the spirit was (its story) and what of theirs is missing, one item rolled from the
seed out of `grave.wants`. Using it with that item lays the spirit to rest: it is gone for good, with no anger, and leaves
`grave.essence` (2) essences and `grave.experience` XP. A grave whose spirit is gone (banished, removed) stays, quiet.

### Sanity

Each player has sanity 0..1, saved with the world; creative and spectator players keep theirs. Per second it loses
`sanity.dark_loss` below light `dark_light` (4) and `spirit_loss` inside a territorial spirit's radius, and gains
`lit_gain` at light `lit_light` (10) or more, `sleep_gain` while sleeping, `fed_gain` with at least `fed_food` food and
`ward_gain` inside a salt ward. Every event that runs on the player takes `event_loss` at once.

Below `sanity.overlay_below` (0.6) the screen darkens from the edges, up to `overlay_peak` at 0. Below
`sanity.reveal_below` (0.35) the spirit box, after its answer, also tells roughly (to 10 blocks) where the answering
spirit's grave is; nothing else gives the place away.

### Journal

The **journal** (book, salt, ink sac) opens as a book of what its reader has learned; the entries are saved per player
with the world, not on the item, so any journal shows them. Written when a photo names a trait, the spirit box whispers
of a grave, a gravestone is read, a spirit goes to rest and a spirit is banished. Each new entry turns a page aloud.

Not built yet: diary pages and portraits in chests, the other quest kinds (bury remains, a letter to a descendant, a
named killer mob, a killer spirit), hallucinations at very low sanity, and research: the journal unlocks no recipes.

## Spread and evolution

### Corruption

A territorial spirit fouls the land around its anchor while the anchor's chunk is loaded, with or without players. Every
`corruption.check_ticks` (100) it tries `tries` (3) random columns in a disc whose radius grows from `start_reach` (6) to
the spirit's own radius over `grow_days` (3) in-game days of its age, and turns the top block of a loaded column as
`corruption.blocks` says: grass to coarse dirt, grass, ferns and flowers to dead bushes. It stops at `max_blocks` (1500).
Each spot keeps what stood there, saved with the world.

Each try also turns that column's biome to `restless:blighted` (`data/restless/worldgen/biome/blighted.json`): grey-brown
grass and leaves, murky water, purple-grey sky and fog that closes in at 64 blocks, a pale sky light and green-tinted
torchlight, drifting ash, cave moods and no music; only hostile mobs spawn in it. Biomes are 4×4×4 cells, so it goes a
column of cells at a time, from `biome_depth` (2) cells below the surface to `biome_height` (8) above. A column some
spirit has already blighted is skipped. Each column keeps the biomes it had, and the chunks are resent to clients at
once, as `/fillbiome` does. The world generator never places this biome, so `/locate biome` does not find it.

Once the spirit is gone (banished, at rest, removed) its land heals: `recede` (48) spots and as many biome columns a
check, wherever their chunk is loaded, go back to what they were; a spot or cell something else has changed since is
left as it is.

### Evolution

A spirit `evolution.days` (5) in-game days old grows up, once and for good: a poltergeist turns **raging**, a shade a
**light-eater**. Its power never drops below `power_floor` (0.7), its ritual takes `extra_waves` (4) more waves, and a
territorial one tries `corruption.evolved_factor` (3) times as many columns. The players it haunts (its own player, or
those in its radius) hear a heartbeat, are told, and it goes in their journal; from then on it goes by its grown name.
A personal spirit grows only while its player is online.

Not built yet: world-born spirits (death spots, abandoned structures, blood moon, deeds), merging, the calendar (full
moon, blood night, day of the dead), fog and possessed mobs in corrupted land, crops stopping, and new events or clues
for evolved spirits.

## Spirits

| Id | Description | Activity | Affinity (missing = ×1) |
|---|---|---|---|
| `poltergeist` | Noisy and physical. Mostly messes with doors and belongings; rarely hurts at first. | 1.3 | doors ×4, items ×3, blocks ×1.5, afflictions ×0.5, assaults ×0.5, sounds ×0.5, presence ×0.5 |
| `shade` | Quiet and oppressive. Snuffs lights, whispers and afflicts, turns violent as its power grows. | 0.8 | lights ×3, sounds ×2.5, afflictions ×2, presence ×2, assaults ×1, doors ×0.3, items ×0.5 |

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
| `lantern_flicker` | 3 | LIGHT | LIGHTS | 0.8 | A lantern or soul lantern within 10 | Removed and put back three times in 1.2 s, ends in place |
| `campfire_out` | 6 | LIGHT | LIGHTS | 0.8 | A lit campfire within 10 | Snuffs one campfire |
| `furnace_dies` | 5 | LIGHT | LIGHTS | 0.8 | A burning furnace, blast furnace or smoker within 10 with fuel in the fuel slot | The fire goes out and the fuel is taken out for 5 s, then returned (dropped if the slot was filled meanwhile or the furnace is gone) |
| `dimming` | 7 | LIGHT | LIGHTS | 0.6 | Always | The player's view fades toward black over 5 s (peak 80%) and back; client-side overlay under the HUD, no block or light level changes |
| `candles_light` | 8 | LIGHT | LIGHTS | 0.8 | An unlit candle within 10 | Lights one candle |
| `new_face` | 9 | LIGHT | LIGHTS | 0.6 | A jack o'lantern within 10 | Turns it to face another way |
| `blackout` | 10 | HARMFUL | LIGHTS | 0.5 | Not peaceful, not daylight under open sky, a light within 16, standing room 5–9 blocks away | Torches break, candles, campfires and lit lamps within 16 go out; 0.75 s later a zombie or skeleton appears 5–9 blocks away and targets the player |
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
| `jukebox_pop` | 101 | LIGHT | DOORS | 0.7 | A jukebox within 20 is playing a disc | The music cuts out and the disc pops out of it |
| `locked_door` | 19 | HARMFUL | DOORS | 0.7 | A wooden door within 12 | Slams shut with a thud and cannot be opened for 10 s. The server tells clients within 64 blocks, so the click is refused locally (locked-chest sound) with no open-and-snap-back |
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
| `music_cut` | 102 | LIGHT | SOUND | 0.4 | Always | Stops the game's background music for the player; it resumes on its usual schedule |
| `whisper` | 35 | LIGHT | SOUND | 0.4 | Always | Chat line `<???> <player name>`, only to the player |
| `echo` | 36 | LIGHT | SOUND | 0.4 | The player made a sound (step, splash, bite) in the last 5 s | That sound plays again 3 s later from where it was made, quieter and lower. Recorded through the loader's sound hook, `Restless.onPlaySound`; only sounds in the player category within 2 blocks of the player count |
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
| `shove` | 52 | HARMFUL | ASSAULT | 0.5 | Health > 5 hearts, on the ground at the edge of a 4–7 block drop onto solid ground | Pushes the player toward the drop (a fall of 1–4 HP). Runs only at a ledge |
| `toss` | 59 | HARMFUL | ASSAULT | 0.5 | Health > 5 hearts, on the ground, not in water or lava | Thrown straight up about 5 blocks; the landing costs a couple of HP |
| `suffocation` | 57 | HARMFUL | ASSAULT | 0.5 | Health > 4 hearts, not in water, light level < 8 | Heartbeat, 2 s later the air bar drains (about 7 s to empty), then 1 HP a second. Stops in light, in water, at 3 hearts, or after 15 s |
| `thunder_knocks` | 58 | HARMFUL | ASSAULT | 0.5 | Health > 5 hearts | Three knocks; 3 s later a bolt strikes nearby; 2 s after that a second bolt hits the player if they have moved less than 2 blocks since (6 HP, never lethal). Bolts set nothing on fire |
| `embrace` | 60 | HARMFUL | ASSAULT | 0.5 | Health > 5 hearts | Growing damage once a second (0.5, 1, 1.5 HP…) until the player is 10 blocks from where it began. Stops at 3 hearts or after 20 hits |
| `the_call` | 100 | HARMFUL | ASSAULT | 0.3 | Night in the overworld (any time elsewhere), health > 5 hearts, lava within 12 blocks or a bottomless column 10 blocks away | A warden moan, then after 2 s the player on the ground is drawn toward the lava (or void) at about 1.5 blocks/s, felt as a velocity nudge every 0.1 s, with soul particles at the feet. Walking 6 blocks further away than the start breaks it; it also ends on arrival, in lava, at 3 hearts, or after 30 s. Lava itself is not capped |
| `shadow` | 61 | HARMFUL | ASSAULT | 0.4 | Not peaceful, no shadow already following the player, health > 7 hearts, standing room 14–20 blocks away | A wither skeleton with no AI and no gear (invulnerable, silent) stands there. Every 0.1 s it takes a 0.3-block stride toward the player, but only while the player is not looking at it (within 45° and in line of sight). Warden heartbeat once a second. A touch (1.2 blocks) deals up to 7 hearts but never below 3 hearts, then it vanishes in smoke; it also vanishes after 60 s or after 40 blocked strides |
| `double` | 93 | HARMFUL | MOBS | 0.3 | Not peaceful, no double already after the player, health > 7 hearts, standing room 12–18 blocks away | A vanilla mannequin with the player's skin, armor and held item (invulnerable, silent) walks up at 0.2 blocks/tick, slower than a sprint. Within 1.8 blocks it hits every 0.5 s for up to 3 hearts, never below 3 hearts; it vanishes in smoke after 4 hits, 30 s, 40 blocked strides, or when the player is at 3 hearts |
| `swap_slots` | 62 | LIGHT | ITEMS | 0.5 | Inventory not empty | Two inventory slots swap |
| `hotbar_shift` | 63 | LIGHT | ITEMS | 0.5 | Always | Selected hotbar slot changes |
| `book_page` | 65 | LIGHT | ITEMS | 0.5 | A book and quill or a written book | One creepy page is appended (nothing is erased) |
| `shuffle_chest` | 66 | LIGHT | ITEMS | 0.5 | A non-empty chest within 10 | Its contents are shuffled |
| `wear` | 67 | HARMFUL | ITEMS | 0.5 | A damageable tool with durability to spare | Loses 10% durability, never breaks |
| `rot` | 68 | HARMFUL | ITEMS | 0.5 | Food in the inventory | One food stack becomes rotten flesh |
| `vanishing_item` | 69 | HARMFUL | ITEMS | 0.5 | Two chests (not halves of one) within 10, one non-empty, one with space | One stack moves to the other chest |
| `false_compass` | 70 | HARMFUL | ITEMS | 0.5 | A plain compass in the inventory | Points at a spot 30–60 blocks away (stays that way) |
| `map_marker` | 71 | LIGHT | ITEMS | 0.5 | A filled map in the inventory without a marker of this event | A red X appears on it somewhere in its area. Permanent: the marker is stored in the map item |
| `wilted_flowers` | 72 | LIGHT | BLOCKS | 0.6 | A potted flower within 10 | Every one becomes a potted dead bush; restored after 2 min |
| `withering_crops` | 73 | LIGHT | BLOCKS | 0.6 | A grown crop within 6 | Every crop within 6 loses one growth stage |
| `turning_frame` | 76 | LIGHT | BLOCKS | 0.6 | A filled item frame within 12 | Rotates its item |
| `new_sign` | 74 | LIGHT | BLOCKS | 0.6 | A sign within 10 | The front text becomes one creepy phrase of up to three short lines (at most eight letters each, so it fits a hanging sign); restored after 1 min unless the player has edited it |
| `new_painting` | 75 | LIGHT | BLOCKS | 0.6 | A painting within 12 with another variant of the same size | Becomes another painting; restored after 1 min |
| `glass_wall` | 77 | LIGHT | BLOCKS | 0.6 | Not peaceful; a one-block wall 2–6 blocks away with standing room behind it | The wall's two blocks in front of the player become glass for 4 s; a silent, motionless husk stands behind them facing the player |
| `dry_cauldron` | 78 | LIGHT | BLOCKS | 0.6 | A water cauldron within 10 | Empties it |
| `dead_ground` | 79 | LIGHT | BLOCKS | 0.6 | Player on grass | A ragged patch of grass (radius 3, dense in the middle, sparse at the edge) becomes dirt or coarse dirt |
| `shifting_floor` | 80 | HARMFUL | BLOCKS | 0.5 | On the ground, and the block under the player has a variant (see below) | A ragged patch of the floor (radius 2, a handful of blocks, always the one underfoot) turns into another kind of the same material for 1.5 s |
| `broken_bed` | 81 | HARMFUL | BLOCKS | 0.6 | A bed within 16 | Breaks it; it drops as an item |
| `wall_hole` | 82 | HARMFUL | BLOCKS | 0.6 | A one-block wall 2–6 blocks away with standing room behind it | Two blocks vanish for 30 s, no drops |
| `cross` | 83 | LIGHT | BLOCKS | 0.6 | Room for a plus of five free blocks on solid ground 3–6 blocks away | A plus of bone blocks for 1 min |
| `animals_stare` | 85 | LIGHT | MOBS | 0.6 | Two animals within 16 | They stop and face the player for 3 s |
| `cat_hiss` | 86 | LIGHT | MOBS | 0.6 | A cat within 16 | The cat hisses |
| `dog_barks` | 87 | LIGHT | MOBS | 0.6 | A wolf within 16 | Turns away and makes its ambient sound |
| `villager_stares` | 88 | LIGHT | MOBS | 0.6 | A villager within 16 | One villager stops and faces the player for 5 s |
| `bats` | 89 | LIGHT | MOBS | 0.6 | Free air 4 blocks away | Five bats appear |
| `open_pen` | 90 | HARMFUL | MOBS | 0.6 | A closed gate with an animal within 5 of it | Opens the gate |
| `wild_pet` | 91 | HARMFUL | MOBS | 0.6 | A tamed pet of the player within 32 | Loses its owner and targets the player |
| `copycat` | 92 | HARMFUL | MOBS | 0.6 | Not peaceful, standing room 3 blocks behind | A zombie in a copy of the player's armor, targeting them |
| `edge_figure` | 94 | LIGHT | PRESENCE | 0.4 | No such figure already, standing room 16 blocks away, 45–65° to a side of where the player looks | A silent, unhurtable mannequin with the player's own skin stands there facing them. Gone, with no sound or particles, when the player looks within 25° of it with a clear line, or after 30 s |
| `fog_silhouette` | 95 | LIGHT | PRESENCE | 0.3 | Open sky and rain or darkness outside, standing room 40 blocks away within 25° of where the player looks | A mannequin in the default skin stands far off. Gone, silently, when the player comes within 24 blocks, or after 60 s |
| `footprints` | 96 | LIGHT | PRESENCE | 0.6 | Standing on or in sand or snow | For 6 s, puffs of the ground and a step sound appear where the player stood a second ago. Particles only, nothing is placed |
| `cold_breath` | 97 | LIGHT | PRESENCE | 0.6 | Warm biome | A breath puff at the face |
| `local_rain` | 98 | LIGHT | PRESENCE | 0.6 | Sky visible, not raining | Rain particles and sound over the player for 6 s |
| `lost_time` | 99 | HARMFUL | PRESENCE | 0.5 | Night | The overworld clock jumps 1000 ticks (affects everyone) |

## Chances

Share of each tag per roll of one spirit, assuming every event can run (in practice many cannot, and the rest renormalise,
so a spirit near no doors never rolls a door). Computed from the weights above.

| Spirit | Power | Lights | Doors | Sound | Affliction | Assault | Items | Blocks | Mobs | Presence |
|---|---|---|---|---|---|---|---|---|---|---|
| `poltergeist` | 0 | 11.0% | 49.1% | 4.6% | 3.1% | 0% | 13.8% | 12.4% | 4.6% | 1.4% |
|  | 0.5 | 9.8% | 44.1% | 3.6% | 4.1% | 2.9% | 16.6% | 12.2% | 5.3% | 1.3% |
|  | 1 | 7.8% | 36.4% | 2.2% | 5.8% | 7.3% | 20.9% | 11.9% | 6.5% | 1.2% |
| `shade` | 0 | 35.7% | 4.0% | 24.8% | 13.2% | 0% | 2.5% | 8.9% | 5.0% | 6.0% |
|  | 0.5 | 31.0% | 3.5% | 19.3% | 17.4% | 6.0% | 2.9% | 8.6% | 5.6% | 5.6% |
|  | 1 | 24.0% | 2.8% | 11.1% | 23.6% | 15.0% | 3.6% | 8.2% | 6.6% | 5.1% |

Cooldown per spirit, minutes (before the daylight factor): `(4 − 2.5 × power) × (1 + roll) / activity`. Poltergeist: 3.1–6.2 at power 0, 1.2–2.3 at 1.
Shade: 5.0–10.0 at power 0, 1.9–3.8 at 1.

## Scheduled events

Multi-step events run through `haunt/Scheduler.java`: `later(player, delay, action)` and
`repeat(player, delay, interval, step)` (the step returns false to stop). Tasks are in memory only and are dropped when
the player dies, leaves or the world closes, so no step may leave something broken behind. Events that hurt
(`suffocation`, `thunder_knocks`, `embrace`, `shadow`, `double`, `the_call`) announce themselves with a sound first and stop at 3 hearts.

## Restoring and locks

Block changes that must not damage a build (`dead_ground`, `shifting_floor`) only swap a block for another kind of
the same material (`haunt/event/Variants.java`): grass to dirt or coarse dirt, dirt to coarse dirt or podzol, stone to
andesite, granite or diorite, cobblestone to mossy, stone and deepslate bricks to cracked or mossy, deepslate to
cobbled. Anything else is left alone. The patch is a scatter (`Place.patch`), not a disc.

- `haunt/Restore.java`: changes that swap a block or an entity (flowers, sign, painting, glass, hole, floor, cross,
  lantern, furnace fuel) register an undo that runs when due and checks that the thing is still what the
  event left, so a block the player has since changed is not overwritten. Block swaps skip neighbour updates (nothing
  attached pops off) and are retried until the chunk is loaded. Everything still pending is undone when the server
  stops (`Restless.onServerStopping`). After a crash the changes stay. Not covered: `map_marker` is permanent.
- `haunt/DoorLocks.java`: doors locked by `locked_door`, in memory. `Restless.onUseBlock` cancels the right click on
  both sides; the loader forwards its block-interaction event to it. The server sends `DoorLockPayload` to clients
  within 64 blocks, which the loader registers and hands to `DoorLocks.put`. A client that joins or walks up later
  than the lock does not know it and sees the server undo its open attempt.

## Config

Every tunable value lives in `core/src/main/resources/restless-config/`, split into files that are slices of one tree:

| File | Holds |
|---|---|
| `haunt.json` | `director`, `power`, `anger`, `salt`, `wards`, `bell`, `candle`, `compass`, `ash`, `camera`, `spirit_box`, `ouija`, `clues`, `ritual`, `artifacts`, `grave`, `sanity`, `corruption`, `evolution`, `journal`, `restore`, `place` |
| `spirits.json` | `spirits` (activity, tag affinities), `traits` (the trait catalog, with each trait's clues, words, hint and weakness), `story` (nickname words, days, causes) |
| `lights`, `mechanisms`, `sounds`, `afflictions`, `assaults`, `items`, `blocks`, `mobs`, `presence` | `events.<namespace>:<id>` for that group (`assaults.json` also holds `assaults.safe_health`) |

Names and descriptions of events live apart, in `restless-config/lang/en.json` and `ru.json` (copied to `config/restless/lang/` on first run,
edit the copy). The debug screen shows them in the client's language, falling back to English, then to the id. A new event needs an entry in both.

Event ids are namespaced like vanilla keys. The namespace names the mod whose content the event uses: `minecraft` for everything that
needs only vanilla, another mod's id for an integration with it (say `supplementaries:globe_spin`). In code a bare id means `minecraft`.

An event holds its weight, radii, durations, damage, thresholds and texts, plus its sound (`sound`: `volume`, `pitch`;
extra ones are named like `pulse_sound`) and particles (`particles`: `count`, `dx`, `dy`, `dz`, `speed`, `offset_y`).
Only fixed units stay in code (ticks per second, 360 degrees, inventory size, sign lines).

On first run each file is copied to `config/restless/`. They are read once at startup (restart to apply a change), and
only the keys a file sets override the defaults. A key the code reads but the defaults lack, a default the code never
reads, or a key defined in two files fails the build (`ConfigTest`).

Versions: every file starts with `"version": N`. A file with no version counts as 1. When a release renames, moves or
drops a key it bumps N by adding a step to `config/Migrations.java` (`Migrations.move` and `remove` help); on startup an
older file is walked through the steps it has not seen, the old copy is kept as `<name>.json.v<N>.bak`, and the migrated
file is written back complete. A new key needs no step, it comes from the defaults. A file with a newer version than
the mod, or one that is not valid JSON, is logged and ignored (the defaults run) and never overwritten.

## Admin command

Permission level: gamemaster. `[player]` defaults to the caller.

- `/haunt`: opens the debug screen (needs the mod on the client): your spirits (seed, traits, power), every event with the chance each spirit rolls it next, with its tier and whether it can run now, and a button that runs the selected event on you after 1 second.
- `/haunt info [player]`: the player's sanity and the numbered spirit list with each spirit's kind, story, seed, traits, power, anger, time to its next event, its grave and the item it wants, whether it has evolved, and its fouled land as `blight=<spots>/<biome columns>`.
- `/haunt sanity <0..1> [player]`: sets sanity.
- `/haunt force <event> [player]`: runs an event now (fails if it cannot run). The event is a namespaced id, `minecraft:cat_hiss`; the namespace is optional for `minecraft`.
- `/haunt spirit add <spirit> [player]`: binds one more personal spirit; for a territorial one, anchors it where the player stands.
- `/haunt spirit age <n> <days> [player]`: makes spirit number `n` that many in-game days older (maturity, blight reach, evolution).
- `/haunt spirit remove <n> [player]`: removes spirit number `n` (1-based, as listed by `info`: the player's personal spirits, then the territorial ones in their dimension).
