# Personal spirit births (plan)

Today a player gets personal spirits two ways: one random one on first join, and `/haunt spirit add`. This plan adds
**births**: a personal spirit that appears because of something the player did. It is the "born from the player's deeds"
line of [design.md](design.md) (Spirit kinds, stage 5). Territorial spirits generated with structures are planned in
[places.md](places.md).

Step 1 (the seam) is built. Hooked up, in `haunt/Deeds.java`: slaughter, far death, pet slain and pet lost (batch 1), and witnessed death (a villager or a pet dies near a player who did not do it) and a long wake (two days without rest) as new ones; batch 2 is still to do. The Effigy (see essence-expansion.md) is the first opt-in birth. Numbers are starting values, all of them in
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

Batch 1 is the three from design.md. Batch 2 reuses hooks that already exist, one small class of conditions each.

| Deed | Hook | Condition | Born | Batch |
|---|---|---|---|---|
| Slaughter | `Restless.onKill`, killer is a player | Animals and villagers killed: animal 1 point, villager 5, hostile mobs 0. 24 points inside 3600 ticks. 30% | `revenant` | 1 |
| Death far from home | `Restless.onKill`, victim is a player | Dies 256+ blocks from their respawn point or below y 0, and not by a spirit event within `relief_window_ticks`. 50% | `lost_one` | 1 |
| Pet lost | `Restless.onKill`, victim is an owned `TamableAnimal` | Owner is an online player. Killed by the owner: `revenant`. Any other cause: `banshee`. 50% | `revenant` or `banshee` | 1 |
| Grave robbed | `Anger.broke`, same `graves` list | Breaks a grave block. 30% | `shade` | 2 |
| Sleepless | `Reactions.used`, bed | Vanilla rest stat over 7 days when they finally lie down. 60% | `mara` | 2 |
| Long dark | director check | 20 minutes at light level 0 counted across the session, resets on birth | `wraith` | 2 |
| Chest broken | `Anger.broke`-style block break | Breaks a chest that still has a loot table (unopened). 5% | `mimic` | 2 |
| Witnessed death | `Restless.onKill`, victim is a player | Another player within 32 blocks. Multiplayer only. 25% | `phantom` | 2 |

Mapping to existing hooks: the three batch 1 deeds and "Witnessed death" share one `onKill` branch; "Grave robbed"
and "Chest broken" need one extra call each in the block-break path next to `Anger.broke`; nothing needs a new loader event. A pet's owner comes from vanilla
`OwnableEntity`, so it works for wolves, cats and parrots without a list.

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
| 3 | Batch 2 | Each deed has a game test on its condition, and the "no spirit event" check of death far from home is covered |
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
