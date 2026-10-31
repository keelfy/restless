---
title: Sound
---

# Sound

30 events.

## `footsteps`

A step sound 2 blocks behind the player

Runs when: Always

## `creeper_hiss`

Creeper fuse hiss nearby

Runs when: Always

## `three_knocks`

Three knocks, on the nearest wooden door within 12 or else from nowhere nearby

Runs when: Always

## `crying_child`

High-pitched villager hurt sound

Runs when: Always

## `block_breaking`

Stone / wood / glass / gravel break sound nearby

Runs when: Always

## `eating_behind_wall`

Eating sound 3 blocks away

Runs when: Always

## `enderman_hiss`

Enderman stare sound

Runs when: Always

## `chest_opens`

Chest open sound nearby

Runs when: Always

## `breathing`

Five breaths 1 block from the player, each louder

Runs when: Always

## `distant_bell`

Loud bell from ~40 blocks away

Runs when: Always

## `phantom_jukebox`

Music disc 11 from a point nearby, no jukebox

Runs when: Always

## `music_cut`

Stops the game's background music for the player; it resumes on its usual schedule

Runs when: Always

## `whisper`

Chat line `<???> <player name>`, only to the player

Runs when: Always

## `echo`

That sound plays again 3 s later from where it was made, quieter and lower. Recorded through the loader's sound hook, `Restless.onPlaySound`; only sounds in the player category within 2 blocks of the player count

Runs when: The player made a sound (step, splash, bite) in the last 5 s

## `death_cry`

Player death sound nearby

Runs when: Always

## `dripping`

Water drip nearby

Runs when: Always

## `distant_mob`

A zombie, skeleton, spider, enderman, witch, husk, drowned or pillager sound 2–40 blocks away; louder the farther, so it carries

Runs when: Always

## `hostile_animal`

A zombie, creeper, skeleton or witch sound bound to it, sent to this player only

Runs when: An animal within 16

## `approaching_steps`

Twelve step sounds of the block underfoot, closing from 14 to 3 blocks on a random bearing

Runs when: Always

## `distant_mining`

Six stone hits and a break sound 8 blocks below the surface

Runs when: Open sky above the player

## `many_breaks`

Up to eight of them sound as if they break in turn; under hardcore they do break, with no drops

Runs when: Six or more glass or stone blocks within 14

## `ladder_steps`

Step sounds climbing a real ladder within 16, or rising from the spirit's point when there is none

Runs when: Always

## `spider_overhead`

A spider sound, then five steps crawling across the ceiling

Runs when: A solid block 2–3 above the head with air below it

## `cracking_floor`

Harmful.
Break particles and sound of the block underfoot; it stays

Runs when: Standing on a block

## `bottle_shatter`

A splash potion shatters 4 blocks away (sound and coloured particles)

Runs when: Always

## `shrieker`

It shrieks. No player is given to it, so nobody is warned and no warden is called

Runs when: A sculk shrieker within 16

## `fake_subtitle`

A sound with no audio that shows the subtitle "Something breathes behind you" from 3 blocks behind the player. Only seen with subtitles on

Runs when: Always

## `phantom_heartbeat`

Eight warden heartbeats at the player, one every 0.9 s

Runs when: Always

## `note_block_voice`

It plays a villager or player hurt, ghast scream, zombie villager or vex sound, with a note particle

Runs when: A note block within 16

## `own_voice`

A chat line `<name> ...` from `lines`, sent only to the player

Runs when: Always
