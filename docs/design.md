# Design roadmap

Where the mod goes after the event backlog. Today a spirit acts and the player endures; the mod has no goal and no
reason to keep playing. This roadmap turns it into a loop with long-term goals. Nothing here is built yet; current
behavior is in [spirits.md](spirits.md), event ideas in [event-ideas.md](event-ideas.md).

## Core loop

**Notice → identify → protect or banish → reward → use the reward on harder spirits.**

What keeps it going past the first hours:

| Driver | Mechanism | Stage |
|---|---|---|
| Variety | Every spirit is procedural: a table of types cannot be memorized | 1 |
| Knowledge | Clues and the journal: a spirit has to be investigated | 2, 4 |
| Pressure | The world gets worse on its own if spirits are left alone | 5 |
| Long goals | Spirits become a resource: binding, servants, spirit-powered machines | 6 |
| Endgame | The Other Side dimension and its bosses | 7 |

## Principles

- **Events stay the vocabulary.** The 100 events are reused by every stage: traits weight them, quests trigger them,
  rituals provoke them, dungeons speed them up. A new stage adds a few events, not a new event system.
- **Protection is local and wears out.** A home can be made safe, the world cannot. Fear moves to mines and trips.
- **Every spirit has an answer.** Each trait that hurts has a counter the player can find through clues.
- **Telegraph before harm.** Same rule as today's harmful events: sound first, stop at `assaults.safe_health`.
- **Layout stays.** Logic in `core` (vanilla only), loader glue behind `Platform`, tunables in `restless-config/`,
  names in `lang/en.json` and `ru.json`.

## Spirit kinds

Two kinds with different jobs. The kind belongs to the spirit type.

| | Personal | Territorial |
|---|---|---|
| Bound to | A player; follows them everywhere | An anchor: a place (structure, death spot, grave, nest) |
| Job | **Scare**: sounds, presence, lights, afflictions, the stalkers (shadow, double, edge figure) | **Foul the land**: corruption, blocks, mobs, doors; anything that changes the place |
| Acts on | Its player, wherever they are | Any player within its radius (about 48 blocks); corruption ticks while the anchor chunk is loaded, with or without players |
| How you meet it | Bound on first join (as today), born from the player's deeds (a revenant after mass killing, a lost one after death) | Generated in structures, born in places (death spot, abandoned build, blood moon) |
| How you escape it | You cannot walk away: identify and banish, or ward the places you sleep in | Leave the area, or cleanse it: banish the spirit and the corruption recedes |
| Harm | Grows with power, always telegraphed | Mostly to the place and what is in it; harm to players stays low |

Types by kind (current and planned):

- Personal: `shade`, mimic, wraith, mara, banshee, revenant, lost one.
- Territorial: `poltergeist` (the classic haunted house), domovoi, hoarder, demon, trickster.

So a poltergeist stops following the player and stays at its anchor. Today's binding of 3 random spirits on first join
draws from personal types only.

## Power instead of danger

World `danger` (days and advancements) is removed. How harmful and how frequent a spirit is depends only on the spirit
itself, so two spirits next to each other can be at very different levels, and a new player in an old world is not
punished for the world's age.

`power` 0..1 per spirit, from its own properties:

`power = max(evolutionFloor, min(1, 0.6 × maturity + 0.4 × anger))`

- `maturity = min(age / maturity_days, 1)`, age counted in in-game days since the spirit was born (`bornAt`).
  Starting value: 30 days.
- `anger` 0..1: raised by the player's actions and by failed rituals, lowered by offerings (stages 1 and 3).
- `evolutionFloor`: each evolution step (stage 5) sets a minimum power, so an evolved spirit never calms back to a
  weak one.
- Territorial spirits also mature from the area they have corrupted, not only from age.

`power` takes `danger`'s place everywhere: the tier factor (`LIGHT = 1 − 0.7 × power`, `HARMFUL = power`), the cooldown
(`4 − 2.5 × power`), how hard the ritual is and how strong the essence is. Numbers are starting values, all in config.

## What exists to build on

