# Personal spirit births (plan)

Today a player gets personal spirits two ways: one random one on first join, and `/haunt spirit add`. This plan adds
**births**: a personal spirit that appears because of something the player did. It is the "born from the player's deeds"
line of [design.md](design.md) (Spirit kinds, stage 5). Territorial spirits generated with structures are planned in
[places.md](places.md).

Step 1 (the seam) and batch 1 are built, in `haunt/Deeds.java`: slaughter, far death, pet slain, pet lost, witnessed
death and a long wake. Five personal types still have no deed; batch 2 below gives each one. The Effigy (see essence-expansion.md) is the first opt-in birth. Numbers are starting values, all of them in
config (`haunt.json`, section `births`).

## Goals and non-goals

- A deed the player can recognise, and later explain in hindsight, adds one spirit whose type fits the deed.
- Births are rare and capped: the world must not fill a player with spirits for farming mobs.
- Every source works in less than 100% of cases: each deed has a chance below 1, and the cooldown and cap sit on top.
- A player never has two spirits of one type: `Director.add` returns the one they have instead of binding a second.
- A born spirit is a normal spirit (traits, anger, clues, grave, ritual, evolution); nothing new to learn or fight.
- Not here: territorial births, binding or merging spirits, a new journal chapter, new spirit types, new events.

## The seam: `haunt/Births.java`

One entry point, `Births.deed(player, Deed, roll)`, to be called from the hooks that already exist (`Restless.onKill`,
`Anger.broke`, `Reactions.used`, the director check). A `Deed` is a config key; `births.deeds.<key>` in `haunt.json`
gives its spirit type and chance, so which spirit a deed makes is data, not code. The pure rules are in `BirthRules`.
`Births` decides, in this order:

1. The player can be haunted (not creative or spectator) and the deed's chance roll passes.
2. **Repeat**: if the player already has a spirit of the picked type, the deed feeds it instead
   (`angered(repeat_anger)`) and no new one is born. This keeps farming from stacking revenants, and it applies during
   the cooldown and at the cap too.
3. **Cooldown**: no birth if the player's newest spirit was born less than `cooldown_days` (3) ago. Derived from
   `bornAt`, so nothing new is saved and a restart cannot reset it. The first-join spirit counts, so a new
   player's first birth waits three days.
4. **Cap**: no birth at `max_personal` (3, configurable) spirits.
5. Otherwise `Director.add` with a head start of `angered(start_anger)` (0.5, about power 0.2), so a born spirit begins
   above harmless. It answers the open question in design.md about fresh spirits for this path only; the first-join
   one stays at 0. Aging it instead would move `bornAt` and break the cooldown above.
6. **No telegraph**: no sound and no message on birth, the player learns of it from the events. The first event waits
   `grace_ticks` (2400). Instead, the new spirit starts with `notice` set to a random 1..`notice_within` (5); every event
   it runs counts it down, and on the last one the journal gets a `DREAM` line, "Something unknown is near me", with no
   spirit behind it. The count is saved with the spirit (`notice`, absent means 0).

Counters (kills in a window, minutes in the dark) live in memory in `Births`, like `Reactions`; a restart
forgets them, which only makes births rarer.

Debug: `/haunt birth <deed> [player]` fires a deed, skipping the roll but not the cooldown, cap or repeat rules.
The deeds known so far are the batch 1 ones; batch 2 adds its keys to `Deed` and `births.deeds` with its hooks.

## Deeds

### Built

| Deed key | Hook | Condition | Born |
|---|---|---|---|
| `slaughter` | `Restless.onKill`, killer is a player | Animal 1 point, villager 5, hostile 0; 24 points inside 3600 ticks. 30% | `revenant` |
| `pet_slain` | `Restless.onKill`, owned pet | Killed by its owner. 50% | `revenant` |
| `pet_lost` | `Restless.onKill`, owned pet | Any other cause, owner online. 50% | `banshee` |
| `witness_death` | `Restless.onKill` | A villager or pet dies within `witness_radius` (16) of a player who did not kill or own it. 20% | `banshee` |
| `far_death` | `Restless.onKill`, victim is a player | Dies 256+ blocks from respawn or below y 0, not by a spirit event. 50% | `lost_one` |
| `lost_long` | `Deeds.tick` (every 200 ticks) | Vanilla `TIME_SINCE_REST` reaches `sleepless_ticks` (48000, two days). 30% | `lost_one` |

### Coverage today

Every territorial type has a structure (see [places.md](places.md)). Five of the eight personal types have **no deed**:
`shade`, `phantom`, `mimic`, `wraith`, `mara`. They come only from the first-join roll (1 of 8), the Requiem bell
(3 random) and `/haunt spirit add`. The Effigy does not help: its essence drops only from banishing or a grave of the same
type, so a player who never met a mara cannot lure one. In a normal game these five are luck of the first roll.

| Type | Kind | Paths now | Gap |
|---|---|---|---|
| `revenant` | personal | slaughter, pet slain | none |
| `banshee` | personal | pet lost, witness death | none |
| `lost_one` | personal | far death, sleepless | none |
| `shade` | personal | first join, bell | no deed |
| `phantom` | personal | first join, bell | no deed |
| `mimic` | personal | first join, bell | no deed |
| `wraith` | personal | first join, bell | no deed |
| `mara` | personal | first join, bell | no deed; its theme (sleep) went to `lost_long` |
| `poltergeist` | territorial | mansion 60%, trail ruins 30% | both structures rare |
| `nix` | territorial | shipwrecks, ocean ruins 25% | none |
| `demon` | territorial | ruined portals, ancient city, bastion, fortress | none |
| `hoarder` | territorial | igloo, desert and jungle pyramid 30% | none |
| `trickster` | territorial | swamp hut 50% | one rare structure |
| `domovoi` | territorial | villages 10% | low |

