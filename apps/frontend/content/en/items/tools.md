---
title: Tools
---

# Tools

These are the tools of my trade: the ones that tell what haunts a person, the ones that keep it off, and the one that rids a place of it. None needs an essence. They are crafted from common items, [salt](#salt) and, for the mid-tier ones, [hallowed wax](#hallowed-wax): the simple ones shapeless, the rest in a shape. A shape is shown as a grid, top row first, `.` an empty cell. Times are in game time; a day is 24000 ticks, twenty minutes.

A spirit I **sense** is one of my own personal spirits, or a territorial spirit whose reach covers me. Every clue tool below reads from those.

| Tool | Does |
| --- | --- |
| [Hallowed Wax](#hallowed-wax) | the seal in every charm and in the mid-tier tools |
| [Ash](#ash) | shows the build of a spirit's type as a print, and sometimes when it acts; never lies |
| [Camera Obscura](#camera-obscura) | photographs a spirit: its type |
| [Spirit Box](#spirit-box) | answers aloud around a word of what the spirit wants or fears, or of how it died; may lie |
| [Ouija Board](#ouija-board) | spells out a habit; may lie |
| [Spirit Candle](#spirit-candle) | its flame leans to the spirit that acts next |
| [Journal](#journal) | writes down everything above |
| [Altar](#altar) | banishes a spirit |
| [Salt](#salt), [Hallowed Lantern](#hallowed-lantern), [Dreamcatcher](#dreamcatcher), [Door Charm](#door-charm), [Scarecrow](#scarecrow) | stop a spirit's events |

## Salt

Recipe: sand and dried kelp make **2**. Placed on the floor, salt behaves like redstone dust: flat, walkable, broken at a touch, and it falls apart when its floor goes. It is also the base of nearly everything else here.

**Salt ring.** While a player stands inside a closed loop of salt, no spirit's event runs on them. The loop is checked on the floor level the player stands on:

- It must close in at most **400** open cells. A diagonal step closes it; walls, blocks and anything solid count as part of the loop, but the ring needs at least one salt cell. A plain walled room is not warded.
- A stopped event is lost, not postponed: the spirit's rest still runs.
- Inside the ring sanity also creeps back, +0.003 a second.
- Assaults are stopped this way too.
- **Salt wears out.** Every event the ring stops wears the salt tiles that close it; after **12** stopped events they turn grey and the ring stops nothing. Time alone never wears it. A fresh tile laid next to the grey ones (for example, one broken and laid again) makes every tile joined to it fresh again. A grey ring also gives no sanity back and does not count for the altar.

The same ring is needed by the [altar](#altar).

## Hallowed Wax

Recipe: honeycomb, salt and ash make **2**, shapeless. Honeycomb comes from bees, so wax is the first step from vanilla into the mod's craft. It is the **seal** of every base [charm](/en/items/charms) (1 each) and goes into the altar, hallowed lantern, door charm, spirit candle, effigy and reliquary. Advanced charms, consumables and relics take none directly.

## Ash

Recipe: 1 charcoal cooked on a campfire gives **4**. Ash is placed like salt. Every 30 seconds each ash block asks the spirits within 16 blocks for a clue (the player's own, and any territorial one whose reach covers the spot). A spirit that answers leaves a trail of prints across the ash, one every 8 ticks, and the print goes into the journal: a personal spirit's only into the journal of the player it haunts, a territorial one's into that of everyone within 16 blocks. A print fades after 2 minutes. Ash with a print within 1.5 blocks stays quiet.

The print is the build of the spirit's **type**, the same build its photograph shows, and says nothing about its nature: two spirits of one type leave the same print, whatever their traits. Ash never lies about the kind of spirit that walked, nor about a condition it shows:

| Print | Types |
| --- | --- |
| a bare foot | Mimic, Lost One, Domovoi, Nix, Trickster |
| a paw | Mara, Hoarder, Boggart, Poltergeist |
| a hoof | Demon, Revenant |
| a drag mark | Shade, Phantom, Wraith, Banshee |

A spirit with the Rain Walker trait leaves damp prints, drawn darker. Each print is noted in the [journal](#journal).

Ash reads **conditions**. When the spirit has any, the ash may also show one of them beside the print, and the journal writes it after the print: "a bare foot; the ash beside it showed that it walks in the rain". The conditions and what the ash says of them are listed in [Reading the Clues](#reading-the-clues).

## Camera Obscura

Recipe: black wool (B), 5 copper ingots (C) and an amethyst shard (A, the lens), shaped:

```
. B .
C A C
C C C
```

When used, it gives a flash and a **Photograph** of one spirit the player can sense, chosen at random if there are several. The photo names its **type** and nothing more ("A silhouette of a Shade"); an evolved spirit shows under its grown name. With nothing to sense, the photo shows nothing. Its rest is 30 seconds, spent either way. While held, it shows spirits' bodies the way the [spirit candle](#spirit-candle) does.

- The picture itself is painted on the player's client and shows in the photo's tooltip. It is also pasted into the spirit's page in the [journal](#journal).
- A developed photo can go in an item frame, where it shows the picture itself, flat on the wall like a map, with the frame itself hidden. Without the mod on the client the frame shows the plain card.
- A photo never lies.
- The first photo of a type adds its card to the journal's bestiary.

## Spirit Box

Recipe: lightning rod (R, the antenna), 4 copper ingots (C), note block (N) and amethyst shard (A), shaped:

```
. R .
C N C
C A C
```

When placed, it faces the player. It listens for a chat line, or a phrase said over Plasmo Voice if that mod is present, from a player within **6 blocks** of it.

- **2 seconds** later it answers in chat, in the speaker's language, with one word of what a sensed spirit wants, fears or hates (a leaning), e.g. `<Spirit Box> bread`. About 4 times in 10 it answers a word of how the spirit died instead (see the table below), and a spirit with no leaning, such as one born before leanings existed, answers only with death words. With no spirit to sense it answers `...static...`.
- **It speaks.** Only the speaker hears the answer, from the box, one word after another in a whisper through a broken radio (English, or Russian if the game is in Russian), with static between. The chat line is its subtitle.
- **It hears what is asked.** Up to 3 broken words come back around the clue word, e.g. `<Spirit Box> near... fire... fire` to "where are you?". The questions it knows, in English and Russian:

  | Question | E.g. | Answers |
  |---|---|---|
  | How many | "how many of you?" | many, all of us, more, legion |
  | Where | "where are your bones?" | here, near, inside, everywhere, in the walls |
  | Who | "who are you?", "your name?" | me, nobody, forgotten, us, nameless |
  | What it wants | "what do you want?", "why?" | rest, home, warmth, return, company |
  | Its death | "how did you die?" | fell, alone, pain, too soon, no air |
  | Go away | "leave us", "go away" | never, stay, not yet, ours |
  | Anyone there | "hello?", "anyone here?" | always, watching, with you, here |

  An answer comes 6 times in 10. Sometimes it repeats the clue word, hisses `ksshh`, or echoes back a word of the speaker's, cut or stuttered. A word said over voice chat comes back in the speaker's own voice, slowed and distorted. **Only the clue word is a clue**, and only it goes into the journal; the answers never use a clue word.
- **Voice:** over Plasmo Voice the server makes out the words with offline speech recognition (Vosk, English or Russian by the game language). The first phrase near a box makes the server download it once: about 25 MB plus 45 MB per language, into `restless/voice` in the game folder. Until then, and on machines Vosk does not run on (it runs on Linux x64, Windows x64 and macOS), the box hears that someone spoke but not the words, and answers with the clue word alone.
- **Shaken:** below 0.35 sanity (0 to 1) it says more, 2 seconds after the word: where the spirit's bones lie, rounded to 10 blocks. The grave still has to be looked for.
- **It may lie.** A spirit with the Liar trait gives a word of a wish or fear it does not have half of the time. A word of how the spirit died is always true, even from a liar. The journal marks every spirit box note "may lie", because it cannot be known ahead of time whether the spirit is a liar. For what a word means, see [Reading the Clues](#reading-the-clues).

**How it died.** Every spirit has a cause of death in its story. When the box speaks of it, it says one of two words for that cause:

| Cause | Words |
| --- | --- |
| Fell into lava | `burn`, `down` |
| Starved in a deep mine | `hungry`, `deep` |
| Was blown up by a creeper | `loud`, `hurt` |
| Drowned in a flooded cave | `drown`, `water` |
| Was shot by a skeleton | `hurt`, `run` |
| Fell from a high place | `down`, `die` |
| Burned down with the house | `fire`, `burn` |
| Was lost in the Nether | `lost`, `fire` |
| Suffocated in a collapsed tunnel | `breathe`, `stone` |
| Was slain by a zombie horde | `teeth`, `run` |
| Never found the way back to bed | `bed`, `lost` |
| Was pushed off by a friend | `friend`, `trust` |

## Ouija Board

Recipe: any planks (P), 2 black dye (D) and a bone (B, the planchette), shaped:

```
D B D
P P P
```

The board is placed flat on the floor. When used, after 1 second the planchette spells a phrase, one letter every 0.3 seconds on the action bar. Each habit has five phrases: a single word ("SHUT IT"), a half-sentence ("YOUR SLEEP IS MINE") or a hint ("DO NOT TURN AROUND"), and none of them says the trait outright. The spirit is not always listening: 30% of tries the board stays still, and after every try it trembles for 30 seconds and does not answer, so spamming gets nothing. The board spells only habits, never a wish, a fear or a condition. One phrase rarely settles a habit, so the board is used several times. It may lie: a spirit with the Liar trait names a habit it does not have half of the time, and the note is marked "may lie", as is every board note. For what a line means, see [Reading the Clues](#reading-the-clues). With no spirit to sense, or when the spirit stays silent: "The planchette does not move." During the pause: "The planchette is still trembling."

## Reading the Clues

Every clue belongs to one trait, and each tool reads only one kind. Traits are of three kinds. A **habit** is what the spirit does, and it has a nature; the ouija board spells habits. A **condition** is when the spirit acts or what it reacts to, and it says nothing about the nature; the ash shows conditions. A **leaning** is what the spirit wants, fears or hates, and it says nothing about the nature either; the spirit box speaks leanings (and how the spirit died). The camera shows only the type. A board phrase is a riddle; the first table gives what each can mean. Objects: it touches things and slams doors. Darkness: it feeds on light and chills the air. Malice: it takes revenge. Deceit: it whispers and lies.

**Habits**, spelled by the ouija board:

| Nature | Meaning | Ouija phrases |
| --- | --- | --- |
| Objects | It reaches for things left lying around | `do not drop it`, `hands`, `i will take it`, `lying unwatched`, `what is left is mine` |
| Objects | It slams doors | `shut it`, `slam`, `get out`, `creak and crash`, `the door is closed` |
| Objects | It meddles with the world around the player | `i will move it`, `rearrange`, `not there`, `nothing is in its place`, `i will touch` |
| Darkness | It feeds on light | `put it out`, `hungry... light`, `darker`, `light tastes good`, `snuff the flame` |
| Darkness | It chills the air around the player | `cold`, `your breath shows`, `frost`, `you will freeze`, `the air grows cold` |
| Malice | It wants revenge | `you will pay`, `blood for blood`, `you`, `revenge`, `debt` |
| Deceit | It whispers | `listen`, `come closer`, `hush`, `in your ear`, `a secret` |
| Deceit | It lies | `yes`, `no`, `maybe`, `it is true`, `do not believe` |

**Conditions**, shown by the ash beside a print, in these words (they point to no nature):

- It walks in the rain.
- It stalks at night.
- It lives in the dark.
- It feeds on noise.
- It guards what is in chests.
- It steals your sleep.
- It calls from below.
- It holds a grudge.
- It haunts the deep places.
- It lurks in the water.

**Leanings**, spoken by the spirit box:

| Kind | Meaning | Spirit box words |
| --- | --- | --- |
| Fear | It fears fire. | `fire`, `burn`, `no` |
| Fear | It fears cats. | `cat`, `eyes`, `no` |
| Fear | It fears bells. | `bell`, `hush`, `no` |
| Fear | It fears running water. | `river`, `cross`, `no` |
| Desire | It wants milk. | `milk`, `thirsty`, `give` |
| Desire | It wants bread. | `bread`, `hungry`, `give` |
| Desire | It wants sweets. | `sweet`, `give`, `honey` |
| Hate | It hates lanterns. | `lantern`, `hate`, `out` |
| Hate | It hates bells. | `bell`, `stop`, `hate` |
| Hate | It hates music. | `music`, `stop`, `hate` |

How the clues add up:

- A spirit born now has 3 or 4 habits, at most one of them foreign to its nature, 1 or 2 conditions, one desire and one fear or hate. Its nature is the one most of its habits point to; conditions and leanings never change it.
- One note decides nothing, so several are gathered: true traits repeat, false ones are scattered over the rest. A lie always stays within the tool's own kind.
- Prints show the type of spirit, not its nature. The nature is read from the board's habits before an offering is put on the [altar](#altar): the wrong nature is costly.
- If the nature will not add up, there is another way: the [gravestone](#gravestone) names the spirit's unfinished business, and no guessing is needed.

## Spirit Candle

Recipe: candle, hallowed wax and soul sand, shapeless. It is held in either hand. Its flame leans toward the sensed spirit that acts next, and burns blue for the last **3 seconds** before that event: the warning is exact, it reads the same timer the spirits run on. A spirit may snuff it for 30 seconds. Its flame also shows the [date](/en/how-it-works#the-calendar): pale on a full moon, red on a blood night, green on the day of the dead.

While it burns in the hand, it also shows the **body** of every spirit sensed within 32 blocks, standing at the spirit's point: pale and see-through, shaped by its type as on a photograph, so two spirits of one type look alike but never the same. Only the holder sees it, and only while the lit candle or the camera obscura is held.

## Journal

Recipe: book, salt and ink sac. A player gets one the first time they join, unless the server turned that off. When used, it opens for reading.

What is read is kept in the **world**, one notebook per player, not in the book: a lost journal costs nothing. It writes down every print, photo, word, board message, grave, banishment and change of type, and turns a page with a sound each time.

Sections show once they have something to say:

| Section | Holds |
| --- | --- |
| Guide | chapters that open as the player runs into what they are about: *Before anything else* (given), *Signs* (the first event), *Telling them apart* (the first clue), *The altar* (the first photo; it also shows the altar's recipe), *Graves*, *The mind* (the first darkening of the edges), *Left alone* (the first evolution), *Essence* (the first banishing or rest), *The calendar* (the first dusk of a full moon, blood night or day of the dead) |
| Spirits | a dossier per spirit met, in order: met means any clue came from it. Until photographed it is an *Unknown spirit*; the photo names its type, its grave its name. Notes under it, and the latest photo. When a spirit evolves, its dossier says so: it names the grown type if the spirit has been photographed, otherwise only that it most likely evolved |
| Archive | spirits banished and filed |
| Loose notes | what was seen of nobody in particular, such as an omen dream |
| Bestiary | a card per type photographed: what it does, where it is met |
| Charms | a page per [charm](/en/items/charms) and [advanced charm](/en/items/advanced) that can be read about: what it does and its recipe. It opens once the journal knows the spirit of every essence in the recipe (photographed or banished); an advanced charm also needs its base charm's page |
| Relics | a page per [relic](/en/items/relics), opened the same way: when all three spirits it is made of are known |
| Recipes | the recipes in the recipe book that make an item of the mod: the ones diary pages taught |
| Chronicle | what happened: rests and banishings |

**Case sheet.** Each dossier carries the player's own guesses: its nature (iron, glowstone, holy water or a mirror), its form (the thing whole, a bit or something crafted) and each habit thought to have been seen. Clicking a line moves it on to its next value; after the last it is cleared. The journal keeps what was marked and never says whether the guess is right.

**Filing.** A dossier offers the banishments it could be: clicking one files it, and the spirit moves to the Archive under the name that note gave it. Types must agree where both are known. The journal cannot tell whether they are the same spirit; that is the player's call.

## Altar

Recipe: hallowed wax (W), gold ingot (G, the bowl), 2 salt (S), amethyst block (A) and 3 chiseled stone bricks (B), shaped:

```
W G W
S A S
B B B
```

Placing it builds a 3 by 3 platform and needs a clear 3 by 3 on solid ground. The four corner pillars take candles. When it is broken the platform goes, and what lay in the bowl comes back.

### Holy water

A **water bottle** laid in the bowl needs only the finished platform, not the ring or the candles. After **12000 ticks** it turns into holy water, which is taken with an empty hand. Taken earlier, it is the bottle again. Holy water stacks to 16. A spirit may cloud it until dawn, and the altar will not take it then. Used while looking down (pitch 70° or more), one bottle is poured at the user's feet and ends every pending event of a Malice spirit on the player, with the same falling ash and chime as a ward. See [Weakness](/en/how-it-works#weakness).

### Blessing a house

A water bottle laid in the bowl of an altar that stands inside a closed ring of salt, with **8** lit candles around it, begins a **blessing** instead of making holy water. Fewer candles or no ring, and it is holy water as before. The blessing runs **12 waves** of 100 ticks. If the player has spirits, each wave one of them may put out one of the 8 candles (25%), and it must be lit again. It fails, and the water is lost, if at the end of a wave the altar is broken, the ring is open or the player is more than 16 blocks away, or if a candle is out at the end of the last wave. Nothing else is lost.

When it succeeds, no haunt event runs within **16 blocks** of the altar for **7 days** (168000 ticks). This does not wear salt, wards or charms. When the week ends, every spirit of the player who blessed the house returns with its anger raised by **0.2**.

### The offering

An offering is used on the altar. It is of one **nature**, which decides which spirit it calls (the one whose traits mostly belong to that nature), and in one **form**, which decides whether that spirit takes it gladly.

| Nature | Weak to | Whole | Bit | Crafted |
| --- | --- | --- | --- | --- |
| Objects | iron | iron ingot | iron nugget | iron chain |
| Darkness | light | glowstone block | glowstone dust | lantern |
| Malice | holy water | holy water | salt | bell |
| Deceit | the mirror, which shows the truth | mirror | glass pane | spyglass |

Mirror recipe: a glass pane between 2 iron nuggets, in one row. A spirit may mark it so that the next time it is laid down it shows a spirit's name. Held in the hand, it shows the fake blocks and the doubles of a Deceit spirit for what they are, see [Weakness](/en/how-it-works#weakness).

For a spirit born now, the form comes from the cause of its death, which its [gravestone](#gravestone) tells: four causes for each form. A wrong form still works, but the fury runs 6 waves longer.

| Form | Causes |
| --- | --- |
| Whole | starved in a deep mine, drowned in a flooded cave, fell from a high place, never found the way back to bed |
| Bit | fell into lava, was blown up by a creeper, burned down with the house, was slain by a zombie horde |
| Crafted | was shot by a skeleton, was lost in the Nether, suffocated in a collapsed tunnel, was pushed off by a friend |

A spirit born before this keeps the form of its type, as it always did (the bestiary card no longer names it):

| Form | Types |
| --- | --- |
| Whole | poltergeist, wraith, revenant, hoarder, demon |
| Bit | shade, phantom, mara, banshee, lost one |
| Crafted | nix, mimic, domovoi, trickster |

### The ritual

Required when the offering goes down:

- the finished 3 by 3 platform;
- at least **4 lit candles** within 3 blocks;
- a closed **salt ring** around it, see [Salt](#salt) (the platform does not count as a wall);
- a sensed spirit that is weak to the offering's nature, and is not wary.

Then it runs in waves of 5 seconds, through four phases:

1. **The call**, 2 waves. The spirit comes in and does nothing yet.
2. **The bargain**, 2 waves. The spirit names what it wants: one of the things it [desires](/en/how-it-works#leanings), any of which it takes, or for a spirit with no desire the thing its [grave](#gravestone) asks for. If it is thrown onto the altar before the bargain is over, the ritual ends: the spirit takes one of it and the offering, grows 0.3 calmer and **stays**. No essence. An evolved spirit with a [second nature](/en/how-it-works#evolving) also asks for an offering of that nature, any form: it must be thrown onto the altar during the bargain, or the ritual fails when the fury would begin.
3. **The fury**, 6 waves. A form the spirit does not take adds 6 waves; a grown spirit adds 4. Each wave, the spirit fights back:
   - 25%: it snuffs one candle. It must be relit before the next wave.
   - 50%: it runs one of its events on the player.
   - 35%: it acts by its nature, whatever the censer does. Objects: it flings the offering off the altar; the same offering must be used on the altar again before the next wave, or the ritual fails. Darkness: it puts out every torch and lantern within 12 blocks for 30 seconds (the ritual's candles stay lit). Malice: it strikes the player for a heart and a half of damage, never leaving them below the safe health. Deceit: it shows two of the candles as dark, but only to the player; they still burn, so they need not be relit.
   - Once per ritual it wipes one tile of the salt ring. Salt must be laid on that tile again before the next wave, or the ritual fails.
   - It also shows itself: its pale body walks the salt ring, and its nature sets a rule of gaze. Darkness and malice: the player must not look at it for more than 2 seconds. Objects and deceit: the player must not look away from it for more than 2 seconds. When the rule is broken it snuffs a candle (the ritual goes on; the candle must be relit before the next wave).
4. **The leaving**, 2 waves. The spirit lets go, but still walks the ring under the same rule of gaze; the candles must stay lit to the end.

**Place and hour.** A ritual held within 8 blocks of the spirit's [gravestone](#gravestone), or begun around midnight (day time 17000 to 19000), is hallowed: the fury runs 2 waves longer and the banished spirit leaves one more essence.

**Two at the altar.** Another player standing within 3 blocks of the altar helps. If they hold a bell or a flint and steel during the fury, the fury is 2 waves shorter (once a ritual). But the spirit's strikes in the fury, its events and the blow of malice, land on everyone in the circle.

**The name.** In the fury and the leaving the spirit listens to the chat. If its name is said, as its [gravestone](#gravestone) gives it (case and punctuation do not matter), in the fury it takes 4 waves off the fury, in the leaving the spirit is banished at once. It works once a ritual. Anything else said then is a wrong name: its anger rises by 0.1.

The player must stay within 16 blocks. The ritual **fails** if a candle is dark at a wave, or the offering is taken off, or the player wanders off: what lay on the altar is lost and the spirit grows 0.3 angrier. If every candle burns at the end, it is **banished**. For the essences it leaves, see [Essences](/en/items/essences#where-they-come-from).

**The wrong nature.** If no sensed spirit is weak to the offering, the spirit eats it, every candle goes out, its anger peaks and it strikes at once with something harmful. It then refuses the altar for a day (24000 ticks) and ages by a day toward evolving.

**The echo.** A spirit banished at the altar does not go quite all the way: it leaves an **echo**, a young, calm spirit of the same type and story that keeps haunting where it was (a personal spirit's echo stays with its player; a territorial one lingers around the altar). An echo has no grave and no essence: it refuses every altar and leaves by itself after **7 days**. A spirit laid to rest at its [grave](/en/spirits#rewards) leaves no echo, which makes the grave the cleaner way out.

### Appeasement

A whole altar takes what a spirit near the player [desires](/en/how-it-works#leanings) without candles or a salt ring. When the item is used on the bowl, the spirit takes it, its anger drops by **0.3** and it **stays**. Each further appeasement of the same spirit costs one more of the item: 1, then 2, then 3. If the player holds fewer than it asks, the spirit says how many it wants and takes nothing. An item no nearby spirit desires does nothing, and a spirit born before desires existed cannot be appeased. An echo refuses the altar.

## Hallowed Lantern

### Interrogation

A plain **Book** used on a whole altar, inside a closed [salt ring](#salt) with at least 4 lit candles within 3 blocks, calls the first spirit the player senses (not an echo, not a wary one). It costs 0.2 [sanity](/en/sanity) and the book is not used up. The spirit stays for 10 seconds (2 waves of 5 seconds). In that time the first chat line that asks a question gets one **true** answer, even from a liar, after a short spirit box delay, and the journal writes it down. The question is read by its words:

- **desire** ("want", "wish"): what the spirit [desires](/en/how-it-works#leanings);
- **grave** ("grave", "buried", "bones"): which quarter (north, east, south or west) of the altar its [grave](#gravestone) lies in;
- **habit** ("do", "habit"): one of its habits. The [hidden habit](/en/how-it-works#hidden-habit) is told only to a shaken player or by an evolved spirit.

They are checked in that order. When there is nothing to tell, the spirit says so. If no question comes in time, the spirit leaves.

Recipe: lantern (L), 2 hallowed wax (W), 2 glowstone dust (G) and 2 salt (S), shaped:

```
W G W
S L S
```

It gives light 12, and stands or hangs. While a player is within **6 blocks** of it, no event of any spirit runs on them. It never wears out.

## Dreamcatcher

Recipe: 4 string (T), 2 feather (F), stick (K) and salt (S), shaped:

```
. K .
T S T
F T F
```

It hangs from the underside of a block. While a player **sleeps** within **5 blocks** of it, no event runs on them. It never wears out. It swings when brushed.

## Door Charm

Recipe: paper, hallowed wax and salt make **2**, shapeless. It sticks to the side of a block. Within **8 blocks** it stops the events that act on doors (those tagged *doors*). It wears: every event it stops takes one use, and the **fifth** breaks it.

## Scarecrow

Recipe: carved pumpkin (P), hay block (H), 2 salt (S) and stick (K), shaped:

```
. P .
S H S
. K .
```

It stands **two blocks tall**, so it needs two free blocks above the floor. Within **16 blocks** it stops any event. Every event it stops takes one use, and the **tenth** breaks it. The scarecrow is also the base of the [effigy](/en/items/essences#the-effigy).

### How wards stack

Checked in this order, for each event about to run on a player: salt ring, hallowed lantern, dreamcatcher (asleep), door charm (door events), scarecrow. The first that applies stops the event. Only the door charm and the scarecrow pay a use, and only one of them does: a door event takes the charm's. A stopped event rings out and drifts ash around the player. The ward stops the event; the spirit's rest still runs, so it is lost, not postponed.

A spirit's fear works like a ward: its events do not run on a player standing near the feared thing, and the stopped event rings out and drifts ash in the same way. See [Leanings](/en/how-it-works#leanings).

## Gravestone

Not crafted. Every spirit that can be sensed gets a grave in the overworld, 100 to 400 blocks from the player, built when its chunk first loads. When the stone is used, it is read: the name, days dead, the cause, and the spirit's **unfinished business**. Once the business is settled, using the stone again lays the spirit to rest: **2 essences** and 30 experience, no anger, and the spirit is gone. If someone else lays a player's personal spirit to rest, the journal notes it, even while the player is away. Breaking a gravestone angers the player's spirits (0.2).

The business is fixed for the spirit's life. How it died decides it 6 times in 10 when the cause has a business of its own; otherwise it is drawn by weight (lost thing 4, bones 2, letter 2, killer 1, rival 1):

| Business | What the stone asks | How it is settled | Causes that lean to it |
|---|---|---|---|
| **Lost thing** | an item it wants back: golden apple, diamond, compass, clock, name tag, jukebox, poppy, cake, iron pickaxe, lantern, emerald or spyglass | the stone is used while holding that item, which is spent | — |
| **Bones** | its bones were never buried; the stone gives where they lie, rounded to 10 blocks | the bones (a named bone) lie on the ground 50 to 150 blocks from the grave once their chunk loads; they are brought and the stone is used while holding them. While anyone carries them, that spirit acts **twice as often** | starved in a deep mine, drowned in a flooded cave, suffocated in a collapsed tunnel |
| **Letter** | a last letter for a descendant, given to the reader (a lost one is given again on the next reading) | the letter is carried within 4 blocks of any villager, then the stone is used | lost in the Nether, never found the way back to bed |
| **Killer** | the mob that killed it stands within 8 blocks of the grave, named | the player kills it, then uses the stone. If it dies any other way (sun, lava) or is gone, the next reading calls it up again | shot by a skeleton (a skeleton), slain by a zombie horde (a zombie), blown up by a creeper (a creeper); otherwise a zombie |
| **Rival** | another of the owner's spirits killed it; the stone names it | that spirit must be gone (banished, laid to rest); with no other spirit, the killer is already gone | pushed off by a friend |

When the business is a lost thing, what the spirit wants back is fixed for its life too. For a spirit born now it is one thing from the family its cause names, picked by its seed; the cause also gives the form of its offering:

| Cause | Form | Wants |
| --- | --- | --- |
| fell into lava | Bit | water bucket or obsidian |
| starved in a deep mine | Whole | bread or cooked beef |
| was blown up by a creeper | Bit | shield or gunpowder |
| drowned in a flooded cave | Whole | oak boat or turtle helmet |
| was shot by a skeleton | Crafted | shield or bow |
| fell from a high place | Whole | feather or water bucket |
| burned down with the house | Bit | oak door or water bucket |
| was lost in the Nether | Crafted | compass or gold ingot |
| suffocated in a collapsed tunnel | Crafted | iron pickaxe or torch |
| was slain by a zombie horde | Bit | iron sword or golden apple |
| never found the way back to bed | Whole | red bed or compass |
| was pushed off by a friend | Crafted | name tag or poppy |

A spirit born before this wants one of: golden apple, diamond, compass, clock, name tag, jukebox, poppy, cake, iron pickaxe, lantern, emerald, spyglass.

## Torn Page

Found, not crafted. A chest in an abandoned mineshaft, dungeon, desert pyramid, jungle temple, woodland mansion, pillager outpost, ruined portal or village house (plains, taiga, snowy, savanna, desert) holds a page 3 times in 10. It teaches one recipe, picked at random from the spirit box, camera obscura, ouija board and ash. When used, the recipe goes into the player's recipe book and the page is spent. A page for a recipe already known is kept.

## Unlit torches and lanterns

Spirits put lights out. A torch or lantern put out is its **unlit** twin: same block, no light. It is relit by using flint and steel (1 durability) or a lit torch on it, or by crafting the unlit item with either in a crafting grid. A torch is not used up and flint and steel loses 1. A lantern with water in it will not relight.

## Spirits tamper

Spirits meddle with these too: [`spirit_candle_out`](/en/events/lights#spirit_candle_out), [`holy_water_cloudy`](/en/events/items#holy_water_cloudy), [`mirror_names`](/en/events/items#mirror_names), [`ouija_hi`](/en/events/blocks#ouija_hi), [`spirit_box_word`](/en/events/blocks#spirit_box_word), [`salt_gap`](/en/events/blocks#salt_gap), [`dreamcatcher_fails`](/en/events/blocks#dreamcatcher_fails), [`lantern_soul_fire`](/en/events/blocks#lantern_soul_fire), [`charm_falls`](/en/events/blocks#charm_falls), [`scarecrow_closer`](/en/events/blocks#scarecrow_closer), [`altar_midnight`](/en/events/blocks#altar_midnight), [`journal_page_torn`](/en/events/blocks#journal_page_torn).
