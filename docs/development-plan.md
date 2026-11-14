# Development plan: depth over breadth

Written 2026-11-14 from a review of the code, configs and docs. It is the work order for an agent orchestrator: every
task below is sized for one agent, names its seams, and says what proves it done.

The review was done by reading, not by playing. Anything about how the mod feels in play is a hypothesis until phase 6
checks it.

## Why this plan

The mod is wide and shallow. It has 652 events and 104 scenarios on a solid engine, a closed loop (clues, altar or
grave, essences, charms, relics, the Requiem) and good tests. What it lacks:

- **Depth of the spirit model.** A weakness matters only at the altar. Ash gives the nature away in one reading.
  16 traits, 4 per nature, so spirits of one nature are nearly the same.
- **A ritual with a climax.** Banishing is 12 identical waves of relighting candles. The spirit never shows itself.
- **A middle of the game.** After the first banishing the player may have no spirit at all. Nothing in the world
  changes with time except blight around territorial anchors.
- **Sound and a body.** 14 recorded haunt sounds, 11 sound events still on vanilla stand-ins, one entity texture.
- **Evidence.** Nobody knows which of the 652 events land and which never roll.

## Rules for the orchestrator

1. **No new haunt events** except the ones a task here names (trait tells, harsh variants, ritual responses). The event
   backlogs (`event-ideas*.md`) are frozen until phase 6 is done.
2. Every agent prompt carries the rules of [AGENTS.md](../AGENTS.md): `caveman` for replies, `lean-build` for changes,
   `player-text` (not `caveman`) for tooltips and journal text, image work only through the `chatgpt-6.1-sol` harness.
3. Every task that changes what a player sees updates the wiki in `apps/frontend/content/en` and `ru` in the same
   change, and [events-index.md](events-index.md) when an event is added, moved or removed.
4. Every number is a config key in `apps/mod/core/src/main/resources/restless-config/`, never a constant. New keys need
   lang entries where the config key coverage test asks for them.
5. Proof for each task is a unit test in `core` for pure rules and a GameTest in `apps/mod/neoforge/src/gameTest` for
   behaviour in the world. Run `./gradlew build` and `./gradlew :neoforge:runGameTestServer` from `apps/mod`.
6. The mod is required on both sides: no fallbacks for a client without it.
7. **Hot files, one task at a time:** `spirits.json`, `spirit/Traits.java`, `spirit/Trait.java`, `banish/Ritual.java`,
   `state/HauntData.java`, `haunt/Director.java`, the two lang files. Tasks that touch the same hot file run in
   sequence, in the order of their ids; everything else may run in parallel inside a wave.
8. Saves must keep loading. New `HauntData` fields are `optionalFieldOf` with a default; a trait id that leaves the
   catalog is skipped on load, as today.
9. A task stops when its "done when" passes. Ideas that come up on the way go to [ideas.md](ideas.md), not into the
   change.

## Decisions for the owner

Agents use the default and do not wait. Change a default here before the wave that needs it starts.

| # | Question | Default | Needed by |
|---|---|---|---|
| D1 | Is a spirit's first hour meant to be harmless (power 0 for the first days)? | Personal spirits start at maturity 0.15 | M10 |
| D2 | May the altar leave an "echo" spirit behind, making the grave the cleaner way out? | Yes | M2 |
| D3 | Where do new recordings come from, and under what licence? | The owner's pack (see S1) first, CC0 sources for what it lacks. Settled 2026-11-14: the owner checked the pack, every file is from an open source and royalty free, so its files may ship. `README.md` credits it as the owner's royalty-free pack until the owner lists the sources | S1, S2, S4 |
| D4 | Does the visible spirit get a model per type or one model with a shape per type? | One model, shapes from `photo/Apparition` | S3 |
| D5 | Is a public alpha wanted, and where? | Not decided; C3 only prepares the build | C3 |
| D6 | Does salt wear out? It changes the "home is safe" rule. | Yes, only from events it stops | M7 |

## Phase 0: ground truth and safety

Small, independent, all in parallel. Nothing here changes play.

### G1. Bring the docs back to the code

- **Do:** [design.md](design.md) says "nothing here is built yet"; mark each stage built, partly built or not built.
  [spirits.md](spirits.md) says only the poltergeist and the shade have an essence; fix it and any other line that the
  code contradicts (check: rewards table, "not built yet" lists, first-join spirit count).
