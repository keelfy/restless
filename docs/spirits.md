# Spirits and haunt events

Reference for the current content. Source of truth is the code:
`spirit/Spirits.java`, `haunt/HauntEvents.java`, `haunt/event/*`, `haunt/Danger.java`, `haunt/Director.java`.
Update this file when any of them changes.

## How it works

- Each player gets a personal spirit on first check, chosen uniformly at random (50/50 today), and keeps it.
- Every player has a cooldown. When it expires, the Director picks one event for that player and schedules the next one.
  Spectators and creative players get no event, but the cooldown still resets.
- The pick is a weighted random choice among events that can run right now (`canRun`):

  `weight = baseWeight × tierFactor(tier, danger) × Π affinity(spirit, tag)`

  `chance = weight / Σ weights of runnable events`. An event that cannot run is excluded and the rest are renormalised.

### Danger

`danger = 0.6 × min(days / 40, 1) + 0.4 × min(advancements / 40, 1)`, range 0..1.

- `days` – overworld clock days. `advancements` – completed advancements that have a toast (recipe unlocks do not count).
- Tier factor: `LIGHT = 1 − 0.7 × danger`, `HARMFUL = danger`. Danger moves weight from light to harmful events;
  harmful events are impossible at danger 0.

### Cooldown

`minutes = (4 − 2.5 × danger) × (1 + roll) / activity`, `roll` uniform in 0..1 (minimum 1 second).

## Spirits

| Id | Description | Activity | Doors | Lights | Affliction | Assault |
|---|---|---|---|---|---|---|
| `poltergeist` | Noisy and physical. Mostly messes with doors; rarely hurts at first. | 1.3 | ×4.0 | ×1.0 | ×0.5 | ×0.5 |
| `shade` | Quiet and oppressive. Snuffs lights and afflicts, turns violent as danger grows. | 0.8 | ×0.3 | ×3.0 | ×2.0 | ×1.0 |

Affinity is a weight multiplier per event tag; a missing tag counts as ×1. Activity 1.3 means 1.3× as often.

## Events

| Id | Tier | Tag | Base weight | Runs when | Effect |
|---|---|---|---|---|---|
| `extinguish_light` | LIGHT | LIGHTS | 1.0 | A lit candle or a torch / soul torch (floor or wall) is within 10 blocks | Snuffs a lit candle; a torch is knocked off and drops as an item |
| `door` | LIGHT | DOORS | 1.0 | A wooden door is within 12 blocks | Opens a closed door or slams an open one |
| `blindness` | LIGHT | AFFLICTION | 0.6 | Player is not already blind | Blindness for 6 s and a low cave ambient sound |
| `cold_touch` | HARMFUL | ASSAULT | 1.0 | Player health > 1 heart | 1–2 hearts of magic damage (2–4 HP), never lethal (leaves at least 0.5 heart), freezing sound |

## Chances

Chance of each event per roll, assuming all four events can run. The remaining rows scale between these points.

### Poltergeist

| Danger | `extinguish_light` | `door` | `blindness` | `cold_touch` | Cooldown, min |
|---|---|---|---|---|---|
| 0.00 (fresh world) | 18.9% | 75.5% | 5.7% | 0% | 3.1–6.2 |
| 0.25 | 18.3% | 73.4% | 5.5% | 2.8% | 2.6–5.2 |
| 0.50 | 17.6% | 70.4% | 5.3% | 6.8% | 2.1–4.2 |
| 0.75 | 16.4% | 65.7% | 4.9% | 13.0% | 1.6–3.3 |
| 1.00 (~40 days and ~40 advancements) | 14.4% | 57.4% | 4.3% | 23.9% | 1.2–2.3 |

### Shade

| Danger | `extinguish_light` | `door` | `blindness` | `cold_touch` | Cooldown, min |
|---|---|---|---|---|---|
| 0.00 (fresh world) | 66.7% | 6.7% | 26.7% | 0% | 5.0–10.0 |
| 0.25 | 62.5% | 6.2% | 25.0% | 6.3% | 4.2–8.4 |
| 0.50 | 56.9% | 5.7% | 22.8% | 14.6% | 3.4–6.9 |
| 0.75 | 49.4% | 4.9% | 19.7% | 26.0% | 2.7–5.3 |
| 1.00 (~40 days and ~40 advancements) | 38.3% | 3.8% | 15.3% | 42.6% | 1.9–3.8 |

## Admin command

`/haunt info|force|spirit [player]` (permission level: gamemaster) shows danger and the next event, forces an event, or reassigns a spirit.
