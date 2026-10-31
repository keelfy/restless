# Spirits and haunt events

Reference for the current content. Source of truth is the code:
`spirit/Spirits.java`, `haunt/HauntEvents.java`, `haunt/event/*`, `haunt/Power.java`, `haunt/Reactions.java`, `haunt/Anger.java`, `spirit/Traits.java`, `spirit/Story.java`, `haunt/Director.java`, `haunt/Scheduler.java`.
Update this file when any of them changes. The numbers below are the shipped defaults; see [Config](#config).

## How it works

- A spirit is **personal** (follows one player) or **territorial** (stays at an anchor); the kind is `kind` in `spirits.json`.
  `shade`, `phantom`, `mimic`, `wraith`, `mara`, `banshee`, `revenant` and `lost_one` are personal; `poltergeist`, `nix`,
  `domovoi`, `hoarder`, `demon` and `trickster` are territorial.
- Each player gets 3 personal spirits (`director.default_spirits`) on first check, each chosen uniformly at random
  (repeats allowed), and keeps them. The list can grow or shrink, down to 0, via `/haunt spirit add|remove`. A poltergeist
  in an older save is dropped.
- A territorial spirit has an anchor, a dimension and a radius (`director.territorial_radius`, 48 blocks), saved per world.
  When its cooldown expires it acts on one random player (survival or adventure) inside the radius, and does nothing, but
  still reschedules, if nobody is there.
- Every spirit has a location. A personal spirit roams in a ring `location.min_distance` to `location.max_distance`
  blocks from its player, drifting `location.step` blocks per second, and is forgotten on restart; a territorial spirit
  stays at its anchor. Sound events with no fixed source (creeper hiss, crying child, eating behind a wall, enderman
  hiss, chest opening, distant bell, phantom jukebox, death cry, dripping, block breaking, bottle shatter, and the knocks when no door
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

A type can carry `when` and `reacts` too (see [Spirits](#spirits)): its `activity` then holds only under that condition
(1 otherwise), and its triggers work like a trait's.

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
`activity` is the type's activity (if its `when` holds) times every trait's active modifier.

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

Only the poltergeist and the shade have an essence; every other type drops nothing (graves give no essence for it either).

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
grass and leaves, murky water, a thick grey haze like the basalt deltas' (fog from 4 to 40 blocks, sky and clouds
greyed out, ash as dense as the deltas', and `restless:mist` wisps creeping along the ground), a pale sky light and
green-tinted torchlight, cave moods and no music; only hostile mobs spawn in it. Biomes are 4×4×4 cells, so it goes a
column of cells at a time, from `biome_depth` (2) cells below the surface to `biome_height` (8) above. A column some
spirit has already blighted is skipped. Each column keeps the biomes it had, and the chunks are resent to clients at
once, as `/fillbiome` does. The world generator never places this biome, so `/locate biome` does not find it.

Mist wisps lie horizontally 0.15–0.35 blocks above solid ground, with at most 12% opacity and a minimum block-light
level of 8 for rendering. They drift together at 0.3–0.5 blocks per second along X and 0.1–0.2 along Z, fading in and
out over three seconds. A wisp disappears when its center reaches an obstacle, fluid, or unsupported ground.

Once the spirit is gone (banished, at rest, removed) its land heals: `recede` (48) spots and as many biome columns a
check, wherever their chunk is loaded, go back to what they were; a spot or cell something else has changed since is
left as it is.

### Evolution

A spirit `evolution.days` (5) in-game days old grows up, once and for good: a poltergeist turns **raging**, a shade a
**light-eater**, a phantom a **doppelganger**, a nix a **drowner**, a mimic a **skinwalker**, a wraith a **dread wraith**, a mara a
**night hag**, a banshee a **keening banshee**, a revenant an **avenger**, a lost one the **forsaken**, a domovoi a **house master**,
a hoarder a **glutton**, a demon an **archdemon** and a trickster a **jester**. Its power never drops below `power_floor` (0.7), its ritual takes `extra_waves` (4) more waves, and a
territorial one tries `corruption.evolved_factor` (3) times as many columns. The players it haunts (its own player, or
those in its radius) hear a heartbeat, are told, and it goes in their journal; from then on it goes by its grown name.
A personal spirit grows only while its player is online.

Not built yet: world-born spirits (death spots, abandoned structures, blood moon, deeds), merging, the calendar (full
moon, blood night, day of the dead), possessed mobs in corrupted land, crops stopping, and new events or clues
for evolved spirits.

## Spirits

| Id | Description | Activity | Affinity (missing = ×1) |
|---|---|---|---|
| `poltergeist` | Noisy and physical. Mostly messes with doors and belongings; rarely hurts at first. | 1.3 | doors ×4, items ×3, blocks ×1.5, afflictions ×0.5, assaults ×0.5, sounds ×0.5, presence ×0.5 |
| `shade` | Quiet and oppressive. Snuffs lights, whispers and afflicts, turns violent as its power grows. | 0.8 | lights ×3, sounds ×2.5, afflictions ×2, presence ×2, assaults ×1, doors ×0.3, items ×0.5 |
| `phantom` | Likes to be seen, as other players: far-off strangers, a name in the player list, someone pillaring or taking the crops. Seldom hurts. | 1.0 | player ×6, presence ×2, doors ×0.5, lights ×0.5, afflictions ×0.5, items ×0.5, assaults ×0.3 |
| `nix` | Territorial, bound to water. Keeps swimmers under, calls players to the water, kills fish and breaks boats. Place it by a lake or the sea with `/haunt spirit add nix` while standing there. | 1.0 | water ×8, afflictions ×1, assaults ×0.5, sounds ×0.5, blocks ×0.3, presence ×0.3 |
| `mimic` | Fakes things: blocks that are not there, strangers, mobs that vanish. | 1.0 | blocks ×3, player ×2, presence ×1.5, mobs ×1.5, lights ×0.5, doors ×0.5, assaults ×0.5 |
| `wraith` | Hunts in the dark: snuffs lights and hurts. | 2.0 in the dark (`when: dark`), 1 elsewhere | assaults ×2.5, lights ×2, afflictions ×1.5, presence ×1.5, doors ×0.5, blocks ×0.5, items ×0.3 |
| `mara` | Rides the sleeper: afflictions and presence at night, may act when the player uses a bed (`reacts: bed`). | 1.8 at night (`when: night`), 1 by day | afflictions ×3, presence ×2, sounds ×1.5, doors ×0.5, items ×0.3, blocks ×0.3 |
| `banshee` | A voice: mostly sounds, rarely touches anything. | 0.9 | sounds ×5, presence ×1.5, doors ×0.5, items ×0.3, blocks ×0.3, assaults ×0.3 |
| `revenant` | Vengeful: hurts and sends mobs, may act after the player kills a mob (`reacts: kill`). | 1.0 | assaults ×3, mobs ×2.5, afflictions ×1, lights ×0.5, doors ×0.3, items ×0.3 |
| `lost_one` | Wanders: figures, strangers and steps, may act when the player goes below y 0 (`reacts: deep`). | 0.9 | presence ×3, player ×2, sounds ×1.5, blocks ×0.5, items ×0.5, assaults ×0.2 |
| `domovoi` | Territorial house spirit: doors, belongings and household noises, hardly harmful. | 0.7 | doors ×2, items ×2, sounds ×1.5, mobs ×0.5, afflictions ×0.3, assaults ×0.2 |
| `hoarder` | Territorial: takes and moves items. | 1.2 | items ×5, blocks ×1.5, doors ×1.5, lights ×0.5, presence ×0.5, assaults ×0.3 |
| `demon` | Territorial and dangerous: assaults, afflictions and mobs. | 1.1 | assaults ×3, afflictions ×2, mobs ×2, lights ×1.5, doors ×0.5, items ×0.5 |
| `trickster` | Territorial prankster: changes blocks and items, shows things. | 1.2 | blocks ×2.5, items ×2, mobs ×1.5, presence ×1.5, assaults ×0.2 |

Affinity is a weight multiplier per event tag. Activity 1.3 means 1.3× as often.

The ten types after `nix` only pick from the existing events. Their own gimmicks from [design.md](design.md) are not built:
fake chat (mimic), the nightmare room (mara), honest warnings (banshee), birth from killing (revenant), replaying a death
path (lost one), winning a domovoi with offerings, the nest (hoarder), pacts (demon) and fake gifts (trickster). None has
an essence or artifact.

## Events

`Idea` is the number in [event-ideas.md](event-ideas.md).

Text events (`whisper`, `own_voice`, `subliminal_title`, `false_death`, `false_advancement`, `stranger_death`, `book_page`,
`new_sign`, `cave_sign`, `item_trail`) never repeat a variant for the same spirit: each spirit remembers what it has said through each of
them. Once every variant is used, the event cannot run for that spirit for `said.rest_ticks` (7 in-game days), then starts
over. A text event with no spirit behind it (command, debug menu) counts against the player instead. Saved with the world.

| Id | Idea | Tier | Tag | Base weight | Runs when | Effect |
|---|---|---|---|---|---|---|
| `extinguish_light` | | LIGHT | LIGHTS | 1.0 | A lit candle or a torch / soul torch (floor or wall) is within 10 blocks | Snuffs a lit candle; a torch is knocked off and drops as an item |
| `door` | | LIGHT | DOORS | 1.0 | A wooden door is within 12 blocks | Opens a closed door or slams an open one |
| `blindness` | | LIGHT | AFFLICTION | 0.6 | Player is not already blind | Blindness for 6 s and a low cave ambient sound |
| `cold_touch` | | HARMFUL | ASSAULT | 1.0 | Player health > 1 heart | 1–2 hearts of magic damage (2–4 HP), never lethal (leaves at least 0.5 heart), freezing sound |
| `candles_out` | 1 | LIGHT | LIGHTS | 0.8 | A lit candle within 10 blocks | Every lit candle within 10 blocks goes out |
| `torch_chain` | 2 | LIGHT | LIGHTS | 0.8 | A torch within 10 | Up to 8 torches knock off one by one, every 0.3 s, nearest to the player last |
| `torches_behind` | 187 | LIGHT | LIGHTS | 0.5 | Underground, a torch within 16 behind the player | Up to 8 torches behind the player's facing turn unlit one by one, every 0.75 s, farthest first; relit after 2 min unless changed (hardcore: stay out) |
| `lamp_off` | 4 | LIGHT | LIGHTS | 0.8 | A lit redstone lamp within 10 | Switches it off for 1 s, back on if still powered |
| `lantern_flicker` | 3 | LIGHT | LIGHTS | 0.8 | A lantern or soul lantern within 10 | Out and back on three times in 1.2 s, ends in place (stays out in hardcore) |
| `campfire_out` | 6 | LIGHT | LIGHTS | 0.8 | A lit campfire within 10 | Snuffs one campfire |
| `furnace_dies` | 5 | LIGHT | LIGHTS | 0.8 | A burning furnace, blast furnace or smoker within 10 with fuel in the fuel slot | The fire goes out and the fuel is taken out for 5 s, then returned (dropped if the slot was filled meanwhile or the furnace is gone) |
| `dimming` | 7 | LIGHT | LIGHTS | 0.6 | Always | The player's view fades toward black over 9 s (peak 80%) and back over 6 s; client-side overlay under the HUD, no block or light level changes |
| `blinking` | 144 | LIGHT | LIGHTS | 0.4 | Always | The dimming overlay, 3–6 quick cycles (0.15 s down, 0.2 s held at 95%, 0.25 s up) |
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
| `lectern_pages` | 174 | LIGHT | DOORS | 0.7 | A lectern within 12 holds a book of more than one page | The book turns to another page with a page-turn sound, through the lectern's own menu so a comparator sees the change |
| `pot_wobble` | 175 | LIGHT | DOORS | 0.7 | A decorated pot within 12 | It wobbles as if an item were refused |
| `bell_swing` | 175 | LIGHT | DOORS | 0.7 | A bell within 20 | It swings and rings toward a random side |
| `redstone_drift` | 178 | HARMFUL | DOORS | 0.5 | A repeater or comparator within 12 | A repeater gets another delay, a comparator the other mode, with no update. Set back after 2 min if unchanged; under hardcore it stays |
| `occupied_bed` | 169 | LIGHT | DOORS | 0.6 | An unoccupied bed within 12 | Both halves read as occupied for 1 min, so the vanilla "This bed is occupied" shows. Always put back, hardcore or not |
| `shuffled_books` | 176 | LIGHT | DOORS | 0.5 | A chiseled bookshelf within 12 with a book | The books take other slots. Put back after 2 min if untouched; under hardcore they stay |
| `dispenser_shot` | 177 | HARMFUL | DOORS | 0.4 | A dispenser within 12 | It clicks as if empty, then after 1 s an arrow leaves its face toward the player. Takes nothing from its contents, the arrow cannot be picked up. Under hardcore it needs an arrow inside and fires that one, which can be picked up |
| `chest_blast` | 117 | HARMFUL | DOORS | 0.3 | The player has a chest open (within 6), health above 10 | A creeper hiss from the chest, then after 1 s an explosion of power 1 that breaks no blocks |
| `wrong_smelt` | 182 | LIGHT | DOORS | 0.5 | A furnace within 10 with something in its output | The output becomes charcoal or rotten flesh of the same count; put back after 2 min unless taken or changed (stays under hardcore) |
| `furniture_moves` | 103 | HARMFUL | DOORS | 0.3 | A single chest, crafting table, furnace or empty bed within 12 | It stands 2 to 5 blocks away on free floor, block entity and contents carried through its save format, a bed both halves. Moved back after 5 min if both spots are unchanged; under hardcore it stays |
| `doorstep_gift` | 186 | LIGHT | DOORS | 0.4 | A door within 12 with free floor on its far side | A poppy, dandelion, bone or raw rabbit lies outside, on the side away from the player; a real item |
| `muddy_footprints` | 184 | LIGHT | DOORS | 0.4 | Indoors, a bed or chest within 10 on the player's level, reachable over free floor | Pairs of muddy prints appear one by one on every first or second block from the player to it, each with a mud step. Each is wiped after 2 min if still there; under hardcore they stay |
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
| `death_cry` | 37 | LIGHT | SOUND | 0.4 | Always | A scream (custom sound) nearby |
| `dripping` | 38 | LIGHT | SOUND | 0.4 | Always | Water drip nearby |
| `distant_mob` | 131 | LIGHT | SOUND | 0.4 | Always | A zombie, skeleton, spider, enderman, witch, husk, drowned, pillager or ghostly moan sound 2–40 blocks away; louder the farther, so it carries |
| `hostile_animal` | 132 | LIGHT | SOUND | 0.4 | An animal within 16 | A zombie, creeper, skeleton or witch sound bound to it, sent to this player only |
| `approaching_steps` | 133 | LIGHT | SOUND | 0.4 | Always | Twelve step sounds of the block underfoot, closing from 14 to 3 blocks on a random bearing |
| `distant_mining` | 134 | LIGHT | SOUND | 0.4 | Open sky above the player | Six stone hits and a break sound 8 blocks below the surface |
| `many_breaks` | 137 | LIGHT | SOUND | 0.3 | Six or more glass or stone blocks within 14 | Up to eight of them sound as if they break in turn; under hardcore they do break, with no drops |
| `ladder_steps` | 205 | LIGHT | SOUND | 0.3 | Always | Step sounds climbing a real ladder within 16, or rising from the spirit's point when there is none |
| `spider_overhead` | 136 | LIGHT | SOUND | 0.4 | A solid block 2–3 above the head with air below it | A spider sound, then five steps crawling across the ceiling |
| `cracking_floor` | 138 | HARMFUL | SOUND | 0.3 | Standing on a block | Break particles and sound of the block underfoot; it stays |
| `bottle_shatter` | 139 | LIGHT | SOUND | 0.4 | Always | A splash potion shatters 4 blocks away (sound and coloured particles) |
| `shrieker` | 199 | LIGHT | SOUND | 0.4 | A sculk shrieker within 16 | It shrieks. No player is given to it, so nobody is warned and no warden is called |
| `fake_subtitle` | 164 | LIGHT | SOUND | 0.3 | Always | A sound with no audio that shows the subtitle "Something breathes behind you" from 3 blocks behind the player. Only seen with subtitles on |
| `phantom_heartbeat` | 199 | LIGHT | SOUND | 0.4 | Always | Eight warden heartbeats at the player, one every 0.9 s |
| `note_block_voice` | 140 | LIGHT | SOUND | 0.5 | A note block within 16 | It plays a villager or player hurt, ghast scream, zombie villager, vex or ghostly moan sound, with a note particle |
| `ghost_laugh` | 135 | LIGHT | SOUND | 0.3 | Always | A ghostly laugh (custom sound) 9 blocks away |
| `ghost_breath` | 135 | LIGHT | SOUND | 0.3 | Always | A cold breath (custom sound) 0.8 blocks behind the player |
| `close_whispers` | 135 | LIGHT | SOUND | 0.3 | Always | Three whispers (custom sound) 1.5 blocks away at the player's ears, left and right in turn, one every 2.5 s |
| `circling_moan` | 135 | LIGHT | SOUND | 0.25 | Always | Four moans (custom sound) 6 blocks away, each 90° further round the player than the last, one every 2.25 s |
| `cut_off_scream` | 135 | LIGHT | SOUND | 0.25 | Always | A scream (custom sound) 12 blocks away that is stopped dead 0.7 s in |
| `banshee` | 150 | LIGHT | SOUND | 0.15 | Always | A 14 s shriek (custom sound) from 30 blocks away and 12 above, loud enough to carry |
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
| `insomnia` | 171 | HARMFUL | AFFLICTION | 0.4 | Not peaceful, fewer than 144000 ticks since rest | The player's "time since rest" is set to 144000 (past three days), so phantoms come the next night; sleeping resets it |
| `dream` | 173 | LIGHT | AFFLICTION | 0.5 | A spirit senses the player, and no dream is waiting | On the next waking (within 1 h) a line in chat and the journal: a nearby spirit's story ("who drowned in a flooded cave") or one of its traits |
| `sleepwalk` | 172 | HARMFUL | AFFLICTION | 0.3 | No walk is waiting | On the next waking after at least 5 s in bed (within 1 h) the player is moved to standing room 4–20 blocks away (solid floor, not magma, no lava next to it), outdoors if one of 4 tries finds sky |
| `ravenous` | 141 | LIGHT | AFFLICTION | 0.5 | No hunger effect | Hunger II for 20 s |
| `infested` | 141 | HARMFUL | AFFLICTION | 0.4 | No infested effect | Infested for 30 s: silverfish come out when the player is hurt |
| `oozing` | 141 | LIGHT | AFFLICTION | 0.4 | No oozing effect | Oozing for 30 s: slimes spill out if the player dies |
| `weaving` | 141 | LIGHT | AFFLICTION | 0.4 | No weaving effect | Weaving for 30 s: cobwebs form if the player dies |
| `drift` | 143 | LIGHT | AFFLICTION | 0.5 | On the ground | For 3 s a small push each tick in one random direction, as by a current |
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
| `undertow` | 149 | HARMFUL | WATER | 0.4 | Health > 5 hearts, the player in water | Every 0.1 s the player is pushed down (a 0.12 downward velocity nudge, so at the surface they are kept from coming up). Ends out of the water, at 3 hearts, or after 10 s. Drowning itself is vanilla's and is not capped |
| `water_call` | 149 | HARMFUL | WATER | 0.3 | Health > 5 hearts, the player out of water, water within 12 blocks | An underwater swell, then after 2 s the player on the ground is drawn toward the nearest water at about 1.5 blocks/s, with splash particles at the feet. Ends on arrival, in water, at 3 hearts, or after 20 s |
| `drowned_fish` | 149 | LIGHT | WATER | 0.4 | A squid or fish within 24 blocks | An elder guardian's curse sounds, and up to 12 squid and fish within 24 blocks die with a puff of ink. They are lost for good (no drops) |
| `broken_boat` | 149 | LIGHT | WATER | 0.4 | An empty boat or raft within 24 blocks | A splintering sound and one of them breaks as if hit hard, dropping its item. A boat with anyone aboard is left alone |
| `shadow` | 61 | HARMFUL | ASSAULT | 0.4 | Not peaceful, no shadow already following the player, health > 7 hearts, standing room 14–20 blocks away | A wither skeleton with no AI and no gear (invulnerable, silent) stands there. Every 0.1 s it takes a 0.3-block stride toward the player, but only while the player is not looking at it (within 45° and in line of sight). Warden heartbeat once a second. A touch (1.2 blocks) deals up to 7 hearts but never below 3 hearts, then it vanishes in smoke; it also vanishes after 60 s or after 40 blocked strides |
| `double` | 93 | HARMFUL | MOBS | 0.3 | Not peaceful, no double already after the player, health > 7 hearts, standing room 12–18 blocks away | A vanilla mannequin with the player's skin, armor and held item (invulnerable, silent) walks up at 0.2 blocks/tick, slower than a sprint, jumping up one-block steps, falling off ledges up to 3 deep and turning up to 80° off to go round what is in its way. Within 1.8 blocks it hits every 0.5 s for up to 3 hearts, never below 3 hearts; it vanishes in smoke after 4 hits, 30 s, 40 blocked strides, or when the player is at 3 hearts |
| `swap_slots` | 62 | LIGHT | ITEMS | 0.5 | Inventory not empty | Two inventory slots swap |
| `hotbar_shift` | 63 | LIGHT | ITEMS | 0.5 | Always | Selected hotbar slot changes |
| `book_page` | 65 | LIGHT | ITEMS | 0.5 | A book and quill or a written book | One creepy page is appended (nothing is erased) |
| `shuffle_chest` | 66 | LIGHT | ITEMS | 0.5 | A non-empty chest within 10 | Its contents are shuffled |
| `wear` | 67 | HARMFUL | ITEMS | 0.5 | A damageable tool with durability to spare | Loses 10% durability, never breaks |
| `rot` | 68 | HARMFUL | ITEMS | 0.5 | Food in the inventory | One food stack becomes rotten flesh |
| `tool_look` | 146 | LIGHT | ITEMS | 0.5 | A wooden, stone, iron or diamond tool in the inventory | Drawn as another such tool for 5 min (it still works the same); under hardcore or real swaps it becomes that tool |
| `armor_off` | — | HARMFUL | ITEMS | 0.4 | Any armor worn | 1–4 random worn pieces come off (a random set, so sometimes all of it). Each goes to the inventory, or to the floor at the player's feet if it is full; nothing is lost |
| `vanishing_item` | 69 | HARMFUL | ITEMS | 0.5 | Two chests (not halves of one) within 10, one non-empty, one with space | One stack moves to the other chest |
| `false_compass` | 70 | HARMFUL | ITEMS | 0.5 | A plain compass in the inventory | Points at a spot 30–60 blocks away (stays that way) |
| `map_marker` | 71 | LIGHT | ITEMS | 0.5 | A filled map in the inventory without a marker of this event | A red X appears on it somewhere in its area. Permanent: the marker is stored in the map item |
| `wilted_flowers` | 72 | LIGHT | BLOCKS | 0.6 | A potted flower within 10 | Every one becomes a potted dead bush; restored after 2 min |
| `withering_crops` | 73 | LIGHT | BLOCKS | 0.6 | A grown crop within 6 | Every crop within 6 loses one growth stage |
| `lost_growth` | 110 | LIGHT | BLOCKS | 0.5 | A crop, stem, berry bush or cocoa past its first stage within 8 | One of them is set back to age 0 |
| `false_windows` | 105 | LIGHT | BLOCKS | 0.4 | Glass or a glass pane within 10 | Only this player's client is told that the 64 nearest glass blocks and panes are, all alike, air, an oak fence (connected to the walls) or the full block most of its neighbours are. 10 s later it gets the real blocks back. Nothing on the server |
| `false_looks` | 106 | LIGHT | BLOCKS | 0.4 | A block from `blocks` within 10 (workstations, containers and other blocks one uses: crafting table, furnaces, chests, barrel, stonecutter, fletching, smithing and cartography tables, loom, grindstone, anvil, lectern, brewing stand and so on) | Only this player's client is told that the 16 nearest of them are each another random block of the list, keeping the facing. 10 s later it gets the real blocks back. Nothing on the server |
| `ghost_tree` | 107 | LIGHT | BLOCKS | 0.3 | Standing room 12 blocks away within 40° of where the player looks (up to 16 bearings tried), with open space (air, grass, flowers) for a 6-block trunk | Only this player's client is told of a small round tree there, pale oak, oak or birch, with a 4 to 6 block trunk and the vanilla blob crown, of `ghost_<wood>_log` and `ghost_<wood>_leaves` (same look and leaf tint, no collision, no drops; leaves only on air), so they walk through it. A minute later it gets the real blocks back. Nothing on the server |
| `torch_line` | 190 | LIGHT | BLOCKS | 0.3 | Outdoors at night (sky visible), or under leaves only with a log within 8; ground for at least 3 torches every 3 blocks from 10 blocks out where the player looks | Only this player's client is told of up to 8 torches in a line there, following the ground up and down. Each is gone for them when they come within 4 blocks, the rest after a minute. Nothing on the server; under hardcore the torches are placed for real and stay |
| `hallucination` | 126 | LIGHT | MOBS | 0.3 | Standing room 20 blocks away within 40° of where the player looks (up to 16 bearings tried) | Only this player's client is told of a mob from a list (iron golem, villager, wandering trader, witch, enderman) standing there facing them, with an id no server entity has, so attacks on it do nothing. Gone when the player comes within 10 blocks, or after 30 s. Nothing on the server |
| `disguise` | 125 | LIGHT | MOBS | 0.3 | A cow, sheep, pig, chicken or horse within 24 blocks | One of them is hidden from this player's client (only its invisible flag is sent) and an entity of another kind (cow→sheep, sheep→pig, pig→cow, chicken→wolf, horse→cow; `looks`) is shown in its place, following it about for 30 s, when the real one is shown again. Sounds and hitbox stay the real animal's; the head turn and limb poses of the copy are the client's own. Under hardcore the animal is really converted into the other kind |
| `dragon_flyover` | 127 | LIGHT | PRESENCE | 0.15 | Overworld, sky visible | Only this player's client is told of an ender dragon flying a straight 240-block line 40 blocks above them, up to 30 to one side, at a random bearing, in 10 s, with its growl at the start (its wing flaps are the client's own). Nothing on the server |
| `moving_tree` | 108 | LIGHT | BLOCKS | 0.3 | A log on dirt within 16 blocks | Takes a log standing on dirt, the logs joined to it and the non-persistent leaves that grew from them (a leaf no nearer to these logs than its own distance to a log, so touching crowns stay; up to 300 blocks, with a crown), and moves them 2 to 4 blocks in a random direction if every spot is open (air, grass, flowers) and the foot stands on dirt; a rustle sounds. 30 s later it moves back, and the grass and flowers it stood in come back, unless it was cut or built on meanwhile. Under hardcore it stays |
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
| `infested_stone` | 191 | HARMFUL | BLOCKS | 0.4 | Stone, cobblestone, deepslate or stone bricks in sight within 6 | The block is shown as its infested kind (a swap, see `swaps.mode`); restored after 30 s |
| `ceiling_crack` | 118 | LIGHT | BLOCKS | 0.5 | A solid block 1–4 above the eyes | Crack progress and particles on it, as if mined from above, silent; the block never breaks, but under hardcore it breaks at the last stage (no drop) unless it holds something, is unbreakable, or has fluid beside it or sand above |
| `ice_over` | 142 | LIGHT | BLOCKS | 0.5 | Source water within 8 | Up to 24 of them become ice (a swap); restored after 30 s |
| `portal_out` | 114 | HARMFUL | BLOCKS | 0.4 | Nether portal blocks within 12 | The whole portal (every connected block) is broken with a fizzle; the frame stays. Outside the Nether it is for good in every mode; in the Nether it comes back after 60 s in every mode, so nobody is stranded |
| `block_blink` | 109 | LIGHT | BLOCKS | 0.4 | The block looked at within 5, no block entity, not below the feet | Breaks with its sound and dust (a swap to air), back after 2 s; under hardcore it stays gone, no drop |
| `leak` | 183 | LIGHT | BLOCKS | 0.5 | Air just under a ceiling 1–4 above the eyes | Water drips from it for 8 s, particles and sound only, in every mode |
| `lava_leak` | 183 | LIGHT | BLOCKS | 0.4 | Same | Lava drips from it for 6 s, particles and sound only, in every mode |
| `mock_grave` | 185 | LIGHT | BLOCKS | 0.4 | Open standing room 5–10 away with a free cell in front | A gravestone with a sign (R.I.P., the player's name, tomorrow's date); removed after 5 min, stays under hardcore |
| `cave_sign` | 189 | LIGHT | BLOCKS | 0.4 | Underground (no sky above), a wall 4–8 blocks out in one of the four directions | A wall sign with a short warning (go back, turn around, too late); removed after 5 min, stays under hardcore |
| `item_trail` | 115 | LIGHT | BLOCKS | 0.3 | Standing room 10–16 away | Bones, rotten flesh and string every 3 blocks along the ground from the player to a standing sign with a short note; the items cannot be picked up. Items cleared and sign removed (if unedited) after 5 min; under hardcore the items can be taken and the sign stays |
| `walled_in` | 116 | HARMFUL | BLOCKS | 0.3 | On the ground, not in water, at least 5 free cells around the feet, head and over the head | Four knocks on the walls, a second apart, then the player is pulled to the middle of their block and every free cell beside the feet and head and over the head becomes the commonest plain block around (no falling blocks, ores or mineral blocks; stone if none), with mining fatigue III for 20 s. The player's own two blocks always stay open. Removed after 20 s; under hardcore the walls stay and the player digs out |
| `sealed_tunnel` | 188 | HARMFUL | BLOCKS | 0.3 | On the ground, a roof above (heightmap), open tunnel straight behind | The tunnel 2–5 blocks behind the player (the first cross-section of at most 6 air cells shut in by full blocks, with nobody in it) is filled with the commonest plain block of its walls (stone if none); opened again after 1 min, stays under hardcore |
| `animals_stare` | 85 | LIGHT | MOBS | 0.6 | Two animals within 16 | They stop and face the player for 3 s |
| `pen_corner` | 195 | LIGHT | MOBS | 0.5 | Two farm animals (not tamable, not leashed) within 16 in a pen fenced on four sides, up to 16 across each way | They walk to a random inside corner and, within 2.5 blocks of it, stand facing into it; let go after 15 s |
| `cat_hiss` | 86 | LIGHT | MOBS | 0.6 | A cat within 16 | The cat hisses |
| `cat_flee` | 129 | LIGHT | MOBS | 0.5 | A standing cat within 16 | The cats run 8 blocks away from the spirit's point for 5 s; one hisses |
| `dog_barks` | 87 | LIGHT | MOBS | 0.6 | A wolf within 16 | Turns away and makes its ambient sound |
| `villager_stares` | 88 | LIGHT | MOBS | 0.6 | A villager within 16 | One villager stops and faces the player for 5 s |
| `villager_refuses` | 192 | LIGHT | MOBS | 0.5 | A villager within 16 | The villager shakes its head for 2 s with the "no" sound |
| `bats` | 89 | LIGHT | MOBS | 0.6 | Free air 4 blocks away | Five bats appear |
| `open_pen` | 90 | HARMFUL | MOBS | 0.6 | A closed gate with an animal within 5 of it | Opens the gate |
| `wild_pet` | 91 | HARMFUL | MOBS | 0.6 | A tamed pet of the player within 32 | Loses its owner and targets the player |
| `copycat` | 92 | HARMFUL | MOBS | 0.6 | Not peaceful, standing room 3 blocks behind | A zombie in a copy of the player's armor, targeting them |
| `golem_flower` | 193 | LIGHT | MOBS | 0.6 | An iron golem within 24 with no target | It holds out its poppy for 15 s, to nobody |
| `dog_growls` | 194 | LIGHT | MOBS | 0.6 | A tamed standing wolf within 16 | It sits, looks at the empty space 2 blocks from the player and growls for 6 s, then stands |
| `angry_bees` | 196 | HARMFUL | MOBS | 0.5 | A bee nest or hive within 16 with bees in it | The bees come out angry at the player, as if it had been broken |
| `leash_snaps` | 181 | HARMFUL | MOBS | 0.5 | A mob tied to a fence post within 16 | The leash comes loose with a snap and drops as an item |
| `goat_rams` | 197 | HARMFUL | MOBS | 0.4 | An adult goat 4 to 14 blocks away | The goat's ram memory is set to the player's position, so it charges and knocks them back |
| `boat_drifts` | 180 | LIGHT | WATER | 0.5 | An empty boat floating within 24 | A steady push away from the player for 10 seconds |
| `minecart_rolls` | 179 | LIGHT | MOBS | 0.5 | An empty minecart on a rail within 24 | A steady push toward the player for 5 seconds; the rail steers it |
| `enderman_takes` | 198 | HARMFUL | MOBS | 0.4 | An enderman with empty hands and a placed block (planks, wool, stone bricks, bricks, bookshelf) with an open face, both within 16 | The block turns to air and the enderman holds it; after 60 seconds the block is back and the enderman's hands are empty |
| `torch_swap` | 112 | LIGHT | LIGHTS | 0.6 | A torch, wall torch or lantern within 10 | Becomes another kind (torch, soul, copper; lantern, soul lantern), facing kept; swapped back after 60 seconds |
| `repaint` | 104 | LIGHT | BLOCKS | 0.5 | A dyed block (wool, concrete, terracotta, stained glass; stairs and slabs of wool and concrete too), planks, cobblestone, stone bricks or deepslate bricks, tiles and cobbled deepslate (with their stairs, slabs and walls) within 10 | Up to 40 blocks of that one kind turn into the neighbouring colour (dyes form a ring), planks into the stripped log of their wood, stone bricks into stone, cobblestone and stone bricks into their mossy or cracked kind and back, one deepslate kind into another; swapped back after 60 seconds |
| `edge_figure` | 94 | LIGHT | PRESENCE | 0.4 | No such figure already, standing room 16 blocks away, 45–65° to a side of where the player looks | A silent, unhurtable mannequin with the player's own skin stands there facing them. Gone, with no sound or particles, when the player looks within 25° of it with a clear line, or after 30 s |
| `fog_silhouette` | 95 | LIGHT | PRESENCE | 0.3 | Open sky and rain or darkness outside, standing room 40 blocks away within 25° of where the player looks | A mannequin in the default skin stands far off. Gone, silently, when the player comes within 24 blocks, or after 60 s |
| `distant_player` | 123 | LIGHT | PRESENCE, PLAYER | 0.3 | Standing room 60 blocks away within 40° of where the player looks (up to 16 bearings tried; grass and flowers count as room) | A silent, unhurtable mannequin in a random default skin (Steve, Alex and the rest) stands there facing them. Gone, silently, when the player comes within 35 blocks, or after 60 s |
| `distant_walker` | 120 | LIGHT | PRESENCE, PLAYER | 0.3 | Standing room 45 blocks away within 40° of where the player looks (up to 16 bearings tried; grass and flowers count as room) | A silent, unhurtable mannequin in a random default skin walks across the player's view at walking pace, or (40%) sprints at 0.28 blocks/tick and jumps now and then; it jumps up one-block steps, falls off ledges up to 2 deep, goes through grass and flowers, and turns up to 80° off to go round what is in its way. Gone, silently, when the player comes within 25 blocks, when no way on is left, or after 30 s |
| `nametag` | 119 | LIGHT | PRESENCE, PLAYER | 0.3 | Half the time standing room 30 blocks away within 40° of where the player looks; otherwise none | A nameplate (text display, seen through walls) with a made-up nickname (as spirits get, at most 16 characters) floats 3 blocks under the player's feet or at head height over that standing room, with nobody under it. Gone after 10 s |
| `pillar` | 121 | LIGHT | PRESENCE, PLAYER | 0.3 | Standing room 24 blocks away within 40° of where the player looks, open for 8 blocks above | A silent, unhurtable mannequin in a random default skin, cobblestone in hand, every 0.5 s looks down at its feet and jumps (a player's jump arc), puts cobblestone (place sound) where its feet were once above it, swings its arm and lands on it, 8 times; at the top it faces the player. It waits at the top and is gone 7 s after it appeared; sooner if the player comes within 10 blocks or the way up is blocked. Each block goes back to air after 2 min if still cobblestone; under hardcore the pillar stays |
| `harvest` | 122 | LIGHT | PRESENCE, PLAYER | 0.3 | A ripe crop within 16 blocks with standing room within 4 blocks of it, at least 8 blocks from the player | A silent, unhurtable mannequin in a random default skin stands there and every 0.4 s turns to the nearest ripe crop within 4 blocks, swings and breaks it (particles and sound, no drops). Gone when none is left, when the player comes within 5 blocks, or after 10 s. Each crop comes back at its age after 2 min if its spot is still empty; under hardcore they stay gone |
| `armor_stand` | 128 | HARMFUL | PRESENCE | 0.2 | No stand already following the player, standing room 8 blocks away 150–180° from where the player looks (behind) | A real armor stand with arms, in one of four poses (arms reaching out, arms raised, head lolling, a lunge), stands there facing the player. Every 0.2 s, while the player is not looking at it (within 50° and in line of sight) and is more than 2 blocks away, it takes a 0.6-block stride toward them, climbing 1 and dropping 3, and (25%) strikes another pose. Breaking it drops nothing, not even itself: any blow breaks it at once, with the vanilla break sound and particles. Gone the same way after 2 min; under hardcore it stays where it ended |
| `friend_double` | 201 | LIGHT | PRESENCE, PLAYER | 0.3 | Another online player in the same world within 16 blocks, standing room 50 blocks away within 40° of where the player looks | A silent, unhurtable mannequin with that player's skin and their name over its head stands there facing the player. Gone, silently, when the player comes within 30 blocks, or after 60 s |
| `footprints` | 96 | LIGHT | PRESENCE | 0.6 | Standing on or in sand or snow | For 6 s, puffs of the ground and a step sound appear where the player stood a second ago. Particles only, nothing is placed |
| `cold_breath` | 97 | LIGHT | PRESENCE | 0.6 | Warm biome | A breath puff at the face |
| `local_rain` | 98 | LIGHT | PRESENCE | 0.6 | Sky visible, not raining | Rain particles and sound over the player for 6 s |
| `lost_time` | 99 | HARMFUL | PRESENCE | 0.5 | Night | The overworld clock jumps 1000 ticks (affects everyone) |
| `false_wound` | 153 | LIGHT | AFFLICTION | 0.3 | Health above 1 heart | The client is told health is 4 hearts lower (never below 1 heart) for 1.5 s, then the real value |
| `false_hearts` | 154 | LIGHT | AFFLICTION | 0.3 | No wither or poison | The client shows wither or poison for 5 s; the server has no effect |
| `false_hunger` | 155 | LIGHT | AFFLICTION | 0.3 | Always | The client is told the food bar is empty for 2 s |
| `guardian_face` | 156 | LIGHT | PRESENCE | 0.15 | Always | The elder guardian face and sound, client only |
| `false_totem` | 157 | LIGHT | PRESENCE | 0.15 | Always | The totem of undying animation (a plain totem if none is carried) |
| `subliminal_title` | 158 | LIGHT | PRESENCE | 0.3 | Always | A dark red title from `texts` for 3 ticks with no fade; fade times reset after |
| `red_vignette` | 160 | LIGHT | PRESENCE | 0.3 | Always | The client draws a red tint at the screen's edges, peaking at 80%, fading in and out over 5 s (`RedVignette`). A client without the mod gets its border warning distance raised instead, so the vanilla tint shows (only with the vignette setting on) |
| `own_voice` | 162 | LIGHT | SOUND | 0.2 | Always | A chat line `<name> ...` from `lines`, sent only to the player |
| `false_death` | 163 | LIGHT | PRESENCE | 0.2 | Always | Their own "was slain by ???" death message, sent only to them |
| `private_rain` | 167 | LIGHT | PRESENCE | 0.3 | Weather dimension, not raining, sky visible | Rain for this client only, a storm 30% of the time, for 20 s; then the real weather |
| `silent_lightning` | 168 | LIGHT | PRESENCE | 0.2 | Weather dimension, sky visible | A visual-only bolt 24 blocks away |
| `false_night` | 159 | LIGHT | PRESENCE | 0.2 | Overworld, daytime | Only this player's client is told it is midnight, with the clock stopped. 15 s later it gets the real time back. Nothing on the server |
| `false_slot` | 161 | LIGHT | PRESENCE | 0.3 | An empty inventory or hotbar slot | Only this player's client is told that slot holds an item from a list of 25 (skulls and heads, bones, rotten flesh, spider eyes, a totem, disc 11 and more; a player head is their own). 20 s later, or at any click, the real slot comes back. Nothing on the server |
| `false_advancement` | 165 | LIGHT | PRESENCE | 0.2 | None | Grants a hidden advancement (`restless:whisper/*`, an impossible criterion, no chat line) so the toast shows, and revokes it 2 ticks later |
| `turned_head` | 166 | HARMFUL | PRESENCE | 0.2 | None | A step sound of the ground at the spirit's position (or 6 blocks away at a random bearing), then the camera turns toward it over 30 ticks, eased, a little every frame on the client (`TurnPayload`, `HeadTurn`); the mouse still works meanwhile |
| `stranger_joins` | 124 | LIGHT | PRESENCE, PLAYER | 0.2 | None | Only this player gets "<name> joined the game" and a player list entry for a made-up nickname (as spirits get, at most 16 characters), in a default skin. 40 s later the entry goes and "<name> left the game" follows. Nothing on the server |
| `stranger_death` | 124 | LIGHT | PRESENCE, PLAYER | 0.2 | None | Only this player gets a vanilla death message (fell, drowned, suffocated, fell out of the world, died) for a made-up nickname |
| `false_whisper` | 202 | LIGHT | PRESENCE, PLAYER | 0.0 | Another player online | A gray italic "X whispers to you" line from a real online player; off by default since it impersonates a real person |

## Chances

Not yet recomputed for `phantom`, the `PLAYER` tag or the events added since.

Share of each tag per roll of one spirit, assuming every event can run (in practice many cannot, and the rest renormalise,
so a spirit near no doors never rolls a door). Computed from the weights above.

| Spirit | Power | Lights | Doors | Sound | Affliction | Assault | Items | Blocks | Mobs | Presence |
|---|---|---|---|---|---|---|---|---|---|---|
| `poltergeist` | 0 | 10.8% | 44.7% | 6.9% | 3.4% | 0% | 12.0% | 14.1% | 4.8% | 3.3% |
|  | 0.5 | 9.6% | 40.4% | 5.6% | 4.4% | 2.8% | 15.2% | 13.5% | 5.8% | 2.8% |
|  | 1 | 7.6% | 33.9% | 3.5% | 5.9% | 7.0% | 20.0% | 12.6% | 7.3% | 2.1% |
| `shade` | 0 | 28.7% | 2.9% | 30.5% | 12.1% | 0% | 1.8% | 8.3% | 4.2% | 11.5% |
|  | 0.5 | 25.7% | 2.7% | 25.1% | 15.9% | 5.0% | 2.3% | 8.1% | 5.2% | 10.1% |
|  | 1 | 21.1% | 2.4% | 16.3% | 21.9% | 13.0% | 3.1% | 7.8% | 6.7% | 7.8% |

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

- `haunt/event/Swaps.java`: the one way events show other blocks in place of the real ones. By default an illusion
  for the haunted player alone (a `ClientboundBlockUpdatePacket`, then the real blocks again after the event's ticks);
  with `swaps.mode` set to `real` in `haunt.json`, or under hardcore, a change in the world for everyone through
  `Restore.swap`. Blocks with a block entity are only ever shown. See [hardcore.md](hardcore.md#block-swaps).
- `haunt/Restore.java`: changes that swap a block or an entity (flowers, sign, painting, glass, hole, floor, cross,
  lantern, furnace fuel) register an undo that runs when due and checks that the thing is still what the
  event left, so a block the player has since changed is not overwritten. Block swaps skip neighbour updates (nothing
  attached pops off) and are retried until the chunk is loaded. Everything still pending is undone when the server
  stops (`Restless.onServerStopping`). After a crash the changes stay. Not covered: `map_marker` is permanent.
- `haunt/Hardcore.java`: in a hardcore world nothing is undone. `Restore.swap` and `Restore.revert` (the world-changing
  undos; `Restore.after` stays for cleanup of figures and doubles) register nothing, so flowers, signs, paintings,
  glass, holes, floors, crosses and furnace fuel stay as the event left them, and a flickering lantern stays out. The
  `hardcore.mode` setting in `haunt.json` is `auto` (the world's vanilla hardcore flag), `true` or `false`. It is
  decided the first time it is asked and saved in `HauntData`, so editing the config later does not flip a world.
  Tricks with a real counterpart come true instead of being shown: a lie's wound, hearts, hunger, night or slot,
  real lightning, a floor that breaks, fake blocks placed for real (not over a chest or furnace), a hallucinated mob
  spawned. The list is in [hardcore.md](hardcore.md#events-with-a-real-branch), with the plan and the rules.
- `haunt/DoorLocks.java`: doors locked by `locked_door`, in memory. `Restless.onUseBlock` cancels the right click on
  both sides; the loader forwards its block-interaction event to it. The server sends `DoorLockPayload` to clients
  within 64 blocks, which the loader registers and hands to `DoorLocks.put`. A client that joins or walks up later
  than the lock does not know it and sees the server undo its open attempt.

## Config

Every tunable value lives in `core/src/main/resources/restless-config/`, split into files that are slices of one tree:

| File | Holds |
|---|---|
| `haunt.json` | `director`, `power`, `anger`, `salt`, `wards`, `bell`, `candle`, `compass`, `ash`, `camera`, `spirit_box`, `ouija`, `clues`, `ritual`, `artifacts`, `grave`, `sanity`, `corruption`, `evolution`, `journal`, `hardcore`, `restore`, `swaps`, `place`, `said` |
| `spirits.json` | `spirits` (activity, tag affinities), `traits` (the trait catalog, with each trait's clues, words, hint and weakness), `story` (nickname words, days, causes) |
| `lights`, `mechanisms`, `sounds`, `afflictions`, `assaults`, `items`, `blocks`, `mobs`, `presence`, `lies` | `events.<namespace>:<id>` for that group (`assaults.json` also holds `assaults.safe_health`) |

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
