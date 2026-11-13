---
title: How it works
---

# How it works

Every spirit has an **age**, an **anger** and a **power**. Together they decide how often it acts and how much it can hurt. For where spirits come from, see [Deeds and places](/en/items/essences#deeds-and-places). Your grip on yourself is a separate bar, see [Sanity](/en/sanity).

## Power

Power runs from 0 to 1 and comes from the spirit alone, never from the world or the day count:

`power = 0.6 × maturity + 0.4 × anger`, never above 1

- **Maturity** grows from 0 to 1 over the spirit's first **30 days** (24000 ticks each).
- **Anger** is 0 to 1. See [Anger](#anger).
- A spirit that has **evolved** never drops below power 0.7, however calm it gets.

A spirit born from a deed starts at anger 0.5, so its power starts at 0.2.

## What power changes

**Which events it rolls.** Light events (scares) are weighted by `1 − 0.7 × power`. Harmful events are weighted by `power`. A calm, young spirit (power 0) never rolls a harmful event; a fully grown, angry one (power 1) rolls light events at 30% of their base weight and harmful events at full weight. The spirit's type and its traits then multiply the weights for each kind of event, see [Spirits](/en/spirits).

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
| Lay an offering of the wrong nature | straight to 1 |
| Do a deed again whose spirit you already have (it angers that one instead of a new birth) | +0.15 |

Your personal spirits take offence at everything you do, wherever you are. A territorial spirit takes it only when it happens inside its radius (48 blocks), and also when its anchor is broken (+0.3, within 3 blocks of it).

Nothing wears anger down with time. It falls when a spirit's anger is spent: you beat an avenger, or a lethal event takes a player.

## Evolving

A spirit left alone for **10 days** grows up: a poltergeist turns raging, a shade a light-eater. Personal spirits grow while their player is online. You are told, and the journal notes it.

An evolved spirit:

- never drops below power 0.7;
- rolls its signature events 3 times as often, see [Signatures](/en/events/signatures);
- asks for 4 more waves in the [banishing ritual](/en/items/tools#the-ritual);
- if territorial, fouls its land 3 times faster.

A wrong offering at the altar ages the spirit by a day toward evolving.

## Blight

A territorial spirit slowly fouls the land around its anchor: grass turns to coarse dirt, flowers to dead bushes. At first it reaches 6 blocks. Over 3 days it grows to the spirit's full radius of 48 blocks. A spirit whose blight has reached 64 spots leaves an extra essence when banished.

## Traits

Each spirit has a name, a story and **3 to 5 traits**, rolled when it is born. A trait pushes the weight of some kinds of events up or down on top of the type, and decides what the spirit hates or fears. Wards, salt and offerings work against them. The journal records what you learn about a spirit, see [Tools](/en/items/tools).
