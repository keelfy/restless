---
title: Tools
---

# Tools

The tools of the trade: how to tell what haunts you, how to keep it off, and how to be rid of it. None needs an essence. Most are crafted in a crafting grid, shapeless, from common items and [salt](#salt). Times are in game time; a day is 24000 ticks, twenty minutes.

A spirit you **sense** is one of your own personal spirits, or a territorial spirit whose reach covers you. Every clue tool below reads from those.

| Tool | Does |
| --- | --- |
| [Ash](#ash) | shows one trait of a spirit as a print; never lies |
| [Camera Obscura](#camera-obscura) | photographs a spirit: its type |
| [Spirit Box](#spirit-box) | answers what you say with a word; may lie |
| [Ouija Board](#ouija-board) | spells out a trait; may lie |
| [Spirit Candle](#spirit-candle) | its flame leans to the spirit that acts next |
| [Spirit Compass](#spirit-compass) | its needle points at the nearest spirit |
| [Journal](#journal) | writes down everything above |
| [Altar](#altar) | banishes a spirit |
| [Salt](#salt), [Hallowed Lantern](#hallowed-lantern), [Dreamcatcher](#dreamcatcher), [Door Charm](#door-charm), [Scarecrow](#scarecrow) | stop a spirit's events |

## Salt

Sand + dried kelp makes **2**. Place it on the floor like redstone dust: flat, walkable, breaks at a touch, falls apart when its floor goes. It is also the base of nearly everything else here.

**Salt ring.** Stand inside a closed loop of salt and no spirit's event runs on you. The loop is checked on the floor level you stand on:

- It must close in at most **400** open cells. A diagonal step closes it; walls, blocks and anything solid count as part of the loop, but the ring needs at least one salt cell. A plain walled room is not warded.
- A stopped event is lost, not postponed: the spirit's rest still runs.
- Inside the ring sanity also creeps back, +0.003 a second.
- Assaults are stopped this way too.

The same ring is needed by the [altar](#altar).

## Ash

1 charcoal makes **4**. Place it like salt. Every 30 seconds each ash block asks the spirits within 16 blocks for a clue (your own, and any territorial one whose reach covers the spot). A spirit that answers leaves a trail of prints across the ash, one every 8 ticks. A print fades after 2 minutes. Ash with a print within 1.5 blocks stays quiet.

The print is the **nature** of one of the spirit's traits. Ash never lies, but a spirit has exactly one trait foreign to its nature, so go by the print you see most:

| Print | Nature |
| --- | --- |
| a paw | iron |
| a drag mark | glowstone |
| a hoof | holy water |
| a bare foot | mirror |

Each print is noted in the [journal](#journal). Nature decides the [altar](#altar) offering.

## Camera Obscura

Iron ingot + glass pane + black dye. Use it: a flash, and a **Photograph** of one spirit you can sense, chosen at random if there are several. The photo names its **type** and nothing more ("A silhouette of a Shade"); an evolved spirit shows under its grown name. With nothing to sense, the photo shows nothing. Rest: 30 seconds, spent either way.

- The picture itself is painted on your client and shows in the photo's tooltip. It is also pasted into the spirit's page in the [journal](#journal). Without the mod on the client the note is still written, only the picture is missing.
- A photo never lies.
- The first photo of a type adds its card to the journal's bestiary.

## Spirit Box

Iron ingot + redstone + note block. Place it: it faces you. It listens for a chat line, or a phrase said over Plasmo Voice if that mod is present (it hears that you spoke, not the words), from a player within **6 blocks** of it.

- **2 seconds** later it answers in chat with one word of a trait of a spirit you sense, e.g. `<Spirit Box> dark`. With no spirit to sense it answers `...static...`.
- **Shaken:** below 0.35 sanity (0 to 1) it says more, 2 seconds after the word: where the spirit's bones lie, rounded to 10 blocks. The grave still has to be looked for.
- **It may lie.** A spirit with the Liar trait gives a word of a trait it does not have half of the time. The journal marks such notes "may lie".

## Ouija Board

Any planks + black dye + salt. Place it flat on the floor. Use it: after 1 second the planchette spells out, one letter every 0.3 seconds on the action bar, what one trait of a spirit you sense is like ("it slams doors", "it feeds on noise"). It can be used again at once. It may lie: a spirit with the Liar trait names a trait it does not have half of the time, and the note is marked "may lie". With no spirit to sense: "The planchette does not move."

## Spirit Candle

Candle + soul sand. Hold it in either hand. Its flame leans toward the spirit you sense that acts next, and burns blue for the last **3 seconds** before that event: the warning is exact, it reads the same timer the spirits run on. A spirit may snuff it for 30 seconds.

## Spirit Compass

Compass + amethyst shard + soul sand. Hold it in either hand. The needle is the lodestone needle: it points, every 4 ticks, at the nearest spirit you sense that has a place. With no spirit to point at, or one that has not yet started to roam, the needle spins.

## Journal

Book + salt + ink sac. A player gets one the first time they join, unless the server turned that off. Use it to read.

What you read is kept in the **world**, one notebook per player, not in the book: a lost journal costs nothing. It writes down every print, photo, word, board message, grave, banishment and change of type, and turns a page with a sound each time.

Sections show once they have something to say:

| Section | Holds |
| --- | --- |
| Guide | chapters that open as you run into what they are about: *Read this first* (given), *Signs* (the first event), *Telling them apart* (the first clue), *The altar* (the first photo), *Graves*, *Your head* (the first darkening of the edges), *Left alone* (the first evolution), *Essence* (the first banishing or rest) |
| Spirits | a dossier per spirit you have met, in order: met means photographed or its grave read. Notes under it, and the latest photo |
| Archive | spirits you banished and filed |
| Loose notes | what was seen of a spirit you have not met yet |
| Bestiary | a card per type you have photographed: what it does, the **form** it takes at the altar, where it is met |
| Chronicle | what happened: rests, banishings, changes of type |

**Filing.** A dossier offers the banishments it could be: click one to file it, and the spirit moves to the Archive under the name that note gave it. Types must agree where both are known. The journal cannot tell whether they are the same spirit; that is your call.

## Altar

Chiseled stone bricks + salt + candle. Placing it builds a 3 by 3 platform and needs a clear 3 by 3 on solid ground. The four corner pillars take candles. Break it and the platform goes, and what lay in the bowl comes back.

### Holy water

Lay a **water bottle** in the bowl. It needs only the finished platform, not the ring or the candles. After **12000 ticks** it turns into holy water; take it with an empty hand. Taken earlier, it is the bottle again. Holy water stacks to 16. A spirit may cloud it until dawn, and the altar will not take it then.

### The offering

Use an offering on the altar. It is of one **nature**, which decides which spirit it calls (the one whose traits mostly belong to that nature), and in one **form**, which decides whether that spirit takes it gladly.

| Nature | Whole | Bit | Crafted |
| --- | --- | --- | --- |
| Iron | iron ingot | iron nugget | iron chain |
| Glowstone | glowstone block | glowstone dust | lantern |
| Holy water | holy water | salt | bell |
| Mirror | mirror | glass pane | spyglass |

Mirror: glass pane + 2 iron nuggets. A spirit may mark it so the next time you lay it down it shows a spirit's name.

Each type takes one form, shown in its bestiary card:

| Form | Types |
| --- | --- |
| Whole | poltergeist, wraith, revenant, hoarder, demon |
| Bit | shade, phantom, mara, banshee, lost one |
| Crafted | nix, mimic, domovoi, trickster |

### The ritual

Needs, when the offering goes down:

- the finished 3 by 3 platform;
- at least **4 lit candles** within 3 blocks;
- a closed **salt ring** around it, see [Salt](#salt) (the platform does not count as a wall);
- a spirit you sense that is weak to the offering's nature, and is not wary.

Then it runs **12 waves of 5 seconds**. A form the type does not take adds 6 waves; a grown spirit adds 4. Each wave, the spirit fights back:

- 25%: it snuffs one candle. Relight it before the next wave.
- 50%: it runs one of its events on you.

Stay within 16 blocks. It **fails** if a candle is dark at a wave, or the offering is taken off, or you wander off: what lay on the altar is lost and the spirit grows 0.3 angrier. If every candle burns at the end, it is **banished**. For the essences it leaves, see [Essences](/en/items/essences#where-they-come-from).

**The wrong nature.** No spirit you sense is weak to it: the spirit eats the offering, every candle goes out, its anger peaks and it strikes at once with something harmful. It then refuses the altar for a day (24000 ticks) and ages by a day toward evolving.

## Hallowed Lantern

Lantern + salt + glowstone dust. Gives light 12, stands or hangs. While you are within **6 blocks** of it no event of any spirit runs on you. It never wears out.

## Dreamcatcher

String + feather + stick + salt. Hangs from the underside of a block. While you **sleep** within **5 blocks** of it no event runs on you. It never wears out. It swings when you brush it.

## Door Charm

Salt + paper + string. Sticks to the side of a block. Within **8 blocks** it stops the events that act on doors (those tagged *doors*). It wears: every event it stops takes one use, and the **fifth** breaks it.

## Scarecrow

Hay block + carved pumpkin + stick + salt. Within **16 blocks** it stops any event. Every event it stops takes one use, and the **tenth** breaks it. The scarecrow is also the base of the [effigy](/en/items/essences#the-effigy).

### How wards stack

Checked in this order, for each event about to run on you: salt ring, hallowed lantern, dreamcatcher (asleep), door charm (door events), scarecrow. The first that applies stops the event. Only the door charm and the scarecrow pay a use, and only one of them does: a door event takes the charm's. A stopped event rings out and drifts ash around you. The ward stops the event; the spirit's rest still runs, so it is lost, not postponed.

## Gravestone

Not crafted. Every spirit you can sense gets a grave in the overworld, 100 to 400 blocks from you, built when its chunk first loads. Use the stone to read it: the name, days dead, the cause, and what the spirit **wants back**. Use it holding that item and the spirit is laid to rest: **2 essences** and 30 experience, no anger, and the spirit is gone. The item is spent. If someone else lays your personal spirit to rest, your journal notes it, even while you are away.

What a spirit wants is fixed for its life, and is one of: golden apple, diamond, compass, clock, name tag, jukebox, poppy, cake, iron pickaxe, lantern, emerald, spyglass. Breaking a gravestone angers your spirits (0.2).

## Torn Page

Found, not crafted. A chest in an abandoned mineshaft, dungeon, desert pyramid, jungle temple, woodland mansion, pillager outpost, ruined portal or village house (plains, taiga, snowy, savanna, desert) holds a page 3 times in 10. It teaches one recipe, picked at random from the spirit box, camera obscura, ouija board and ash. Use it: the recipe goes into your recipe book and the page is spent. A page for a recipe you know is kept.

## Unlit torches and lanterns

Spirits put lights out. A torch or lantern put out is its **unlit** twin: same block, no light. Relight it by using flint and steel (1 durability) or a lit torch on it, or craft the unlit item with either in a crafting grid. A torch is not used up and flint and steel loses 1. A lantern with water in it will not relight.

## Spirits tamper

Spirits meddle with these too: [`spirit_candle_out`](/en/events/lights#spirit_candle_out), [`holy_water_cloudy`](/en/events/items#holy_water_cloudy), [`mirror_names`](/en/events/items#mirror_names), [`ouija_hi`](/en/events/blocks#ouija_hi), [`spirit_box_word`](/en/events/blocks#spirit_box_word), [`salt_gap`](/en/events/blocks#salt_gap), [`dreamcatcher_fails`](/en/events/blocks#dreamcatcher_fails), [`lantern_soul_fire`](/en/events/blocks#lantern_soul_fire), [`charm_falls`](/en/events/blocks#charm_falls), [`scarecrow_closer`](/en/events/blocks#scarecrow_closer), [`altar_midnight`](/en/events/blocks#altar_midnight), [`journal_page_torn`](/en/events/blocks#journal_page_torn).
