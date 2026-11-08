# Spirit arcs

Plan only. Every spirit type gets its own **arc**: a run of signature events that only that type can roll, tied together
by a saved relationship with the player. Today the fourteen types differ only in affinities over shared events (see
[spirits.md](spirits.md#spirits)); the gimmicks from [design.md](design.md) are still missing. An arc is where a
gimmick lives.

The rule for every arc: the player can tell which spirit it is from the arc alone, the arc reacts to what the player
does, and it ends one way or the other.

## Shared pieces

| Piece | What | Difficulty |
|---|---|---|
| `bond` | Per-spirit value -1..1 and a `stage` 0..N, saved with the spirit next to `anger`. Arc events move it; player answers move it. Separate from anger: a spirit can love and be angry at once. | 2 |
| `spirit` filter | An event lists `"spirit": "banshee"` (and optional `"stage": [1, 3]`) in its config; the director skips it for any other type and stage. `Scenario.signature` already reads `spirit` for scenarios; this extends it to plain events. | 2 |
| `letter` | A written book (or a paper with a custom name and lore) from lang pools, signed with the `Story` nickname. Text picked by stage and bond, with `%player%` and the spirit's days dead filled in. Placed on a lectern, in a chest, under the pillow (on the bed), or dropped from the air. | 2 |
| `gift` | An item placed where the player will find it: on the bed, at the door, in the hotbar's empty slot, in an item frame. Custom name and lore, a `restless:gift` component with the spirit's uuid, so the arc knows if it was kept, dropped, burned or given back. | 2 |
| `answer` | Hooks for the player's reply: an item tossed into fire or lava, an item dropped at the spirit's grave or altar, a book written and placed on a lectern (looks for a few words: `yes`, `no`, `leave`, the spirit's name), a gift kept in the inventory for a day. Each arc maps answers to bond changes. | 3 |
| journal | Every stage change writes a page; letters can be copied into it. | 1 |

## Arcs

Stages run left to right. `+` raises the bond, `-` lowers it.

### Banshee: the suitor

She falls for the player. Courtship that stays creepy whatever the player does.

| Stage | Events |
|---|---|
| 0 Notices | humming outside the window at night (shriek sound, low pitch, short); the player's name in a whisper subtitle; flowers by the bed turn to wither roses |
| 1 Gifts | a gift on the bed every few nights: a wither rose, a bone tied with string ("For you"), a skull, a gold nugget named "Ring", a bat's wing (phantom membrane), the item the player lost at their last death, a fish that died days ago |
| 2 Letters | love letters on a lectern or under the pillow; tone moves from shy to devoted to possessive. Signed with her nickname. A letter can name what the player did today ("You looked tired by the river") |
| 3 Jealousy | if the player sleeps near villagers, trades a lot, breeds or tames animals, or stands near another player: the other party is chilled, scared off, a pet goes missing for a day; a letter "Who was she?" |
| 4 Proposal | a veil of cobweb over the bed, a ring in an item frame, the letter "Stay with me". The player answers (below) |

Answers: keeping gifts `+`, putting a flower at her grave `+`, writing back `yes` `+`; burning letters `-`, throwing a
gift away `-`, sleeping with a villager nearby `-`.

Endings:

- **Accepted** (bond ≥ 0.6 at stage 4): she guards the player. Honest warnings (the design gimmick): before a real
  danger (a creeper behind, lava under the next step, a mob spawn in the dark, low health) a short wail and the
  direction. Death Wail charge comes back twice as fast. Gifts go on, rarer. She still hates rivals.
- **Rejected** (bond ≤ -0.6): she turns into a keening banshee early (evolution now), anger +0.3, letters become
  threats, `foretold` becomes possible regardless of evolution timing.
- **Ignored** for 10 days at stage 2+: she grows quiet, then one last letter, and the arc falls back to stage 1.

### Poltergeist: the child

Wants to play. A tantrum when nobody plays.

- Rolls an item to the player's feet (the "ball"); picking it up and dropping it back counts as playing `+`.
- Hide and seek: one item from a chest vanishes, a knock comes from where it is hidden; finding it in time `+`.
- Stacks loose items into towers; builds a little "fort" of chests and crafting tables.
- Drawings: maps with scribbles left on the floor.
- Tantrum when bond < 0: throws everything in a room, smashes flower pots and glass, slams every door at once.
- End: played with enough, it hands back everything it ever took, in one chest, with a drawing "us".

### Shade: the hunger

Eats light and keeps count.

- A "receipt" on a paper: the number of torches it ate this week.
- Asks for a dark room: a letter in charcoal "leave me one room". A room kept dark (no light ≥ 4) for a day `+`; then
  it leaves the rest of the house lit.
- Light that the player places near its room is eaten at once and counted double.
- End, fed: a darkroom of its own, and mobs never spawn in the player's house (design: "shade suppresses mob spawns").
  Starved: every light in a radius goes out at dusk, one by one.

### Phantom: the understudy

Learns to be the player.

- Copies one block the player placed today, wrong by one block, somewhere nearby.
- A stranger with the player's skin seen doing what the player did an hour ago.
- Fake chat lines in the player's voice: repeats the player's own last messages to others.
- "Player joined the game" with the player's name while the player is online.
- End: when the player logs out, it "plays" in their place: on return, the base has a few changes the player did not
  make and the journal has a page of what "they" did. Bond decides whether the changes help or harm.

### Nix: the drowned bride

Calls the player to the water and trades with it.

- Fish laid on the shore in a row; a drowned item from the player comes back on the beach.
- The song from the water at dusk (a voice under the water sounds).
- Asks for a gift: a letter in a bottle "give me something you love". An enchanted item or a named one thrown into its
  water `+`; it takes an item from the player's boat otherwise.
- End: kept happy, it never lets the player drown in its water (air refills); angry, it pulls whole boats under.

### Mimic: the parrot

Learns words and things.

- Collects the player's chat words and says them back in fake chat from a fake player.
- A chest that is a mimic: opens on its own, holds the player's own items in a wrong order.
- A copy of the player's door or bed placed nearby; using it does nothing.
- Leaves a letter written entirely from the player's own sentences.
- End: it "becomes" one of the player's blocks for good; the player has to find which one (a clue tool shows it).

### Wraith: the hunt

Marks the player and counts the nights.

- Claw marks (decal) on the wall of the room the player slept in; one more each night.
- A trophy: a mob head the player failed to kill, placed at the door.
- Long stalking: footsteps that stop when the player stops, in the dark only.
- End: the hunt's last night is told a day ahead; the player survives till dawn in light (bond up, it leaves for a
  week) or is caught (lethal, telegraphed, as `foretold`).

### Mara: the dream diary

Writes the player's dreams.

- A diary page in the morning: what the player "dreamed", built from the day (where they were, what they killed).
- Sits on the chest: the player wakes with slowness and the chest is locked till noon.
- A night without sleep `-` (she is hungry), sleeping every night with a dreamcatcher `+`.
- End: the nightmare room (design gimmick), entered from a bed, left by finding the real bed.

### Revenant: the ledger

Counts kills.

- A ledger book on a lectern: every mob the player killed, by name and count, updated.
- Mobs named after animals the player killed come back for them.
- A grave with the name of a killed pet.
- Sparing a mob that flees, breeding instead of killing `+`; a killing spree `-`.
- End: the ledger closes; either the debt is forgiven (it rests) or every name in it comes back at once.

### Lost one: the way home

Asks the way and leaves a trail.

- Signs and cairns (cobblestone piles) leading somewhere: the spot where it died.
- A compass item that points to it.
- Footprints replaying a death path (design gimmick) toward the death spot.
- Following the trail to the end and putting a grave there `+`; breaking its cairns `-`.
- End: led home, it rests and leaves its essence; abandoned, it follows the player closer every day.

### Domovoi: the housekeeper

Is won with offerings (design gimmick).

- Bread or milk left on a table overnight is gone in the morning `+`.
- Fed: tidies up (sorts a chest, closes doors, puts out a fire, refills a furnace with fuel).
- Neglected: hides tools, sours milk, spoils food in chests, cats hiss at a corner.
- Moving house: it follows only if the player carries its bast shoe (a gift it leaves when bond is high).
- End: a house master that guards the house (spirits act less inside), or one that drives the player out.

### Hoarder: the trader

Takes and pays.

- Takes an item and leaves "payment": junk of the same stack size, sometimes something better.
- A nest the player can find (design gimmick) with everything it took.
- Raiding the nest `-`; leaving shiny things (gold, gems) near its anchor `+`.
- End: a fair trader (a fixed exchange at its anchor) or a thief that empties a chest a night.

### Demon: the contract

Offers deals.

- Signs and books with offers: "a diamond for your bed", "strength for a day for 3 hearts".
- Accepting (placing the asked item on its altar) pays at once; the price comes later and is worse than said.
- Every deal is a page in the journal; unpaid ones grow.
- End: a debt collector, or the player closes all deals and it loses its hold (pacts, design gimmick).

### Trickster: the gift box

Gives gifts that lie (design gimmick).

- A gift chest with fake loot (items that turn to dirt when picked up).
- A treasure map to nothing, or to a pit.
- One gift in five is real and good; the player cannot tell which.
- Riddles on signs; answering one (writing the answer on a sign) `+`.
- End: a jester that gives one great true gift, or one that swaps a real valuable for a fake.

## Order

1. Shared pieces: `bond`, `spirit` filter, `letter`, `gift`. Then `answer` for fire, grave and lectern.
2. Banshee arc in full, as the template (it uses every piece).
3. Domovoi, demon and trickster: they make the missing design gimmicks (offerings, pacts, fake gifts).
4. The rest, cheapest first: poltergeist, hoarder, revenant, lost one, nix, mara, shade, mimic, phantom, wraith.

## Open questions

- Does an arc run beside the normal events or replace some of them? Proposal: arc events are ordinary events with the
  `spirit` filter and a fair weight, so the haunt stays mixed.
- Territorial spirits have many players in range: the bond is per spirit-and-player.
- Letter text in both `en_us` and `ru_ru`; how many lines per stage (proposal: 8).