### Batch 2: one deed per orphan

Goal: every personal type has at least one deed. Each reuses a hook that exists; no new loader event, no mixin.

| Deed key | Hook | Condition | Chance | Born |
|---|---|---|---|---|
| `grave_robbed` | `Restless.onBreakBlock`, next to `Anger.broke` | Block is in `anger.graves` | 30% | `shade` |
| `long_dark` | `Deeds.tick` | Player at block light 0 and sky light 0, counted 200 ticks per check; `dark_ticks` (24000, 20 min) in a session; counter resets on roll | 40% | `wraith` |
| `loot_opened` | `Restless.onUseBlock`, before vanilla unpacks | Block entity is a `RandomizableContainerBlockEntity` with a loot table still set | 5% | `mimic` |
| `nightmare` | `Reactions.used`, bed | Player lies down while one of their spirits has anger above `nightmare_anger` (0.5) or sanity below half | 25% | `mara` |
| `lonely` | `Deeds.tick` | Online `lonely_ticks` (72000, 3 days) in a row with no other player in the same dimension; counter resets when one comes or on roll | 30% | `phantom` |

Notes:

- `long_dark` and `lonely` keep counters in memory in `Deeds`, like `WAKING`; a restart forgets them, which only makes
  births rarer.
- `lonely` fires in single player too: that is the point. A phantom is the other players you do not have.
- `nightmare` gives mara its own sleep path without taking `lost_long` back. Alternative: move `lost_long` to `mara`
  (one config line, needs a `Migrations` step for saved configs) and give `lost_one` a "deep" deed (20 min below y 0).
  Recommended: keep `lost_long` as is, add `nightmare`.
- `loot_opened` at 5%: loot chests are common, the cooldown and cap still hold. Breaking an unopened loot chest counts
  the same (check in `onBreakBlock`), so smashing does not dodge it.

Acceptance: `/haunt birth <key>` adds the right type for each new key; a game test per condition (grave block broken,
20 min in the dark, loot chest opened, bed with an angry spirit, 3 days alone). Wiki pages for the five types and the
births page list the deed in both languages.

### Batch 3 (later): territorial births from deeds

Out of scope for batch 2; design.md says territorial spirits come only with structures. Listed so the gaps above have
an answer if that changes. Each anchors with `Births.place`, so spacing holds.

| Deed | Condition | Born |
|---|---|---|
| Home kept | Same respawn bed used 7 nights | `domovoi` at the bed |
| Home left | Respawn bed not visited for 7 days | `poltergeist` at the bed |
| Drowned | Player drowns | `nix` at the death spot |
| Portal lit | Player lights a nether portal. 5% | `demon` at the portal |
| Witch killed | Player kills a witch. 10% | `trickster` at the spot |

## Opt-in births (later)

Deeds above are things that happen to the player. These are the player's choice of risk and each needs a block or item
that does not exist yet, so they wait for their own stage.

- **Ouija call**: the board used three times in one night with a lit candle beside it: something answers and follows.
  Fits the board's "failed ritual raises anger" role in design.md; needs a state machine on the board entity.
- **Lure** (design.md stage 3): a scarecrow stuffed with essence calls a spirit of that essence's type. Needs the
  scarecrow behaviour and the essence stuffing first.
- **Haunted object**: a doll or music box in structure loot that binds its spirit to whoever carries it for a day.
  Needs an item with state and a loot entry.

## Order of work

| Step | Content | Done when |
|---|---|---|
| 1 (done) | `Births` + `BirthRules`, `births` section in `haunt.json` (new keys need no migration), `/haunt birth` | A unit test on the pure rules (cooldown, cap, repeat, head start) passes. `/haunt birth slaughter` adds a revenant with power around 0.2; a second one within 3 days does nothing; with a revenant it raises that one's anger instead |
| 2 | Batch 1: slaughter, death far from home, pet lost | A game test kills 24 cows and finds a revenant; kills a player far from spawn and finds a lost one; kills an owned wolf and finds a banshee. Hostile mobs alone never trigger |
| 3 | Batch 2: `grave_robbed`, `long_dark`, `loot_opened`, `nightmare`, `lonely` | Each personal type has a deed; each deed has a game test on its condition |
| 3b | Batch 3, territorial deeds, only if design.md allows it | Decided after batch 2 plays |
| 4 | Opt-in births | Decided when the board, scarecrow and loot items are built |

Docs to update per step: `spirits.md` (How it works: births), the design.md bullet in stage 5, the frontend
`content/spirits.md` page.

## Risks and open questions

- **Culling farms** kill 24 animals in three minutes without meaning anything. The cooldown, cap and repeat rule limit
  the damage to at most one revenant; if it still feels wrong, the window can count only kills outside a pen (a roof
  and fence check), at the cost of a rule players cannot see.
- **Births stay unexplained.** No message, no cause in the journal line; the spirit's own story and clues are the only
  hint. If players cannot connect a spirit to a deed, the journal line can name it later.
- **Cap value.** Three personal spirits at power 0.6 each is already many events; the cap may still need to drop with
  hardcore.
- **Sleepless and long dark** reward not playing normally; if players start gaming them on purpose they become opt-in
  rather than random, which is acceptable.
