---
title: How it works
---

# How it works

Every spirit has an **age**, an **anger** and a **power**. Together they decide how often it acts and how much it can hurt. For where spirits come from, see [Deeds and places](/en/items/essences#deeds-and-places). Your grip on yourself is a separate bar, see [Sanity](/en/sanity).

## Power

Power runs from 0 to 1 and comes from the spirit alone, never from the world or the day count:

`power = 0.6 × maturity + 0.4 × anger`, never above 1

- **Maturity** grows to 1 over the spirit's first **30 days** (24000 ticks each). A spirit that haunts a person (not a place) starts with 0.15 of it, so even a brand new one is not harmless; a spirit bound to a place starts at 0.
- **Anger** is 0 to 1. See [Anger](#anger).
- A spirit that has **evolved** never drops below power 0.7, however calm it gets.

The spirit you get when you first join and a spirit born from a deed both start at anger 0.5, so their power starts at 0.29.

When the last spirit that haunts you is gone (banished, laid to rest or otherwise removed), a new one of a different type finds you 2 to 4 days later, at anger 0.5. It does not come if you already have another spirit, if you are released by the Requiem, or while you are in creative or spectator mode.

An [echo](/en/items/tools#the-ritual) left by an altar banishing does not count here: it neither keeps the new spirit away nor brings one when it leaves, and it does not count toward the limit of personal spirits or their cooldown. A new spirit of the echo's own type takes the echo's place.

## What power changes

**Which events it rolls.** Light events (scares) are weighted by `1 − 0.7 × power`. Harmful events are weighted by `power`. A spirit with power 0 (only a place-bound spirit just born) never rolls a harmful event; a new spirit that haunts you can roll one within the first days, although it stays rare; a fully grown, angry one (power 1) rolls light events at 30% of their base weight and harmful events at full weight. The spirit's type and its traits then multiply the weights for each kind of event, see [Spirits](/en/spirits).

**How often.** The wait between two events is `4 − 2.5 × power` minutes, stretched by a random factor of 1 to 2 and divided by the spirit's activity. A calm spirit acts every 4 to 8 minutes; a fully powerful one every 1.5 to 3. The wait is never shorter than 5 seconds. A new spirit's very first event comes 90 seconds after the journal is handed out, not after a full wait.

**How hard an event hits.** Every event has an impact, from 0 for a far figure or sound to 1 for a lethal attack. It does not change which event rolls, only when the next one comes. After a light event the wait is multiplied by 0.25 to 0.5, so the spirit reminds you of itself soon. After a middling one (impact 0.6) the wait is unchanged. After a heavy attack it is multiplied by 1.25 to 1.5, so you get time to recover.

**When it is quiet.** Spirits hold back in the open. Each of these stretches the wait by 1.2: it is day, you can see the sky, no thunderstorm is running. All three together stretch it by 1.7.

## Who hears it

The sounds of your personal spirit's events reach only you: a player standing beside you hears nothing. A territorial spirit's sounds are heard by everyone near, like any other sound. What an event does to the world, such as a block it breaks, everyone still sees.

## Anger

A spirit's anger rises when you do something spirits hate:

| What you do | Anger |
| --- | --- |
| Kill a cat | +0.15 |
| Break a grave: a skeleton skull, a wither skeleton skull, a bone block or a gravestone | +0.2 |
| Burn a forest | +0.1 |
| Use the Bargain, the Black Bargain or the Infernal Contract | +0.3 |
| Use the Borrowed Dream | +0.2 |
| Use the Devil's Coin | +0.1 |
| Fail a ritual at the altar | +0.3 |
| Say a wrong name in a ritual's fury or leaving | +0.1 |
| Lay an offering of the wrong nature | straight to 1 |
| Do a deed again whose spirit you already have (it angers that one instead of a new birth) | +0.15 |

Your personal spirits take offence at everything you do, wherever you are. A territorial spirit takes it only when it happens inside its radius (48 blocks), and also when its anchor is broken (+0.3, within 3 blocks of it).

Nothing wears anger down with time. It falls when a spirit's anger is spent: you beat an avenger, or a lethal event takes a player. A spirit's [desire](#leanings) left near you also calms it, and its hate angers it. A gift in the [bargain of a ritual](/en/items/tools#the-ritual) lowers it by 0.3.

Above anger 0.7 some [tells](/en/events/tells) turn harsh: the door slammer locks a door near you (`locked_door`), the cold touch brings frostbite (`frostbite`), the vengeful strikes (`wrath`) and the whisperer drains sanity (`sanity_drain`). Where the harsh form has nothing to act on, the mild tell runs.

## Evolving

A spirit left alone for **10 days** grows up: a poltergeist turns raging, a shade a light-eater. Personal spirits grow while their player is online. You are told, and the journal notes it.

An evolved spirit:

- never drops below power 0.7;
- gains one more habit of its own nature, so it starts to act in a new way;
- no longer hides its [hidden habit](#hidden-habit);
- rolls its signature events 3 times as often, see [Signatures](/en/events/signatures);
- asks for 4 more waves in the [banishing ritual](/en/items/tools#the-ritual);
- if territorial, fouls its land 3 times faster.

A wrong offering at the altar ages the spirit by a day toward evolving.

## Blight

A territorial spirit slowly fouls the land around its anchor: grass turns to coarse dirt, flowers to dead bushes. At first it reaches 6 blocks. Over 3 days it grows to the spirit's full radius of 48 blocks. A territorial spirit angrier than 0.7 stops spreading evenly: its blight grows as a narrow strip, about 5 blocks wide, toward the bed of the player who angered it, up to 64 blocks away. This works only while that player is online and their bed is in the same dimension; otherwise it spreads evenly again. The strip goes away with the spirit, like the rest of the blight. A spirit whose blight has reached 64 spots leaves an extra essence when banished. Blighted ground turns into the Blighted Land biome: grey fog, ash in the air and a low drone while you stand in it.

## Traits

Each spirit has a name, a story and **3 or 4 habits and 1 or 2 conditions**, and **one desire and one fear or hate**, rolled when it is born; one in four also has a [veil](#veils). Habits come from the spirit's nature, except one that comes from another nature. A trait pushes the weight of some kinds of events up or down on top of the type, and decides what the spirit hates or fears. Wards, salt and offerings work against them. The journal records what you learn about a spirit, see [Tools](/en/items/tools).

### Habits

A habit decides what the spirit does. The favored kinds of events are multiplied in on top of the type.

### Objects (iron)

| Trait | What it does | Favors |
|---|---|---|
| Restless hands | It reaches for things left lying around. | items ×2, doors ×1.5 |
| Door slammer | It slams doors. | doors ×2.5 |
| Meddler | It meddles with the world around you. | blocks ×2.5, mobs ×1.5 |
| Thrower | It throws what it finds. | items ×2.5, blocks ×1.2 |
| Stacker | It stacks things in neat piles. | blocks ×2, items ×1.5 |
| Knocker | It knocks on walls and doors. | doors ×2, sounds ×1.5 |

### Darkness (glowstone)

| Trait | What it does | Favors |
|---|---|---|
| Light eater | It feeds on light. | lights ×2.5 |
| Cold touch | It chills the air around you. | afflictions ×2 |
| Shadow caster | It casts shadows where there are none. | presence ×2, lights ×1.3 |
| Blinder | It takes away sight. | afflictions ×2, lights ×1.3 |
| Hearth killer | It puts out hearths and fires. | lights ×2, blocks ×1.3 |
| Corner lurker | It waits in corners. | presence ×1.5, mobs ×1.5 |

### Malice (holy water)

| Trait | What it does | Favors |
|---|---|---|
| Vengeful | It wants revenge. | assaults ×2 |
| Hunter | It hunts by scent. | presence ×2, assaults ×1.3 |
| Scratcher | It leaves scratches and wounds. | afflictions ×2, assaults ×1.3 |
| Beast caller | It calls beasts to its side. | mobs ×2.5 |
| Drowner | It pulls its prey under water. | water ×2.5, assaults ×1.2 |
| Snarler | It snarls and threatens. | sounds ×2, assaults ×1.3 |

### Deceit (mirror)

| Trait | What it does | Favors |
|---|---|---|
| Whisperer | It whispers. | sounds ×2, presence ×1.5 |
| Liar | It lies through the spirit box and the ouija board. | — |
| Face thief | It wears a familiar face. | player ×2.5 |
| Illusionist | It makes things appear that are not there. | blocks ×2, presence ×1.3 |
| False friend | It poses as a friend. | mobs ×2, player ×1.3 |
| Lure | It lures its prey away. | presence ×2, sounds ×1.3 |

### Conditions

A condition decides when the spirit acts or what it reacts to. Activity ×n means the spirit acts that many times as often while the condition holds.

| Trait | When or reacts to | Effect | Favors |
|---|---|---|---|
| Rain walker | Rain | Activity ×2 while it rains | — |
| Night stalker | Night | Activity ×1.8 at night | — |
| Dark dweller | Darkness (light level below 8) | Activity ×1.8 in the dark | — |
| Cave haunter | Underground (below y 40 with no sky overhead) | Activity ×2 underground | — |
| Water lurker | The player is in water | Activity ×2 in water | water ×1.5 |
| Noise feeder | Every sound the player makes | Its next event comes 6 ticks sooner per sound; sprinting hastens it most | — |
| Chest watcher | The player opens a chest or barrel | 25% chance to act on the next check | items ×1.5 |
| Sleep thief | The player uses a bed | 25% chance to act on the next check | presence ×1.5 |
| Deep caller | The player goes below y 0 | 25% chance to act on the next check | sounds ×1.5 |
| Grudge | The player kills a mob | 25% chance to act on the next check | assaults ×1.5 |

### Leanings

Every spirit born now also has one desire and one fear or hate. Spirits born before this change have none.

- **Fear.** The spirit's events do not run on a player within 4 blocks of the feared block or creature. Water counts only while it flows, never a still source. A stopped event shows the same ash and chime as a ward. A territorial spirit whose radius reaches the player counts too.
- **Desire.** One of the wanted items lying on the ground within 3 blocks of the player is taken, with a puff of soul smoke and a sound, and the spirit's anger drops by 0.25. This happens at most once every 5 minutes.
- **Hate.** When the player uses the hated block, or uses a block while holding the hated item, the spirit's anger rises by 0.1. This happens at most once a minute. Placing a lantern, ringing a bell and playing a note block or jukebox all count.

| Trait | Kind | What it is about |
|---|---|---|
| Fears fire | Fear | Fire, soul fire, campfires, soul campfires and lava. |
| Fears cats | Fear | Cats and ocelots. |
| Fears bells | Fear | Bells. |
| Fears running water | Fear | Flowing water. |
| Wants milk | Desire | A bucket of milk on the ground. |
| Wants bread | Desire | Bread on the ground. |
| Wants sweets | Desire | Cake, cookies, pumpkin pie and honey bottles on the ground. |
| Hates lanterns | Hate | Lanterns and soul lanterns, when placed. |
| Hates bells | Hate | Bells, when rung. |
| Hates music | Hate | Note blocks and jukeboxes, when played. |

### Hidden habit

A spirit with two or more habits keeps one of them hidden: no camera, spirit box or ouija board names it while the player's sanity is above 35%. A shaken player, below 35% sanity, can learn it; so can anyone once the spirit has evolved.

### Veils

One spirit in four is born with a veil: it hides from one tool. That tool finds nothing of it, and the journal notes the silence, which is itself a clue. The other tools read it as usual.

| Trait | Hidden from |
|---|---|
| Unseen | The camera: it never shows on a photograph. |
| Light-footed | Ash: it leaves no prints. |
| Voiceless | The spirit box: it never answers. |

## Weakness

The material a spirit is weak to holds it back outside the altar too. A stopped event shows the same ash and chime as a ward.

- **Objects** (iron): its door and item events do not run within 6 blocks of an iron door or a container under iron bars.
- **Darkness** (glowstone): its light events do not run within 4 blocks of glowstone.
- **Malice** (holy water): holy water poured at your feet (use it while looking down) ends the events of this spirit that are still running.
- **Deceit** (mirror): a mirror in your hand shows a fake block or a double of this spirit for what it is.

## The calendar

Days are counted from the start of the world, and three of them stand apart:

- **Full moon**, every 8th day (the vanilla full moon): every spirit's power is 0.2 higher, up to 1.
- **Blood night**, every 7th day: each event a ward stops wears it twice (salt rings, door charms, scarecrows). At dusk you are warned in chat.
- **Day of the dead**, the last day of every 28-day season: spirits run no harmful events, and no tool lies. A blood night that falls on it does not happen.

At the first dusk of any of them the journal opens its *Calendar* chapter. The [spirit candle](/en/items/tools#spirit-candle) shows the date by its flame.

## Advancements

The mod has its own advancement tab, **Restless**, that follows the hunt: the first clue, the first photograph of a spirit, a banishing at the altar with an offering that suits the spirit's nature, the first spirit laid to rest at its grave, then one advancement for every kind of essence, and the [Requiem](/en/items/requiem) at the end.
