# Signature events

Every spirit type has three or four events that only that type can roll, so a player who knows the mod can name the type
from one of them. No saved relationship, no stages: each is an ordinary event with a type filter.

**Status: all 46 are built** (`event/Signatures.java` and the group classes it gathers, listed in
[events-index.md](events-index.md); rows in [spirits.md](spirits.md#events); a GameTest each in
`SignatureGameTests` and `SignatureBatchTests`). The tables below are the plan; [What changed](#what-changed-while-building)
lists where the build differs from it.

Today the fifteen types differ only in affinities over shared events (see [spirits.md](spirits.md#spirits)). The
evolved scenarios in [scenarios.md](scenarios.md) are per type, but they come late and are mixes of shared events.

## What makes a good signature

1. **Only this type.** The event is filtered to one type.
2. **Not a copy of a shared event.** If a shared event already does nearly the same, the player learns nothing. Every
   idea below was checked against the shipped events.
3. **Minecraft-native.** Only things the game can show: real blocks, items, mobs, sounds, particles, signs, books,
   item names and lore, vanilla mechanics. No states the game has no picture for (crumbs, a bone tied with string, a
   fish dead for days, writing on paper).
4. **Fits the type's folklore and its affinities.** A banshee is a voice and an omen; a domovoi keeps the house.
5. **Mod conventions.** A change to the world is undone after a while unless hardcore; a real item given stays; a
   packet-only lie changes nothing on the server.

## Review of the first draft

| Type | First idea | Problem | Verdict |
|---|---|---|---|
| banshee | Love letter | Fine; a written book is a real item. Differs from `lectern_swap` and `ghost_author` by place (the bed), tone and signature. | keep |
| banshee | Gift: rose, bone tied with string, "Ring", membrane, fish dead for days | "Tied" and "dead for days" cannot be shown. Bone and poppy are already in `doorstep_gift`. | rework: named items with lore, on the bed, wither rose as the tell |
| banshee | Serenade (hum) | No hum sound; `bedroom_lullaby` already plays a melody from the bed. | rework: her own custom `banshee` sound, soft, from outside a window |
| banshee | Jealous (villager runs off, letter) | Villagers cannot be told to run off; the letter makes it two events. | rework: the rival freezes (powder snow shake), villager shakes its head, cat hisses |
| poltergeist | Ball rolls out of a chest | Items do not roll; `item_throwback` throws dropped items. | rework: the chest opens and tosses items out at the player |
| poltergeist | Tower of item frames | Nonsense in Minecraft. | replace: a child hitting everything: note blocks, bells, buttons |
| poltergeist | Tantrum (doors, pots, glass) | Scenario `tantrum` exists but is evolved-only and spread over time. Breaking glass destroys builds. | keep as one instant burst, no glass |
| shade | Receipt in charcoal on paper | Paper cannot be written on. | replace: it collects the lights it took into one dark pile |
| shade | One by one at dusk | Same as `torch_chain` and `torches_behind`. | replace: lights go out exactly where the player looks |
| shade | Dark corner with a note | The note is not possible; the room part is fine. | keep the room, drop the note |
| phantom | "Joined the game" | `stranger_joins` does it with a made-up name; the player's own name is new. | keep |
| phantom | Rerun of the path an hour ago | Needs an hour of positions; `own_walker` is a skin copy walking across. | rework: it walks the player's last 30 s of path behind them |
| phantom | Wrong copy of a block | That is faking, the mimic's job. | replace: the double is at home when the player comes back |
| nix | Fish in a row on the shore | Fine: dropped raw fish. | keep |
| nix | Lost item washes up | Items in water despawn in 5 min, tracking them is heavy. | rework: an item that falls into its water is carried to the player |
| nix | Song from the water | No song sound; `bubble_columns` and the scenario `voice_from_the_water` are close. | rework: a line of bubbles crosses the water toward the player |
| mimic | Parrot | `chat_repeats` already echoes as `<???>`. | rework: echoes under the player's name, with a word swapped |
| mimic | Chest in reverse order | Nobody would notice; `shuffle_chest` exists. | replace: a chest that bites |
| mimic | Twin door or bed | `second_door` already shows a fake door. | replace: an animal with the wrong voice |
| wraith | Claw marks | `claw_marks` already exists as a shared event. | drop |
| wraith | Steps that stop when the player stops | Fine, and new. | keep, dark only |
| wraith | Trophy head of a mob the player fled from | "Fled" is hard to know; heads drop only from charged creepers. | replace: an animal taken in the dark |
| wraith | (new) | | add: it waits at the edge of the light |
| mara | Dream page | Fine: the mod has diary pages. "Built from the day" needs history. | keep, built from the biome and the last kill only |
| mara | Sits on the chest, locked till noon | There is no chest lock in the game. | replace: stolen rest, so real phantoms come |
| mara | Pressed on waking | Close to `sleep_paralysis` and `pinned`. | rework: weight on waking, with the sound of something getting off the bed |
| revenant | Ledger on a lectern | Fine; needs a kill count per day (`onKill` is hooked for births). | keep |
| revenant | Mob named after a killed animal | Animals have no names. | rework: name tag with the count, "Cow 14" |
| revenant | Pet's grave | Only with a dead pet, rare; `mock_grave` is close. | replace: a skeleton rises where the player killed |
| lost_one | Cobblestone cairn | Not a Minecraft habit; `cave_torches` already makes a false torch trail. | replace: a wanderer asking the way |
| lost_one | Sign "which way is home?" | Fine; `cave_sign` only warns, these only ask. | keep |
| lost_one | Footprints from the last death | Fine: the game stores the last death location. | keep |
| domovoi | Ate the bread, crumbs | Crumbs do not exist; `cake_bite` exists. | rework: food taken from a chest, empty bucket or bowl left |
| domovoi | Tidied | Fine and real: a sorted chest, shut doors. | keep |
| domovoi | Bast shoe by the stove | Not a Minecraft item. | replace: the hearth keeper lights and feeds the furnace |
| hoarder | Payment in junk | Fine. | keep |
| hoarder | Shiny things moved | `vanishing_item` moves any stack; only shiny ones is new. | keep, gold and gems only |
| hoarder | Nest | Needs a saved place; the scenario `hoard` is only shuffles. | keep, last |
| demon | Offer on a sign | Fine. | keep |
| demon | Sulfur, netherrack | `netherrack_behind` exists. | rework: hell under the player's feet as they walk |
| demon | Signed contract, -1 heart | Close to `ghost_author`, but the title and the cost differ. | keep |
| trickster | Gift box, loot turns to dirt | Fine. | keep |
| trickster | Map with an X | `map_marker` and the scenario `x_marks_the_spot` exist. | replace: suspicious sand with a joke inside |
| trickster | Riddle on a sign | Hard to fit a riddle on a sign, harder to translate. | replace: a TNT that is a dud |

## Events

Tier: **L** light, **H** harmful. Difficulty on the 1–10 [scale](event-ideas-2.md#scale). Ids get the type as prefix.
"Undone" follows the mod's rule: restored after the time given unless hardcore.

### Banshee: the suitor

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `banshee_letter` | L, ITEMS | Night, a bed within 16 | A written book lies on the bed: title "To <player>", author her nickname, one page from a pool of 8 (shy, devoted, possessive). | real item, stays | 2 |
| `banshee_gift` | L, ITEMS | A bed within 16 | One named item with her lore on the bed: a wither rose "For you", a gold nugget "Ring", a phantom membrane "Veil", a skeleton skull "Mine", a poppy. | real item, stays | 2 |
| `banshee_serenade` | L, SOUND | Night, glass within 12 | Her `banshee` sound, quiet and lower, from just outside the nearest window; subtitle "<nickname> sings". | — | 1 |
| `banshee_jealous` | L, MOBS | A villager, cat, wolf or parrot within 8 | The animal or villager shakes with frost as in powder snow for 4 s; a villager shakes its head, a cat hisses. | — | 2 |

### Poltergeist: the child

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `poltergeist_toss` | L, ITEMS | A non-empty chest within 10 | The lid opens and up to 3 stacks hop out toward the player, one a second, with a giggle at the end. | items stay dropped | 2 |
| `poltergeist_play` | L, DOORS | Two or more note blocks, bells or buttons within 12 | Every one of them is hit in turn, fast, like a child running along. Levers are left alone, so no redstone fires. | — | 2 |
| `poltergeist_tantrum` | H, DOORS | Three or more doors, trapdoors or gates within 12 | All of them slam in the same tick, then every flower pot in range pops: pot and plant drop as items. | items stay dropped | 2 |

### Shade: the light-eater

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `shade_gathered` | L, LIGHTS | 3+ torches within 12, a dark spot (light < 4) within 16 | The torches vanish one by one; all of them lie in one pile of dropped items in the dark spot. | items, nothing lost | 2 |
| `shade_glance` | L, LIGHTS | 3+ lights within 16 | For 20 s every light the player looks at goes out (burnt-out `unlit_` torches, lamps off), up to 5. | relit after 2 min | 3 |
| `shade_room` | H, LIGHTS | The player is in a closed room (`Enclosure`) with lights | Every light in the room goes out; while the player stays, the view darkens at the edges. | relit when the player leaves, at most 3 min | 3 |

### Phantom: the double

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `phantom_self_join` | L, PLAYER | Always | "<player> joined the game" and a second entry with the player's own name and skin in the list; 40 s later "<player> left the game". | packets only | 1 |
| `phantom_rerun` | L, PLAYER | The player moved 20+ blocks in the last 30 s | A mannequin in the player's skin walks the same path 15 s behind them; it stops when looked at and fades. | — | 3 |
| `phantom_home` | L, PLAYER | The player comes within 32 of their bed after 5+ min away | A mannequin in their skin stands at the bed or the door, then steps out of sight. | — | 2 |

### Nix: the drowned one (territorial, water)

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `nix_catch` | L, WATER | Its water within 16, a shore with 3+ free cells | 3 to 5 raw cod and salmon in a row on the shore, a splash. | real items | 1 |
| `nix_returns` | L, WATER | A dropped item in water within its radius | The item is carried through the water to the player's feet, dripping. | — | 2 |
| `nix_call` | L, WATER | The player on land within 12 of its water | A line of bubbles crosses the water toward the player, ending in a conduit sound at the shore. | — | 1 |

### Mimic: the copier

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `mimic_parrot` | L, PLAYER | The player said a line in the last 10 min | The line comes back in chat under the player's own name, with one word swapped for another from their recent lines. | packets only | 1 |
| `mimic_chest` | H, BLOCKS | A chest within 10 with a free cell beside it | A second chest appears beside it. Opening it: the lid snaps shut with evoker fangs and 2 damage, the chest is gone. | removed after 2 min | 3 |
| `mimic_voice` | L, MOBS | An animal within 12 | It makes another mob's sound 3 times: a cow with a villager's "hmm", a sheep with a zombie groan, a chicken with a cat. | — | 1 |

### Wraith: the hunter in the dark

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `wraith_steps` | L, PRESENCE | The player in the dark (light < 4) | Steps behind them for 30 s that stop exactly when the player stops, and go on when they move. | — | 2 |
| `wraith_edge` | L, PRESENCE | The player in light, dark cells within 6 | A dark figure stands on the last dark cell before the light, facing them. Remove the light and it steps in, then fades. | — | 3 |
| `wraith_taken` | H, MOBS | Dark, a passive animal within 16 | The animal dies without a sound or drops and goes in a puff of smoke. | lost for good | 1 |

### Mara: the night rider

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `mara_dream` | L, AFFLICTIONS | On waking | A diary page in the inventory: the night's dream, from a pool, with the biome the player was in and the last mob they killed. | real item, stays | 2 |
| `mara_stolen_rest` | H, AFFLICTIONS | On waking | Sleep does not count: the "time since rest" statistic is not reset, so real phantoms come as if the player never slept. | — | 1 |
| `mara_weight` | H, AFFLICTIONS | On waking | Slowness and mining fatigue for 60 s, and the sound of something heavy getting off the bed. | — | 1 |

### Revenant: the avenger of the killed

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `revenant_ledger` | L, ITEMS | A lectern within 12, 5+ passive mobs killed today | A written book on the lectern: each kind and how many, "Cows: 14". | original book back after 2 min | 2 |
| `revenant_tally` | H, MOBS | Not peaceful, night, a passive kind killed today | A hostile mob spawns nearby with a name tag of that kind and count, "Cow 14", targeting the player. | — | 1 |
| `revenant_rises` | H, MOBS | Not peaceful, the player killed a mob within 32 in the last 10 min | A skeleton digs up out of the ground at that spot, with dirt particles; for a horse, a skeleton horse. | — | 2 |

### Lost one: the wanderer

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `lost_one_sign` | L, BLOCKS | A wall within 8 | A sign with a question: "which way is home?", "is this the way?", "where is spawn?". | removed after 5 min | 1 |
| `lost_one_wanderer` | L, PLAYER | Standing room 20–30 away | A mannequin in a default skin walks in a slow circle, looking around; when the player comes within 8 a chat line from it, "<nickname> which way to spawn?", and it fades. | — | 2 |
| `lost_one_death_path` | L, PRESENCE | The player's last death within 64 | Footprints (the `muddy_footprints` look) lead from that spot toward the player. | removed after 3 min | 2 |

### Domovoi: the house keeper (territorial)

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `domovoi_supper` | L, ITEMS | A chest within 12 with bread, a milk bucket, stew or cookies | One is eaten: bread and cookies gone, the milk bucket becomes a bucket, the stew a bowl; an eating sound from the chest. | — | 1 |
| `domovoi_tidy` | L, DOORS | A chest within 12, or open doors | One chest is sorted by item and its stacks merged; open doors, trapdoors and gates in range shut. | — | 2 |
| `domovoi_hearth` | L, LIGHTS | An unlit campfire, or a furnace with input and no fuel, within 12 | The campfire lights, or one coal goes into the furnace and it starts. | — | 1 |

### Hoarder: the magpie (territorial)

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `hoarder_payment` | H, ITEMS | A chest within 10 with a stack | The stack is gone; in its slot a stack of the same size of junk: dirt, sticks, rotten flesh, string. | lost for good | 1 |
| `hoarder_shiny` | H, ITEMS | Gold, diamonds or emeralds in a chest within 10, another chest within its radius | Only those move to the other chest. | — | 2 |
| `hoarder_nest` | L, ITEMS | It has taken 5+ stacks | A barrel appears in a hidden spot near its anchor with everything it took. The spot is saved; banishing removes the barrel and drops the rest. | stays | 4 |

### Demon: the dealer (territorial)

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `demon_offer` | L, BLOCKS | A wall or ground within 8 | A sign with an offer: "a diamond for your bed", "strength for your sleep". Nothing more. | removed after 5 min | 1 |
| `demon_trail` | L, BLOCKS | The player walking on solid ground | For 15 s the block under each step shows as netherrack, then magma, then back. | packets only | 2 |
| `demon_contract` | H, AFFLICTIONS | A free inventory slot | A written book "Contract" with the player's name at the bottom appears in the inventory; max health -2 hearts till dawn. | modifier off at dawn | 2 |

### Trickster: the prankster (territorial)

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `trickster_gift` | L, ITEMS | Free floor within 8 | A chest with diamonds and gold; anything taken out turns to dirt in the inventory, with a laugh. | chest removed after 3 min | 3 |
| `trickster_dig` | L, BLOCKS | Sand or gravel within 12 | It becomes suspicious sand or gravel; brushing gives a joke: a pufferfish, a poisonous potato, a stick named "Treasure". | — | 2 |
| `trickster_dud` | L, MOBS | Standing room 3–6 away | A lit TNT (packets only) hisses at the player's feet, then pops into a firework with a laugh. | packets only | 2 |

### Boggart: the illusionist (personal)

| Id | Tier, tag | Condition | What happens | Undone | Diff. |
|---|---|---|---|---|---|
| `boggart_spiders` | L, MOBS | Not in water | 40 tiny spiders (scale 0.2, packets only) run at the player from every side, circle them, then scatter. | packets only | 3 |
| `boggart_crawl` | L, PRESENCE | Not in water | 24 tinier spiders run up close and climb in circles up the body to the face, then scatter. | packets only | 3 |
| `boggart_silverfish` | L, MOBS | Not in water | 40 silverfish pour over the floor, circle the feet, then scatter. | packets only | 2 |

The three share `event/kit/Swarm.java`: a `PacketEntity` per critter, shrunk by the `scale` attribute, moved every tick
(rush to the player, orbit, flee). The Requiem's boggart movement sends the same spiders (`Swarm.spiders`).

## Engine work

1. **Filter.** `"spirit": "<id>"` on any event in `restless-config` makes it that type's only: `HauntEvent.only()`, checked
   in `Director.open`. `/haunt force` ignores it. A scenario's `spirit` stays a boost.
2. **How often.** No separate boost. A signature event has `weight` 2.0 in its config, where the average event has 0.3.
   Not yet measured on a test world: the goal of about one event in eight is unchecked.
3. **State the events needed**, all in memory and reset with the server (`Session.SERVER`):
   - `Trail`: the last 30 s of each player's positions, one sample a second (`phantom_rerun`).
   - `Kills`: the day's passive kills by kind, and the last mob killed with where and when (`revenant_*`, `mara_dream`).
   - `Wakes`: when each player last woke; a wake also counts as using a bed for spirits that react to one (`mara_*`).
   - `Overheard`: the last five chat lines (`mimic_parrot`).
   - The last death location comes from the player (`lost_one_death_path`).
4. **Text.** Names and descriptions in the config lang files (en, ru). Sign text, letters, dreams and gifts are English
   lines in `signatures.json`.

## What changed while building

- **No `Gifts` or `Letters` helper.** A written book and a named item were each a few lines inside the event.
- **`banshee_serenade`** plays the existing `haunt.moan`, quiet and low. A sound of its own needs a recording.
- **`poltergeist_play`** strikes note blocks and bells only. Pressing buttons would fire redstone.
- **`demon_trail`** shows netherrack and then soul soil, not magma: with swaps set to real, magma would burn the player.
- **`mara_dream`** is a written book "A dream". The mod's diary page is a recipe page, not a text.
- **`mara_*`** run for 2 minutes after a waking (`window_ticks`), and a waking makes a mara roll soon.
- **`mara_stolen_rest`** sets the time since rest to 80000 ticks. The game resets it on lying down, so the value before
  sleep is not available to keep.
- **`wraith_edge`** is a figure that stands on the dark cell and is gone when looked at or approached. It does not step
  into the dark when the light is taken.
- **`phantom_home`** works from the respawn bed and the distance (12 to 32), not from time away.
- **`phantom_rerun`** walks the whole 30 s path from its start, not 15 s behind.
- **`nix_*`, `domovoi_*` and `hoarder_*`** look at the player's surroundings, not at the spirit's own territory.
- **`hoarder_payment`** no longer loses the stack: the hoarder keeps it for `hoarder_nest`. The nest is a barrel that
  stays; banishing the hoarder does not remove it, and what the hoarder took is forgotten on a restart.
- **`trickster_gift`** needs no inventory hook: the chest holds dirt with the model and name of treasure, and a check
  every half second turns it back into plain dirt once it is in the player's inventory.
- **`shade_glance`** and **`shade_room`** put out torches and lanterns only.
- **`revenant_tally`** spawns a zombie, **`revenant_rises`** a skeleton or skeleton horse; both in the threat band.

## Order

Built in two slices: the filter and ten difficulty-1 events first, then the other 33 with `Trail`, `Kills`, `Wakes` and
`Overheard`.

## Done when

- All 43 events run, each only for its type, with a GameTest. Done.
- On a test world, about one event in eight a spirit runs is its own signature. Not measured.
- Each type's description in [spirits.md](spirits.md#spirits) names its signatures. Done.
