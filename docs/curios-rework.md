# Curios rework: charms as worn accessories

Status: built, named and drawn. Done: `Worn` seam, Curios adapter, slot tags and player entity file, Nightwalk Shroud
worn-state, satchel death and key wear through `Worn`, tooltip lines in two colours, wiki en+ru. Renames are done. Icons are redrawn (32x32, via `chatgpt-6.1-sol`): active tools carry a ~5x5 red wax seal in the bottom-right corner, worn charms carry none. Open: `ICurioRenderer`, a manual run of `runClient` with Curios.

Findings while building:

- Curios hands out the stored stack itself, so a change to it (a rest, a link, a key's room) persists with no write-back
  handle. Only emptying a slot needs `Platform.clearAccessory`.
- Look the stack up with the predicate form `findFirstCurio(stack -> ...)`. The by-item form caches and returned a stale
  empty result inside one tick.
- A player has no Curios slots until its inventory is reset after the data loads; a real join does this, a test's mock
  player does not.
- Game tests run without Curios (the plain off-hand path, 419 pass). With Curios on the classpath the old charm tests
  fail as designed, since they equip the off hand. A throwaway test with Curios checked the slot-only rule, a rest kept
  on the slot's stack and the belt satchel cleared on death; it was removed.

Today every charm is a hand item; passive ones work from the off hand only, whatever the charm is. This rework adds
[Curios API](https://modrinth.com/mod/curios) as an **optional** dependency and splits charms in two families with
different identities and different homes: **worn** charms go to accessory slots, **active** items stay in the hand.

Curios for 26.3: `17.0.1+26.3` (NeoForge, released 2026-11-09). Its old maven (`maven.octo-studios.com`) is dead; use
Modrinth maven, `maven.modrinth:curios:17.0.1+26.3`. Slots in 17: `head necklace back body bracelet hands ring belt
charm feet curio`.

## Decisions

1. **With Curios, a worn charm works only from its accessory slot**, never from the off hand. Without Curios the
   off-hand rule stays as it is now.
2. **Active items stay in the hand** (the ones used with right-click) and get an identity apart from worn charms, so a
   player never wonders what goes into a slot and what is held. See [Identity](#identity).
3. Slot layout below is accepted. `belt` is one slot shared by three items on purpose.

## Worn charms (passive, accessory slots)

| Slot | Charm | Why |
| --- | --- | --- |
| `necklace` | Death Wail, Requital Wail | a bell on the neck |
| `bracelet` | Drowned Stone, Tidecaller | stone on the wrist |
| `ring` | Grudge, Blood Debt | a ring of blood |
| `belt` | House Key, Keeper's Key, Grave Satchel | keys and a satchel hang on a belt; one slot, pick one |
| `hands` | Magpie's Hook | hook on a glove |
| `head` | Truthseer's Eye | a circlet with a third eye |
| `back` | Nightwalk Shroud | a cloak: worn means on, no toggle with Curios |
| `charm` | Ferryman's Coin | an obol kept on you |
| inventory | Stash | works from any inventory slot already; unchanged |

Exorcist's Censer stays a held item (swung at the altar), so it joins the active family.

## Active items (stay in the hand)

Dark Lens, Telekinesis Rod, Borrowed Face, Changeling Guise, Wraith's Breath, Lost Step, Fetch's Path, Phantom Double,
Twin's Mirror, Bargain, Black Bargain, Borrowed Dream, Hearth Dream, Trickster's Coin, Devil's Coin, Exorcist's Censer.

Note: Dark Eye and Telekinesis Amulet (now Dark Lens and Telekinesis Rod) were named "eye"/"amulet" but act on use, and Wraith Shroud and Changeling Mask (now Wraith's Breath and Changeling Guise)
are named like clothing but are used. The names alone mislead, so identity has to carry the split.

## Identity

Goal: a glance at the tooltip, name or icon says "wear it" or "use it". Proposal to settle before art and lang work:

- **Worn charms keep the jewelry/garment look.** Metal, cord, leather, stone; calm, still icons. Tooltip first line is
  the slot ("Necklace", "Ring", ...), which Curios already shows.
- **Active items become tools of the craft**: held, handled, spent. Rename where the name reads as clothing or jewelry
  (renamed: Dark Eye to Dark Lens, Telekinesis Amulet to Telekinesis Rod, Wraith Shroud to Wraith's Breath, Changeling
  Mask to Changeling Guise; the upgrades Truthseer's Eye, Magpie's Hook and Nightwalk Shroud, which are worn, keep
  theirs).
- **Shared visual mark for active items**: a ritual glyph or wax-seal accent in the icon, plus a distinct tooltip line,
  "Use: ..." in a fixed colour, next to the existing one-line reminder. Worn charms get "Worn: ..." in another colour.
- **Rest bar already marks active items** (they show a cooldown); keep that as the second cue.
- Wiki: split `charms.md` into **Worn charms** and **Tools of the craft** sections (or two pages), mirror in `ru`.

Image work goes to the `chatgpt-6.1-sol` harness as the project rules say. No texture is drawn by hand.

## Architecture

1. **Seam in core.** Core stays loader-free. Add to `Platform` a default `worn(LivingEntity, Item...)` that finds the
   item in its accessory slot, returning nothing when Curios is absent. A small `content/Worn` helper wraps it:
   with Curios it looks only in the slot, without Curios it looks in the off hand (exactly today's behavior).
   Every passive check goes through it, replacing `offhand()` and `getOffhandItem()` in `Charms` (`held`, `grudge`,
   `spared`, `keeps`, `died`, `wail`), `Advanced` (`truthful`, `truthSpent`, `hook`, `walk`), `Relics.ferried` and
   `Ritual` (censer stays hand, untouched).
2. **Write-back.** Curios 17 stores stacks behind a transfer `ResourceHandler<ItemResource>`, so editing a found stack
   in place may not persist. `Worn` returns a handle with `save(stack)`, used by `Rest.start`, key wear
   (`hurtAndBreak` currently targets `EquipmentSlot.OFFHAND`), `HOOK_LINK` removal, `KEY_ROOM` binding. Verify the
   in-place behavior first; if it persists, drop the handle.
3. **NeoForge adapter.** A `CuriosCompat` class loaded only when `ModList.get().isLoaded("curios")`, so a missing
   Curios never throws `NoClassDefFoundError`. `build.gradle`: Modrinth maven repo, `compileOnly` Curios,
   `localRuntime` for `runClient`. `neoforge.mods.toml`: dependency `curios`, `type="optional"`, `versionRange="[17,)"`.
4. **Data** (neoforge resources). `data/curios/tags/item/<slot>.json` per table above. `data/restless/curios/entities/`
   file giving players the used slots (`necklace bracelet ring belt hands head back charm`); Curios shows no slot
   without it.
5. **Death.** Grave Satchel on the belt must stay with the player. `Charms.died()` finds it through `Worn` and clears the
   slot through `save(EMPTY)`. Check order against Curios' `CurioDropsEvent`; if `died()` runs too late, mark the satchel
   `DropRule.ALWAYS_KEEP` and take it ourselves.
6. **Nightwalk Shroud.** With Curios, wearing it in `back` is the on-state and the toggle `use()` is gone; without
   Curios the toggle stays.

## Steps

1. `Worn` seam and replace every passive check. No behavior change; existing game tests pass
   (`./gradlew :neoforge:runGameTestServer` from `apps/mod`).
2. Curios dependency, `CuriosCompat`, tags, entities json.
3. Nightwalk Shroud worn-state.
4. Death, wear and write-back.
5. Identity pass: renames, tooltip lines, icon mark (art via `chatgpt-6.1-sol`), lang `en_us` and `ru_ru`.
6. Wiki en+ru: `items/index.md` rule "one at a time" rewritten (slot rule with Curios, off hand without), `charms.md`,
   `advanced.md`, `relics.md`, with every worn charm naming its slot and its no-Curios fallback.
   Update `docs/events-index.md` only if events change (none planned).
7. Verify: game tests for the no-Curios path; manual `mise run mod:client` with Curios (wear each charm, Ferryman's
   Coin, death with a satchel, key wear, relog persistence of rests).

Done when: without Curios nothing changes and tests are green; with Curios each worn charm works from its slot only and
keeps its state across relog; active items are told apart from worn ones by name, tooltip and icon.

## Out of scope

Rendering on the player model (`ICurioRenderer`), a keybind to use worn items, extra slot counts (a second `ring`
could come later), changes to numbers or balance.
