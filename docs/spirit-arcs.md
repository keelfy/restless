# Signature events

Plan only. Every spirit type gets a few events that only that type can roll. A player who knows the mod can name the
type from one of them. No saved state, no stages: each is an ordinary event with a filter.

Today the fourteen types differ only in affinities over shared events (see [spirits.md](spirits.md#spirits)).

## The one shared piece

`spirit` filter: an event config lists `"spirit": "banshee"`; the director skips the event for every other type.
`/haunt force` still runs it for anyone. `Scenario.signature` already reads `spirit` for evolved scenarios; plain events
need the same field, checked where the director builds the runnable list. Difficulty 2.

Letters and gifts are just items: a written book or a named paper from lang pools (`en_us`, `ru_ru`), signed with the
`Story` nickname, placed on the bed, a lectern, in a chest or at the door. One helper for both, difficulty 2.

## Events

Tier: **L** light, **H** harmful. Difficulty on the 1–10 [scale](event-ideas-2.md#scale).

| Type | Event | Tier | What happens | Diff. |
|---|---|---|---|---|
| banshee | Love letter | L | A letter on the bed or a lectern, signed with her nickname: shy, devoted or possessive, picked at random. Can name where the player was today. | 2 |
| banshee | Gift | L | A gift on the bed: a wither rose, a bone tied with string ("For you"), a skull, a gold nugget named "Ring", a phantom membrane, a fish dead for days. | 2 |
| banshee | Serenade | L | At night, a low hum under the nearest window, the player's name in a subtitle. | 1 |
| banshee | Jealous | L | A villager or animal near the player is chilled and runs off; a letter "Who was that?". | 2 |
| poltergeist | Ball | L | An item rolls out of a chest to the player's feet, again and again. | 1 |
| poltergeist | Tower | L | Loose items in a room stacked into a column of item frames or dropped in a neat pile. | 2 |
| poltergeist | Tantrum | H | Every door in range slams at once, flower pots and glass panes break. | 2 |
| shade | Receipt | L | A paper on the floor in charcoal: "torches: 7". The number is the lights it snuffed. | 2 |
| shade | One by one | H | At dusk the lights in the house go out one after another, toward the player. | 2 |
| shade | Dark corner | L | One room's lights are gone and a note asks to keep it that way. | 2 |
| phantom | Joined | L | "%player% joined the game" with the player's own name while they play. | 1 |
| phantom | Rerun | L | A figure with the player's skin repeats the path the player walked an hour ago. | 3 |
| phantom | Wrong copy | L | A copy of a block the player placed today appears nearby, one block off. | 2 |
| nix | Offering | L | Fish laid in a row on the shore of its water. | 1 |
| nix | Returned | L | An item the player lost in water washes up on the beach. | 2 |
| nix | Song | L | At dusk, a voice from under the water near the player. | 1 |
| mimic | Parrot | L | A fake player says the player's own last chat line back. | 2 |
| mimic | Wrong chest | L | A chest holds the player's items in reverse order. | 1 |
| mimic | Twin door | L | A copy of the player's door or bed placed beside it; it does nothing. | 2 |
| wraith | Claw marks | L | Scratches (decal) on the wall of the room the player slept in. | 3 + art |
| wraith | Stop and go | L | In the dark, steps that stop exactly when the player stops. | 2 |
| wraith | Trophy | L | The head of a mob the player fled from, at the door. | 2 |
| mara | Dream page | L | A morning diary page: the night's "dream", built from where the player was and what they killed. | 3 |
| mara | Sits on the chest | H | The player wakes with slowness; the nearest chest is locked till noon. | 2 |
| mara | Pressed | H | On waking: no move for 3 s, heavy breathing, a weight sound. | 1 |
| revenant | Ledger | L | A book on a lectern: every mob the player killed this day, by kind and count. | 2 |
| revenant | Namesake | H | A hostile mob named after an animal the player killed. | 1 |
| revenant | Pet's grave | L | A grave with the name of a dead pet near the player's bed. | 2 |
| lost_one | Cairn | L | A small cobblestone pile and a sign pointing away, as if marking a road. | 2 |
| lost_one | Which way? | L | A sign near the player: "which way is home?". | 1 |
| lost_one | Death path | L | Footprints from the player's last death spot toward them. | 3 |
| domovoi | Ate the bread | L | Bread or milk in a chest is gone; crumbs (particles) on the table. | 1 |
| domovoi | Tidied | L | One chest sorted; open doors in the house closed. | 2 |
| domovoi | Bast shoe | L | A named item "bast shoe" by the stove (furnace). | 1 |
| hoarder | Payment | L | An item gone from a chest, a stack of junk of the same size in its place. | 2 |
| hoarder | Shiny | L | Gold and gems in the player's inventory moved to the bottom of a chest far away. | 2 |
| hoarder | Nest | L | A heap of the player's lost items in a hole near the anchor. | 3 |
| demon | Offer | L | A sign or book with a deal: "a diamond for your bed". Nothing happens if the player ignores it. | 2 |
| demon | Smell of sulfur | L | Fire particles and a blaze sound in a corner; a netherrack block that was not there. | 1 |
| demon | Signed | H | A book appears in the player's inventory with their name at the bottom. Max health -1 till dawn. | 2 |
| trickster | Gift box | L | A chest with good loot that turns to dirt when picked up. | 2 |
| trickster | Map | L | A map item with an X; there is a pit or nothing at the spot. | 3 |
| trickster | Riddle | L | A riddle on a sign near the player. | 1 |

Some of these are close to ideas in [event-ideas-3.md](event-ideas-3.md#signatures); the numbers there can be dropped
once one here covers them.

## Order

1. The `spirit` filter and the letter/gift helper.
2. The cheapest one or two per type (difficulty 1–2), so every type has a signature soon.
3. The rest.