- **Done when:** every "not built yet" line in the two files is true against the code.
- **Size:** S.

### G2. Event telemetry

- **Do:** count, per event id and saved with the world: times rolled, times run, times a ward stopped it, times it
  could not run. `haunt/History` keeps only the last entries in memory; add the counters beside it and feed them from
  `Director`. `/haunt stats` prints the top and bottom of the list and the ids that never ran; an option writes the
  whole table to a file in the world folder.
- **Done when:** a GameTest forces three events and reads their counts back after a save and load.
- **Size:** M. **Needed by:** C1, S2.

### G3. Persisted undo log

- **Do:** `haunt/Restore` is memory only, so a crash leaves changed blocks in a player's build for good. Save pending
  block undos (position, dimension, old state, due time) in `HauntData`; apply the ones that are due on load. Entity
  cleanup (`Restore.after`) stays in memory.
- **Done when:** a GameTest swaps a block, drops the session without the shutdown flush, loads, and the block is back.
  Hardcore worlds still keep their changes.
- **Size:** M.

### G4. Roll cost on real terrain

- **Do:** [improvements.md](improvements.md) step 2 measured 8 ms on a stone plot against a goal of 1 ms, and never on
  real terrain. Measure `Director.pick` for one spirit in generated overworld, cave and village chunks; then cut it:
  one neighbourhood snapshot per roll shared by all `canRun`s, or weights first and `canRun` on the winner only.
- **Done when:** a GameTest on generated terrain reports the roll under 2 ms, and the event distribution test (add one
  if the weights-first route is taken) shows no event's share moved by more than a fifth.
- **Size:** L.

### G5. Journal request limit

- **Do:** `ReadPayload` makes the server send the whole journal with photos on every request. Limit it per player
  (one answer per second, later requests dropped).
- **Done when:** a GameTest sends ten requests in a tick and counts one answer.
- **Size:** S.

## Phase 1: the spirit model

The foundation for phases 2 to 4. T1 first, alone; the rest as the dependencies allow.

Today (`spirits.json`, `spirit/Traits.java`): 16 traits, each with a `weakness` (`Focus`: iron, glowstone, holy water,
mirror). A spirit rolls a nature evenly, takes all its traits but one from that nature's group and one from outside.
Players see natures by name: objects, darkness, malice, deceit.

### T1. Three axes

- **Do:** a trait gets a `kind`: `habit` (what the spirit does: the affinity traits and `liar`), `condition` (when it
  acts: `rain_walker`, `night_stalker`, `dark_dweller`, `noise_feeder`, and the reacting ones `chest_watcher`,
  `sleep_thief`, `deep_caller`, `grudge`) or `leaning` (a fear or a desire, filled by T6). Only habits carry a
  `weakness` and vote for the nature. `Traits.roll` draws habits by the nature rule (all but one from the nature), then
  one or two conditions, then one leaning once T6 has added them.
- **Saves:** trait ids are saved with the spirit, so an old spirit keeps its list. `Traits.weakness` counts habit votes
  only and falls back to the old all-trait vote when a spirit has no habit.
- **Done when:** `TraitsTest` shows that a rolled spirit's nature is the strict majority of its habits, that conditions
  never change it, and that a pre-change trait list still gives the nature it gave before wherever its habits allow.
- **Size:** M. **Hot:** `spirits.json`, `Traits`, `Trait`.

### T2. A catalog of forty

- **Do:** grow to 6 to 8 habits per nature, 10 conditions, and leave room for 10 leanings (T6). Each new trait needs
  its affinities or modifier, clue list, spirit box words and planchette lines in both languages, then
  `mise run voice:generate` for the new words. New conditions may need new `Trait.Condition` values (for example
  underground, in water, near a village, full moon once M5 exists) and triggers.
- **Rule:** a habit must read as its nature without explanation. Malice hurts or threatens. Deceit shows what is not
  there. Objects moves things. Darkness takes light and warmth.
- **Done when:** the catalog has at least 24 habits, 6 or more per nature, and 10 conditions; `LangTest` and the config
  coverage test pass; the wiki trait tables list them all.
- **Size:** L. **After:** T1.

