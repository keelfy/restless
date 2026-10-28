# Haunt event ideas

Raw backlog of 100 event ideas. All 62 **Easy** and 28 of the 29 **Medium** ones are implemented (see the `Idea` column in
`spirits.md`); 23 (piston push) was built and dropped as looking wrong. Of the 9 **Hard** ones, 7 (dimming), 36 (echo), 61 (shadow), 93 (double), 94 (edge figure), 95 (fog silhouette) and 100 (the call) are done, and 45 and 84 were skipped for now. Numbers stay as they are so the difficulty section keeps working.

Tiers: **L** light, **H** harmful, **D** deadly (must be telegraphed so the player can react).
Tags in parentheses; `*` marks tags that do not exist in code yet.

## Lights (LIGHTS)

1. L: all candles in the room go out at once.
2. L: torches go out one by one, a chain walking toward the player.
3. L: a lantern flickers (removed and placed back a few times).
4. L: a manually powered redstone lamp switches off for a second.
5. L: a furnace fire dies while fuel is left.
6. L: a campfire goes out.
7. L: light level in the area slowly drops, no blocks touched.
8. L: candles light themselves in an empty room.
9. L: a jack o'lantern gets a new face carved.
10. H: all light within 16 blocks goes out and a hostile mob spawns right after.
11. H: the torch "burns the hand": it drops out of the player's hand.

## Doors and mechanisms (DOORS)

12. L: a door slowly opens and closes in a breathing rhythm.
13. L: a trapdoor slams.
14. L: a fence gate opens by itself.
15. L: a button is pressed with nobody around.
16. L: a lever clicks.
17. L: a pressure plate triggers.
18. L: an iron door opens without redstone.
19. H: a door slams and locks; it cannot be opened for 10 s and the player is trapped.
20. H: every door in the house is wide open at night, so mobs can walk in.
21. L: a chest lid clicks (sound and animation, contents untouched).
22. L: a barrel opens.
23. H: a piston pushes a block into the player's position. (Built, then dropped: it looked wrong.)
101. L: the jukebox music is cut off: the playing disc is knocked out.

## Sounds (SOUND*)

24. L: footsteps behind the player, source 2 blocks back.
25. L: creeper hiss with no creeper.
26. L: three knocks on the door.
27. L: a crying child (villager baby sound) where there is no village.
28. L: block breaking sound nearby.
29. L: someone eating behind a wall.
30. L: enderman hiss.
31. L: chest opening sound the player did not cause.
32. L: breathing right next to the player, getting louder.
33. L: a distant bell.
34. L: a jukebox plays a disc that is not there (sound only).
35. L: the player's name whispered in chat by "???".
36. L: echo: the player's last sound repeats 3 s later.
37. L: a player death sound nearby.
38. L: dripping water in a dry cave.
102. L: the game's background music cuts out (stop packet for the music category).

## Afflictions (AFFLICTION)

39. L: nausea for 5 s.
40. L: slowness ("heaviness").
41. L: short mining fatigue.
42. L: hunger drops a little.
43. L: short darkness effect.
44. L: the camera shakes slightly (small knockback).
45. L: inverted controls for 3 s (needs client).
46. H: poison.
47. H: weakness in combat.
48. H: levitation for 2 s, then a fall.
49. H: freezing like powder snow, outside snow.
50. H: experience drains from "cold".
51. H: bad luck: worse loot via the unluck effect.

## Assaults (ASSAULT)

52. H: a push in the back, more likely near a ledge.
53. H: trip: the player stops in place as if sneaking.
54. H: the player catches fire for a second.
55. H: damage from nowhere, like `cold_touch` but stronger.
56. H: the spirit snatches the held item and throws it 5 blocks away.
57. D: suffocation: air bar drains out of water; get to the light to stop it.
58. D: after "three knocks", lightning strikes nearby, then the player if they have not moved.
59. D: the player is tossed up over a drop (only over safe ground, or not).
60. D: "embrace": damage grows every second until the player moves 10 blocks away.
61. D: a shadow follows the player: it freezes while watched and closes in when the player looks away; touch kills.

## Items and inventory (ITEMS*)

62. L: two inventory slots swap.
63. L: the selected hotbar slot changes.
64. L: an item gets a strange name: "Why are you here".
65. L: a book in the inventory is rewritten with creepy lines.
66. L: items in a chest are shuffled.
67. H: a tool loses some durability.
68. H: food in the inventory rots into rotten flesh.
69. H: an item vanishes from a chest and turns up later in another chest.
70. H: a compass points at "the spirit" instead of spawn.
71. L: a map shows a marker that is not there.

## Blocks (BLOCKS*, all changes reversible)

72. L: potted flowers wither into dead bushes.
73. L: nearby crops wither one growth stage.
74. L: a sign is rewritten.
75. L: a painting changes to another one.
76. L: an item frame rotates.
77. L: wall blocks briefly turn to glass and "someone is standing" behind the wall.
78. L: water in a cauldron disappears.
79. L: grass around the player turns to dirt, a circle 3 blocks wide.
80. H: the floor under the player turns to sand or gravel for a second.
81. H: the bed breaks ("you will not sleep here") and drops as an item.
82. H: a hole appears in the house wall.
83. L: blocks are placed in a cross or pattern near the base.
84. L: a message spelled in blocks, e.g. "BEHIND".

