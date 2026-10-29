# Hardcore mode (plan)

Not built. A world setting under which every temporary change stays and every purely visual trick becomes real:
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