### T3. Ash stops naming the nature

- **Do:** `AshBlock` picks a print from `shown.get().weakness()`, so one trail names the nature with a 67 to 80%
  chance. Make the print depend on the spirit's type build (the same source `photo/Apparition` uses), and let a
  condition add a mark on top (a wet print for `rain_walker`). The nature is then read from two or three habits.
- **Done when:** a GameTest shows two spirits of one type and different natures leaving the same print, and
  `AshGameTests` still pass. The journal's weakness chapter and the wiki no longer teach print equals nature.
- **Size:** S. **After:** T1.

### T4. Every habit has a tell

- **Do:** one small visible sign per habit that only it produces, fired through the director like a light event with
  low impact: frost on glass and breath for `cold_touch`, a chest lid left ajar for `chest_watcher`, and so on. This is
  the fifth clue channel: watching. A tell writes a journal note of its own kind.
- **Done when:** every habit in the catalog has a tell with a GameTest that it runs, each is in
  [events-index.md](events-index.md) and on the wiki, and the journal shows the note.
- **Size:** L, splits by nature into four agents after the first tell sets the pattern. **After:** T2.

### T5. A weakness that works every day

- **Do:** the material of a spirit's nature holds it back outside the altar.
  - objects: its item and door events skip iron doors and containers under iron bars;
  - darkness: its light events skip lights within a few blocks of glowstone;
  - malice: holy water thrown at the player's feet ends a running event of that spirit;
  - deceit: a mirror in the hand shows a fake block or a double for what it is.
- **Seam:** a check beside `ward/Wards.stops`, with the same "blocked" ash and chime, so the player sees it work. It is
  also the cheap way to test a guess before the altar.
- **Done when:** four GameTests, one per nature, show the material stopping its own spirit and not a spirit of another
  nature.
- **Size:** L. **After:** T1.

### T6. Fears and desires

- **Do:** fill the `leaning` axis with about ten traits. A fear (fire, cats, a bell, running water, iron) is a
  situational ward: the spirit's events do not run on a player beside the feared thing. A desire (milk, music, a toy,
  bread) is an offering that lowers anger (used by R6). A hate raises anger when the player uses the hated thing: the
  lantern for a light-eating spirit, the bell for a noise-hating one (both already listed as not built in
  [spirits.md](spirits.md)).
- **Done when:** unit tests cover the roll; GameTests cover one fear, one desire and one hate end to end; each leaning
  has clue text so tools can reveal it.
- **Size:** L. **After:** T1, T2.

### T7. A type leans to a nature

- **Do:** `spirits.<type>.natures` holds weights instead of the even 25% (poltergeist 60% objects, shade 60% darkness,
  and so on). Knowing the type becomes a prior, and the rare combination a surprise.
- **Saves:** the nature of an existing spirit must not change; roll with the new weights only for spirits born after
  the change (a version mark on the spirit, optional field).
- **Done when:** `TraitsTest` shows the distribution over many seeds, and an old spirit keeps its nature.
- **Size:** S. **After:** T1.

### T8. Traits that foil a tool

- **Do:** next to `liar` (lies through the box and the board), one trait per tool: a spirit that does not show on
  photographs, one that leaves no prints, one the spirit box cannot hear. A spirit has at most one. The tool then
  answers nothing for that spirit, and the journal notes the silence, which is itself a clue.
- **Done when:** a GameTest per trait shows the foiled tool silent and the others working.
- **Size:** M. **After:** T2.

### T9. The form comes from the death

- **Do:** today the form (whole, bit, crafted) belongs to the type and the bestiary names it after a photo. Take it
  from the story's cause of death instead (12 causes in `spirits.json`, 4 per form), which the gravestone tells. The
  cause also picks the family of the grave's wanted item (I4). The photo stops giving the form away.
- **Saves:** existing spirits keep the form of their type (same version mark as T7).
- **Done when:** unit tests map every cause to a form; `RitualGameTests` pass with the form read from the story; the
  journal's weakness chapter is rewritten with `player-text`.
- **Size:** M. **After:** T7.

### T10. A hidden trait

- **Do:** one habit of each spirit stays hidden from every tool until the player is shaken (sanity below
  `sanity.reveal_below`) or the spirit has evolved. Evolution also adds one new habit, rolled from the seed, so growing
  up changes behaviour, not only the power floor.