| Piece | Reused for |
|---|---|
| `Director` weight formula (`baseWeight × tierFactor × Π affinity`) | Trait affinities and wards multiply into it; `power` replaces `danger` in the tier factor |
| `HauntData` (saved per world, per player, per spirit) | Spirit identity, kind, anchor, traits, anger, age |
| `Scheduler` | Rituals, scenes, the shared timer of a dungeon |
| `Restore` | Corruption that must be undoable, salt lines removed by events |
| `Place` | Spawning graves, nests, apparitions |
| `Restless.onUseBlock`, `onPlaySound`, `Echoes` | Reactive triggers, noise as fuel, offerings |
| Payloads (`DimPayload`, `DoorLockPayload`) | Client effects: sanity overlay, visible spirits |
| Debug screen, `/haunt` | Inspecting traits, anger, quests while tuning |
| Config migrations | `HauntData` and config growing without breaking saves |

## Stage 0: Foundation

Nothing playable, but every later stage needs it.

- **Content registration.** Blocks, items, block entities, entities, sounds and the creative tab are declared in `core`
  and registered by the loader through `Platform` (NeoForge `DeferredRegister`). Recipes, loot tables, models and
  textures are plain resources in `core`. Today `core` registers only commands and payloads.
- **Spirit identity.** `HauntData.Haunting` grows from `(spirit, nextEventAt)` to an instance: `uuid`, `type`, `seed`,
  `name`, `traits`, `anger`, `bornAt`. Old saves get a seed rolled on load (optional codec fields with defaults).
- **Two kinds.** `HauntData` keeps personal spirits per player (as today) and territorial spirits per dimension with
  their anchor position and radius. The `Director` rolls a territorial spirit's events on a player inside its radius.
  Today's poltergeist becomes territorial; first-join binding draws from personal types only.
- **Spirit location.** A personal spirit gets a roaming position near its player, kept in memory; a territorial one has
  its anchor. Events whose source is free (sounds, knocks, figures) come from that point. Detection tools in stage 2
  point at it.
- **Power replaces danger.** `Danger.java` is replaced by per-spirit `power` (see above). Before anger and evolution
  exist, power is maturity only. `spirits.md` and the debug screen show power per spirit instead of danger.

