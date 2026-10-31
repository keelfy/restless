# Hardcore mode (plan)

Built so far: the setting, `Restore.revert`, and the real branches of the second batch's events (see Events with a real
branch below). Not yet: dimming (7), and an operator command. A world setting under which every temporary change stays and every purely visual trick becomes real:
a swapped block is not swapped back, a fake block becomes a real one, a hallucinated mob is spawned for real.
The exact design is decided later; this file only fixes the seams and the rules.

Difficulty uses the same 1–10 scale as [event-ideas-2.md](event-ideas-2.md#scale).

## Where it plugs in

| Piece | What changes | Difficulty |
|---|---|---|
| Setting | `hardcore` in the main config: `auto` (follow the world's vanilla hardcore flag, `LevelData.isHardcore()`), `true`, `false`. Fixed per world on first load and saved in `HauntData`, so toggling the config mid-world does not flip it. `config/Migrations.java` adds the key. | 2 |
| `haunt/Restore.java` | Split by intent. `Restore.after` stays for cleanup (despawning the double, the shadow, figures: entity lifecycle, not a world change). A new `Restore.revert` and the existing `Restore.swap` register their undo only when hardcore is off; with it on, the change is applied and forgotten, and the shutdown flush has nothing to undo. Call sites in `BlockEvents` and `Lights` move to `revert`. | 2 |
| Visual-only events | Each picks its branch at the event: `Hardcore.on() ? real : fake`. The real counterpart is listed per idea in the `Hardcore` column of [event-ideas-2.md](event-ideas-2.md). | per event |
| Docs and wiki | `spirits.md` "Restoring and locks" gets a hardcore paragraph; the wiki gets a page. | 1 |

Total for the seam itself: about 3. The cost is in the per-event real branches.

## Existing events under hardcore

| Event | Now | Hardcore |
|---|---|---|
| `wilted_flowers`, `new_sign`, `new_painting`, `cross` | swapped back | stay |
| `glass_wall` | wall turns back | glass stays |
| `wall_hole` | hole filled back | hole stays |
| `shifting_floor`, `dead_ground` | variant swapped back | variant stays (same material, so it never breaks a build) |
| lantern flicker, furnace fuel | put back | lantern stays out / fuel stays lost |
| dimming (7) | client overlay | no real light to lower; candidate: nearest lights go out for good |
| `locked_door` | timed lock | stays timed (see rules) |
| double, shadow, figures | cleanup | unchanged |
| `map_marker`, item and inventory events | already permanent | unchanged |

## Events with a real branch

| Event | Normal | Hardcore |
|---|---|---|
| `false_wound` (153) | health shown lower | real magic damage, never below one heart |
| `false_hearts` (154) | wither or poison hearts shown | the effect for real, as long |
| `false_hunger` (155) | empty hunger bar shown | 6 food points really lost, down to `shown_food` |
| `guardian_face` (156) | the elder guardian face | plus mining fatigue III for 5 min, as the guardian gives |
| `false_night` (159) | night shown to one client | the world clock really jumps to midnight, for everyone |
| `false_slot` (161) | an item shown in an empty slot | the item is really put there |
| `silent_lightning` (168) | visual-only bolt | real lightning (hurts, sets fires) |
| `cracking_floor` (138) | break particles and sound | the block under the feet really breaks, no drop, only where the fall is one block |
| `ceiling_crack` (118) | crack shown, the block stays | the block breaks at the last stage, no drop, unless fluid or sand would come down |
| `dispenser_shot` (177) | an arrow from nowhere, nothing spent | only a dispenser with arrows; it fires one of them for real |
| `portal_out` (114) | — | unchanged: for good outside the Nether, back after 60 s in the Nether |
| `false_windows` (105), `false_looks` (106), `ghost_tree` (107) | shown to one client (`Swaps.show`) | placed for real and kept (`Swaps.show` makes every swap real in hardcore); a block that holds something (chest, furnace) is still only shown |
| `hallucination` (126) | packet-only mob | the mob is spawned for real |
| `disguise` (125) | the real animal hidden, another kind shown in its place | the animal is converted into the other kind (`convertTo`) |
| `moving_tree` (108) | the tree moves, and moves back after 30 s | the tree stays where it moved |
| `wilted_flowers` (72), `glass_wall`, `wall_hole`, `cross`, `shifting_floor`, `lantern_flicker`, `pillar` (121), `redstone_drift` (178), `enderman_takes` (198), `torch_swap` (112), `repaint` (104) | shown to the haunted player only, then the real blocks again | real swaps that stay (`Swaps.show`); see [block swaps](#block-swaps) |
| `harvest` (122), `lost_growth` (110), `shuffled_books` (176) | undone later | stay (through `Restore`) |
| `walled_in` (116), `sealed_tunnel` (188) | walls put up for real, taken down later | stay; the player digs out (the player's own two blocks are never filled, so there is always air) |
| `dragon_flyover` (127), the other-player events (119, 120, 123, 124), the other lies and sounds | — | unchanged |

## Block swaps

Every event that shows other blocks in place of the real ones goes through `haunt/event/Swaps.show`, which has three
modes:

1. **Illusion** (the default, `swaps.mode` is `illusion` in `haunt.json`): only the haunted player's client is told of the
   other blocks, and after the event's ticks it is told the real ones again. The server's world is not touched, so other
   players see nothing and nothing needs undoing. The player's own movement is still checked against the real blocks.
2. **Real for a time** (`swaps.mode` is `real`): the blocks are changed in the world for everyone and put back through
   `Restore.swap` when due (and at server stop), so every player sees the event.
3. **Hardcore**: as real, but the change stays, whatever `swaps.mode` says.

In the two real modes a block that holds something (a chest, a furnace) is still only shown, since its contents would
have nowhere to go. Events that change a block entity, move things or put out lights for good (`new_sign`,
`new_painting`, `shuffled_books`, `moving_tree`, `harvest`, `occupied_bed`, `extinguish_light`, `torch_chain`,
`blackout`, `furnace_dies`) do not use it and change the world as before.

Tests: the hardcore gametests run in a batch of their own, since the switch is one for the whole server.

## Rules

- Never permanent when it can soft-lock the player: door locks, enclosures with no way out, suffocation. These keep
  their timer even in hardcore.
- A block with an inventory (chest, furnace, barrel) is never turned into another block without moving its contents
  first; when it cannot move them, the event does not run.
- Things too destructive to make real stay visual even in hardcore: a real wither or ender dragon, explosions that eat
  a chest's contents.
- Banishing still returns what a spirit took (it is a reward, not a rollback). Whether hardcore lowers that is open.

## Open questions

- Per-world fixed (like vanilla hardcore) or switchable by an operator with `/haunt hardcore`?
- Should hardcore also raise power (e.g. a floor) or only stop rollback?
- Does the client need to know (e.g. a different sanity overlay), or is it server-only?