- **Done when:** GameTests show the hidden habit never revealed at full sanity over many readings and revealed when
  shaken, and an evolved spirit carrying one more habit after a save and load.
- **Size:** M. **After:** T2.

### T11. Anger you can see

- **Do:** above `anger` 0.7 a habit's events use a harsher variant where one exists (`door_slammer` locks the door
  instead of slamming it). Start with one harsh variant per nature, reusing existing harmful events where they fit.
- **Done when:** a GameTest per variant shows the mild form below the threshold and the harsh one above.
- **Size:** M. **After:** T4.

### T12. A second nature

- **Do:** an evolved spirit's foreign habit counts as a second nature. Its ritual asks for a second offering of that
  nature at the middle phase (R1). The investigation has to go past the first majority.
- **Done when:** `RitualGameTests` cover an evolved spirit banished with both offerings and failing with one.
- **Size:** M. **After:** R1, T10.

## Phase 2: investigation

### I1. Each tool tells a different thing

- **Do:** three of the four tools now give "a trait". Split them: the spirit box tells the story and the desire, the
  ouija board a habit (and may lie), ash the build and a condition (T3), the camera the type. `divine/Clues.reveal` and
  `Divination` choose by the trait's `kind`.
- **Done when:** `CluesTest` shows each clue kind drawing from its own axis, and the journal's clue chapter and the
  wiki tool pages say what each tool is for.
- **Size:** M. **After:** T1, T3, T6.

### I2. A case sheet

- **Do:** on a spirit's dossier in the journal the player marks their own guesses: nature, form, habits seen. The
  journal stores them and never confirms them, in line with its rule of keeping what was seen and no conclusions.
- **Done when:** `JournalGameTests` cover marking, saving and reloading a guess.
- **Size:** M.

### I3. More unfinished business

- **Do:** `spirit/land/Graves` has one quest: bring the wanted item. Add, chosen by the seed and the cause of death:
  1. bury the remains: bones lie 50 to 150 blocks off, the spirit acts more often while they are carried;
  2. a letter to a descendant: a villager in the nearest village takes it;
  3. the named killer: a mob with the name from the story, spawned near the death place;
  4. a killer spirit: rest comes only after another of the player's spirits is gone.
- **Done when:** each kind has a GameTest from reading the gravestone to the spirit at rest, and the journal's graves
  chapter covers them.
- **Size:** L, one agent per kind after the first adds the quest-kind seam. **After:** T9 for the item family.

## Phase 3: rituals

`banish/Ritual.java` runs `ritual.waves` (12) equal waves of 5 seconds: the spirit may snuff a candle and may strike.

### R1. Phases

- **Do:** turn the flat waves into four phases with their own length and rules: the call (quiet), the bargain (the
  spirit offers to stop for a gift: accept and the ritual ends with anger lowered), the fury (candles and events, as
  today), the leaving. Phase lengths replace `ritual.waves`; `evolution.extra_waves` and `ritual.off_form_waves`
  lengthen the fury. The phase is sent to the client for sound and particles.
- **Done when:** `RitualGameTests` and `AltarGameTests` pass on the phase model, with new cases for each phase edge.
- **Size:** L. **Hot:** `Ritual`. Everything else in this phase comes after it.

### R2. The spirit shows itself

- **Do:** in the fury and the leaving a `content/entity/Silhouette` in the spirit's build walks the salt ring. A gaze
  rule by nature: for two natures the player must not look at it longer than 2 seconds, for the other two must not
  look away. Breaking the rule costs a candle, not the ritual.
- **Done when:** GameTests cover both gaze rules. Uses the body from S3 when it exists, the silhouette until then.
- **Size:** L. **After:** R1.

### R3. Each nature fights its own way

- **Do:** in the fury, objects throws the offering off the altar (put it back before the next wave), darkness puts out
  every light but the candles, malice strikes the player, deceit shows candles as out that are lit.
- **Done when:** one GameTest per nature.
- **Size:** L. **After:** R1.

### R4. The name

- **Do:** a player who read the gravestone may say the spirit's name in chat during the leaving
  (`Restless.onChat`): the fury is shortened by 4 waves at the next ritual step, or the leaving ends at once. A wrong
  name raises anger.
