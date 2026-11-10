# Spirits and haunt events

Reference for the current content. Source of truth is the code:
`spirit/Spirits.java`, `haunt/event/HauntEvents.java`, `haunt/event/*`, `spirit/life/Power.java`, `haunt/Reactions.java`, `spirit/life/Anger.java`, `spirit/Traits.java`, `spirit/Story.java`, `haunt/Director.java`, `haunt/Scheduler.java`.
Update this file when any of them changes. The numbers below are the shipped defaults; see [Config](#config).

## How it works

- A spirit is **personal** (follows one player) or **territorial** (stays at an anchor); the kind is `kind` in `spirits.json`.
  `shade`, `phantom`, `mimic`, `wraith`, `mara`, `banshee`, `revenant`, `lost_one` and `boggart` are personal; `poltergeist`, `nix`,
  `domovoi`, `hoarder`, `demon` and `trickster` are territorial.
- Each player gets 1 personal spirit (`director.default_spirits`) on first check, chosen uniformly at random, and keeps it.
  A player never has two spirits of one type, whether they come from the first check, a deed or `/haunt spirit add`. The list can grow or shrink, down to 0, via `/haunt spirit add|remove`. A poltergeist
  in an older save is dropped.
- A territorial spirit has an anchor, a dimension and a radius (`director.territorial_radius`, 48 blocks), saved per world.
  When its cooldown expires it acts on one random player (survival or adventure) inside the radius, and does nothing, but
  still reschedules, if nobody is there.
- A territorial spirit can be born with a structure. When a chunk is generated for the first time, each structure start
  in it listed under `births.structures` rolls its `chance`; the spirit is anchored at the centre of the structure's
  box, unless another territorial spirit of that dimension is within `births.structure_spacing` (96) blocks. Each
  structure rolls once, ever; chunks generated earlier stay empty. `/haunt birth structure` does it for the structure
  you stand in, without the roll.
- Every spirit has a location. A personal spirit roams in a ring `location.min_distance` to `location.max_distance`
  blocks from its player, drifting `location.step` blocks per second, and is forgotten on restart; a territorial spirit
  stays at its anchor. Sound events with no fixed source (creeper hiss, crying child, eating behind a wall, enderman
  hiss, chest opening, distant bell, phantom jukebox, death cry, dripping, block breaking, bottle shatter, and the knocks when no door
  is near) come from that point, so a territorial spirit's sounds are only heard near its anchor. Events started by hand
  with `/haunt force` have no spirit and keep a random point around the player. `/haunt info` shows a personal spirit's
  current point as `at=`.
- Every spirit has its own cooldown. When it expires, the Director picks one event for that player using that spirit's
  affinities and schedules the spirit's next one. More spirits means proportionally more events.
  Spectators and creative players get no event, but the cooldown still resets. If the spirit had somebody to haunt but
  no event could run (nothing to act on, or the event failed), it tries again after `director.retry_ticks` (600, 30 s)
  instead of a full cooldown; a ward-stopped event still costs the full cooldown.
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

Anger is saved per spirit. It goes up from what the player does and down only when a spirit is satisfied (see the table below). The amounts are the `anger` config:

| Source | Amount | Who takes it |
|---|---|---|
| Killing a cat | `kill_cat` 0.15 | all the player's spirits; territorial ones if it happened in their radius |
| Breaking a grave (`graves`: skeleton and wither skeleton skull, bone block) | `break_grave` 0.2 | same |
| Lighting a log or leaves with flint and steel or a fire charge | `burn_forest` 0.1 per ignition | same |
| Breaking a block within `anchor_reach` (3) of a territorial spirit's anchor | `break_anchor` 0.3 | that spirit only |

Two things lower it, in `assaults.json`:

| Source | Amount | Who loses it |
|---|---|---|
| A lethal event killed the player (they die within `relief_window_ticks`, 2400, of it) | `death_relief` 0.5 | the player's spirits, and territorial ones whose radius covers the death |
| An avenger (`the_avenger`) beaten | its own amount | same |

Anger is floored at 0 and capped at 1. A territorial spirit takes nothing from a player outside its radius, and a player's actions never
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

A trait lists which of four clues it leaves (`clues` in `spirits.json`). Every tool answers whenever a spirit is near:
the traits that leave the clue are only likelier, by `clues.preferred_weight` (3 against 1), to be the one shown. The
tools read the player's own spirits and the territorial ones whose radius holds them.

| Clue | Tool | What it shows |
|---|---|---|
| footprints | **Ash powder** (charcoal, makes 4) | Placed flat like salt. After `ash.reveal_ticks` (30 s), if a spirit that leaves footprints is within `ash.reach` of it, it takes one of four prints (bare foot, paw, hoof, drag). Each trail shows one random trait of the spirit, as the print of that trait's nature: mirror a bare foot, iron a paw, holy water a hoof, glowstone a drag. |
| silhouette | **Camera obscura** (iron, glass pane, black dye) | Use it: a **photograph** with the spirit's type only (no trait, no weakness), `camera.cooldown_ticks` apart. The figure on the picture is drawn from the spirit's type (build: hooded, hunched, horned...) and its seed (small differences), so the same spirit always looks the same (`photo/Apparition.java`). |
| voice | **Spirit box** (block: iron, redstone, note block) | Say something in chat within `spirit_box.radius` of it: `spirit_box.delay_ticks` later (40) it answers with a word of one trait. With [Plasmo Voice](https://github.com/plasmoapp/plasmo-voice) (2.2.0-beta.2+) on the server, finishing a phrase on voice chat does the same; it hears that you spoke, not the words. |
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

Every trait names a **weakness** (`weakness` in `spirits.json`): iron, glowstone, holy water or a mirror, four traits
each. A spirit has a **nature**, picked from its seed evenly among the four: all its traits but one come from that
nature's group and one is foreign, so it is weak to the nature (`Traits.weakness` counts votes; a spirit from before
natures keeps its old vote, ties to the first in that order). Nothing shows the nature directly: the player reads the
traits (ouija board, spirit box, ash) and finds the group two of them share.

The type adds a **form** (`form` in `spirits.json`): `whole`, `bit` or `crafted`. The bestiary card, which a photo
opens, names it. The offering is the nature's category in a form:

| Nature | whole | bit | crafted |
|---|---|---|---|
| iron | iron ingot | iron nugget | iron chain |
| glowstone | glowstone | glowstone dust | lantern |
| holy water | **holy water** | salt | bell |
| mirror | **mirror** | glass pane | spyglass |

whole: poltergeist, wraith, demon, revenant, hoarder. bit: shade, mara, banshee, phantom, lost_one. crafted: nix, mimic,
domovoi, trickster, boggart.

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
with an offering from the table above (the **mirror** is made from a glass pane and two iron nuggets) takes it and calls
the first spirit near the player whose nature is its category:

- The ritual runs `ritual.waves` (12) waves, plus `evolution.extra_waves` for a grown spirit and `ritual.off_form_waves`
  (6) when the form is not the type's of `ritual.wave_ticks` (5 s). Each wave the spirit may snuff one of the
  candles (`ritual.snuff_chance`) and may act on the player at once, past every ward (`ritual.fury_chance`).
- At the next wave every candle must be lit again, the focus still on the altar and the player within `ritual.radius`
  (16). Otherwise the ritual breaks: the focus is lost and the spirit's anger rises by `ritual.fail_anger`.
- After the last wave the spirit is gone for good and drops its **essence** (`<type>_essence`).
- No spirit near has that nature: one that is not wary eats the offering, its anger goes to 1, it strikes at once with a
  harmful event, every candle goes out, it turns **wary** for `ritual.wary_ticks` (a day) and is aged by
  `ritual.wrong_age_ticks` (a day) toward evolving. A wary spirit refuses the altar (the offering is kept), including
  its own offering.

Altars with nothing to answer, or missing candles or salt, refuse the offering. Empty-handed use takes the item back.
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

The **journal** (book, salt, ink sac) is put in every new player's inventory (the `journal_on_join` setting) and can be crafted
again. It opens as a book; its notebook is saved per player with the world (`notebooks` in the haunt data), not on the
item, so any journal shows it. It keeps what the player saw, never conclusions: it does not work out a weakness or a type.

Notes are typed (`Notebook.Kind`) and tied to the spirit they came from: a photo (type only), a print in ash
(every player within `ash.reach` of it), a spirit box word, the planchette's letters, the spirit box's word of a grave, a
read gravestone, a dream, and the chronicle kinds: rested, banished, evolved. Each new note turns a page aloud; the same
note is never written twice. `JournalPages` lays the notebook out as:

- **Contents**: one clickable line per non-empty section.
- **The last owner's notes**: the guide, chapters opened as the player runs into what each is about (`Notebook.Chapter`):
  `start` with the journal, `haunted` at the first event a spirit runs on them, `clues` at the first clue, `weakness` at
  the first photo (five pages: natures, ash, forms, the cost of a mistake), `graves` at the first grave or word of one, `sanity` when sanity first drops below
  `sanity.overlay_below`, `evolution` at the first grown spirit, `essence` at the first spirit gone for good. Opening one
  says so on the action bar.
- **Spirits**: a dossier per spirit the player has *met* (photographed, or its grave read), named once its grave is
  read and otherwise "an unnamed <type>", numbered when several share a type. It lists every note about that spirit,
  including ones written before they met. Spirit box words and the planchette are marked "(it may lie)".
- **Archive**: dossiers the player filed a banishment under. A banishment (chronicle) does not find its dossier by
  itself: under each unfiled dossier of the same type (or any, when either side has no type) a clickable line offers
  "Banished: <name>", once per banishment still unfiled. Clicking files that banishment under that dossier for good, and
  the dossier moves from *Spirits* to *Archive* under the name the banishment gave. The book does not check that they are
  the same spirit, so a wrong guess stays wrong. Nothing is archived until the player files it, and a rested spirit is
  not archived.
- **Loose notes**: clues of spirits not met yet, without saying whose. They move to the dossier on meeting.
- **Bestiary**: a page per type the player has photographed, with its grown name once a grown one was photographed.
- **Charms / Relics**: a page per charm (basic then advanced) and relic, built on the server by `journal/Codex.java`
  (not stored): the item's `.tip` line and a recipe row (ingredient icons, result, names). A page opens when
  `Notebook.knows(type)` holds (photographed or banished) for every `<type>_essence` in the item's recipe; an advanced
  charm also needs its base charm's page. The ids are the lists in `Codex`: add a new charm or relic there.
- **Recipes**: the player's recipe book entries (`restless:` recipes whose id is an item id), which diary pages fill.
  Reading a page re-syncs the journal.
- **Chronicle**: rested, banished, evolved, and entries from saves before typed notes (read once from the old `journal`
  field).

`journal.entries_per_page` notes form a source page, `journal.traits_per_page` bestiary traits.
The screen splits these further when the text exceeds the available space. Texts are `journal.restless.*`
in the item lang files.

The journal opens a dedicated parchment screen with section bookmarks. Only discovered sections are sent to the
reader. Text wraps and splits into additional pages at the current GUI size and font; wheel and Page Up/Page Down
turn pages, Tab navigates controls, and Escape closes it. The screen supports narration.

Not built yet: illustrated photos and footprint stamps, portraits in chests,
the other quest kinds (bury remains, a letter to a descendant, a named killer mob, a killer spirit), hallucinations at
very low sanity, and research: the journal unlocks no recipes.

### Diary pages

A **torn page** is how a player learns a gadget's recipe. It sits in chests of structures: mineshafts, dungeons, desert
and jungle temples, mansions, pillager outposts, ruined portals and village houses. A chest rolls one with chance 0.3,
and the page names one recipe (`restless:recipe`): spirit box, camera obscura, Ouija board or ash. Using it adds that
recipe to the recipe book and uses the page up; a page of a recipe already known stays in hand. The page is titled by the
item the recipe makes, so a page's recipe id must equal that item's id.

Which chests and which recipes are datapack: `data/restless/loot_modifiers/diary_pages.json` (the structure chests) and
`data/restless/loot_table/chests/diary_pages.json` (the pages). Recipes can still be crafted without a page; the page only
shows them.

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

A spirit `evolution.days` (10) in-game days old grows up, once and for good: a poltergeist turns **raging**, a shade a
**light-eater**, a phantom a **doppelganger**, a nix a **drowner**, a mimic a **skinwalker**, a wraith a **dread wraith**, a mara a
**night hag**, a banshee a **keening banshee**, a revenant an **avenger**, a lost one the **forsaken**, a domovoi a **house master**,
a hoarder a **glutton**, a demon an **archdemon** and a trickster a **jester**. Its power never drops below `power_floor` (0.7), its ritual takes `extra_waves` (4) more waves, and a
territorial one tries `corruption.evolved_factor` (3) times as many columns. The players it haunts (its own player, or
those in its radius) hear a heartbeat, are told, and it goes in their journal; from then on it goes by its grown name.
A personal spirit grows only while its player is online.

Not built yet: world-born spirits (death spots, blood moon), merging, the calendar (full
moon, blood night, day of the dead), possessed mobs in corrupted land, crops stopping, and new events or clues
for evolved spirits.

## Spirits

| Id | Description | Activity | Affinity (missing = ×1) |
|---|---|---|---|
| `poltergeist` | Noisy and physical. Mostly messes with doors and belongings; rarely hurts at first. Signatures: `poltergeist_toss`, `poltergeist_play`, `poltergeist_tantrum`. | 1.3 | doors ×4, items ×3, blocks ×1.5, afflictions ×0.5, assaults ×0.5, sounds ×0.5, presence ×0.5 |
| `shade` | Quiet and oppressive. Snuffs lights, whispers and afflicts, turns violent as its power grows. Signatures: `shade_gathered`, `shade_glance`, `shade_room`. | 0.8 | lights ×3, sounds ×2.5, afflictions ×2, presence ×2, assaults ×1, doors ×0.3, items ×0.5 |
| `phantom` | Likes to be seen, as other players: far-off strangers, a name in the player list, someone pillaring or taking the crops. Seldom hurts. Signatures: `phantom_self_join`, `phantom_rerun`, `phantom_home`. | 1.0 | player ×6, presence ×2, doors ×0.5, lights ×0.5, afflictions ×0.5, items ×0.5, assaults ×0.3 |
| `nix` | Territorial, bound to water. Keeps swimmers under, calls players to the water, kills fish and breaks boats. Place it by a lake or the sea with `/haunt spirit add nix` while standing there. Signatures: `nix_catch`, `nix_call`, `nix_returns`. | 1.0 | water ×8, afflictions ×1, assaults ×0.5, sounds ×0.5, blocks ×0.3, presence ×0.3 |
| `mimic` | Fakes things: blocks that are not there, strangers, mobs that vanish. Signatures: `mimic_voice`, `mimic_parrot`, `mimic_chest`. | 1.0 | blocks ×3, player ×2, presence ×1.5, mobs ×1.5, lights ×0.5, doors ×0.5, assaults ×0.5 |
| `wraith` | Hunts in the dark: snuffs lights and hurts. Signatures: `wraith_taken`, `wraith_steps`, `wraith_edge`. | 2.0 in the dark (`when: dark`), 1 elsewhere | assaults ×2.5, lights ×2, afflictions ×1.5, presence ×1.5, doors ×0.5, blocks ×0.5, items ×0.3 |
| `mara` | Rides the sleeper: afflictions and presence at night, may act when the player uses a bed (`reacts: bed`). Signatures: `mara_dream`, `mara_stolen_rest`, `mara_weight`. | 1.8 at night (`when: night`), 1 by day | afflictions ×3, presence ×2, sounds ×1.5, doors ×0.5, items ×0.3, blocks ×0.3 |
| `banshee` | A voice: mostly sounds, rarely touches anything. Signatures: `banshee_letter`, `banshee_gift`, `banshee_serenade`, `banshee_jealous`. | 0.9 | sounds ×5, presence ×1.5, doors ×0.5, items ×0.3, blocks ×0.3, assaults ×0.3 |
| `revenant` | Vengeful: hurts and sends mobs, may act after the player kills a mob (`reacts: kill`). Signatures: `revenant_ledger`, `revenant_tally`, `revenant_rises`. | 1.0 | assaults ×3, mobs ×2.5, afflictions ×1, lights ×0.5, doors ×0.3, items ×0.3 |
| `lost_one` | Wanders: figures, strangers and steps, may act when the player goes below y 0 (`reacts: deep`). Signatures: `lost_one_sign`, `lost_one_wanderer`, `lost_one_death_path`. | 0.9 | presence ×3, player ×2, sounds ×1.5, blocks ×0.5, items ×0.5, assaults ×0.2 |
| `domovoi` | Territorial house spirit: doors, belongings and household noises, hardly harmful. Signatures: `domovoi_supper`, `domovoi_hearth`, `domovoi_tidy`. | 0.7 | doors ×2, items ×2, sounds ×1.5, mobs ×0.5, afflictions ×0.3, assaults ×0.2 |
| `hoarder` | Territorial: takes and moves items. Signatures: `hoarder_payment`, `hoarder_shiny`, `hoarder_nest`. | 1.2 | items ×5, blocks ×1.5, doors ×1.5, lights ×0.5, presence ×0.5, assaults ×0.3 |
| `demon` | Territorial and dangerous: assaults, afflictions and mobs. Signatures: `demon_offer`, `demon_trail`, `demon_contract`. | 1.1 | assaults ×3, afflictions ×2, mobs ×2, lights ×1.5, doors ×0.5, items ×0.5 |
| `trickster` | Territorial prankster: changes blocks and items, shows things. Signatures: `trickster_gift`, `trickster_dig`, `trickster_dud`. | 1.2 | blocks ×2.5, items ×2, mobs ×1.5, presence ×1.5, assaults ×0.2 |
| `boggart` | Illusions that disgust: swarms of tiny spiders and silverfish that only the player sees. Signatures: `boggart_spiders`, `boggart_crawl`, `boggart_silverfish`. | 1.1 | mobs ×3, presence ×3, sounds ×2, blocks ×1.5, lights ×1, afflictions ×0.5, assaults ×0.3, doors ×0.3, items ×0.3 |

Affinity is a weight multiplier per event tag. Activity 1.3 means 1.3× as often.

Besides their signature events (below), the ten types after `nix` only pick from the existing events. Their own gimmicks from [design.md](design.md) are not built:
a fake chat with a name of its own (mimic), the nightmare room (mara), honest warnings (banshee), birth from killing
(revenant), winning a domovoi with offerings, a hoarder's nest that banishing empties, pacts with a real term (demon)
and fake gifts beyond the one chest (trickster). None has an essence or artifact.

## Events

`Idea` is the number in [event-ideas.md](event-ideas.md).

Signature events: an event with `spirit` in its config is rolled by that type only (`HauntEvent.only`, checked with the
other free checks in `Director.open`; `/haunt force` ignores it). Its `weight` is raised (2.0, against 0.3 for the
average event) so that a player sees it. All 43 are built; the plan is in
[signature-events.md](signature-events.md). A scenario's `spirit` is not a filter: it only boosts an evolved spirit's
own scenario.

Text events (`whisper`, `own_voice`, `subliminal_title`, `false_death`, `false_advancement`, `stranger_death`, `book_page`,
`new_sign`, `cave_sign`, `item_trail`) never repeat a variant for the same spirit: each spirit remembers what it has said through each of
them. Once every variant is used, the event cannot run for that spirit for `said.rest_ticks` (7 in-game days), then starts
over. A text event with no spirit behind it (command, debug menu) counts against the player instead. Saved with the world.

| Id | Idea | Tier | Tag | Base weight | Runs when | Effect |
|---|---|---|---|---|---|---|
| `extinguish_light` | | LIGHT | LIGHTS | 1.0 | A lit candle or a torch / soul torch (floor or wall) is within 10 blocks | Snuffs a lit candle; a torch is knocked off and drops as an item |
| `door` | | LIGHT | DOORS | 1.0 | A wooden door is within 12 blocks | Opens a closed door or slams an open one |
| `blindness` | | LIGHT | AFFLICTION | 0.6 | Player is not already blind | Blindness for 6 s and a low cave ambient sound |
| `sanity_crash` | — | HARMFUL | AFFLICTION | 0.3 | Survival or adventure player with sanity above zero | Removes 0.9 sanity plus normal event shock (0.04), leaving at most 0.06 with defaults; clamped at zero. Uses normal sanity sync and journal unlock. Mara favors it through AFFLICTION affinity |
| `sanity_drain` | — | HARMFUL | AFFLICTION | 0.6 | Survival or adventure player with sanity above zero | Removes a uniformly random 0.1–0.25 sanity plus normal event shock (0.04); clamped at zero. Range is configured by min_loss and max_loss. Uses normal sanity sync and journal unlock |
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
| `cut_off_scream` | 135 | LIGHT | SOUND | 0.25 | Always | A pre-cut scream (custom sound) 12 blocks away |
| `long_scream` | — | LIGHT | SOUND | 0.25 | Always | A sustained scream (custom sound) 12 blocks away |
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
| `moving_tree` | 108 | LIGHT | BLOCKS | 0.3 | A log on earth (dirt, grass, moss, mud) within 16 blocks | Takes a log standing on earth, the logs joined to it and the non-persistent leaves that grew from them (a leaf no nearer to these logs than its own distance to a log, so touching crowns stay; up to 300 blocks, with a crown), and moves them 2 to 4 blocks in a random direction if every spot is open (air, grass, flowers) and the foot stands on earth; a rustle sounds. 30 s later it moves back, and the grass and flowers it stood in come back, unless it was cut or built on meanwhile. Under hardcore it stays |
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
| `infested_stone` | 191 | HARMFUL | BLOCKS | 0.4 | Stone, cobblestone, deepslate or stone bricks in sight within 6 | The block is shown as its infested kind (a swap, see the `swaps` setting); restored after 30 s |
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
| `golem_rage` | — | HARMFUL | MOBS | 0.4 | A living iron golem within 24 with AI, no target and able to attack this player; no creative, spectator or peaceful | The nearest eligible golem stops offering its flower and attacks the player, with vanilla persistent anger. Player-built golems remain friendly |
| `golem_flower` | 193 | LIGHT | MOBS | 0.6 | An iron golem within 24 with no target | It holds out its poppy for 15 s, to nobody |
| `wolf_rage` | — | HARMFUL | MOBS | 0.4 | Living wild wolves within 16 with AI, no target and able to attack this player; no creative, spectator or peaceful | All eligible wolves attack the player with vanilla persistent anger. Tamed wolves remain friendly |
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
| `pillar` | 121 | LIGHT | PRESENCE, PLAYER | 0.3 | Standing room 24 blocks away within 40° of where the player looks, open for 8 blocks above | A silent, unhurtable mannequin in a random default skin, a block in hand (cobblestone, stone, dirt or any planks), every 0.5 s looks down at its feet and jumps (a player's jump arc), puts that block (its place sound) where its feet were once above it, swings its arm and lands on it, 8 times; at the top it faces the player. It waits at the top and is gone 7 s after it appeared; sooner if the player comes within 10 blocks or the way up is blocked. Each block goes back to air after 2 min if still that block; under hardcore the pillar stays |
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
| `turned_head` | 166 | HARMFUL | PRESENCE | 0.2 | None | A step sound of the ground at the spirit's position (or 6 blocks away at a random bearing), then the camera turns toward it, up or down as well, over 30 ticks, eased, a little every frame on the client (`TurnPayload`, `HeadTurn`); the mouse still works meanwhile |
| `stranger_joins` | 124 | LIGHT | PRESENCE, PLAYER | 0.2 | None | Only this player gets "<name> joined the game" and a player list entry for a made-up nickname (as spirits get, at most 16 characters), in a default skin. 40 s later the entry goes and "<name> left the game" follows. Nothing on the server |
| `stranger_death` | 124 | LIGHT | PRESENCE, PLAYER | 0.2 | None | Only this player gets a vanilla death message (fell, drowned, suffocated, fell out of the world, died) for a made-up nickname |
| `false_whisper` | 202 | LIGHT | PRESENCE, PLAYER | 0.0 | Another player online | A gray italic "X whispers to you" line from a real online player; off by default since it impersonates a real person |
| `distant_warden` | 312 | LIGHT | SOUND | 0.25 | Always | A warden roar, then a sonic boom 2 s later, from 40 blocks away; loud enough to carry |
| `sky_dragon` | 313 | LIGHT | SOUND | 0.2 | Always | An ender dragon growl from 30 blocks away and 25 above, loud enough to carry |
| `distant_wither` | 314 | LIGHT | SOUND | 0.2 | Always | A wither spawn sound from 45 blocks away, loud enough to carry |
| `end_portal_open` | 315 | LIGHT | SOUND | 0.25 | Always | An end portal opening sound from 30 blocks away |
| `sky_ghast` | 316 | LIGHT | SOUND | 0.25 | Always | A ghast scream from 20 blocks away and 14 above |
| `fox_screech` | 317 | LIGHT | SOUND | 0.3 | Not bright outside (night or storm) | A fox screech 12 blocks away |
| `rabbit_death` | 318 | LIGHT | SOUND | 0.3 | Always | A rabbit death sound 4 blocks away |
| `phantom_hurt` | 319 | LIGHT | SOUND | 0.3 | Always | The player hurt sound at the player; no damage |
| `phantom_tool_break` | 320 | LIGHT | SOUND | 0.3 | Always | The item break sound at the player; nothing is damaged |
| `elytra_overhead` | 321 | LIGHT | SOUND | 0.25 | Always | Twelve elytra wind sounds sweeping 45 blocks each way across a line 10 blocks up |
| `nether_ambience` | 322 | LIGHT | SOUND | 0.25 | Always | Six mood and additions sounds of a soul sand valley or basalt deltas, 2 s apart, at the player |
| `equip_behind` | 323 | LIGHT | SOUND | 0.3 | Always | Four equip sounds of one armor material, 0.3 s apart, 1.5 blocks behind the player |
| `phantom_fishing` | 324 | LIGHT | SOUND | 0.3 | Always | Bobber throw, splash and retrieve sounds 1.2 s apart, 6 blocks away |
| `zombie_at_door` | 325 | LIGHT | SOUND | 0.3 | A wooden door within 12 | Four door-battering sounds and a break sound at a wooden door within 12 |
| `phantom_anchor` | 326 | LIGHT | SOUND | 0.3 | Always | A respawn anchor charge or deplete sound 5 blocks away |
| `bucket_spill` | 327 | LIGHT | SOUND | 0.3 | No open sky above the player | A bucket empty sound 3 blocks away |
| `lost_villager` | 328 | LIGHT | SOUND | 0.3 | Always | A villager idle sound 4 blocks behind the player |
| `phantom_arrow` | 329 | LIGHT | SOUND | 0.3 | A wall in front of the player | An arrow hit sound at a one-block-thick wall in front of the player |
| `phantom_glass` | 330 | LIGHT | SOUND | 0.3 | Glass within 12 | Break particles and sound of a glass block or pane within 12; it stays |
| `pen_cries` | 331 | LIGHT | SOUND | 0.3 | An animal within 16 | A sheep, piglet or cow death sound at an animal within 16 |
| `sniffing_behind` | 332 | LIGHT | SOUND | 0.3 | Always | A sniffer sniff 1 block behind the player |
| `distant_goat_horn` | 333 | LIGHT | SOUND | 0.25 | Always | A random goat horn call from 40 blocks away, loud enough to carry |
| `false_raid_horn` | 334 | LIGHT | SOUND | 0.2 | Always | A raid horn from 35 blocks away |
| `heavy_splash` | 335 | LIGHT | SOUND | 0.3 | Water within 16 | A low splash at water within 16 |
| `distant_ravager` | 336 | LIGHT | SOUND | 0.25 | Always | A ravager roar from 30 blocks away, loud enough to carry |
| `golem_circling` | 337 | LIGHT | SOUND | 0.25 | Always | Fourteen golem steps on a point circling 7 blocks out at ground level |
| `burp_behind` | 338 | LIGHT | SOUND | 0.3 | Always | A player burp 1.5 blocks behind the player |
| `bubble_columns` | 339 | LIGHT | SOUND | 0.3 | Always | Six bubble column sounds, 0.5 s apart, 1 block from the player |
| `lava_pops` | 340 | LIGHT | SOUND | 0.3 | Always | A lava ambient sound and three pops, 0.7 s apart, 2 blocks away |
| `chain_rattle` | 341 | LIGHT | SOUND | 0.25 | Always | Twelve chain step sounds on a point circling 4 blocks out at ground level |
| `pumpkin_head` | 342 | LIGHT | PRESENCE | 0.2 | Always | Only this player's client is told the head slot holds a carved pumpkin, so the pumpkin blur covers the screen. 4 s later the real slot comes back. Nothing on the server. In hardcore, a real pumpkin is put on an empty head slot and taken off again |
| `xp_drain` | 343 | LIGHT | PRESENCE | 0.2 | Has experience points or levels | The client is told the experience bar and level are zero for 3 s, then the real values |
| `phantom_flames` | 344 | LIGHT | PRESENCE | 0.2 | Not on fire, not in water | The client is told the player is on fire for 3 s, so the fire shows on screen. Nothing burns; the real flags come back after. In hardcore, the player is set on fire for 1 s |
| `phantom_drowning` | 345 | LIGHT | PRESENCE | 0.2 | Full air | The client is told the air supply drains from full to 20% over 4 s, so bubbles show on land; then the real value |
| `phantom_frost` | 346 | LIGHT | PRESENCE | 0.2 | Not freezing | The client is told the frozen ticks rise to full over 2.5 s, so frost creeps over the screen, held until 5 s; then the real value. In hardcore, the player really freezes |
| `moon_phase` | 350 | LIGHT | PRESENCE | 0.2 | Overworld, night | Only this player's client is shown the clock moved a random 1 to 7 whole days on, so the moon has another phase and the time of day is the same. 20 s later it gets the real time back. Nothing on the server |
| `false_boss_bar` | 352 | LIGHT | PRESENCE | 0.2 | Always | A purple boss bar with a name from `names` is sent to this player only. It fills over 3 s, empties over 2 s and is removed. No boss exists |
| `action_bar_whisper` | 353 | LIGHT | PRESENCE | 0.3 | Always | A dark red line from `lines` ("Don't turn around" and others) on the action bar, cleared after 2 s |
| `false_absorption` | 356 | LIGHT | PRESENCE | 0.2 | No absorption | The client is told the player has 8 absorption (4 golden hearts), draining to 0 over 4 s; then the real value |
| `empty_armor_bar` | 357 | LIGHT | PRESENCE | 0.2 | Wearing armor | The client is told the armor attribute is 0 for 4 s, so the armor bar vanishes; then the real attribute |
| `hurt_tilt` | 359 | LIGHT | PRESENCE | 0.3 | Always | The hurt animation (screen tilt) from a random side, with no damage |
| `own_hand_swings` | 360 | LIGHT | PRESENCE | 0.2 | Always | The player's own main hand swings twice, 14 ticks apart |
| `night_eyes` | 361 | LIGHT | PRESENCE | 0.2 | No night vision or darkness | The client is told of 1.5 s of night vision, then 4 s of darkness; then the real effects. No effect exists on the server |
| `dead_silence` | 362 | LIGHT | SOUND | 0.2 | Always | A stop-all-sounds packet every tick for 5 s, so nothing plays |
| `all_on_cooldown` | 363 | LIGHT | PRESENCE | 0.2 | An item on the hotbar or in the hands | Every item on the hotbar and in the hands is shown on a 2 s cooldown. The client counts it down by itself |
| `noon_moon` | 365 | LIGHT | PRESENCE | 0.2 | Overworld, daytime | Only this player's client is told it is midnight under a full moon, with the clock stopped. 10 s later it gets the real time back. Nothing on the server |
| `elytra_flicker` | 421 | LIGHT | ITEMS | 0.4 | Elytra worn, with durability to spare, and the player 12 or more blocks above the ground | Only this player's client is told the elytra is almost broken; 2 s later it gets the real one back. Under hardcore the elytra is worn down for real (10%, never breaks) |
| `stew_bowl` | 424 | HARMFUL | ITEMS | 0.4 | Mushroom, beetroot, rabbit or suspicious stew in the inventory | One stew becomes an empty bowl, with an eating sound |
| `bucket_pufferfish` | 427 | LIGHT | ITEMS | 0.4 | A water bucket in the inventory | Only this player's client is told it is a pufferfish bucket; 5 s later it gets the real one back. Under hardcore the stack is swapped for real and stays |
| `torch_burn` | 428 | HARMFUL | ITEMS | 0.4 | A stack of torches or soul torches of 2 or more | Half of the stack is gone, at least one is left, with a hiss |
| `bundle_bone` | 429 | LIGHT | ITEMS | 0.4 | A bundle that has room for a bone | One bone is added to its contents (nothing is taken out) |
| `shield_ghost` | 430 | LIGHT | ITEMS | 0.4 | A shield in either hand | The sound of a shield blocking a hit, with nothing there |
| `item_throwback` | 433 | LIGHT | ITEMS | 0.4 | A dropped item 3–12 blocks away | One of them is thrown at the player, slowly enough to dodge |
| `phantom_horn` | 436 | LIGHT | ITEMS | 0.3 | A goat horn with a tune in the inventory | The tune of that horn sounds at the player |
| `armor_dye` | 437 | LIGHT | ITEMS | 0.4 | A leather helmet, chestplate, leggings or boots (worn or carried) that is not black | It is dyed black, and its old colour comes back after 5 min wherever the player carries it then. Under hardcore it stays black |
| `item_glint` | 438 | LIGHT | ITEMS | 0.4 | An item that does not glint | It glints as if enchanted for 10 s, then stops |
| `item_lore` | 439 | LIGHT | ITEMS | 0.4 | Any item in the inventory or worn | A lore line ("it remembers you" and the like) is added, and removed after 5 min wherever the player carries it then. Under hardcore it stays |
| `tool_mirage` | 440 | HARMFUL | ITEMS | 0.4 | A damageable tool with durability to spare | Only this player's client is told it has full durability, 3 s later that it is almost broken, 3 s later the truth. Under hardcore the tool is worn down for real (30%, never breaks) |
| `hook_click` | 219 | LIGHT | DOORS | 0.6 | A tripwire hook within 12 | It clicks on, then off 4 ticks later, no string |
| `daylight_flip` | 221 | LIGHT | DOORS | 0.5 | A daylight detector within 12 | Flips `inverted`; set back after 1 min unless hardcore |
| `rod_sparks` | 224 | LIGHT | DOORS | 0.5 | An unpowered lightning rod within 16 | Powered for a moment with spark particles, no storm |
| `torch_burnout` | 240 | LIGHT | DOORS | 0.6 | A lit redstone torch within 12 | Goes out with the burnout smoke and fizz; relights after 8 s or on any update beside it |
| `chests_open` | 243 | LIGHT | DOORS | 0.4 | Indoors, two or more chests within 10 | Every lid opens at once, shuts 2 s later, contents untouched |
| `anvil_lands` | 210 | LIGHT | DOORS | 0.5 | Night, and an anvil within 16 | The anvil-landing sound; under hardcore the anvil gains one damage stage |
| `cake_bite` | 211 | LIGHT | DOORS | 0.5 | A cake within 12 with a slice left | One more bite gone, with an eating sound; restored after 1 min unless hardcore |
| `tnt_hiss` | 220 | LIGHT | DOORS | 0.4 | A block of TNT, or a chest holding TNT, within 12 | The primed-TNT hiss at it, nothing lit |
| `pot_holds` | 246 | LIGHT | DOORS | 0.5 | An empty decorated pot within 12 | A bone, a skull sherd or an echo shard sits in it with a wobble; taken out after 2 min unless hardcore |
| `lava_cauldron` | 239 | LIGHT | BLOCKS | 0.4 | A water cauldron within 10 | Shown as a lava cauldron with a sizzle; restored after 30 s |
| `cauldron_fills` | 245 | LIGHT | BLOCKS | 0.5 | Indoors, and a dry cauldron within 10 | Fills to a random level with drip particles; restored after 1 min |
| `turning_terracotta` | 235 | LIGHT | BLOCKS | 0.5 | Glazed terracotta within 10 | Every piece turns a quarter; restored after 1 min |
| `end_rod_gaze` | 237 | LIGHT | BLOCKS | 0.5 | An end rod within 12 not already facing the player | One rod turns to point at the player; restored after 1 min |
| `parrot_echo` | 247 | LIGHT | MOBS | 0.4 | A parrot within 16 | The parrot repeats a whisper or the player's hurt sound; the sound is bound to the parrot and only this player hears it |
| `allay_bone` | 248 | LIGHT | MOBS | 0.3 | An allay with an empty hand within 24 | A bone is put in its hand; it flies to the player and drops it within 3 blocks, or the bone is taken back after 30 s |
| `fox_snatch` | 249 | HARMFUL | MOBS | 0.2 | Dark outside, the player holds an item, an adult awake fox with an empty mouth within 16 | The fox runs at the player and, within 1.8 blocks, takes the held stack into its mouth (drop chance 100%); 30 s later it drops the stack |
| `armadillo_curls` | 250 | LIGHT | MOBS | 0.4 | An adult armadillo, not already rolled up, within 24 | The one nearest the spirit's point rolls up and is kept rolled for 20 s, then unrolls |
| `sheep_blacken` | 251 | LIGHT | MOBS | 0.3 | Two sheep that are not black within 16 | Up to 8 of them turn black, one every 2 s; 30 s later each gets its colour back. Under hardcore they stay black |
| `horse_bolts` | 252 | HARMFUL | MOBS | 0.2 | The player rides a horse, donkey or mule | The rider is thrown off, the horse rears and runs 16 blocks at a time toward the spirit's point for 10 s |
| `axolotl_plays_dead` | 253 | LIGHT | MOBS | 0.4 | An axolotl in water within 16 | Its play-dead state is set for 10 s |
| `cat_gift` | 254 | LIGHT | MOBS | 0.3 | Bright outside, a cat within 16 | The cat meows and a bone drops at its feet |
| `chickens_look_up` | 255 | LIGHT | MOBS | 0.4 | Two chickens within 16 | They stop and look straight up for 5 s |
| `cows_face_anchor` | 256 | LIGHT | MOBS | 0.4 | Dark outside, two cows within 16 | They stop and face the spirit's point for 10 s |
| `villager_nitwit` | 258 | LIGHT | MOBS | 0.3 | An adult villager with a profession within 16 | It becomes a nitwit; 1 min later its profession and trades are put back. Under hardcore its trades are lost |
| `villager_prices` | 259 | LIGHT | MOBS | 0.3 | A villager within 16 | Every villager gets a minor negative gossip (20) about the player, which fades on its own and raises prices |
| `bees_stay_out` | 262 | HARMFUL | MOBS | 0.3 | A beehive with bees in it within 16 | The bees are let out (not angry) and cannot re-enter a hive for 1 min |
| `fish_face` | 263 | LIGHT | MOBS | 0.4 | Two fish within 16 | They stop and face the player for 5 s |
| `snowman_melts` | 264 | HARMFUL | MOBS | 0.2 | A snow golem within 16 | A drip of water particles and 1 point of fire damage every second until it dies, up to 10 s |
| `frogs_croak` | 265 | LIGHT | MOBS | 0.35 | Two quiet frogs within 16 | Up to 8 frogs croak 0.3 s apart, then are silenced for 10 s |
| `ink_cloud` | 266 | LIGHT | WATER | 0.3 | Water 3 deep (water above and below) within 14 | Squid ink particles rise from a random such block, 4 puffs 0.4 s apart |
| `rabbits_stream` | 268 | LIGHT | MOBS | 0.35 | A rabbit within 16 | The rabbits run 10 blocks away from the player, re-aimed every 0.5 s for 4 s |
| `witch_giggles` | 269 | HARMFUL | MOBS | 0.25 | A witch 6 to 24 blocks away and out of sight | The witch's laugh from where it stands; 1.5 s later witch particles and a splash sound at the player and Darkness for 6 s |
| `hostiles_retreat` | 270 | LIGHT | MOBS | 0.3 | A hostile mob (with AI) within 16 | They drop their target and run 10 blocks away from the player, re-aimed every 0.5 s for 4 s |
| `creeper_calms` | 271 | LIGHT | MOBS | 0.35 | A creeper (with AI) within 8 | The nearest creeper loses its target, deflates and walks 10 blocks away for 4 s |
| `endermen_stare` | 273 | LIGHT | MOBS | 0.3 | An enderman within 24 with no target | They face the player and are kept from getting angry for 10 s; one the player looks at (97% in line) steps 6 to 12 blocks away with a teleport sound |
| `llama_spits` | 274 | LIGHT | MOBS | 0.35 | A llama within 16 | It spits once in a direction at least 90° off the player |
| `goat_screams` | 275 | LIGHT | SOUND | 0.3 | Overworld, not bright outside, player at Y 100 or above | A screaming goat sound 40 blocks away, loud enough to carry |
| `bats_hang` | 276 | LIGHT | MOBS | 0.35 | A free block under a solid one, light 4 or less, no sky, 3 to 10 blocks away | Up to 4 bats spawn there resting; removed after 5 min |
| `sniffer_digs_up` | 277 | LIGHT | MOBS | 0.3 | An adult sniffer within 20 | A drop-seed sound and the player's head or a bone appears in front of it; removed after 1 min if not taken |
| `cat_at_anchor` | 279 | HARMFUL | MOBS | 0.25 | One of the player's untied cats within 32 | The cat is teleported to standing room at the spirit's point (or 8 to 16 blocks from the player) and left sitting |
| `wind_burst` | 280 | LIGHT | PRESENCE | 0.35 | No sky above the player | A gust burst with its sound 2 blocks away; loose items within 5 are shoved away and up |
| `mobs_go_still` | 281 | HARMFUL | MOBS | 0.25 | A mob within 16 that has AI, is not silent, rides nothing and carries nobody | They lose AI and sound for 10 s, then get both back |
| `bricked_door` | 282 | LIGHT | BLOCKS | 0.4 | A wooden door within 10 | Only this player's client is told that the door's two blocks are the full block most of the blocks around it are (bricks if none are). 10 s later it gets the real blocks back. Nothing on the server |
| `boarded_windows` | 283 | LIGHT | BLOCKS | 0.3 | Sky visible at the player's feet, and glass or a glass pane within 12 | Only this player's client is told that the 48 nearest glass blocks and panes are oak planks. 15 s later it gets the real blocks back. Nothing on the server |
| `stair_wall` | 285 | LIGHT | BLOCKS | 0.3 | The last step of a flight of stairs within 10: a bottom stair with another below it behind, solid floor ahead and open space above | Only this player's client is told that the two blocks above that step are the floor block ahead of it, so the stairs lead into a wall. 15 s later it gets the real blocks back. Nothing on the server |
| `yard_pit` | 286 | LIGHT | BLOCKS | 0.3 | Standing room under open sky 8 to 14 blocks away | Only this player's client is told that a 3×3 patch of ground two blocks deep there is air (and the plants over it). 15 s later it gets the real blocks back. Nothing on the server |
| `unlit_torches` | 287 | LIGHT | LIGHTS | 0.4 | A torch within 12 | Only this player's client is told that the 24 nearest torches are the burnt-out `unlit_` ones. 30 s later it gets the real blocks back. Nothing on the server |
| `fake_ore` | 288 | LIGHT | BLOCKS | 0.3 | No sky overhead, and stone or deepslate touching air within 8 | Only this player's client is told that one such block is a diamond ore (deepslate diamond ore in deepslate). A click on it shows the stone. After 1 min it gets the real block back. Nothing on the server |
| `ladder_stub` | 290 | LIGHT | BLOCKS | 0.3 | A column of at least 3 ladders within 10 | Only this player's client is told that the upper half of the column is air. 20 s later it gets the real blocks back. Nothing on the server |
| `second_door` | 291 | LIGHT | DOORS | 0.3 | A wall one block thick and two high in front of the player with free room behind it | Only this player's client is told of an oak door in the wall. 20 s later it gets the real blocks back. Nothing on the server |
| `false_harvest` | 292 | LIGHT | BLOCKS | 0.3 | A crop not fully grown within 10 | Only this player's client is told that the 40 nearest such crops are fully grown. 20 s later it gets the real blocks back. Nothing on the server |
| `lava_pond` | 293 | LIGHT | WATER | 0.2 | A source of water with open air above within 12 | Only this player's client is told that the 200 nearest such water blocks are lava. 1 s later it gets the real blocks back. Nothing on the server |
| `lava_as_water` | 294 | HARMFUL | WATER | 0.2 | Health > 5 hearts, at least 6 lava blocks within 16 and none within 9 | A sizzle sounds at the lava, and only this player's client is told that the 300 nearest lava blocks are water. They show as lava again when the player comes within 8 blocks, or after 30 s, or at once on a click. Always an illusion, never real, in any swaps mode |
| `roof_snow` | 295 | LIGHT | BLOCKS | 0.3 | Sky visible at the player's feet, and a roof of planks, cobblestone, stone bricks, bricks, terracotta or deepslate within 8, at least 2 blocks above the player | Only this player's client is told of two layers of snow on the 80 nearest roof blocks. 1 min later it gets the real blocks back. Nothing on the server |
| `black_glass` | 296 | LIGHT | BLOCKS | 0.3 | Glass or a glass pane within 10 | Only this player's client is told that the 48 nearest glass blocks are tinted glass and the panes black stained glass panes. 15 s later it gets the real blocks back. Nothing on the server |
| `red_wool` | 297 | LIGHT | BLOCKS | 0.3 | Wool or a carpet within 10 that is not red | Only this player's client is told that the 64 nearest wool blocks and carpets are red. 15 s later it gets the real blocks back. Nothing on the server |
| `sign_whisper` | 298 | LIGHT | BLOCKS | 0.3 | A sign within 10 | Only this player's client gets the sign's block entity with one of the listed texts on its front. 15 s later it gets the real text. Nothing on the server |
| `wither_roses` | 299 | LIGHT | BLOCKS | 0.3 | A small flower within 12 that is not a wither rose | Only this player's client is told that the 48 nearest small flowers are wither roses. 15 s later it gets the real flowers back. Nothing on the server |
| `block_flicker` | 301 | LIGHT | BLOCKS | 0.3 | A block of the list `from` (stone, dirt, grass, cobblestone, deepslate, sand, oak planks) within 8 | Only this player's client is told that up to 200 nearest blocks of one kind found there are one random block of the list `to`. One tick later it gets the real blocks back. Nothing on the server |
| `lone_gravestone` | 302 | LIGHT | BLOCKS | 0.3 | Standing room 9 blocks ahead (within 40 degrees) under the open sky | Only this player's client is told of a gravestone there. 30 s later it gets the real block back. Nothing on the server |
| `broken_portal` | 304 | LIGHT | BLOCKS | 0.3 | A nether portal block within 16 | Only this player's client is told that 3 random obsidian blocks touching the portal are air. 15 s later it gets the real blocks back. Nothing on the server |
| `dark_circle` | 307 | LIGHT | BLOCKS | 0.3 | Underground (no sky) with block light 5 or less at the player, and standing room 8 blocks ahead with at least 6 of 12 ring points free | Only this player's client is told of light blocks (level 12) in a ring of radius 3 and in its middle. They go out when the player comes within 3 blocks of the middle, or after 30 s. Nothing on the server |
| `fence_gaps` | 308 | LIGHT | BLOCKS | 0.3 | At least 4 fence posts within 12 | Only this player's client is told that 3 random fence posts there are air. 20 s later it gets the real posts back. Nothing on the server |
| `corner_cobwebs` | 310 | LIGHT | BLOCKS | 0.3 | At least 3 empty corners within 8 where the ceiling meets two walls | Only this player's client is told that up to 12 random such corners hold a cobweb. 20 s later it gets the real blocks back. Nothing on the server |
| `redstone_trail` | 311 | LIGHT | BLOCKS | 0.3 | Room on the floor for at least 5 blocks of a straight line in one of the four directions, 3 to 15 blocks away | Only this player's client is told of the dust, following the ground up and down. 20 s later it gets the real blocks back. Nothing on the server |
| `composter_swing` | 212 | LIGHT | BLOCKS | 0.4 | A composter within 12 | An empty one looks full, any other looks empty; shown to the player for 1 min (really changed in `real` mode) |
| `banner_skull` | 213 | LIGHT | BLOCKS | 0.4 | A banner within 12 with fewer than six patterns | A black skull pattern added; removed after 2 min unless hardcore |
| `stairs_flip` | 236 | LIGHT | BLOCKS | 0.3 | Right-way-up stairs within 12 behind the player's view | Up to 4 stairs show `half=top` for 1 min |
| `lectern_swap` | 242 | LIGHT | BLOCKS | 0.3 | A lectern with a book within 12 | The book is swapped for a written one signed by the spirit; the original is put back after 2 min (dropped if the new one was taken), unless hardcore |
| `piston_twitch` | 217 | LIGHT | DOORS | 0.5 | An unextended piston within 12 with air in front | Extend and retract two times, sent to the player only; the real blocks are re-sent after |
| `hopper_swallows` | 218 | HARMFUL | DOORS | 0.3 | An enabled hopper within 12 and a dropped item within 4 of it | The item slides over the hopper and gets taken |
| `sculk_chain` | 225 | LIGHT | DOORS | 0.5 | Two or more idle sculk sensors within 16 | Up to 8 sensors activate 6 ticks apart, farthest first |
| `observer_pulse` | 241 | HARMFUL | DOORS | 0.3 | An unpowered observer within 12 | `powered` toggles three pulses 4 ticks apart, ending off |
| `bed_missing` | 215 | HARMFUL | PRESENCE | 0.2 | The player has a respawn point | The vanilla "home bed was missing or obstructed" message; spawn untouched |
| `ender_note` | 244 | LIGHT | PRESENCE | 0.3 | An ender chest is open with an empty slot | A named paper note appears in an empty slot by packet only; any click shows the truth |
| `creeper_behind` | 392 | LIGHT | MOBS | 0.3 | Standing room 3 blocks away, 140–180° from where the player looks (up to 16 bearings tried) | Only this player's client is told of a creeper standing there facing them, with an id no server entity has. Gone after 4 checks (every tick) with it within 35° of where they look, or after 10 s. Nothing on the server |
| `drowned_rises` | 394 | LIGHT | WATER | 0.25 | Source water at least 3 deep with air above it, 4 to 16 blocks away | Only this player's client is told of a drowned that rises from 2.2 blocks down to chest height in the pond over 2 s, then sinks again, facing them. Nothing on the server |
| `ghost_horse` | 395 | LIGHT | MOBS | 0.25 | Ground 16 blocks away at 60° to either side of where the player looks | Only this player's client is told of a riderless horse that crosses from one side to the other in 2.5 s (about 0.8 blocks/tick), following the ground. Nothing on the server |
| `bed_cat` | 396 | LIGHT | MOBS | 0.3 | A bed within 12 blocks, at least 3 away | Only this player's client is told of a cat sitting on the head of the bed, facing the headboard (the wall). Gone when the player comes within 2.5 blocks, or after 30 s. Nothing on the server |
| `child_runs` | 397 | LIGHT | MOBS | 0.25 | Under a roof (no sky above), ground 5 blocks away at 70° to either side of where the player looks | Only this player's client is told of a baby villager that runs across from one side to the other in 1.5 s, following the ground and ignoring walls. Nothing on the server |
| `doorway_enderman` | 398 | LIGHT | MOBS | 0.2 | A door within 14 blocks, at least 4 away | Only this player's client is told of an enderman standing in the doorway, facing them. When it has been within 30° of where they look for 3 checks (every tick) it is gone with the teleport sound; otherwise after 20 s. Nothing on the server |
| `forest_wolves` | 399 | LIGHT | MOBS | 0.25 | Forest biome; standing room for at least 2 of 4 wolves 22–28 blocks away within 45° of where the player looks | Only this player's client is told of 2–4 wolves standing there, all facing them. Gone when the player comes within 10 blocks, or after 20 s. Nothing on the server |
| `ceiling_spider` | 400 | LIGHT | MOBS | 0.3 | Standing room 3–8 blocks away with a solid ceiling within 5 blocks above it | Only this player's client is told of a spider hanging upside down under the ceiling (given the name Dinnerbone, not shown, which makes the client flip it). Gone when the player comes within 3 blocks, or after 20 s. Nothing on the server |
| `bat_swarm` | 401 | LIGHT | MOBS | 0.3 | Always | Only this player's client is told of 6 bats that cross a 30-block line through the player at a random bearing and head height, a little apart, in 2 s, walls or not. Nothing on the server |
| `window_zombie` | 402 | LIGHT | MOBS | 0.25 | A glass block or pane within 10 blocks, at least 3 away, with the player on one side and standing room on the other | Only this player's client is told of a zombie standing outside it, facing in. Gone when the player comes within 1.5 blocks of it, or after 20 s. Nothing on the server |
| `ghast_drift` | 403 | LIGHT | PRESENCE | 0.15 | Overworld, sky visible, forest biome | Only this player's client is told of a ghast drifting a straight 160-block line 45 blocks above them, up to 40 to one side, at a random bearing, in 30 s, with its ambient cry at the middle of the line. Nothing on the server |
| `vex_through_wall` | 405 | LIGHT | MOBS | 0.3 | A straight 24-block line at eye height, centred 12 blocks away at a random bearing (up to 16 tried), that starts and ends in the open and passes through 3 to 16 solid samples (half a block each) | Only this player's client is told of a vex flying that line in 3 s, through whatever stands on it, with an id no server entity has. Nothing on the server |
| `phantom_swoop` | 409 | LIGHT | MOBS | 0.3 | Sky visible | Only this player's client is told of a phantom 24 blocks away and 14 up, at a random bearing, with its swoop screech; it flies at 1 block/tick straight at the player's eyes, as they move, and is gone within 1.5 blocks or after 5 s. Nothing on the server |
| `dead_on_path` | 413 | LIGHT | MOBS | 0.3 | Standing room 12 blocks away within 35° of where the player looks (up to 16 bearings tried) | Only this player's client is told of a cow, sheep, pig, chicken or villager there and then of its death, so it lies on its side (its death sound plays). Gone when the player comes within 4 blocks, or after 30 s. Nothing on the server |
| `llama_caravan` | 414 | LIGHT | MOBS | 0.25 | Standing room 16 blocks away within 25° of where the player looks (up to 16 bearings tried) | Only this player's client is told of 4 trader llamas, 2.5 blocks apart, walking a 40-block line at right angles to where the player looks, centred on that spot, at 0.15 blocks/tick and following the ground up 2 and down 4. Each is gone at the end of the line. Nothing on the server |
| `giant_slime` | 415 | LIGHT | MOBS | 0.25 | Standing room 32 blocks away within 40° of where the player looks (up to 16 bearings tried) | Only this player's client is told of a size 8 slime there, hopping on the spot once a second and facing the player. Gone when the player comes within 14 blocks, or after 30 s. Nothing on the server |
| `lodestone_lure` | 422 | LIGHT | ITEMS | 0.3 | A compass with a lodestone target in the inventory, and a territorial spirit whose reach holds the player | The needle points at that spirit's anchor; the old target comes back after 5 min wherever the player carries it then. Under hardcore it stays |
| `ghost_author` | 423 | LIGHT | ITEMS | 0.3 | A free inventory slot, and no written book signed with the player's name yet | A written book signed with the player's own name, with one creepy page (never the same page twice in a row) |
| `potion_swap` | 425 | LIGHT | ITEMS | 0.3 | A potion, splash or lingering potion with a base potion and no custom effects | It becomes another potion from a fixed list; the old one comes back after 5 min wherever the player carries it then. Under hardcore it stays |
| `vanishing_arrows` | 426 | HARMFUL | ITEMS | 0.3 | An arrow of any kind in the inventory | One arrow every 2 s, up to 6, leaves the inventory; they are handed back after 3 min (dropped at the spot if the player is gone). Under hardcore they stay gone |
| `crossbow_misfire` | 431 | HARMFUL | ITEMS | 0.2 | A loaded crossbow in either hand | It fires straight up with reduced power (the arrow comes back down) |
| `map_elsewhere` | 432 | LIGHT | ITEMS | 0.3 | A map in either hand, and a territorial spirit whose reach holds the player | The map is swapped for a biome outline of the area around that spirit's anchor, same scale; the old map comes back after 5 min wherever the player carries it then. Under hardcore it stays |
| `shy_item` | 434 | LIGHT | ITEMS | 0.3 | A dropped item within 5 | One of them is pushed away from the player |
| `orb_scatter` | 435 | LIGHT | ITEMS | 0.3 | An experience orb within 8 | Every orb there is thrown away from the player (they drift back by themselves) |
| `piglins_stare` | 442 | LIGHT | MOBS | 0.35 | Nether, a piglin or brute within 24 | Each loses its AI, faces the player (re-aimed every 0.25 s) for 8 s, then gets its AI back |
| `piglin_barter` | 443 | LIGHT | MOBS | 0.3 | Nether, an adult piglin within 16 | It plays its admiring sound; 2 s later a player head (the player's own) or a bone is thrown from it toward the player, and is gone after 30 s |
| `ghast_cry` | 444 | LIGHT | SOUND | 0.35 | Nether | A ghast's scream 40 blocks away (the spirit's point if one sent it), 2.5 s later its fireball shot, 1.25 s after that an explosion from the same spot |
| `lava_figure` | 445 | LIGHT | PLAYER | 0.25 | Nether, a lava source with two free blocks above it within 24, at least 10 away | A vanilla mannequin in a stranger's skin (invulnerable, silent) stands on that lava, facing the player; gone when the player is within 8 blocks, or after 30 s |
| `strider_shiver` | 446 | LIGHT | MOBS | 0.35 | Nether, a strider within 24 that is not already cold | Only this player's client is told the striders are cold (the shiver) for 8 s, then the real state again. Nothing changes on the server |
| `soul_fire_lean` | 447 | LIGHT | PRESENCE | 0.3 | Nether, soul fire within 16 | Up to 6 soul fires send soul fire flame particles streaming horizontally toward the player, 2 per pulse every 0.2 s, 30 pulses |
| `skulls_turn` | 449 | LIGHT | BLOCKS | 0.3 | Any floor or wall skull within 12, in any dimension | Every such skull is shown turned to face the player (a swap, see the `swaps` setting); restored after 30 s |
| `still_blaze` | 450 | LIGHT | MOBS | 0.25 | Nether, standing room 10 blocks away within 40° of where the player looks | Only this player's client is told of a blaze there, facing the player and not moving. Gone when the player comes within 5 blocks, or after 20 s. Nothing on the server |
| `magma_hop` | 451 | LIGHT | MOBS | 0.35 | Nether, a magma cube on the ground within 16 | All of them jump at once, with their jump sound |
| `anchor_charge` | 452 | LIGHT | BLOCKS | 0.3 | Nether, a respawn anchor below full charge within 10 | One of them shows one more charge, with the charging sound (a swap, see the `swaps` setting); restored after 30 s, and under hardcore it stays |
| `vines_reach` | 453 | LIGHT | BLOCKS | 0.3 | Nether, a weeping vine end with air below within 12 | The vine nearest the player grows up to 3 blocks down into the air, ending in a full-grown head; put back after 30 s, and under hardcore it stays |
| `steady_compass` | 454 | LIGHT | ITEMS | 0.3 | Nether, a plain compass in the inventory, and a territorial spirit whose reach holds the player | Only this player's client is shown the compasses locked on that spirit's anchor for 30 s, then the real stacks again |
| `soul_sand_drag` | 464 | HARMFUL | AFFLICTION | 0.2 | Nether, standing on soul sand or soil, not slowed | Slowness II for 5 s and a whisper 2 blocks away |
| `piglins_angered` | 465 | HARMFUL | MOBS | 0.15 | Nether, the player wears gold armor (not creative), a piglin within 16 | Idle piglins within 16 turn on the player as if they wore no gold (vanilla anger, 30 s) |
| `endermen_turn` | 455 | LIGHT | MOBS | 0.3 | End, an enderman with no target within 32 blocks | Each such enderman faces the player and has its target cleared and its path stopped every tick, for 10 s. They stay calm however long the player looks back. Nothing to revert |
| `shulker_peeks` | 456 | LIGHT | MOBS | 0.3 | End, a shulker with no target within 24 blocks | One shulker opens its shell fully with the open sound, and shuts it with the close sound 3 s later (put back at shutdown too) |
| `empty_pillar_crystal` | 457 | LIGHT | MOBS | 0.25 | End, a pillar top (bedrock on obsidian, free air above, no crystal on it) 8 or more blocks away in the 48-block square | Only this player's client is told of an end crystal on it, with an id no server entity has. Gone when the player comes within 12 blocks, or after 20 s. Nothing on the server |
| `void_wind` | 458 | HARMFUL | ASSAULT | 0.2 | End, health > 5 hearts, on the ground, not riding, 24 or more blocks from the middle, and ground (within 4 blocks below) at every second block out to 10 blocks along the way outward | A wind-burst howl, then after 2 s the player on the ground is nudged outward at about 2.4 blocks/s every 0.1 s for 5 s, with the howl repeated each second. It ends the moment ground is missing within 10 blocks along the way, so the edge is never reached |
| `gateway_beam` | 459 | LIGHT | BLOCKS | 0.3 | End, a gateway in the 9×9 chunks around the player | The gateway's own beam event is sent, as when an entity enters it, so it shows its beam for 2 s. Nobody is moved |
| `chorus_shift` | 460 | LIGHT | BLOCKS | 0.3 | End, a chorus plant or flower within 12 blocks with free air beside it | Only this player's client is told that the 24 nearest such blocks are one block over in a single direction (air where they were). After 10 s it gets the real blocks back. Nothing on the server |
| `phantom_egg` | 461 | LIGHT | BLOCKS | 0.25 | End, standing room 4–12 blocks away | Only this player's client is told of a dragon egg standing there. A click shows the truth. After 10 s it gets the real block back. Nothing on the server |
| `ender_chests_open` | 462 | LIGHT | SOUND | 0.3 | End | Five ender chest open sounds, 3–8 blocks away at random bearings, 0.4 s apart, each followed by the close sound 1.5 s after. Nothing in the world |
| `nowhere_portal` | 463 | LIGHT | BLOCKS | 0.2 | Overworld, a 4×5 empty space on solid ground 14 blocks away within 40° of where the player looks | Only this player's client is told of a lit nether portal in a frame of obsidian (no corners) there, facing them, with the portal ambient sound. A click shows air. After 15 s it gets the real blocks back. Nothing on the server |
| `enderman_home_block` | 466 | LIGHT | MOBS | 0.2 | End, the player carries a block item (not one with a block entity) and has standing room 18 blocks away within 50° of where they look | Only this player's client is told of an enderman standing there, facing them, holding one of the blocks they carry. Gone with the teleport sound once it has been within 30° of where they look for 3 checks (every tick), or silently within 6 blocks or after 20 s. Nothing on the server |
| `desert_husk_haze` | 467 | LIGHT | MOBS | 0.3 | Desert, and standing room 28 blocks away within 40° of where the player looks | A husk shown only to this player, facing them; gone within 14 blocks or after 20 s |
| `desert_sand_trickle` | 468 | LIGHT | BLOCKS | 0.3 | Desert, and a solid block within 6 above the player's head | Falling sand particles under that block, 12 pulses 0.25 s apart, with a sand sound |
| `desert_plate_click` | 469 | LIGHT | SOUND | 0.3 | Desert | A stone pressure plate click 5 blocks away |
| `swamp_lily_drift` | 470 | LIGHT | WATER | 0.3 | Swamp or mangrove swamp, and a lily pad within 10 that can step toward the player | One pad steps toward the player 6 times, 1 s apart, stopping 2.5 blocks away; put back after 1 min (stays under hardcore) |
| `cauldron` | 471 | LIGHT | BLOCKS | 0.3 | A cauldron within 12 | Brewing bubble sounds and bubble particles over the cauldron, 8 pulses 0.5 s apart |
| `fireflies_dark` | 472 | LIGHT | BLOCKS | 0.3 | A firefly bush within 12 | Every firefly bush within 12 shown as a plain bush for 30 s |
| `snow_door_drift` | 473 | LIGHT | BLOCKS | 0.3 | Snowy biome, and a wooden door within 12 with open sky and free ground outside it | Four layers of real snow outside the door and three on each side along the wall; removed after 2 min (stays under hardcore) |
| `snow_snowball` | 474 | HARMFUL | ASSAULT | 0.2 | Snowy biome | A snowball thrown from 10 blocks away at the player's eyes; it does no damage |
| `snow_powder_path` | 475 | HARMFUL | ASSAULT | 0.2 | Snowy biome, health above the safe floor + 2 hearts, and standing room 5 blocks ahead on a block with solid ground under it | That floor block becomes powder snow, one block deep; put back after 1 min (stays under hardcore) |
| `mountain_avalanche` | 476 | LIGHT | SOUND | 0.3 | Mountain biome | A rumble that carries and a cloud of snow falling 25 blocks down at a point 50 blocks away, over 24 steps |
| `mountain_wind_push` | 477 | HARMFUL | ASSAULT | 0.2 | Mountain biome, health above the safe floor + 2 hearts, and the player at the edge of a drop | Three wind howls 0.5 s apart, then a slight push toward the edge |
| `ocean_ship_bell` | 478 | LIGHT | SOUND | 0.3 | Ocean biome | Three bell tolls 2 s apart, 40 blocks away; loud enough to carry |
| `boat_grabbed` | 480 | HARMFUL | WATER | 0.2 | In an ocean biome, riding a boat that floats in water | For 8 s the boat's motion is set to zero every tick and the client is told, so it will not move; drowned groans sound under it every 1.5 s |
| `parrots_scatter` | 481 | LIGHT | MOBS | 0.3 | In a jungle biome, a parrot with no owner within 24 | Every such parrot is thrown up if it is on the ground and sent toward a point 24 blocks away from the player and 10 up, again every 0.5 s for 8 s; a wing beat sounds |
| `window_vines` | 482 | LIGHT | BLOCKS | 0.3 | In a jungle, swamp or mangrove swamp biome, glass or a glass pane within 12 with air beside it | Real vines grow on up to 24 of the nearest air blocks beside the glass, hung toward it. 60 s later each goes unless it was changed meanwhile. Under hardcore they stay |
| `mooshroom_brown` | 483 | LIGHT | MOBS | 0.3 | In the mushroom fields, a red mooshroom within 24 | Up to 8 of them turn brown; 30 s later each is red again. Under hardcore they stay brown |
| `eyeblossoms_open` | 484 | LIGHT | BLOCKS | 0.3 | A closed eyeblossom within 12, regardless of biome or view | Only this player's client is told that up to 12 of the nearest such flowers are open, and an opening sounds. 30 s later it gets the real blocks back. Nothing on the server |
| `creaking_heart_call` | 485 | LIGHT | SOUND | 0.3 | In a forest, flower forest, birch forest or old growth birch forest | A creaking heart sounds 3 times, 2 s apart, from one point 14 blocks away at a random bearing (or the spirit's point) |
| `resin_clumps` | 486 | LIGHT | BLOCKS | 0.3 | In a forest, flower forest, birch forest, old growth birch forest, dark forest or pale garden, a plank, cobblestone, stone brick, brick or wool wall within 12 with air beside it | Real resin clumps grow on up to 8 air blocks beside the walls, clustered around a random spot, with a placing sound. 2 min later each goes unless it was changed meanwhile. Under hardcore they stay |
| `black_petals` | 487 | LIGHT | PRESENCE | 0.3 | In the cherry grove | For 10 s ash particles fall in a cloud above and around the player, like petals |
| `canopy_closes` | 488 | LIGHT | BLOCKS | 0.3 | In the dark forest or pale garden, at least 6 air blocks touching leaves at least 3 above the player within 8 | Only this player's client is told that the 40 nearest such blocks are persistent dark oak leaves. 15 s later it gets the real blocks back. Nothing on the server |
| `sculk_creep` | 489 | LIGHT | BLOCKS | 0.2 | In the deep dark, sculk-replaceable ground with air above, 8 to 14 blocks away | Real sculk spreads in a trail of up to 12 blocks from far ground toward the player, one every second, each next to the last and nearer, stopping 3 blocks away. 2 min later each goes unless it was changed meanwhile. Under hardcore it stays |
| `berries_dark` | 490 | LIGHT | LIGHTS | 0.3 | Glow berries within 14, in any biome | Up to 10 of the nearest bunches lose their berries, one per second; 1 min later each has them again. Under hardcore they stay dark |
| `roof_figure` | 369 | LIGHT | PRESENCE, PLAYER | 0.3 | A stair, slab or planks block open to the sky within 20 blocks, at least 12 blocks from the player | A silent, unhurtable mannequin in a random default skin crouches on top of it, facing the player. Gone, silently, when the player comes within 10 blocks or after 30 s |
| `hill_waver` | 371 | LIGHT | PRESENCE, PLAYER | 0.3 | Standing room open to the sky 45 blocks away within 40° of where the player looks, at least 6 blocks above the player (up to 16 bearings tried) | A silent, unhurtable mannequin in a random default skin stands there facing the player and swings its arm every 0.4 s. Gone, silently, when the player comes within 20 blocks or after 20 s |
| `wall_miner` | 372 | LIGHT | PRESENCE, PLAYER | 0.3 | A one-block-thick wall two blocks high in front of the player, with standing room behind it | A silent, unhurtable mannequin in a random default skin stands behind the wall and every 0.4 s swings at it: cracks run up the block over 10 steps (only this player's client is told) with its digging sound, then clear. The wall is never broken. Gone when it is done, when the player comes within 2.5 blocks, or after 10 s |
| `table_figure` | 373 | LIGHT | PRESENCE, PLAYER | 0.3 | A crafting table within 16 blocks with standing room within 2 blocks of it, at least 6 blocks from the player | A silent, unhurtable mannequin in a random default skin stands there looking down at the table. Gone, silently, when the table is gone, when the player comes within 4 blocks, or after 15 s |
| `cave_torches` | 374 | LIGHT | PRESENCE, PLAYER | 0.3 | Player under a roof, and a bearing with at least 3 floor spots 6 to 28 blocks off, 4 blocks apart, out of the sky and lit 7 or less (up to 12 bearings tried) | Only this player's client is told those spots hold torches. 1 min later it gets the real blocks back (placed for real, and put back, under the `swaps` setting `REAL`; they stay under hardcore) |
| `own_walker` | 377 | LIGHT | PRESENCE, PLAYER | 0.3 | Standing room 45 blocks away within 40° of where the player looks | A silent, unhurtable mannequin in the player's own skin walks across their view at 0.2 blocks/tick, jumping up one-block steps, dropping off ledges up to 2 deep and going round what is in its way. Gone, silently, when the player comes within 25 blocks, when no way on is left, or after 30 s |
| `night_runner` | 379 | LIGHT | PRESENCE, PLAYER | 0.3 | Light at the player 4 or less, and standing room 22 blocks away within 35° of where the player looks | A silent, unhurtable mannequin in a random default skin sprints across the view at 0.36 blocks/tick, jumping now and then. Gone, silently, when the player comes within 7 blocks, when no way on is left, or after 15 s |
| `bed_sleeper` | 384 | LIGHT | PRESENCE, PLAYER | 0.3 | A free bed within 24 blocks with room above it, at least 8 blocks from the player | A silent, unhurtable mannequin in a random default skin lies asleep on it (the bed is not marked taken). Gone, silently, when the player comes within 6 blocks or after 20 s |
| `shrieker_figure` | 385 | LIGHT | PRESENCE, PLAYER | 0.3 | Deep dark biome, and a sculk shrieker within 24 blocks with standing room within 3 blocks of it, at least 8 blocks from the player | A silent, unhurtable mannequin in a random default skin crouches there looking at the shrieker. Gone, silently, when the shrieker shrieks, when the player comes within 6 blocks, or after 20 s |
| `hello_chat` | 387 | LIGHT | PRESENCE, PLAYER | 0.3 | A line left in `lines` that the spirit has not said | Only this player gets a vanilla chat line `<name> hello?` (from `lines`) under a made-up nickname |
| `ghost_teleport` | 388 | LIGHT | PRESENCE, PLAYER | 0.2 | A name left in `names` that the spirit has not said | Only this player gets the vanilla command feedback `Teleported ??? to <player>`. Nobody is moved |
| `stranger_advancement` | 391 | LIGHT | PRESENCE, PLAYER | 0.2 | A title left in `titles` that the spirit has not said | Only this player gets the vanilla chat line `<name> has made the advancement [Who's There?]` (title from `titles`) under a made-up nickname |
| `grave_figure` | 389 | LIGHT | PRESENCE, PLAYER | 0.3 | The player's last death was in this dimension, 20 to 60 blocks away, in a loaded chunk with standing room | A silent, unhurtable mannequin in a random default skin stands there facing the player. Gone, silently, when the player comes within 12 blocks or after 20 s |
| `bedside_figure` | 390 | LIGHT | PRESENCE, PLAYER | 0.3 | No such wait already | Waits up to 20 min for the player to sleep and wake. Then a silent, unhurtable mannequin in a random default skin stands within 3 blocks of the bed, facing the player, for 1.25 s |
| `scarecrow_closer` | 522 | LIGHT | BLOCKS | 0.3 | A scarecrow within 14, at least 4 away, with a free cell and a floor one step toward the player | The scarecrow moves one block toward the player; put back after 60 seconds, stays under hardcore |
| `ouija_hi` | 523 | LIGHT | PRESENCE | 0.3 | An ouija board within 8 | The planchette spells "HI" or "HELLO" on the action bar, letter by letter, with no question and no journal note |
| `spirit_box_word` | 524 | LIGHT | SOUND | 0.3 | A spirit box within 6 | A word from a sensed spirit's voice pool in chat after a moment, or static when none speaks; noted in the journal like a real answer |
| `salt_gap` | 525 | HARMFUL | BLOCKS | 0.2 | At least 4 salt cells within 8 | The salt cell nearest the spirit vanishes with ash and a sand sound; back after 2 minutes, stays under hardcore |
| `grave_name` | 528 | LIGHT | PRESENCE | 0.3 | A gravestone within 16 | A chat line from the config ("Here lies <player>.") and ash over the stone; nothing changes in the world |
| `journal_page_torn` | 530 | LIGHT | ITEMS | 0.3 | The player carries a journal with at least 3 notes | One note leaves the notebook with a page-turn sound and returns in its place after 5 minutes, hardcore or not |
| `dreamcatcher_fails` | 531 | LIGHT | AFFLICTION | 0.3 | A dreamcatcher within 8 and a `dream` that can run | The dreamcatchers swing hard, then the real `dream` event runs 2 seconds later, past the ward |
| `lantern_soul_fire` | 532 | LIGHT | LIGHTS | 0.3 | A hallowed lantern within 10 and a spirit that senses the player | The lanterns become soul lanterns (no ward meanwhile) for 30 seconds, then are put back, hardcore or not |
| `charm_falls` | 533 | HARMFUL | BLOCKS | 0.2 | A door charm within 10 | The charm breaks off and lies there as an item, its wear lost; the same under hardcore |
| `altar_midnight` | 536 | LIGHT | LIGHTS | 0.3 | Overworld night and an altar within 12 with unlit candles within 3 of it | The candles light one after another, 10 ticks apart; they stay lit |
| `brewing_swap` | 208 | LIGHT | DOORS | 0.3 | A brewing stand within 10 with a potion bottle | One bottle becomes another potion from the list (water, awkward, night vision, swiftness, healing, harming, poison, weakness, slowness, invisibility); put back after 5 min if untouched (stays under hardcore) |
| `brewing_bubbles` | 209 | LIGHT | DOORS | 0.3 | A brewing stand within 10 with nothing in any slot | The brewing sound and bubbles above it, 6 times, 0.4 s apart. Nothing changes |
| `beacon_out` | 214 | HARMFUL | BLOCKS | 0.2 | An active beacon within 16 with air above it | An obsidian or black stained glass block is shown right above it, so the beam goes out or turns black; 30 s, this player only (a real block under `real` swaps; under hardcore it stays) |
| `statue_pose` | 222 | LIGHT | DOORS | 0.3 | A copper golem statue within 12 outside 60° of where the player looks | Its `pose` becomes another one; set back after 2 min (stays under hardcore) |
| `note_tune` | 226 | LIGHT | SOUND | 0.3 | A note block with a tunable instrument within 12 (air above unless the instrument sounds from above) | One of 4 fixed tunes, one note per 0.3 s, each on the next note block nearest first, with the note particle. The blocks keep their own notes |
| `campfire_cooks` | 227 | LIGHT | BLOCKS | 0.3 | A campfire within 10 with a free slot | Rotten flesh or a rabbit foot shows on the grill for 1 min, through the block entity's update packet to this player only; nothing cooks and the real campfire is unchanged |
| `spawner_shows` | 229 | LIGHT | BLOCKS | 0.3 | A monster spawner within 16 | The spinning mob inside is a villager, iron golem, cat, wandering trader, chicken or witch for 30 s, this player only; the real spawner is unchanged |
| `trial_ominous` | 230 | LIGHT | BLOCKS | 0.3 | A calm trial spawner within 16 | Shown with `ominous=true` for 10 s |
| `vault_head` | 231 | LIGHT | BLOCKS | 0.3 | A vault within 16 | Its display item is the player's own head for 30 s, through the block entity's update packet to this player only |
| `black_bed` | 233 | LIGHT | BLOCKS | 0.3 | A bed that is not black within 12 | Both halves show as the black bed, same facing and part; 1 min, this player only (a bed is a block entity, so it is never really swapped) |
| `black_carpets` | 234 | LIGHT | BLOCKS | 0.3 | Indoors, a carpet that is not black within 8 | The 48 nearest show as black carpets for 1 min (really changed in `real` mode; stays under hardcore) |
| `black_ash` | 515 | LIGHT | PRESENCE | 0.3 | Indoors | Dark ash particles fall around the player from 2 blocks up, 24 bursts 0.25 s apart |
| `border_closes` | 351 | LIGHT | PRESENCE | 0.2 | No real border within 80 blocks | Only this player's client is shown a world border centered on them, shrinking from 80 to 40 blocks wide over 4 s and held 2 s; then the real border. Nothing on the server |
| `false_recipe` | 355 | LIGHT | PRESENCE | 0.25 | Always | Only this player's client gets a "recipe unlocked" toast for a made-up bone and skull recipe (the result is a skull, bone block, soul lantern or wither rose); 10 s later it is removed from the recipe book. No recipe exists |
| `view_narrows` | 358 | LIGHT | PRESENCE | 0.25 | Always | The client is told the movement speed attribute is 0.02 for 1 s, so the field of view pinches; then the real attribute |
| `line_pulls` | 418 | HARMFUL | ITEMS | 0.25 | The player's bobber floats in water | A shove of 0.6 toward the bobber, with a splash there |
| `odd_catch` | 419 | LIGHT | ITEMS | 0.3 | The player's bobber floats in water | A player head in their own likeness, a bone, a skeleton skull or rotten flesh is thrown out of the water at the player, as a real catch is. It vanishes after 1 min if not picked up |
| `mob_eyes` | 347 | LIGHT | PRESENCE | 0.3 | Player on the ground, not in a fluid, and a spider, cave spider, creeper or enderman within 24 blocks | Only this player's client is sent to see through that mob (`ClientboundSetCameraPacket`) for 2.5 s, with the vanilla shader of that mob; then back. The view also ends early if the mob dies or vanishes, the player is hurt or changes dimension; death, logout and server stop reset it too. Nothing changes on the server |
| `out_of_body` | 348 | LIGHT | PRESENCE | 0.3 | Player on the ground, not in a fluid | The view moves to a packet-only entity 3.5 blocks behind the player (nearer if blocked) and 0.6 above their eyes, looking at the back of their head, for 3 s; then back and the entity is removed. Same early resets as `mob_eyes` |
| `spirit_sight` | 349 | LIGHT | PRESENCE | 0.3 | Player on the ground, not in a fluid | The view moves for 1 s to the spirit's point (10 blocks away at a random bearing when no spirit sent it), looking at the player; does nothing if that point is over 40 blocks away or not loaded. Same early resets as `mob_eyes` |
| `open_door_view` | 366 | LIGHT | DOORS | 0.3 | Player on the ground, not in a fluid, and an open wooden or other door 3 to 12 blocks away | The view moves for 3 s into the doorway of the nearest open door, looking in toward the player; then back. Same early resets as `mob_eyes` |
| `lake_fisher` | 367 | LIGHT | PRESENCE, PLAYER | 0.3 | A lake (a water source with water on three sides for 3 blocks) within 16 blocks, with standing room on its bank at least 8 blocks from the player | A silent, unhurtable mannequin in a random default skin stands on the bank with a fishing rod, facing the water, casts, and every 4th second swings and splashes (fishing particles and sounds). No bobber is drawn. Gone, silently, when the player comes within 6 blocks or after 30 s |
| `window_figure` | 368 | LIGHT | PRESENCE, PLAYER | 0.3 | The player is under open sky; a glass block or glass pane within 14 blocks has a roofed room behind it (standing room one block below or level with it, at least 4 blocks from the player) and open air on the player's side | A silent, unhurtable mannequin in a random default skin stands behind the glass facing the player. Gone, silently, when the player comes within 3 blocks or after 20 s |
| `trailing_figure` | 370 | LIGHT | PRESENCE, PLAYER | 0.3 | Standing room 30 blocks behind the player (150-180° from where they look) | A silent, unhurtable mannequin in a random default skin faces the player and steps toward them by as far as the player moved each 0.1 s (up to 1 block), so it keeps its distance while they walk and stops when they stop; it sprints when they do. Gone, silently, when the player looks within 12° of it, comes within 8 blocks, or after 60 s |
| `deep_swimmer` | 376 | LIGHT | PRESENCE, PLAYER | 0.3 | The player's head is underwater, with water 8 blocks away within 15-60° of where they look (12 bearings tried) | A silent, unhurtable mannequin in a random default skin lies in the swimming pose in the water there and swims across the view at 2.4 blocks/s, trailing bubbles. Gone, silently, when the water ends, when the player comes within 2 blocks, or after 15 s |
| `note_dropper` | 378 | LIGHT | PRESENCE, PLAYER | 0.3 | Standing room 14 blocks away within 30° of where the player looks | A silent, unhurtable mannequin in a random default skin faces the player for 1.6 s, then drops a torch or (half the time) paper named with a short note, and sprints away from the player. The item is real and stays. Gone, silently, when the player comes within 2 blocks, when no way on is left, or after 20 s |
| `horizon_choir` | 380 | LIGHT | PRESENCE, PLAYER | 0.2 | The player is under open sky and standing room is found at the surface for at least 4 of 6 points 45-51 blocks away spread over 60° of where they look | Six silent, unhurtable mannequins in random default skins stand there, all facing the player. All gone at once, silently, when the player looks within 10° of any of them, comes within 20 blocks of any, or after 30 s |
| `blade_stalker` | 382 | LIGHT | PRESENCE, PLAYER | 0.25 | Standing room 16 blocks behind the player (140-180° from where they look) | A silent, unhurtable mannequin in a random default skin crouches with an iron sword and creeps toward the player at 1.3 blocks/s, facing them. It does not move while the player looks within 25° of it, and is gone after 2 s of being looked at in all, or when it comes within 3 blocks, or after 30 s |
| `cliff_faller` | 386 | LIGHT | PRESENCE, PLAYER | 0.25 | Standing room 12-22 blocks away with flat ground within 6 blocks in some direction ending in an 8-40 block drop onto solid ground (24 tries) | A silent, unhurtable mannequin in a random default skin sprints along that line off the edge and falls; after 5 blocks of falling it makes the player death sound and is gone, silently, with no body. Gone too if the player comes within 6 blocks or after 15 s |
| `lake_ferry` | 383 | LIGHT | PRESENCE, PLAYER | 0.2 | A lake (as for `lake_fisher`) within 16 blocks, with a surface point at least 10 blocks from the player | A silent, unhurtable mannequin in a random default skin sits in an invulnerable, silent oak boat there (real entities, removed with the figure) and sails across the player's view along the longer stretch of open water at 2.4 blocks/s, with paddle sounds. Gone, with the boat, when the player comes within 6 blocks, when the water ends, or after 30 s |
| `cave_cart` | 416 | LIGHT | PRESENCE, PLAYER | 0.2 | The player is below y 50 with no open sky above, and cave standing room 14 blocks away within 10-40° of where they look | A silent, unhurtable mannequin in a random default skin sits in an invulnerable, silent minecart (real entities, no rails needed) that rolls along the ground across the view at 6 blocks/s, going round or up and down small obstacles, with rolling sounds. Gone, with the cart, when the player comes within 5 blocks, when no way on is left, or after 20 s |
| `roof_skeleton` | 393 | LIGHT | MOBS | 0.25 | A stair or slab within 16 blocks, at least 8 away and 2 above the player, with open sky and two free blocks over it | Only this player's client is told of a skeleton standing on it with its bow drawn and no arrow, facing the player. Gone when looked at for 6 checks (every 5 ticks, within 25°), within 6 blocks, or after 20 s. Nothing on the server |
| `water_guardian` | 404 | LIGHT | WATER | 0.2 | Source water at least 4 deep with air above, within 20 blocks, at least 6 away | Only this player's client is told of a guardian 2 blocks under the surface whose beam is set on the player, with its attack sound, for 4 s. Nothing on the server |
| `far_warden` | 406 | LIGHT | MOBS | 0.15 | Overworld, standing room 30 to 42 blocks away | Only this player's client is told of a warden there with its emerge animation and sound; after 6.7 s and 2 s of standing it digs back in (dig animation and sound) and is gone 5 s later. Nothing on the server |
| `swamp_witch` | 407 | LIGHT | MOBS | 0.25 | Swamp or mangrove swamp; a glass block or pane, or a one-block gap over a plank sill walled on both sides, within 12 blocks and 4 away, with the player outside and standing room inside | Only this player's client is told of a witch standing inside the window, facing the player. Gone when looked at for 8 checks (within 20°), within 2.5 blocks, or after 20 s. Nothing on the server |
| `wall_shulker` | 408 | LIGHT | MOBS | 0.2 | A free floored block under cover against a solid wall block within 8 blocks, 3 away and within 2 of the player's height | Only this player's client is told of a shulker on that wall; after 1 s it opens its shell fully with the open sound, 3 s later shuts it with the close sound, and is gone 0.6 s after. Nothing on the server |
| `allay_thief` | 410 | LIGHT | ITEMS | 0.25 | The player carries anything | Only this player's client is told of an allay 3 blocks away at eye height, within 50° of where they look, holding a copy of one of their stacks (nothing is taken), that flies 45 blocks straight away and 6 up in 5.5 s with its item sound. Nothing on the server |
| `forest_creaking` | 411 | LIGHT | MOBS | 0.2 | Forest biome, standing room 24 to 36 blocks away | Only this player's client is told of a creaking that walks to the player at 0.175 blocks/tick, following the ground, but only while they are not looking at it (within 50° with a clear line); gone within 3 blocks, after 60 s, or when stuck for 20 steps. Nothing on the server |
| `cow_herd_blink` | 412 | LIGHT | MOBS | 0.2 | The client can show the blink (the mod), grass under open sky for at least 3 of up to 6 cows 20 to 26 blocks ahead within 35° | Only this player's client is told of the cows. When the player has looked at the first for 4 checks (within 40°, every 5 ticks) the view blinks once and they are gone at its darkest; otherwise gone within 8 blocks or after 30 s. Nothing on the server |
| `spyglass_figure` | 417 | LIGHT | PRESENCE | 0.25 | The player carries a spyglass | Waits up to 2 min for the spyglass to be raised, then tells the client of a villager 48 blocks off along where they look (on the ground, up 6 or down 10); it is removed when the spyglass is lowered or after 1 min. Nothing on the server |
| `flash_figure` | 491 | LIGHT | PRESENCE | 0.2 | Overworld, raining, sky visible | 4 visual-only bolts, 2 s then 2 to 4.5 s apart; at each, only this player's client is told of a wither skeleton on one bearing (within 40° of where they look) for 4 ticks, 34 blocks away at the first flash and 7 closer each time (not under 10), with the bolt 8 blocks beyond it. Nothing on the server |
| `netherrack_behind` | 284 | LIGHT | BLOCKS | 0.3 | A solid block with a free face 2 to 8 away, more than 100° from where the player looks | Only this player's client is told that the 40 nearest such blocks are netherrack. They get the real blocks back when the player looks within 70° of any of them, or after 30 s. Nothing on the server (with the `swaps` setting `REAL`, changed for 30 s) |
| `roof_gone` | 289 | LIGHT | BLOCKS | 0.3 | Indoors, with at most 6 blocks of roof over the player's head; at least 4 blocks to hide | Only this player's client is told that every block from 2 above the player to the sky, 2 blocks to either side, is air. 20 s later it gets the real blocks back. Nothing on the server |
| `path_rearranged` | 300 | LIGHT | BLOCKS | 0.3 | At least 8 floor blocks 3 to 10 behind the player (1 to either side) of at least two kinds | Only this player's client is told that those floor blocks are shuffled among each other. They get the real blocks back when the player looks within 70° of any of them, or after 30 s. Nothing on the server |
| `landmark_tree_gone` | 303 | LIGHT | BLOCKS | 0.2 | The player's bed is in this dimension; a tree of at most 250 blocks and 8 leaves within 24 of it, 18 to 64 from the player | Only this player's client is told that the tallest such tree (logs and leaves) is air. It gets the real blocks back when the player comes within 12 of it, or after 60 s. Nothing on the server |
| `tunnel_filled` | 305 | LIGHT | BLOCKS | 0.3 | Under no sky; at least 6 walkway cells 2 to 10 behind the player (1 to either side) with a solid block 2 above and no sky | Only this player's client is told that both blocks of each such cell are stone. They get the real blocks back when the player looks within 70° of any of them, or after 30 s. Nothing on the server. Not limited to tunnels the player dug |
| `ceiling_furniture` | 309 | LIGHT | BLOCKS | 0.3 | Indoors, an air block under the ceiling 6 blocks around the player, at least 1.5 from them and above their feet | Only this player's client is told that up to 6 such blocks hold an upside-down stair or slab, or a bed (two blocks), at random facings. 30 s later it gets the real blocks back. Nothing on the server |
| `sea_lights` | 479 | LIGHT | WATER | 0.3 | In an ocean biome, a straight run of at least 8 water blocks 3 under the surface, starting 6 to 18 away | Only this player's client is told of a sea lantern that moves along the run, one block every 0.2 s, 2 lit at a time, each put back behind it. Nothing on the server |
| `crafter_crafts` | 216 | LIGHT | BLOCKS | 0.3 | A crafter within 12 | The crafter shows its crafting state (6 ticks) with the craft sound and particles, and ejects a bone, skeleton skull or paper named after the player from its face; the item vanishes after 1 min. Its real contents are not touched |
| `golem_wrong_chest` | 223 | LIGHT | MOBS | 0.3 | A copper golem within 12, a copper chest with an item and another, non-adjacent chest with a free slot | One stack moves from the copper chest to the other chest, with the golem's get and drop animations and chest sounds, 1 s apart. The move stays |
| `jukebox_quiet` | 228 | LIGHT | SOUND | 0.3 | A playing jukebox within 12 and a chest with a free slot | The disc is taken out of the jukebox, so the music stops, and put into a free slot of a chest. The move stays |
| `scaffold_collapses` | 232 | HARMFUL | BLOCKS | 0.2 | Standing on a scaffolding column on solid ground, a fall of under 4 blocks | Three creaks, 14 ticks apart, then every scaffolding block of the column breaks and drops (only if the player still stands on it) |
| `wall_map_shows` | 238 | LIGHT | BLOCKS | 0.3 | A framed filled map within 12 and a territorial spirit whose reach holds the player, with the 128-block area around its anchor loaded | The framed map is replaced by a new map of the area around the spirit's anchor; the original is put back after 2 min (stays under hardcore) |
| `spirit_candle_out` | 527 | LIGHT | LIGHTS | 0.3 | The spirit candle in a hand, not already out | The candle's flame, lean and blue warning stop for 30 s, with the extinguish sound and a puff of smoke |
| `holy_water_cloudy` | 534 | HARMFUL | ITEMS | 0.2 | Clear holy water in the inventory | Every holy water stack gets a "cloudy" mark and a lore line; the altar refuses it until the next dawn, then the mark is removed. Stays |
| `mirror_names` | 535 | LIGHT | ITEMS | 0.25 | A mirror in the inventory, a spirit that senses the player, none already marked | A soft chime marks the mirror. The next time a mirror is placed on the altar, the name of a spirit around the player is shown in chat, once. The mark lapses after 30 min |
| `chat_repeats` | 354 | LIGHT | SOUND | 0.3 | The player said a line of at most 100 characters between 10 s and 10 min ago | 2 to 12 s later `<???> <the line>` appears as a grey italic system message; the line is used once |
| `villagers_at_windows` | 257 | LIGHT | MOBS | 0.3 | Overworld at night, a villager within 20 and a window (glass or pane with open air on both sides) within 12 with a roofed floor cell at its foot | Up to 3 villagers wake, walk (up to 20 s) to the nearest unclaimed window, then lose their AI and face the player for 30 s; the AI comes back after |
| `strange_trader` | 260 | LIGHT | MOBS | 0.2 | Overworld at night, standing room 12 blocks away within 40° of where the player looks | A real wandering trader (no AI, facing the player) with offers from `offers` (a bone for the player's head, essence for echo shards, ...), 1 use each. Gone once the player has seen it and looks away (unless trading), or after 2 min |
| `golem_in_corner` | 261 | LIGHT | MOBS | 0.3 | An idle iron golem within 40, and a roofed floor cell in a corner (two walls) within 6 of the player, at least 2 away | The golem walks there (up to 30 s), loses its AI and faces the corner for 30 s; the AI comes back after |
| `dolphins_lead` | 267 | LIGHT | WATER | 0.3 | The player in water, a dolphin in water within 32 | Up to 3 dolphins swim 16 blocks at a time toward the spirit's point (60 blocks away at random if none sent it), calling now and then; one more than 12 blocks from the player stops and faces them. 30 pulses of 1 s |
| `spiders_at_window` | 272 | LIGHT | MOBS | 0.25 | At least 2 idle spiders within 24, and a window within 12 with open ground under it outside (up to 3 below) | Up to 5 spiders walk to the ground at the foot of that window, lose their AI and face it for 30 s; the AI comes back after |
| `squid_goes_dark` | 278 | LIGHT | WATER | 0.35 | A glow squid within 32 that is glowing | Each such squid has its dark ticks set to 60 s (the glow is off, then returns by itself) |
| `pearl_strays` | 420 | HARMFUL | ITEMS | 0.15 | An ender pearl in the inventory | For 5 min the first pearl the player throws is moved above standing room 8 to 24 blocks away and dropped, so the player lands there (with the usual 5 damage) |
| `hoglins_charge` | 448 | HARMFUL | MOBS | 0.2 | Nether, at least 2 adult hoglins within 24, farther than 3.2 blocks from the player | Each is set on the player for up to 5 s; one that comes within 3.2 blocks loses its AI and faces the player for 4 s, then gets it back and is told to avoid the player for 10 s |
| `haunted_painting` | 492 | LIGHT | BLOCKS | 0.3 | A painting within 12 that is not the mod's own and has a mod variant of the same size (1x1 eyes, 1x2 portrait, 2x1 house at night, 2x2 dusk) | Becomes a mod variant of its size; put back after 1 min unless hardcore |
| `painting_darkens` | 493 | LIGHT | BLOCKS | 0.25 | A 2x2 painting within 12 | Steps through four dusk variants, 20 s apart, a figure nearer each time; put back after 3 min unless hardcore |
| `pale_pet` | 494 | LIGHT | MOBS | 0.25 | Night, and a tame wolf or cat owned by the player within 24 | One pet gets the pale coat; put back at dawn, no sooner than 5 min, unless hardcore |
| `pale_herd` | 495 | LIGHT | MOBS | 0.3 | A cow, pig or chicken within 24 | Up to 4 of them get the pale coat; put back after 2 min unless hardcore |
| `spirit_song` | 496 | LIGHT | SOUND | 0.25 | A jukebox with no disc within 16 | A disc with the spirit song (the moan recording) is put in and plays; taken out after 30 s if still there (stays under hardcore) |
| `spirit_horn` | 497 | LIGHT | SOUND | 0.25 | In the overworld | The spirit horn instrument's sound, low, 40 blocks away or from the spirit's point. Nothing changes |
| `sigil_banner` | 498 | LIGHT | BLOCKS | 0.3 | A banner within 12 with room for another pattern | A black spirit sigil added; removed after 2 min unless hardcore |
| `whispering_item` | 500 | LIGHT | ITEMS | 0.25 | The player holds an item that has no Curse of the Restless | The held item gets the curse (it whispers now and then while held); removed from the inventory after 3 min unless hardcore |
| `hollow_trim` | 501 | LIGHT | ITEMS | 0.25 | Worn trimmable armor with no trim | One worn piece shows a hollow trim in quartz; removed after 2 min unless hardcore |
| `counting_children` | 516 | LIGHT | SOUND | 0.3 | Under a roof | Ten whispers in a rising child's pitch 8 blocks away, then ten steps closing from 8 to 2.5 blocks with a door half way and a laugh. Nothing changes |
| `wall_scratching` | 517 | LIGHT | SOUND | 0.3 | Under a roof | 14 scratches, 0.25 s apart, 3 blocks away, each 0.7 blocks further along a wall. Nothing changes |
| `chest_knocks` | 518 | LIGHT | SOUND | 0.3 | A chest, trapped chest or barrel within 12 | Four muffled knocks from it, 0.7 s apart. Nothing changes |
| `bedroom_lullaby` | 519 | LIGHT | SOUND | 0.25 | A bed within 14 | A 23-note flute melody from the bed. Nothing changes |
| `abandoned_camp` | 375 | LIGHT | BLOCKS | 0.3 | Overworld; open standing room for a 4×3 camp 6–14 away; the spirit still has a book page it has not said | A bed, a campfire and a chest holding a signed book in the spirit's voice (a page from a pool, never repeated for the same spirit); every block is removed after 10 min unless it was changed, a chest with the player's own items stays; stays under hardcore |
| `dirt_hut` | 381 | LIGHT | BLOCKS | 0.3 | Overworld; open ground for a 3×3 hut 5–12 away | A dirt hut with a two-high doorway and a roof; removed block by block after 7.5 min, stays under hardcore |
| `overnight_shrine` | 520 | LIGHT | PRESENCE | 0.3 | Overworld; open ground for a 3×3 shrine 6–14 away; a sign text the spirit has not used | A row of bone blocks with two skulls and lit candles on top and a sign with a few words in front; phantom (only this player's client is told, see `Swaps`); gone after 10 min |
| `far_house` | 521 | LIGHT | LIGHTS | 0.2 | Overworld at night, under a roof; open ground 28–44 away | The 9×9×5 volume round the player is shown as fake blocks on open ground far off, with a lantern lit on the floor; only this player's client, put back after 30 s |
| `ash_sign` | 526 | LIGHT | PRESENCE | 0.3 | Under a roof; bare floor ahead (3 away, within 25° of the view) for the shape | Ash laid as a ring of radius 3 or as the initial of the spirit's name, readable from the player's side (cells on bare floor only, at least 70%); swept away after 5 min, stays under hardcore |
| `screen_handprint` | 512 | LIGHT | PRESENCE | 0.3 | The client has the mod | A pale handprint over the player's view for 3.5 s (peak 80%), quick in and slow out; client-side overlay under the HUD |
| `screen_frost` | 513 | LIGHT | PRESENCE | 0.3 | The client has the mod | Frost along the screen's edges for 8 s (peak 70%), in over 2.4 s and out over 4 s; client-side overlay |
| `screen_face` | 514 | LIGHT | PRESENCE | 0 (off) | The client has the mod | A pale face over the view for 2 ticks (90%); weight 0, so it never runs until a player raises it |
| `eyes_in_dark` | 511 | LIGHT | PRESENCE | 0.3 | The client has the mod; standing room 7–14 blocks ahead (within 60°) with light level at most 3 | A pair of eyes hangs 1.5 blocks above that spot for 2–3 s, blinks once and goes out; a client-only particle, fully lit |
| `photo_figure` | 529 | LIGHT | ITEMS | 0.2 | A developed photo in the inventory | A pale hollow-eyed figure 34–60 px tall is painted into that photo's picture; the change is kept |
| `frost_prints` | 502 | LIGHT | PRESENCE | 0.3 | Indoors, glass or a glass pane within 8 with a free block on its far side from the player | Up to 3 frost handprints appear one by one, 1.5 s apart, on the outer face of the glass, each with a tap on glass. Each is removed after 2 min; under hardcore they stay |
| `frost_writing` | 503 | LIGHT | PRESENCE | 0.25 | Indoors, a run of glass within 8 with free blocks on the player's side, long enough for a phrase from the config | A phrase (`let me in`, `cold`, `help`, `open`) forms word by word, 2 s apart, one word per window, read left to right from the player's side. Each word is removed after 2 min; under hardcore they stay. The name of the player is not written: the glyph set is a few fixed words |
| `claw_marks` | 504 | LIGHT | DOORS | 0.3 | Indoors, a closed door within 8 with free blocks on the player's side | Claw marks appear on the player's side of both halves of the door with a scrape. Removed after 3 min; under hardcore they stay |
| `doll` | 505 | LIGHT | PRESENCE | 0.25 | A spot of free floor under a roof within 10, 4+ away and outside 55° of where the player looks | A doll appears and, whenever it is not watched, turns to face the player and every 4th check (20 ticks) hops to another such spot, up to 4 times. It keeps still while watched. Gone after 2 min or when broken |
| `music_box` | 506 | LIGHT | SOUND | 0.25 | A spot of free floor under a roof within 10, 3+ away and outside 55° of where the player looks | An open music box appears and plays a 14-15 note tune on vanilla chime sounds (no sound file of its own), then shuts; gone 10 s later |
| `rocking_chair` | 507 | LIGHT | BLOCKS | 0.2 | A spot of free floor under a roof within 10, 3+ away and outside 55° of where the player looks | A rocking chair appears and leans forward and back 24 times with a creak, then is gone 5 s later |
| `rotten_planks` | 508 | LIGHT | BLOCKS | 0.25 | Indoors, planks within 10 | One plank turns into rotten planks, then the rot spreads to touching planks, 2 per second, up to 24 blocks. They are put back after 2 min; under hardcore they stay |
| `black_candle` | 509 | LIGHT | LIGHTS | 0.25 | Indoors, an unlit candle within 10, or free floor to set one on | The candle turns black and lights itself (or a lit black candle is set down), with soul fire flames for 20 s. Put back after 2 min (stays under hardcore) |
| `cracked_mirror` | 510 | LIGHT | BLOCKS | 0.25 | Indoors, a wall at head height within 8, 2+ away and outside 60° of where the player looks | A cracked mirror appears on the wall. When the player looks at it from within 6 blocks, a dark figure shows in it for 2 s. Gone after 30 s more |
| `hurled_block` | — | HARMFUL | ASSAULT | 0.4 | Not peaceful, health above 5 hearts, a loose block (gravel, cobblestone and the like) within reach | A loose block nearby shivers and lifts in a cloud of dust, then flies at you. Step aside. |
| `flung_tools` | — | HARMFUL | ASSAULT | 0.3 | Not peaceful, health above 5 hearts, a chest with tools in it nearby | A chest nearby rattles and opens, and the tools inside fly at you one by one. Dodge them or empty the chest. |
| `volley` | — | HARMFUL | ASSAULT | 0.4 | Not peaceful, health above 5 hearts, light level below `dark_below` | A skeleton rattles somewhere in the dark, then a few arrows come from that one direction. Take cover. |
| `cave_in` | — | HARMFUL | ASSAULT | 0.3 | Not peaceful, health above 5 hearts, a solid roof overhead | Dust and creaking overhead, then gravel falls on the spot where you stand. Step out from under it. |
| `dripstone_rain` | — | HARMFUL | ASSAULT | 0.3 | Not peaceful, health above 5 hearts, a solid roof overhead | Pointed dripstone breaks off the ceiling around you, wave after wave. Keep moving. |
| `possessed_mob` | — | HARMFUL | ASSAULT | 0.4 | Not peaceful, health above 5 hearts, a hostile mob nearby | A hostile mob nearby is lit by spirit fire, then turns fast and strong and comes for you. Kill it. |
| `frenzy` | — | HARMFUL | ASSAULT | 0.3 | Not peaceful, health above 5 hearts, at least `min_mobs` hostile mobs nearby | Every hostile mob nearby is seized by spirit fire, then turns fast and strong. Fall back. |
| `turned_pets` | — | HARMFUL | ASSAULT | 0.3 | Not peaceful, health above 5 hearts, the player has a tamed wolf or cat nearby | Your tamed wolves and cats stand and growl, then go for you for a while. Walk away. |
| `vex_swarm` | — | HARMFUL | ASSAULT | 0.3 | Not peaceful, health above 5 hearts | A cold wail, then a few short-lived vexes named "?" come out of the air. Fight back. |
| `ghost_creeper` | — | HARMFUL | ASSAULT | 0.3 | Not peaceful, health above 5 hearts | A hiss behind you, then a small blast there that breaks no blocks. Run on the hiss. |
| `stalker` | — | HARMFUL | ASSAULT | 0.3 | Not peaceful, health above 5 hearts, standing room for the figure in line of sight | A figure appears far off and stands watching, then runs at you, hits once and is gone. Outrun it. |
| `hail_of_bones` | — | HARMFUL | ASSAULT | 0.3 | Not peaceful, health above 5 hearts, overworld, night, open sky | Rattling in the night sky, then blocks of bone fall around you. Take cover under a roof. |
| `rake` | — | HARMFUL | ASSAULT | 0.5 | Health above 5 hearts | Three scratches in the dark, then three blows. Moving between them is enough to be missed. |
| `teeth_in_dark` | — | HARMFUL | ASSAULT | 0.5 | Health above 5 hearts, light level 0 | A growl where the light is gone, then a bite every few seconds. Find light. |
| `searing_armor` | — | HARMFUL | AFFLICTION | 0.4 | Health above 5 hearts, a non-leather chestplate worn, dry, not fire resistant | The chestplate glows and burns every second. Take it off or jump into water. |
| `frost_grip` | — | HARMFUL | AFFLICTION | 0.4 | Health above 5 hearts, can freeze, not warm (no fire or lava nearby, not in water) | A cold like powder snow on dry land, and it bites. Get to fire or a light. |
| `pinned` | — | HARMFUL | AFFLICTION | 0.4 | Health above 5 hearts, not slowed, a hostile mob nearby | Heavy limbs, and every hostile nearby turns on you. Fight or wait it out. |
| `throat` | — | HARMFUL | AFFLICTION | 0.4 | Health above 5 hearts, not in water, no bad luck effect | Air drains on dry land and a whisper says hush. Crouch and wait, or drink milk. |
| `undertow_dry` | — | HARMFUL | ASSAULT | 0.4 | Health above 5 hearts, sprinting | Something drags you back while you sprint. Stop running. |
| `lantern_burst` | — | HARMFUL | ASSAULT | 0.4 | Health above 5 hearts, a torch or lantern within reach | A torch or lantern sputters, then bursts into fire. Stay away from the lights. |
| `blood_cough` | — | HARMFUL | AFFLICTION | 0.4 | Health above 5 hearts, no nausea | A wet cough every few seconds, five in all. Milk ends it. |
| `wrong_step` | — | HARMFUL | BLOCKS | 0.4 | On the ground, standing room 1 block ahead | The ground ahead crumbles into a hole, a fall of one to three blocks. Watch where you step. |
| `ash_storm` | — | HARMFUL | ASSAULT | 0.5 | Health above 5 hearts, in the Nether | A wind of soul fire in the Nether. Take shelter behind a wall. |
| `hand_below` | — | HARMFUL | ASSAULT | 0.4 | Health above 5 hearts, on the ground | A circle of souls under your feet. Stay on it and a hand takes hold. Step off. |
| `blade_behind` | — | HARMFUL | ASSAULT | 0.4 | Health above 5 hearts | A sweep sounds at your back. Turn to face it before it lands. |
| `door_bite` | — | HARMFUL | DOORS | 0.3 | Health above 5 hearts, a door in the player's path | A door swings open by itself and slams on whoever passes. Use another way. |
| `night_terror` | — | HARMFUL | ASSAULT | 0.5 | Health above 5 hearts, the player is asleep with at least a second of the night left | A heartbeat in your sleep, then you are thrown awake screaming. Wards hold it off. |
| `stone_fists` | — | HARMFUL | ASSAULT | 0.4 | Health above 5 hearts, leaning on a wall | The wall you lean on rumbles and strikes. Step away from it. |
| `final_blow` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, health above `min_health` | A roar and a ring of souls. Whoever is still inside it when it ends is struck down to half a heart. |
| `the_hanging` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, on the ground, not in water, no slow falling | You are lifted high into the air, then let go. Slow falling, water, or a block under you softens the drop. |
| `held_under` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, under water, no water breathing | In deep water something pushes you down until your breath is gone. Swim sideways out of the column. |
| `pyre` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, not in water, not fire immune | A fire that will not go out, and fire resistance fails. Water puts it out. |
| `withering` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, no wither effect | A wither that goes on and on. Milk ends it. |
| `count` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, not in light | A countdown on the screen: three, two, one. Reach light or holy ground before it ends. |
| `the_hollow` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, standing room for a hostile | A named creature of the spirit hunts you, and its blows cannot kill. Kill it and the spirit calms. |
| `ledge_push` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, at the edge of a drop | A strong shove at the edge of a drop. Crouch when the warning comes. |
| `circle_of_knives` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts | Arrows hang in a ring around you, then close in. Break out of the ring. |
| `smother_brink` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, not in water, light level below `dark_below` | The air is taken from you in the dark, past the safe point. Find light. |
| `embrace_brink` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts | Cold hands close around you, harder each time, past the safe point. Get ten blocks away. |
| `red_room` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, standing room for a figure | Darkness, a heartbeat, and a figure in sight that strikes every second you look at it. Look away. |
| `drain` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts | Your health drains a heart a second and your hunger bar empties. Eat. |
| `bound` | — | HARMFUL | ASSAULT | 0.3 | Power ≥ 0.6, not peaceful, health above 2 hearts, standing room for a hostile | You are held in place while a hostile is dragged to you. Kill it. |
| `judgement` | — | HARMFUL | ASSAULT | 0.2 | Evolved `demon` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown | Three knocks, a bolt that misses, then a bolt that kills whoever has not moved. |
| `reaping` | — | HARMFUL | ASSAULT | 0.2 | Evolved `shade`/`wraith` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown, standing room for the shadow | A shadow stands far off and walks closer whenever the player looks away. If it reaches them, its touch kills. It freezes while watched. |
| `last_call` | — | HARMFUL | ASSAULT | 0.2 | Evolved `lost_one` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown, night, lava or a bottomless column near | A moan, then a voice draws the player toward lava or the void and does not stop at any floor. Walking far enough away breaks it. |
| `drowner` | — | HARMFUL | ASSAULT | 0.2 | Evolved `nix` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown, in water | In water, bubbles and a gurgle, then the player is held under until the water takes them. Leaving the water in time, or breathing under it, saves them. |
| `the_avenger` | — | HARMFUL | MOBS | 0.2 | Evolved `revenant` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown, a killing spree: `min_kills` fresh mob kills, and standing room for it | After a killing spree, a named avenger rises and hunts the player to the death. Beating it spends the spirit's anger. |
| `hellring` | — | HARMFUL | ASSAULT | 0.2 | Evolved `demon` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown | A roar and a ring of fire that closes in over ten seconds. Whoever is still inside when it shuts burns to death. |
| `the_double` | — | HARMFUL | ASSAULT | 0.2 | Evolved `phantom` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown, standing room for the double | A copy of the player walks up and kills with one touch. Three blows from the player drive it away. |
| `foretold` | — | HARMFUL | ASSAULT | 0.2 | Evolved `banshee` at power ≥ 0.9, not peaceful, lethal setting allows it, warned by a brink event, off cooldown | A bell and a true omen in the journal: at dusk the spirit takes the player, unless they are in a bed or inside a ward by then. |
| `phantom_self_join` | — | LIGHT | PRESENCE, PLAYER | 2.0 | None | Only this player gets "<name> joined the game" and a player list entry with their own name and skin (a fresh id). 40 s later the entry goes and "<name> left the game" follows. Nothing on the server. Only `phantom`. |
| `nix_catch` | — | LIGHT | WATER | 2.0 | Water within 16 with a bank of 3 free cells beside it | 3 to 5 raw cod and salmon lie in a row on the bank, with a splash. Real items. Only `nix`. |
| `nix_call` | — | LIGHT | WATER | 2.0 | The player is out of water, water 3 to 12 away that runs 3+ cells toward them | A line of bubbles runs through the water toward the player, one cell per 3 ticks, and a conduit sound plays at its end. Only `nix`. |
| `mimic_voice` | — | LIGHT | MOBS | 2.0 | An adult animal within 12 | The animal makes another mob's sound 3 times, a second apart: a cow or mooshroom the villager's "hmm", a sheep the zombie's groan, a chicken an ocelot's mew, any other one of those three. Only `mimic`. |
| `wraith_taken` | — | HARMFUL | MOBS | 2.0 | Light level under 4 at the player, a wild animal within 16 (not named, tamed, leashed or ridden) | The animal is gone in a puff of smoke, no sound and no drops. Lost for good. Only `wraith`. |
| `domovoi_supper` | — | LIGHT | ITEMS | 2.0 | A chest within 12 holding bread, a cookie, a milk bucket or a stew (mushroom, rabbit, beetroot) | One is eaten, with an eating sound: one bread or cookie is gone, the milk bucket becomes a bucket, the stew a bowl. Only `domovoi`. |
| `domovoi_hearth` | — | LIGHT | LIGHTS | 2.0 | A cold campfire within 12, or a furnace, smoker or blast furnace with input and no fuel | The campfire lights, or one coal goes into the furnace. Only `domovoi`. |
| `hoarder_payment` | — | HARMFUL | ITEMS | 2.0 | A chest within 10 holding something that is not junk | One stack becomes junk (dirt, sticks, rotten flesh or string) of the same size, up to its stack limit. The hoarder keeps the stack for its nest. Only `hoarder`. |
| `demon_offer` | — | LIGHT | BLOCKS | 2.0 | Standing room 3 to 8 away, an offer left unsaid | A crimson sign with an offer ("a diamond for your bed") stands there for `restore_ticks` (5 min), turned to the player. Nothing more. Never repeats a variant. Only `demon`. |
| `lost_one_sign` | — | LIGHT | BLOCKS | 2.0 | Standing room 3 to 8 away, a question left unasked | An oak sign that asks the way ("which way is home?") stands there for `restore_ticks` (5 min). Never repeats a variant. Only `lost_one`. |
| `banshee_letter` | — | LIGHT | ITEMS | 2.0 | Night, a free bed within 16, a page left unsaid | A written book "To <player>", author the spirit's nickname, with one of 8 pages (shy, devoted, possessive), lies on the bed. Real item. Never repeats a variant. Only `banshee`. |
| `banshee_gift` | — | LIGHT | ITEMS | 2.0 | A free bed within 16, a gift left unsaid | One named item with a lore line lies on the bed: wither rose "For you", gold nugget "Ring", phantom membrane "Veil", skeleton skull "Mine" or poppy "Remember me". Real item. Only `banshee`. |
| `banshee_serenade` | — | LIGHT | SOUND | 2.0 | Night, glass or a glass pane within 12 | A quiet, low `haunt.moan` from 2 blocks outside the glass, on the far side from the player. Only `banshee`. |
| `banshee_jealous` | — | LIGHT | MOBS | 2.0 | An adult villager, wolf, cat or parrot within 8 | It shakes with frost as in powder snow for 3 s, and its health is put back after. A villager shakes its head, a cat hisses, a wolf growls, a parrot imitates a ghast. Only `banshee`. |
| `poltergeist_toss` | — | LIGHT | ITEMS | 2.0 | A chest with something in it within 10 | The lid opens and 3 stacks hop out toward the player, one a second, then the lid shuts with a giggle. The items stay on the floor. Only `poltergeist`. |
| `poltergeist_play` | — | LIGHT | DOORS | 2.0 | 2 or more note blocks or bells within 12 | Each is struck in turn, nearest first, 3 ticks apart, up to 12. Buttons and levers are left alone. Only `poltergeist`. |
| `poltergeist_tantrum` | — | HARMFUL | DOORS | 2.0 | 3 or more wooden doors, trapdoors or fence gates within 12 | All of them fly open in one tick, 6 ticks later slam shut, and up to 8 flower pots pop off: pot and plant drop as items. Only `poltergeist`. |
| `shade_gathered` | — | LIGHT | LIGHTS | 2.0 | 3 or more torches within 12, a dark spot (light under 4) 3 to 16 away | The torches (up to 6) go out one by one, 8 ticks apart, and their items lie in one pile on the dark spot. Nothing is lost. Only `shade`. |
| `shade_glance` | — | LIGHT | LIGHTS | 2.0 | 3 or more torches or lanterns within 16 | For 20 s every torch or lantern the player looks at (within 16° and in sight) goes out, up to 5. They are lit again after 2 min; under hardcore they stay out. Only `shade`. |
| `shade_room` | — | HARMFUL | LIGHTS | 2.0 | The player is in a room shut off from the sky of at most 150 floor cells, with a torch or lantern in it | Every torch and lantern in the room goes out and Darkness is renewed each second while the player stays. They are lit again when the player leaves, at most after 3 min. Only `shade`. |
| `wraith_steps` | — | LIGHT | PRESENCE | 2.0 | Light level under 4 at the player, not in water | For 30 s a footstep sounds 3 blocks behind the player every 8 ticks they move, with the sound of the ground they stand on, and none while they stand still. Only `wraith`. |
| `wraith_edge` | — | LIGHT | PRESENCE | 2.0 | The player stands in light (8+), a dark floor cell (under 4) 4 to 6 away | A figure stands on the dark cell facing the player, gone when looked at straight or approached within 3. Only `wraith`. |
| `phantom_rerun` | — | LIGHT | PLAYER, PRESENCE | 2.0 | The last 30 s of the player's path (one sample a second) spans 20 or more blocks | A figure in the player's own skin walks that path, smoothed, from where it began, gone when looked at or approached. Only `phantom`. |
| `phantom_home` | — | LIGHT | PLAYER, PRESENCE | 2.0 | The player's respawn bed stands 12 to 32 away in the same dimension | A figure in the player's own skin stands by the bed, gone when the player comes within 8. Only `phantom`. |
| `nix_returns` | — | LIGHT | WATER | 2.0 | An item entity lying in water within 16 | It is carried through the water toward the player, dripping, until it leaves the water. Only `nix`. |
| `mimic_parrot` | — | LIGHT | PLAYER | 2.0 | The player said something in chat in the last 10 min | 5 s later the last line comes back as a chat message under the player's own name, with one word swapped for one from another recent line or a fixed word. Chat only. Only `mimic`. |
| `mimic_chest` | — | HARMFUL | BLOCKS | 2.0 | A chest within 10 with a free cell beside it (not touching another chest) | A second chest appears there. Opened, it shuts with evoker-fang sound and 2 damage (capped by the threat floor) and is gone; left alone it is removed after 2 min. Only `mimic`. |
| `mara_dream` | — | LIGHT | AFFLICTION | 2.0 | The player woke in the last 2 min, a free inventory slot, a dream left unsaid | A written book "A dream" with one of 6 lines about the biome the player was in and, if they have killed any, the last mob. Real item. Only `mara`. |
| `mara_stolen_rest` | — | HARMFUL | AFFLICTION | 2.0 | Woke in the last 2 min, not peaceful, time since rest under 80000 | Time since rest is set to 80000 ticks, past the 72000 phantoms go by. Only `mara`. |
| `mara_weight` | — | HARMFUL | AFFLICTION | 2.0 | Woke in the last 2 min | Slowness and mining fatigue for 60 s, with a heavy step and a wool step. Only `mara`. |
| `revenant_ledger` | — | LIGHT | ITEMS | 2.0 | A lectern within 12, 5 or more passive animals killed today | A written book "Ledger" on the lectern lists each kind killed today and its count. The lectern's own book (or none) is put back after 2 min. Only `revenant`. |
| `revenant_tally` | — | HARMFUL | MOBS | 2.0 | Night, not peaceful, a passive animal killed today, standing room 8 to 16 away | A zombie (threat band) named with the most-killed kind and its count, "Cow 14", goes for the player. Only `revenant`. |
| `revenant_rises` | — | HARMFUL | MOBS | 2.0 | Not peaceful, the player killed a mob within 32 in the last 10 min | A skeleton (a skeleton horse for a horse, donkey or mule) climbs out of the ground there, with dirt flying, and goes for the player. Only `revenant`. |
| `lost_one_wanderer` | — | LIGHT | PRESENCE, PLAYER | 2.0 | Standing room 20 to 30 away, a line left unsaid | A figure in a default skin walks a slow circle there. When the player comes within 8 it says a line in chat under a made-up nickname and is gone. Only `lost_one`. |
| `lost_one_death_path` | — | LIGHT | PRESENCE | 2.0 | The last death is in this dimension 16 to 64 away, 3 or more spots for prints on the way | Footprints appear one by one, 10 ticks apart, on the line from 4 to 14 blocks out toward the death, each pointing at the player, from the farthest in. Wiped after 3 min. Only `lost_one`. |
| `domovoi_tidy` | — | LIGHT | DOORS | 2.0 | A chest within 12 that is not tidy, or an open wooden door, trapdoor or gate within 12 | The chest is merged and sorted by item id with no gaps, and the open doors, trapdoors and gates shut. Only `domovoi`. |
| `hoarder_shiny` | — | HARMFUL | ITEMS | 2.0 | Gold, diamond, emerald or netherite in a chest within 10, another chest (not touching) with a free slot | Only those stacks move to the other chest. Only `hoarder`. |
| `hoarder_nest` | — | LIGHT | ITEMS | 2.0 | The hoarder has taken 5 or more stacks (by `hoarder_payment`) since the server started, a spot 14 to 30 away out of the player's sight | A barrel there holds all of them (up to 27 slots; the rest drop beside it). Real items, stays. Not remembered across a restart. Only `hoarder`. |
| `demon_trail` | — | LIGHT | BLOCKS | 2.0 | The player stands on the ground | For 15 s the block under each new step shows as netherrack for 2 s, then soul soil for 2 s, then the real block. Shown to the player only, unless swaps are set to real. Only `demon`. |
| `demon_contract` | — | HARMFUL | AFFLICTION | 2.0 | A free inventory slot, no contract signed, max health at least 6 above the price | A written book "Contract" with the player's name at the bottom, and max health down by 4 (two hearts) until the next dawn. Only `demon`. |
| `trickster_gift` | — | LIGHT | ITEMS | 2.0 | Standing room 3 to 8 away | A chest there holds dirt that looks like 5 diamonds, 9 gold ingots and 4 emeralds. Taken out, it is shown as dirt again within half a second, with a laugh. The chest is gone after 3 min. Only `trickster`. |
| `trickster_dig` | — | LIGHT | BLOCKS | 2.0 | Sand, red sand or gravel with air above within 12, a joke left unsaid | It becomes suspicious sand or gravel holding a joke: a pufferfish, a poisonous potato or a stick named "Treasure". Put back after 10 min. Only `trickster`. |
| `trickster_dud` | — | LIGHT | MOBS | 2.0 | Standing room 3 to 6 away | A primed TNT (packets only) hisses there for 3 s, then pops into firework sparks with a laugh. Only `trickster`. |
| `boggart_spiders` | — | LIGHT | MOBS | 2.0 | The player is not in water | 40 spiders at a fifth of their size, client-only, appear 4 to 10 blocks out in the open, run at the player from every side, circle them at 0.6 to 1.8 blocks for six seconds, then scatter. Only `boggart`. |
| `boggart_crawl` | — | LIGHT | PRESENCE | 1.5 | The player is not in water | 24 spiders at 0.15 scale, client-only, appear 1.5 to 4 blocks out, run up close and climb in circles up the body from the feet to the head, then scatter. Only `boggart`. |
| `boggart_silverfish` | — | LIGHT | MOBS | 1.5 | The player is not in water | 40 silverfish at 0.8 scale, client-only, pour across the floor round the player, circle at their feet, then scatter. Only `boggart`. |

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
the same material (`haunt/event/kit/Variants.java`): grass to dirt or coarse dirt, dirt to coarse dirt or podzol, stone to
andesite, granite or diorite, cobblestone to mossy, stone and deepslate bricks to cracked or mossy, deepslate to
cobbled. Anything else is left alone. The patch is a scatter (`Place.patch`), not a disc.

- `haunt/event/kit/Swaps.java`: the one way events show other blocks in place of the real ones. By default an illusion
  for the haunted player alone (a `ClientboundBlockUpdatePacket`, then the real blocks again after the event's ticks);
  with the `swaps` setting set to `REAL` in `config/restless/settings.toml`, or under hardcore, a change in the world for everyone through
  `Restore.swap`. Blocks with a block entity are only ever shown. See [hardcore.md](hardcore.md#block-swaps).
- `haunt/Restore.java`: changes that swap a block or an entity (flowers, sign, painting, glass, hole, floor, cross,
  lantern, furnace fuel) register an undo that runs when due and checks that the thing is still what the
  event left, so a block the player has since changed is not overwritten. Block swaps skip neighbour updates (nothing
  attached pops off) and are retried until the chunk is loaded. Everything still pending is undone when the server
  stops (`Restless.onServerStopping`). After a crash the changes stay. Not covered: `map_marker` is permanent.
- `haunt/Hardcore.java`: in a hardcore world nothing is undone. `Restore.swap` and `Restore.revert` (the world-changing
  undos; `Restore.after` stays for cleanup of figures and doubles) register nothing, so flowers, signs, paintings,
  glass, holes, floors, crosses and furnace fuel stay as the event left them, and a flickering lantern stays out. The
  `hardcore` setting in `config/restless/settings.toml` is `AUTO` (the world's vanilla hardcore flag), `ON` or `OFF`. It is
  decided the first time it is asked and saved in `HauntData`, so editing the config later does not flip a world.
  Tricks with a real counterpart come true instead of being shown: a lie's wound, hearts, hunger, night or slot,
  real lightning, a floor that breaks, fake blocks placed for real (not over a chest or furnace), a hallucinated mob
  spawned. The list is in [hardcore.md](hardcore.md#events-with-a-real-branch), with the plan and the rules.
- `haunt/effect/DoorLocks.java`: doors locked by `locked_door`, in memory. `Restless.onUseBlock` cancels the right click on
  both sides; the loader forwards its block-interaction event to it. The server sends `DoorLockPayload` to clients
  within 64 blocks, which the loader registers and hands to `DoorLocks.put`. A client that joins or walks up later
  than the lock does not know it and sees the server undo its open attempt.

## Config

Every tunable value lives in `core/src/main/resources/restless-config/`, split into files that are slices of one tree:

| File | Holds |
|---|---|
| `haunt.json` | `director`, `power`, `anger`, `salt`, `wards`, `bell`, `candle`, `compass`, `ash`, `camera`, `spirit_box`, `ouija`, `clues`, `ritual`, `artifacts`, `grave`, `sanity`, `corruption`, `evolution`, `journal`, `hardcore`, `restore`, `swaps`, `place`, `said` |
| `spirits.json` | `spirits` (activity, tag affinities), `traits` (the trait catalog, with each trait's clues, words, hint and weakness), `story` (nickname words, days, causes) |
| `lights`, `mechanisms`, `sounds`, `afflictions`, `assaults`, `ambushes`, `torments`, `brinks`, `lethals`, `items`, `blocks`, `mobs`, `presence`, `lies`, `signatures` | `events.<namespace>:<id>` for that group (`assaults.json` also holds the `assaults` section: `safe_health`, `brink_health`, `mercy_ticks`, `lethal`, `lethal_cooldown_days`, `death_relief`, `relief_window_ticks`; see [assaults.md](assaults.md)) |

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
- `/haunt force <event> [seconds] [player]`: runs an event now (fails if it cannot run), or after `seconds`, when its conditions are checked, so you can first get into the state it needs (sprinting, in bed). The event is a namespaced id, `minecraft:cat_hiss`; the namespace is optional for `minecraft`.
- `/haunt birth structure`: gives the structure you stand in its territorial spirit now, skipping the chance but not the spacing.
- `/haunt spirit add <spirit> [player]`: binds one more personal spirit; for a territorial one, anchors it where the player stands.
- `/haunt spirit age <n> <days> [player]`: makes spirit number `n` that many in-game days older (maturity, blight reach, evolution).
- `/haunt spirit remove <n> [player]`: removes spirit number `n` (1-based, as listed by `info`: the player's personal spirits, then the territorial ones in their dimension).