## Mobs (MOBS*)

85. L: farm animals all turn their heads to the player at once.
86. L: a cat hisses at nothing.
87. L: a dog barks into a corner.
88. L: a villager freezes and stares at the player.
89. L: bats fly out of a wall.
90. H: an animal escapes: the pen gate opens.
91. H: one tamed mob goes wild.
92. H: a zombie wearing the player's armor spawns behind them.
93. D: "double": a mob with the player's skin that hits hard.

## World and presence (PRESENCE*)

94. L: a figure at the edge of vision vanishes when looked at.
95. L: a "herobrine" silhouette in the fog.
96. L: footprints appear behind the player on snow or sand.
97. L: breath particles at the face in a warm biome.
98. L: rain only over the player (particles).
99. H: at night, time jumps 1000 ticks forward: "you lost time".
100. D: "the call": at night the player is slowly pulled sideways toward lava or the void unless they resist.

## Fit with spirits

- `poltergeist`: doors and mechanisms (12–23), items (62–71), pushes (52, 56).
- `shade`: lights (1–11), sounds (24–38), afflictions (39–51), presence (94–98).
- Future spirit types:
  - mimic: echo (36), double (93), figure at the edge of vision (94), name in chat (35);
  - demon: fire (54), rot (68), the call (100);
  - wraith: footprints (96), cold (49), shadow (61).

## Cost

- Cheap and server-only, good first picks: sounds (24–38), buttons and levers (15–16), trapdoors and gates (13–14),
  slot swaps (62–63), withering (72–73).
- Harder (slow animation plus a telegraph): shadow (61), embrace (60), the call (100).
- Needs client code: inverted controls (45), edge-of-vision silhouette (94–95), rain over the player (98).

## Implementation difficulty

Levels:

- **Easy**: one action in one tick using vanilla API, like the current events. Server only.
- **Medium**: needs shared infrastructure that does not exist yet (tags below). Each piece is built once and then
  unlocks a group of events.
- **Hard**: a custom entity with AI or rendering, client code, or a multi-step mechanic with a telegraph.

Infrastructure tags:

- `sched`: delayed and repeating tasks per player (wait N ticks, repeat while a condition holds).
- `restore`: remember changed blocks and put them back later (also on server stop).
- `place`: find a valid spot (a ledge, a wall, open floor, a spawnable position).
- `hook`: a loader event exposed through the `Platform` seam (e.g. cancel a block interaction).
- `access`: access to a private field (access transformer).
- `map`: map item decorations.

### Easy (62)

1, 6, 8, 9, 11, 13, 14, 15, 16, 17, 18, 20,
24, 25, 27, 28, 29, 30, 31, 33, 34, 35, 37, 38,
39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50, 51,
53, 54, 55, 56, 62, 63, 64, 65, 66, 67, 68, 69, 70,
73, 76, 78, 79, 81, 86, 87, 89, 90, 91, 92, 97, 99.

### Medium (29, 28 done)

3, 5, 10, 19, 52, 59, 71, 72, 74, 75, 77, 80, 82, 83, on top of the 14 that needed only `sched`. 23 was dropped.

- `sched`: `haunt/Scheduler.java`.
- `restore`: `haunt/Restore.java`, undo entries run when due and when the server stops.
- `place`: `haunt/event/Place.java`: a wall, standing room, a ledge, a flat spot for a plus.
- `hook`: `Restless.onUseBlock` plus `haunt/DoorLocks.java`; the NeoForge module forwards `PlayerInteractEvent.RightClickBlock`.
- `access`: not needed. The furnace's private burn time is zeroed through its own save format (`saveCustomOnly` / `loadCustomOnly`).
- `map`: not needed. `MapItemSavedData.addTargetDecoration` writes the marker into the item.

### Hard (9, 7 done)

7 is a client overlay: the `Dimming` HUD layer under the HUD, started by `DimPayload` (lighting is client-side, so no light level is touched).

36 records the player's own sounds through `PlayLevelSoundEvent` (`Restless.onPlaySound`, `haunt/Echoes.java`) and replays the last one in another sound category so it is not recorded again.

61 is a vanilla wither skeleton with its AI off, moved by hand each step by `event/ShadowEvent.java`: no entity type, renderer or client code, so it needs nothing the client lacks. 93 is the vanilla `mannequin` entity given the player's game profile, moved by the same `Walk` helper (`event/DoubleEvent.java`), so it too needs no renderer. 94 and 95 (`event/PresenceFigure.java`) turned out server-only too: the view-cone check is a dot product against the look vector, and "fog" is rain or darkness under open sky. 100 is a velocity nudge every few ticks with `ClientboundSetEntityMotionPacket`; resisting is simply walking away, because the client's own input competes with the pull.

| # | Idea | Why |
|---|---|---|
| 45 | inverted controls | client input code and networking |
| 84 | message in blocks | glyph font, finding a large valid surface, restore |

### Order

1. Easy events: pick them for spirit coverage and tags; no new code beyond the event itself.
2. `sched` (done, `haunt/Scheduler.java`): needed by 18 medium events; it alone unlocked 14 of them, including the
   deadly 57, 58, 60 (and it is the base for 100).
3. `restore` (done, `haunt/Restore.java`): also required before any block-replacement event ships.
4. `place`, `hook` (done). Hard events after the client module exists.
