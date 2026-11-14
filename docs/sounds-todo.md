# Sounds to source

The list of sound events in `apps/mod/core/src/main/resources/assets/restless/sounds.json` that still play a
placeholder, and of the ones that got a recording nobody has listened to yet. Keep it current: a sound leaves the first
table when its file is wired, and the second when the owner has heard it in the game.

**Format:** Ogg Vorbis, **mono** (stereo does not attenuate or pan in 3D), 44.1 kHz, peaks around -3 dB.
Put files in `apps/mod/core/src/main/resources/assets/restless/sounds/haunt/`.

**Wiring a file:** in `sounds.json`, replace the event's `{ "name": "<stand-in>", "type": "event" }` with one
`{ "name": "restless:haunt/<file>" }` per variant (no extension, no `type`). Java and config need no change. Credit the
source in `README.md`.

**Licence:** CC0, or the owner's royalty-free pack (decision D3, settled 2026-11-14), or generated. The raw pack is never
committed; `apps/voice/haunt_pack.py` cuts it and synthesises the generated notes.

## Still on a placeholder

| Event | Files wanted | Length | Used by | What it is | Placeholder | Searched |
|---|---|---|---|---|---|---|
| `haunt.song` | `song` (1, a 2nd as an alternative) | exactly 12 s | `spirit_song`: empty jukebox | Wordless humming, thin, a little off-key, as if from another room. Reads as music. | `haunt.moan` | OpenGameArt: no CC0 humming voice. **The owner supplies it** |
| `haunt.lullaby` | `lullaby_note` (1) | 0.5–0.8 s, fade-out | `bedroom_lullaby` | One hummed "mm" or "la", soft and breathy, adult female. **Record at the melody's highest note** (it is played at 1.0×). The lowest notes are pitched an octave down, so the sample must survive that. | flute | OpenGameArt: no CC0 humming voice. **The owner supplies it** |
| `haunt.mirror_crack` | `mirror_crack_1..3` | 0.5–1.5 s | `restless:cracked_mirror` | A hairline crack slowly running across glass. No shatter. | glass hit | Only shatters found: `bfh1_glass_breaking_*` in [75 CC0 breaking / falling / hit sfx](https://opengameart.org/content/75-cc0-breaking-falling-hit-sfx) (rubberduck), and `Glass Shatter` in the owner's pack. Neither is a slow crack. **The owner supplies it** |

## Wired, not yet heard

Chosen by file name and by measuring the waveform (length, envelope, peak, spectral centroid): nobody could listen.
Every file is trimmed, levelled to -3 dB peak and Ogg Vorbis 44.1 kHz; mono unless it is a loop played on the player.
The owner listens in a dev client and either strikes the row or
moves the event back to the table above.

| Event | Files | Source (CC0 OpenGameArt, the owner's pack, or generated) | Doubt |
|---|---|---|---|
| `haunt.horn` | `horn_1`, `horn_2`, 6 s each | [War Horns](https://opengameart.org/node/153572), Eldritch Grim | Cut from a 49 s layered piece, not one clean blast: other horns may sound under it. Wanted is one low, mournful blast |
| `haunt.chair_creak` | `chair_creak_1`, `chair_creak_2`, 0.26 s each | `wood_squeak_01`, `_02` of [100 CC0 metal and wood SFX](https://opengameart.org/content/100-cc0-metal-and-wood-sfx), rubberduck | Shorter than the 0.4–0.8 s asked, and two variants where four were asked. It plays 24 times, every 0.7 s |
| `haunt.claw_scratch` | `claw_scratch_1..3`, 1.1–1.3 s | `scrape-1`, `-2`, `-4` of [Scrapes](https://opengameart.org/content/scrapes), AntumDeluge | An object dragged over a cinder block, not claws on wood |
| `haunt.rustle` | `rustle_1..3`, 0.85–0.9 s | Three cuts of [Fabric Rustling](https://opengameart.org/content/fabric-rustling), Iochi Glaucus | The author notes it is compressed and noise-reduced. It should be very soft, with no clear attack |
| `haunt.jolt` | `jolt_1` (0.57 s), `jolt_2` (0.97 s), `jolt_3` (0.61 s) | Pack: `Impact Slam … 1`, `Horror Stab ….ogg`, `Impact Slam … 3` | `jolt_2` has a 0.4 s tail, more than "no tail" asks. Plays at volume 2.0 for `jolt_behind`, `jolt_side` and anything else that still uses `haunt.jolt` |
| `haunt.music_box` | `music_box_note`, 1.3 s | Generated: F♯4 (369.99 Hz), partials 1, 2, 3.01, 5.93, 16.6 with exponential decay (τ 0.42 s), a 4 ms tine click | Synthetic; may sound like an electric piano rather than a comb. Pitch is exact F♯4, so the tune is in tune at 0.5×–2× |
| `haunt.mirror_ring` | `mirror_ring_1` (E6, 1318.5 Hz), `mirror_ring_2` (G♯6, 1661.2 Hz), 1.8 s each | Generated: partials 1, 2.32, 4.25, 6.63, the lower three as detuned pairs beating 3–6 Hz for the shimmer, τ 0.55 s | Synthetic; the shimmer may read as a wobble. `mirror_names` pitch is 1.0 |
| `haunt.sting_roar` | `sting_roar_1..3`, 1.3–1.4 s | Pack: `Jumpscare Roar Sound Effect`, `… 2`, `… 4` | Figure in the face: `container_close`, `door_slam_face`, `jolt_lunge` (lunge). A beast's roar may not fit a silent black figure |
| `haunt.sting_scream` | `sting_scream_1..3`, 1.5–1.8 s | Pack: `Echoing Scream …`, `… 1`, `… 2` (`… 1` kept its louder channel: wide stereo) | Face at close range: `mined_face`, `eating`. The echo tail may blur at volume 2.0 |
| `haunt.sting_violin` | `sting_violin_1..3`, 0.9–1.0 s | Pack: `Screeching Violin … 1 - sharp`, `… 2 - drawn-out, twitching`, `… 3 - drawn-out, increasing` | Seen far off: `slow_turn`, and the figure of `name_left_right`. Two of the three swell in (0.25–0.35 s to half peak), so the sting lands late |
| `haunt.sting_bass` | `sting_bass_1..3`, 1.1–1.5 s | Pack: `Bass Drop Shock … 1`, `… 2`, `… 3` | The dark: `dig_down`. Centroid 400–500 Hz: thin on small speakers |
| `haunt.sting_blast` | `sting_blast_1..3`, 1.8–2.4 s | Pack: `Sound Blast …`, `… 1`, `… 3` | The dark: `jolt_dark`, `hide` (caught). Longest of the stings; `… 1` is 2.4 s |
| `haunt.blight_bed` | `blight_bed`, 30 s stereo loop | Pack: `Creepy Horror Atmosphere (Low Tone Ambience)`, 8–38 s, 2 s cross-fade | The blighted biome's ambient loop (`blighted.json`). The seam was cross-faded but nobody heard it loop. Volume is the biome loop's own (1.0) |
| `haunt.sanity_bed` | `sanity_bed`, 24 s stereo loop | Pack: `suspense-dark-ambient`, 8–32 s, 2 s cross-fade | Below `sanity.overlay_below`, louder as sanity falls. Source peaks at 0.29, so levelling raised it about 2.4× with its noise |
| `haunt.heartbeat` | `heartbeat_1`, `heartbeat_2`, 1.6 s each | Pack: `sfx-slow-powerful-heartbeat`, one lub-dub each (1.72–3.35 s, 6.55–8.15 s), louder channel kept | A brink event, and sanity below `sanity.heartbeat_below`. Very low (centroid 100 Hz): may be inaudible on laptop speakers |
| `haunt.evolve` | `evolve`, 4.9 s | Pack: `rising-evil`, 0–5 s, louder channel kept | A spirit evolving (was the warden heartbeat). The source is clipped at full scale for its first second |
| `haunt.requiem` | `requiem`, 3.3 s | Pack: `build-up-of-transition`, second riser (5.2–8.75 s) | The Requiem begins, at the altar. Ends on its peak with a 30 ms cut |
| `haunt.laid_to_rest` | `laid_to_rest`, 4.9 s | Pack: `Horror Swell`, 0–5 s | Each stone the Requiem raises. A swell may feel ominous where release is meant |
| `haunt.chain_rattle` | `chain_rattle_1..3`, 2.1–2.2 s | Pack: `chain-rattling-prison`, cuts at 0.3–2.4, 3.6–5.8, 7.9–10 s | New event `dragged_chains`. Spiky source (peak 0.96, RMS 0.04): quiet overall after levelling |
| `haunt.creature_rattle` | `creature_rattle_1..3`, 2.4–3.4 s | Pack: `Alien Monster Noises (Horror Creature Rattle)`, 2.9–6.3, 7.95–10.7, 11.95–14.3 s | New event `creature_rattle`. Source peaks at 0.22, raised about 3× with its noise |
| `haunt.laugh` | adds `laugh_2`, 3.9 s | Pack: `Horror Kid Laugh (Layered with Echo)`, louder channel kept | A child's laugh beside the existing adult cackle: the two variants differ a lot |
| `haunt.screech` | adds `screech_2`, 3.7 s | Pack: `Demonic Woman Scream (Horror Screech)` | Longer than the existing `screech` |
| `haunt.ritual_fury` | `ritual_fury`, 30 s stereo loop | Pack: `Tense & Pressured Horror Ambience`, 5.5–35.5 s, 2 s cross-fade | **Not played yet:** for the fury phase of the rewritten ritual (R1) |
| `haunt.ritual_leaving` | `ritual_leaving`, 3.3 s | Pack: `build-up-of-transition`, first riser (0–3.75 s) | **Not played yet:** for the leaving phase of the rewritten ritual |
| `haunt.offering_refused` | `offering_refused`, 3.3 s | Pack: `metallic-surge`, 0–3.5 s, louder channel kept | **Not played yet:** for a wrong offering, which lives in `banish/Ritual.java` |

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