- **Done when:** GameTests for the right and the wrong name.
- **Size:** S. **After:** R1.

### R5. The ring breaks

- **Do:** once per ritual the spirit wipes one salt tile of the ring; it must be laid again before the next wave or
  the ritual fails as it does for an unlit candle.
- **Done when:** a GameTest for the repaired and the unrepaired ring.
- **Size:** S. **After:** R1.

### R6. Appeasement

- **Do:** an offering of the spirit's desire (T6) on the altar, no candles and no ring needed: anger falls by 0.3 and
  the spirit stays. Each further appeasement of the same spirit costs one more item. This is the first thing that
  lowers anger on purpose.
- **Done when:** GameTests for the anger drop, the rising cost and a wrong desire doing nothing.
- **Size:** M. **After:** T6.

### R7. Interrogation

- **Do:** a short ritual (the call only) that costs sanity and makes the spirit answer one question truthfully, `liar`
  included: the player picks what to ask (a habit, the desire, the direction of the grave).
- **Done when:** GameTests for each question and the sanity cost.
- **Size:** M. **After:** R1, I1.

### R8. Place and hour

- **Do:** a ritual held within a few blocks of the spirit's gravestone or begun at midnight gives one more essence and
  a longer fury.
- **Done when:** unit test for the reward rule, GameTest for one case.
- **Size:** S. **After:** R1.

### R9. Blessing a house

- **Do:** a long ritual with 8 candles: no event runs within 16 blocks for 7 days, then every spirit of the player
  comes back with anger raised by 0.2. Reuses the lasting spells in `ward/Spells`.
- **Done when:** GameTests for the quiet week and the return.
- **Size:** M. **After:** R1.

### R10. Two at the altar

- **Do:** a second player inside the ring holding a bell or relighting candles shortens the fury; the spirit's strikes
  then land on both. The first shared mechanic for multiplayer.
- **Done when:** a GameTest with two mock players.
- **Size:** M. **After:** R1.

## Phase 4: the middle of the game

### M1. A successor

- **Do:** when a player's last personal spirit is gone, a new one of another type is bound after 2 to 4 days through
  `spirit/life/Births`, with some maturity so it is not harmless. `births.max_personal` still holds.
- **Done when:** `BirthGameTests` cover the delay, the different type and the cap.
- **Size:** S.

### M2. An echo

- **Do:** a spirit banished at the altar (not laid to rest at its grave) leaves an echo: a weak spirit of the same
  type with no essence that leaves by itself after 7 days. It makes the grave the better way and keeps the house from
  going silent. Default per D2.
- **Done when:** GameTests for the echo after the altar, none after the grave, and its leaving.
- **Size:** M.

### M3. A lure

- **Do:** `Births.lure` already exists; check what calls it first. Wanted: an effigy stuffed with an essence calls a
  spirit of that essence's type, so a player can go after the types the Requiem still needs.
- **Done when:** a GameTest from the stuffed effigy to the bound spirit, or a note in this file that it is already
  built.
- **Size:** S.

### M4. A haunted thing

- **Do:** rare loot in structure chests (through `data/restless/loot_modifiers`) carries a personal spirit that binds
  to the player who picks it up. Nothing marks the item; the events do.
- **Done when:** a GameTest for the binding on pickup and for the cap.
- **Size:** M.

### M5. A calendar

- **Do:** three dates from the game day and the moon phase. Full moon: every spirit's power is 0.2 higher. Blood
  night every N days: wards wear twice as fast and a warning comes at dusk. Day of the dead once a season: no harmful
  events, tools never lie. The journal gets a chapter; the spirit candle shows the date by its flame.
- **Done when:** `PowerTest` covers the moon; GameTests cover the blood night's wear and the quiet day.
- **Size:** L.

### M6. Salt wears out

- **Do:** a salt ring turns grey after it has stopped N events and stops nothing until a tile is laid again. Only
  stopped events wear it, never time. Default per D6.
- **Done when:** `EnclosureTest` and a GameTest for the wear and the repair.
- **Size:** M.

### M7. Merging

- **Do:** two personal spirits of one player, both older than 15 days, merge into one that carries the habits of both
  and two natures (the ritual of T12). One of the two graves stays.
