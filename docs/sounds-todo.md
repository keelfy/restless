# Sounds to source

The list of sound events in `apps/mod/core/src/main/resources/assets/restless/sounds.json` that still play a
placeholder, and of the ones that got a recording nobody has listened to yet. Keep it current: a sound leaves the first
table when its file is wired, and the second when the owner has heard it in the game.

**Format:** Ogg Vorbis, **mono** (stereo does not attenuate or pan in 3D), 44.1 kHz, peaks around -3 dB.
Put files in `apps/mod/core/src/main/resources/assets/restless/sounds/haunt/`.

**Wiring a file:** in `sounds.json`, replace the event's `{ "name": "<stand-in>", "type": "event" }` with one
`{ "name": "restless:haunt/<file>" }` per variant (no extension, no `type`). Java and config need no change. Credit the
source in `README.md`.

**Licence:** CC0 only, unless the owner says otherwise for a file.

## Still on a placeholder

| Event | Files wanted | Length | Used by | What it is | Placeholder | Searched |
|---|---|---|---|---|---|---|
| `haunt.song` | `song` (1, a 2nd as an alternative) | exactly 12 s | `spirit_song`: empty jukebox | Wordless humming, thin, a little off-key, as if from another room. Reads as music. | `haunt.moan` | OpenGameArt: no CC0 humming voice |
| `haunt.lullaby` | `lullaby_note` (1) | 0.5–0.8 s, fade-out | `bedroom_lullaby` | One hummed "mm" or "la", soft and breathy, adult female. **Record at the melody's highest note** (it is played at 1.0×). The lowest notes are pitched an octave down, so the sample must survive that. | flute | OpenGameArt: no CC0 humming voice |
| `haunt.music_box` | `music_box_note` (1; 2–3 slightly different timbres if you like) | 1–1.5 s | `restless:music_box` | One tinny comb note, quick decay. **Record at F♯ (note 12).** The game pitches it 0.5×–2× to play a tune. | note block chime | OpenGameArt: only a whole tune (`musicbox.mp3` in "CC0 Scraps"), no single note. Can be synthesised if no recording turns up |
| `haunt.mirror_ring` | `mirror_ring_1..2` | 1–2 s | `mirror_names`: mirror is marked | A thin glass or crystal ring with a faint shimmer. Quiet, it plays at the player. | amethyst chime | Not found. Can be synthesised |
| `haunt.mirror_crack` | `mirror_crack_1..3` | 0.5–1.5 s | `restless:cracked_mirror` | A hairline crack slowly running across glass. No shatter. | glass hit | Only shatters found: `bfh1_glass_breaking_*` in [75 CC0 breaking / falling / hit sfx](https://opengameart.org/content/75-cc0-breaking-falling-hit-sfx) (rubberduck), and `Glass Shatter` in the owner's pack. Neither is a slow crack |
| `haunt.jolt` | `jolt_1..3` | 0.3–1 s | `jolt_behind`, `jolt_side`, `jolt_dark`, the scares | A sudden loud bang or sting, sharp attack, no tail. Plays at volume 2.0 right behind or beside the player. | anvil land | `Impact Slam` and `Horror Stab` in the owner's pack fit. Not wired: the pack's source and licence are not named yet |

## Wired, not yet heard

Chosen by file name and by measuring the waveform. The owner listens in a dev client and either strikes the row or
moves the event back to the table above.

| Event | Files | Source (CC0, OpenGameArt) | Doubt |
|---|---|---|---|
| `haunt.horn` | `horn_1`, `horn_2`, 6 s each | [War Horns](https://opengameart.org/node/153572), Eldritch Grim | Cut from a 49 s layered piece, not one clean blast: other horns may sound under it. Wanted is one low, mournful blast |
| `haunt.chair_creak` | `chair_creak_1`, `chair_creak_2`, 0.26 s each | `wood_squeak_01`, `_02` of [100 CC0 metal and wood SFX](https://opengameart.org/content/100-cc0-metal-and-wood-sfx), rubberduck | Shorter than the 0.4–0.8 s asked, and two variants where four were asked. It plays 24 times, every 0.7 s |
| `haunt.claw_scratch` | `claw_scratch_1..3`, 1.1–1.3 s | `scrape-1`, `-2`, `-4` of [Scrapes](https://opengameart.org/content/scrapes), AntumDeluge | An object dragged over a cinder block, not claws on wood |
| `haunt.rustle` | `rustle_1..3`, 0.85–0.9 s | Three cuts of [Fabric Rustling](https://opengameart.org/content/fabric-rustling), Iochi Glaucus | The author notes it is compressed and noise-reduced. It should be very soft, with no clear attack |

## Notes

- `spirit_song`: `length_in_seconds` in `data/restless/jukebox_song/spirit_song.json` is 12.0. Change it if your clip
  differs.
- `spirit_horn`: `use_duration` in `data/restless/instrument/spirit_horn.json` is 6.0, the length of the horn files.
- Config pitches that were tuned for the stand-ins are now 1.0 (`spirit_horn`, `claw_marks`, `rocking_chair`,
  `cracked_mirror`, `mirror_names`). Re-tune them by ear once the files are approved.
- `cliff_faller` reuses the existing `haunt.cut_off_scream`, so it needs no new file.

## Optional, still vanilla

Vanilla fits these, but a custom sound would add atmosphere. They are not wired up.

- `restless:doll`: steps (wood place plus wool break). Small cloth-and-wood shuffles, 3 variants.
- `restless:rotten_planks`: planks breaking (wood break). A damp creak and crumble, 2–3 variants.
