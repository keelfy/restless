# Territorial births (plan)

Today a territorial spirit appears only through `/haunt spirit add` (`Director.anchor`). This plan lets the world make
them: **when a structure is generated, it may get a territorial spirit** whose type fits it. A haunted mansion is
haunted from the moment it exists, before any player arrives. It is the "generated in structures" line of
[design.md](design.md) (Spirit kinds). The personal half is [births.md](births.md).

Step 1 is built (rules, config, `Births.generated`, debug command); the chunk hook (step 2) is not. Numbers are starting values, all in config (`haunt.json`, section `births.structures`).

## Goals and non-goals

- A generated structure of a configured kind gets a territorial spirit with a chance below 1.
- One roll per structure, ever, with nothing saved to remember it: a structure start lives in exactly one chunk, and
  that chunk is new exactly once.
- Two haunted structures never share an area.
- A born spirit is a normal territorial spirit (traits, anger, corruption, evolution, ritual); nothing new to learn.
- Not here: births from events (deaths, homes, lit portals), the blood moon, spirits outside structures, new spirit
  types, the domovoi's offerings, pacts and fake gifts. Chunks generated before this ships stay empty; old worlds get
  spirits only in newly explored land.

## The seam: `Births.generated`

One loader hook, `Restless.onNewChunk(ServerLevel, LevelChunk)`, from NeoForge's `ChunkEvent.Load` when
`isNewChunk()`. It reads the chunk's structure starts (`getAllStarts()`): a cheap map lookup, no search, nothing per
player. For each start of a configured structure it calls `Births.generated(level, start)`, which decides in order:

1. The structure id is in `births.structures.<id>` and its chance roll passes.
2. **Spacing**: any territorial anchor within `spacing` (96 blocks, twice the radius) in that dimension: nothing.
   Two shipwrecks side by side get at most one nix. Pure, in `BirthRules.settle`, unit-tested.
3. Otherwise `Director.anchor` at the centre of the structure's bounding box, so the radius covers the whole of it.

No cap per dimension: exploration is what makes more spirits, and a cap would leave every far structure empty. No
start anger, no grace and no notice line: nobody is there to notice, and the spirit is met by walking in. Corruption
starts at age 0, so a fresh structure's land is still clean; one found late, after its chunk sat loaded, is greyer.

Debug: `/haunt birth structure` rolls for the structure the player stands in, skipping the chance but not spacing.

## Structures

`births.structures.<id>`: `{ "spirit": ..., "chance": ... }`. Ids are vanilla's; modded structures are added the
same way.

| Structure | Born | Chance |
|---|---|---|
| `woodland_mansion` | `poltergeist` | 0.6 |
| `trail_ruins` | `poltergeist` | 0.3 |
| `shipwreck`, `shipwreck_beached`, `ocean_ruin_cold`, `ocean_ruin_warm` | `nix` | 0.25 |
| `ruined_portal` (all variants), `ancient_city` | `demon` | 0.2, 0.5 |
| `bastion_remnant`, `fortress` | `demon` | 0.2 |
| `swamp_hut` | `trickster` | 0.5 |
| `igloo`, `desert_pyramid`, `jungle_pyramid` | `hoarder` | 0.3 |
| `village_*` | `domovoi` | 0.1 |

## Order of work

| Step | Content | Done when |
|---|---|---|
| 1 (done) | `BirthRules.settle` (spacing), `births.structures` config, `Births.generated`, `/haunt birth structure` | A unit test covers spacing (empty world, an anchor inside 96 blocks, one just outside). Standing in a mansion, `/haunt birth structure` anchors a poltergeist at its centre; a second call does nothing |
| 2 | `Restless.onNewChunk` hooked on NeoForge | A game test generates a structure start in a new chunk with chance 1 and finds one anchor; reloading the chunk adds none; a structure not in config adds none |

Docs to update: `spirits.md` (How it works, and drop "abandoned structures" from "Not built yet"), the design.md
bullet in stage 5, the frontend `content/spirits.md` page.

## Risks and open questions

- **Thread.** `ChunkEvent.Load` must run on the server thread to touch `HauntData`. If NeoForge fires it during
  generation off-thread, queue the starts and anchor them on the next server tick instead.
- **Anchor height.** The bounding box centre of a tall or buried structure (ancient city, ocean ruin) may sit inside
  stone. The radius (48) still covers the structure; a source of sounds inside rock may be audible oddly. If it is,
  snap the anchor to the nearest air.
- **Many anchors.** `Director.tickTerritorial` and `Corruption.tick` walk every anchor each check, and exploration
  makes hundreds. Both skip unloaded ones quickly; if it shows up in profiling, index anchors by dimension and chunk.
- **Villages.** A village is common and big; 0.1 and a 48-block radius may cover only its middle.