- **Done when:** a GameTest for the merge, the saved result and the ritual with two offerings.
- **Size:** M. **After:** T12.

### M8. Blight walks home

- **Do:** an angry territorial spirit (anger above 0.7) grows its blight as a strip toward the bed of the player who
  angered it instead of evenly. `spirit/land/Corruption` already keeps and undoes every spot.
- **Done when:** a GameTest shows the strip's direction and that it recedes with the spirit.
- **Size:** M.

### M9. First hours with stakes

- **Do:** per D1, a new personal spirit starts with maturity so the first harmful (telegraphed, never lethal) event
  can come in the first play session. Tune only config: `power`, `births.start_anger`, the first-join spirit.
- **Done when:** `PowerTest` shows the new start value and a simulated first three days roll at least one harmful
  event in most runs.
- **Size:** S.

## Phase 5: sound and body

### The owner's sound pack

The owner supplied `/home/keelfy/tmp/horror sounds.zip` (72 files, 33 MB, outside the repository). The raw pack is
never committed; only the processed files are. It was sorted by file name and by measuring the waveform, not by ear,
so every mapping below is a proposal the owner confirms in a dev client.

| Folder | Files | What it holds |
|---|---|---|
| `jump scares` | 37 `.ogg`, stereo, 44.1 kHz, 0.7 to 2.5 s | Nine families of four or five: bass drop, echoing scream, electronic glitch, glass shatter, horror stab, impact slam, roar, screeching violin, sound blast |
| `generic thriller` | 35 `.mp3`, stereo, 24 to 48 kHz, 2 s to 6 min | Beds and drones (about 15), swells, risers and transitions (about 10), one-shots: heartbeat, chain rattle, child's laugh, woman's scream, creature rattle, yell, whoosh, piano hits, violins |

Processing, the same for every file: trim silence at both ends, peaks at -3 dB, Ogg Vorbis at 44.1 kHz. Mono for
anything played at a point in the world (stereo does not attenuate or pan); a bed or a sting played on the player may
stay stereo. The machine has no `ffmpeg`; the `apps/voice` venv has `soundfile`, which reads both formats and writes
Ogg Vorbis.

### S1. Finish the sound list

- **State (2026-11-14):** four of the ten events are wired from CC0 recordings (`haunt.horn`, `haunt.chair_creak`,
  `haunt.claw_scratch`, `haunt.rustle`) but nobody has heard them yet. Six still play a placeholder: `haunt.song`,
  `haunt.lullaby`, `haunt.music_box`, `haunt.mirror_ring`, `haunt.mirror_crack`, `haunt.jolt`.
  [sounds-todo.md](sounds-todo.md) is the list: what each needs, what was searched, and the doubt about each wired
  file. The owner has allowed CC0 searching and will supply what cannot be found.
- **Do:** the owner listens to the four wired events and approves or rejects each. Wire `haunt.jolt` from `Impact Slam`
  and `Horror Stab` of the pack once D3 names its source. Source or synthesise `haunt.music_box` and
  `haunt.mirror_ring`; keep the placeholder for anything not found and leave it in the list. Re-tune the config
  pitches the list names.
- **Done when:** [sounds-todo.md](sounds-todo.md) has no row in "Wired, not yet heard", and every row left in "Still on
  a placeholder" says who will supply the sound.
- **Size:** S. Needs the owner's ear.

### S2. Sound where it is heard most

- **Do:** from the counts of G2, give the 30 most frequent events that still play a vanilla sound a recording or a
  processed variant of their own.
- **Done when:** the list of the 30 and their new sounds is in the change, and the sounds play in a dev client.
- **Size:** L. **After:** G2, C1.

### S3. A body for the spirit

- **Do:** a visible spirit entity: translucent, shaped by type and seed the way `photo/Apparition` draws the
  photograph, seen only through the spirit candle or the camera, standing at the spirit's `Location`. Texture work goes
  through the `chatgpt-6.1-sol` harness. R2 uses it at the altar.
- **Done when:** a GameTest shows the entity at the spirit's point for a player holding the candle and not for one
  without, and a screenshot of each build is attached to the change.
- **Size:** L.

### S4. Put the rest of the pack to work

Most of the pack fits no stand-in of S1; it fits places that have no sound of their own yet.