Done when: one test block and item register on NeoForge, an old world loads with its spirits intact (an old
poltergeist binding is dropped or re-anchored at the player's spawn), `/haunt info` shows each spirit's name, seed,
kind and power, and a territorial spirit placed with `/haunt` acts only on players within its radius.

## Stage 1: Procedural spirits

A spirit is a **type** (base behavior, as today) plus **3–5 traits** rolled from its seed.

- A trait is data in config: event tag affinities, activity modifiers (weather, light, time, noise), the clue it
  leaves, the weakness it gives, and the dialogue words it uses. Examples: *fears fire*, *loves children*, *touches only
  iron*, *active in rain*, *hates cats*, *leaves frost*, *lies in the spirit box*, *feeds on noise*.
- The weight formula becomes `baseWeight × tierFactor × Π typeAffinity × Π traitAffinity × activityModifiers`.
- **Noise as fuel** (a trait, not a rule): sprinting, breaking blocks and jumping shorten the cooldown, sneaking
  lengthens it. Fed by `Echoes`.
- **Reactive triggers** (a trait property): small chance to fire an event right after the player opens a chest, lies
  down, goes below y 0 or kills a mob. Hooked into `onUseBlock` and loader events.
- **Name and story**: seed → name, year and cause of death. Shown nowhere yet; stage 4 uses it.
- **Anger** 0..1 per spirit. Raised by the player's actions (killing a cat, breaking a grave, burning a forest), lowered
  by offerings later. Feeds `power`. For a territorial spirit, actions in its area count (breaking its anchor
  structure, cutting the grove it lives in).

Done when: two spirits of the same type behave measurably differently (debug screen shows trait-weighted chances), and
a saved spirit keeps its traits across restarts.

## Stage 2: Detection and protection

First items. The player can learn what haunts them and defend a base.

**Detection** (each reveals one clue kind; a spirit's traits decide which clues it leaves, 3 of about 7):

| Item / block | Shows |
|---|---|
| Spirit candle | Flame leans toward the spirit; turns blue 2–3 s before an event (a warning) |
| Spirit compass | Points at the spirit's position |
| Ash powder | Scattered on the floor, shows footprints after 30 s; the print shape depends on the spirit |
| Camera obscura | Takes a photo item with the spirit's silhouette and one trait hint |
| Spirit box (block) | The player asks in chat nearby, the spirit answers a word from its traits' vocabulary |
| Ouija board (block) | The planchette moves letter by letter; spells a trait hint or a lie (*lies* trait) |

**Protection** (local, wears out):

| Block | Effect |
|---|---|
| Salt line | Placed like redstone. Events whose source is inside a closed loop do not fire. Salt comes from evaporating water in a cauldron over a fire: a new resource |
| Door charm | DOORS events skip doors within 8 blocks; breaks after 5 blocked events |
| Dreamcatcher | Over a bed: no events while sleeping |
| Hallowed lantern | LIGHTS cannot snuff it; blocks events within 6; raises anger of light-hating traits |
| Scarecrow | Draws events onto itself instead of the player; wears out |
| Bell (vanilla) | Ringing pushes back every nearby spirit's cooldown; angers noise-hating traits |

Events that hit a ward emit a visible "blocked" effect, so protection is felt, not guessed.

Done when: in survival a player can craft the candle and salt, see a candle warning before an event, and a closed
salt loop around a room stops events from inside it.

## Stage 3: Banishing and rewards

- **Altar**: a multiblock of a center block, 4 candles and a salt ring. The center takes a **focus** matching the
  spirit's weakness (from traits, not type): iron and a bell, glowstone with ash, holy water, a mirror.
- **Ritual**: 60 s in waves via `Scheduler`. The spirit fights back: events 3× as often, it tries to snuff the candles.
  A snuffed candle fails the ritual.
- **Success**: the spirit leaves and drops **spirit essence** of its type. **Wrong focus**: anger to 1 and a harmful
  event at once.
- **Holy water**: a bottle of water left on the altar overnight. Thrown, it ends a running event (shadow, double,
  embrace, the call).
- **Offerings**: an item on the altar or a lectern; the spirit takes it and calms down. Each trait wants its own kind.
- **Essence uses**: per type, one artifact each (telekinesis amulet, dark eye, mimic mask, pact enchantment).
- **Lure**: a scarecrow stuffed with essence calls a new spirit. Farming spirits is the player's choice of risk.

Done when: a spirit can be identified from clues and banished in survival, wrong focus punishes, the essence crafts
its artifact.

## Stage 4: Unfinished business, sanity, journal

This is the main time sink and it is generated, so it does not run out.

**Unfinished business**: a spirit is a dead person; its story (stage 1 seed) becomes a quest.

- A **grave** generates 100–400 blocks away (`Place`), with the spirit's name and year.
- **Clues**: diary pages and a portrait in chests of nearby structures, lines in the spirit box.
- Quest kinds: return a stolen item to the grave, bury the remains, deliver a letter to a villager descendant, kill a
  named mob that killed it, find its killer (another spirit).
- Resolving it lets the spirit go **peacefully**: better reward than a ritual, no anger risk.

**Sanity** 0..1 per player:

- Falls in darkness, near spirits, during events. Recovers with light, sleep, food, a warded home.
- Low sanity **reveals**: some clues, writings and paths are visible only below a threshold. Knowledge costs risk.
- Very low sanity: hallucinations (fake mobs, fake blocks, fake chat), client-side.
- Shown as a client overlay (reuses the `Dimming` path), not a bar.

**Journal** (the "Necronomicon"):

- Fills by observing: a clue seen adds an entry, a type identified adds an article, 3 banished of a type unlocks its
  essence recipe.
- Recipes past stage 2 basics are unlocked by research, not available from the start. A tree of 30–60 nodes.

Done when: a spirit spawns with a grave and a quest, the quest can be completed end to end, the journal records it,
and low sanity shows a clue that is otherwise hidden.

## Stage 5: Spread and evolution

The world gets worse unless the player acts.

- **Corruption** (territorial only): a spirit left alone corrupts chunks around its anchor. Grass greys, fog, crops stop growing, mobs are
  replaced by possessed variants. Spreads slowly; recedes when the spirit is banished. Block changes go through
  `Restore`-style rules so they can be undone.
- **World-born spirits**: territorial ones appear at a player's death spot, in abandoned structures, on a blood moon;
  personal ones are born from deeds (mass killing, a pet's death). `/haunt spirit add` stops being the only way in.
- **Evolution**: a spirit not banished for N days grows up: poltergeist → raging, shade → light-eater. New events,
  new clues, a harder ritual, and a higher power floor.
- **Merging**: two spirits of one player can merge into a stronger one.
- **Calendar**: full moon stronger spirits, blood night every N days (survive till morning, a test of the base),
  a peaceful day of the dead each season (trade with spirits).

Done when: an ignored spirit visibly corrupts its area within a few in-game days, evolves, and banishing it reverts the
area.

## Stage 6: Binding and servants

Spirits stop being only a threat and become a resource.

- **Binding**: a ritual variant that locks a spirit into a **vessel** (doll, lantern, jar) instead of banishing it.
- **Servants**: a bound spirit works near its vessel. Poltergeist moves items like a wireless hopper, domovoi farms,
  shade suppresses mob spawns, banshee warns of threats.
- **Ectoplasm**: a resource bound spirits produce, used as power for spirit machines (a small tech tree).
- **Resentment**: a bound spirit has to be fed. Neglected, it breaks free much stronger than before.
- **Domovoi**: a neutral type that can be won with offerings and then guards the house against other spirits.

Done when: a bound poltergeist moves items between two chests, ectoplasm accumulates, and a neglected vessel breaks.

## Stage 7: The Other Side

The endgame.

- **Dimension**: a mirror of the overworld, dead and dark, entered by a ritual or by dying with a charm.
- **Spirits are visible and material** there: they can be fought. Uses the visible spirit entity.
- **Resources**: ghost iron and others, for the top tier of tools and wards.
- **Structures**: crypt, asylum, drowned cathedral.
- **Bosses**: the Masters, ancient spirits, each reached by a quest chain from stage 4. Unique artifacts.
- Time runs differently; sanity drains while there, so trips are short.

Done when: a player can enter and leave, find a structure, and beat one boss.

## Side tracks

Can go in any time after the stage they depend on.

| Track | Needs | Content |
|---|---|---|
| Haunted structures | 1, 3 | Generated houses, lighthouses, asylums with one strong spirit; events 5× as often inside; harder farther from spawn |
| New spirit types | 1 (+ the stage of their gimmick) | All ten exist as types on the existing events (see [spirits.md](spirits.md)); the gimmicks remain: mimic (fake blocks, fake chat), wraith (stronger in the dark), demon (pacts), mara (sleep, nightmare room), hoarder (nest you can raid), banshee (honest warnings), revenant (born from killing), lost one (replays a death path), trickster (fake gifts) |
| Visible spirit entity | 0 | Translucent model per type, visible through the camera or the candle; a spirit's position gets a body |
| Possession | entity | A mob or a player is possessed for 1–2 min; killing a possessed mob stuns the spirit |
| Multiplayer | 2, 4 | Roles (medium, exorcist, scholar), spirits hopping between players, playing as a ghost for 5 min after death |

## Dependencies

```
0 Foundation
├── 1 Procedural spirits
│   ├── 2 Detection and protection
│   │   └── 3 Banishing and rewards
│   │       ├── 4 Unfinished business, sanity, journal
│   │       │   └── 7 The Other Side (also needs the visible spirit entity)
│   │       └── 6 Binding and servants
│   └── 5 Spread and evolution (better after 3, so it can be undone)
└── Visible spirit entity
```

Stages 1–3 give a complete loop (a few hours). Stages 4–5 make it last (tens of hours). Stages 6–7 are the long tail.

## Open questions

- **Hardness of the clue game.** How many clues per spirit, and can two trait sets share the same clues (forcing
  the spirit box or the journal to break ties)?
- **Territorial radius and overlap.** Two anchors near each other: do their areas stack, and does corruption from one
  feed the other?
- **Fresh personal spirits.** At power 0 the three first-join spirits are harmless for about a month of in-game days.
  Is that the intended onboarding, or should a personal spirit start with some maturity?
- **Fabric.** Registration through `Platform` keeps the port possible; stage 7 (a dimension) is where the loaders
  differ most.
