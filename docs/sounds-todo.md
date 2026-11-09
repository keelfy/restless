# Sounds to source

Nine new sound events in `HauntSounds`, declared in `apps/mod/core/src/main/resources/assets/restless/sounds.json`.
Until a recording exists each event plays the vanilla stand-in listed below (a `"type": "event"` entry in `sounds.json`),
so nothing is silent.

**Format:** Ogg Vorbis, **mono** (stereo does not attenuate or pan in 3D), 44.1 kHz, peaks around -3 dB.
Put files in `apps/mod/core/src/main/resources/assets/restless/sounds/haunt/`.

**Wiring a file:** in `sounds.json`, replace the event's `{ "name": "<stand-in>", "type": "event" }` with one
`{ "name": "restless:haunt/<file>" }` per variant (no extension, no `type`). Java and config need no change.

| Event | Files | Length | Used by | What it is | Stand-in |
|---|---|---|---|---|---|
| `haunt.song` | `song` (1, a 2nd as an alternative) | exactly 12 s | `spirit_song`: empty jukebox | Wordless humming, thin, a little off-key, as if from another room. Reads as music. | `haunt.moan` |
| `haunt.horn` | `horn_1`, `horn_2` | 4–7 s | `spirit_horn`: heard 40 blocks away | One low, mournful horn blast (ram's horn, conch), long reverb tail, no attack click. | goat horn |
| `haunt.music_box` | `music_box_note` (1; 2–3 slightly different timbres if you like) | 1–1.5 s | `restless:music_box` | One tinny comb note, quick decay. **Record at F♯ (note 12).** The game pitches it 0.5×–2× to play a tune. | note block chime |
| `haunt.lullaby` | `lullaby_note` (1) | 0.5–0.8 s, fade-out | `bedroom_lullaby` | One hummed "mm" or "la", soft and breathy, adult female. **Record at the melody's highest note** (it is played at 1.0×). The lowest notes are pitched an octave down, so the sample must survive that. | flute |
| `haunt.claw_scratch` | `claw_scratch_1..3` | 0.6–1.2 s | `restless:claw_marks`: claw marks on a door | Nails or claws dragged slowly down wood. Three variants so repeats do not match. | axe scrape |
| `haunt.chair_creak` | `chair_creak_1..4` | 0.4–0.8 s | `restless:rocking_chair` (played 24 times, every 0.7 s) | A rocker creaking on a floorboard, dry and short. Ideally forward and back pairs. | trapdoor open |
| `haunt.mirror_ring` | `mirror_ring_1..2` | 1–2 s | `mirror_names`: mirror is marked | A thin glass or crystal ring with a faint shimmer. Quiet, it plays at the player. | amethyst chime |
| `haunt.mirror_crack` | `mirror_crack_1..3` | 0.5–1.5 s | `restless:cracked_mirror` | A hairline crack slowly running across glass. No shatter. | glass hit |
| `haunt.jolt` | `jolt_1..3` | 0.3–1 s | `jolt_behind`, `jolt_side`, `jolt_dark` | A sudden loud bang or sting, sharp attack, no tail. Plays at volume 2.0 right behind or beside the player. | anvil land |
| `haunt.rustle` | `rustle_1..3` | 0.5–1.5 s | `jolt_lunge`: heard from 8–12 blocks, through a wall, every 2 s | Something shifting in the dark: cloth or dry leaves, a slow drag. Very quiet and soft, no clear attack, nothing that reads as a bang. | azalea leaves step |

**Total:** 10 events, about 24 files.

## Notes

- `spirit_song`: `length_in_seconds` in `data/restless/jukebox_song/spirit_song.json` is 12.0. Change it if your clip
  differs.
- `spirit_horn`: `use_duration` in `data/restless/instrument/spirit_horn.json` is 7.0.
- Config pitches that were tuned for the stand-ins are now 1.0 (`spirit_horn`, `claw_marks`, `rocking_chair`,
  `cracked_mirror`, `mirror_names`). Re-tune them once the real files are in.
- `cliff_faller` now reuses the existing `haunt.cut_off_scream`, so it needs no new file.

## Optional, still vanilla

Vanilla fits these, but a custom sound would add atmosphere. They are not wired up.

- `restless:doll`: steps (wood place plus wool break). Small cloth-and-wood shuffles, 3 variants.
- `restless:rotten_planks`: planks breaking (wood break). A damp creak and crumble, 2–3 variants.