- **Do:**
  1. **A sting per scare.** Every jump scare now shares `haunt.jolt`. Give the scare events of `event/Scares.java`
     and the jolts their own family from `jump scares`, chosen by what the scare is: a roar or an echoing scream for
     a figure in the face, a violin screech for one seen far off, a bass drop or a sound blast for the dark. One new
     sound event per family, the event's sound id in its config. `Electronic Glitch` fits the spirit box and the
     camera, not a scare.
  2. **Beds.** A looping low bed for the blighted biome (its biome file has no ambient loop), one for sanity below
     `sanity.overlay_below`, one for the fury phase of the ritual (R1). Candidates: `Creepy Bass Ambience`,
     `Creepy Horror Atmosphere`, `excellent-drone`, `suspense-dark-ambient`, `tense-atmosphere`. Cut each to a loop of
     20 to 40 s with matched ends.
  3. **Stings for moments.** A spirit evolving, a wrong offering, the leaving phase, a spirit laid to rest, the
     Requiem. Candidates: `Horror Swell`, `rising-evil`, `metallic-surge`, `Scary Transition`, `build-up-of-transition`.
  4. **One-shots.** The heartbeat for the brink damage band and very low sanity; the chain rattle and the creature
     rattle as new sourceless sounds in `event/Sounds.java` (allowed under rule 1: a sound for an event, not a new
     mechanic); the child's laugh as a second `haunt.laugh` variant (it has one file); the woman's scream as a second
     `haunt.screech`.
- **Not used:** the two long tracks (`track-04` at 6 min, `Typical Horror Ambience` at 2 min) unless cut to a loop, and
  the low-level files (`brief-horror-theme`, `scaryviolins`: peaks under 0.1, noise will come up with the gain).
- **Done when:** each wired file is listed in the change with its event, the owner has heard them in a dev client, and
  the wiki's sound page names the new sourceless sounds.
- **Size:** L, splits into the four parts above. Part 2's ritual bed comes after R1. **After:** D3 settled.

## Phase 6: curation and release

### C1. Cut and reweigh

- **Do:** play with G2 running (the owner or a scripted long run), then for each event read its counts: never ran
  (fix its `canRun` or remove it), runs too often (lower its weight), weak in play (owner's call). Remove what is
  cut from the code, config, [events-index.md](events-index.md) and the wiki.
- **Done when:** no event has zero runs in a 20-hour sample without a written reason, and the counts in the index and
  the wiki match the code.
- **Size:** L. **After:** G2. Needs the owner.

### C2. Advancements as a path

- **Do:** there are 4 advancements. Add a chain that follows the loop: first clue, first photograph, nature guessed
  right, first banishing, first spirit laid to rest, each essence, the Requiem.
- **Done when:** the advancement files load and a GameTest grants one from its trigger.
- **Size:** S.

### C3. An alpha build

- **Do:** version, changelog, licence line and a release job in `.github/workflows/mod.yml` that builds the jar on a
  tag. Publishing anywhere waits for D5.
- **Done when:** a tag produces a downloadable jar from CI.
- **Size:** S.

## Order

| Wave | Tasks (parallel unless a hot file says otherwise) | Gate |
|---|---|---|
| 0 | G1, G2, G3, G4, G5 | none |
| 1 | T1, then T3, T5, T7 in parallel; M1, M3, M9, C2 beside them | G1 |
| 2 | T2, then T4, T6, T8, T10 in parallel; R1 beside them; I2 | T1 |
| 3 | I1, T9, T11; R2, R3, R4, R5, R8 in sequence on `Ritual`; M2, M4, M6 | T2, T6, R1 |
| 4 | T12, R6, R7, R9, R10; I3; M5, M8; S1, S3, S4 | wave 3 |
| 5 | M7; C1, then S2; C3 | T12, G2 |

The smallest slice that changes how the mod plays is G2, T1, T3, T5, R1, R2 and M1. If the plan has to be cut, keep
those.

## Not in this plan

- Binding spirits into vessels, servants and ectoplasm ([design.md](design.md) stage 6). The binding ritual is the
  natural next variant after R1, but it opens a whole tech tree.
- The Other Side dimension and its bosses (stage 7).
- A Fabric port.
- Multiplayer roles beyond R10.
- Structures of the mod's own.
- New haunt events from the backlogs.
