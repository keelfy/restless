---
title: Signatures
---

# Signatures

43 events that only one type of spirit can roll, so that one of them tells you what you are dealing with. They are likelier than most events. Each type has three or four: the banshee, poltergeist, shade, phantom, nix, mimic, wraith, mara, revenant, lost one, domovoi, hoarder, demon and trickster.

## `banshee_letter`

A written book "To \<player>" lies on a bed nearby, from the banshee, with a page that is shy, devoted or possessive. It is a real item. Only a banshee rolls it.

Runs when: Night, a free bed within 16

## `banshee_gift`

A named item with a line of lore lies on a bed nearby: a wither rose "For you", a gold nugget "Ring", a phantom membrane "Veil", a skeleton skull "Mine" or a poppy. It is a real item. Only a banshee rolls it.

Runs when: A free bed within 16

## `banshee_serenade`

A soft, low moan comes from just outside a glass window nearby. Only a banshee rolls it.

Runs when: Night, glass or a glass pane within 12

## `banshee_jealous`

A villager, wolf, cat or parrot near you shakes with frost for a few seconds, as in powder snow: a villager shakes its head, a cat hisses, a wolf growls. It is not hurt. Only a banshee rolls it.

Runs when: An adult villager, wolf, cat or parrot within 8

## `poltergeist_toss`

A chest nearby opens and tosses three stacks out toward you, one a second, and shuts with a giggle. The items stay on the floor. Only a poltergeist rolls it.

Runs when: A chest with something in it within 10

## `poltergeist_play`

Every note block and bell nearby is struck in turn, fast, nearest first, like a child running along. Only a poltergeist rolls it.

Runs when: 2 or more note blocks or bells within 12

## `poltergeist_tantrum`

**Harmful.**
Every wooden door, trapdoor and fence gate nearby flies open in the same moment and then slams, and up to eight flower pots pop off, pot and plant on the floor. Only a poltergeist rolls it.

Runs when: 3 or more wooden doors, trapdoors or fence gates within 12

## `shade_gathered`

The torches nearby go out one by one and every one of them lies in a single pile in a dark spot. Nothing is lost. Only a shade rolls it.

Runs when: 3 or more torches within 12 and a dark spot 3 to 16 blocks away

## `shade_glance`

For 20 seconds every torch or lantern you look at goes out, up to five. They light again after two minutes. Only a shade rolls it.

Runs when: 3 or more torches or lanterns within 16

## `shade_room`

**Harmful.**
Every torch and lantern in the closed room you are in goes out, and while you stay the edges of your view darken. They light again when you leave, after three minutes at the latest. Only a shade rolls it.

Runs when: You are in a room shut off from the sky, with a torch or lantern in it

## `phantom_self_join`

Only this player gets "\<name> joined the game" and a player list entry with their own name and skin. 40 s later the entry goes and "\<name> left the game" follows. Nothing on the server. Only a phantom rolls it.

Runs when: None

## `phantom_rerun`

A copy of you, in your skin, walks the path you took in the last half minute. It is gone when you look at it or come near. Only a phantom rolls it.

Runs when: You walked 20 or more blocks in the last 30 seconds

## `phantom_home`

A copy of you, in your skin, stands by your bed when you are 12 to 32 blocks from it, and is gone when you come close. Only a phantom rolls it.

Runs when: Your respawn bed is 12 to 32 blocks away

## `nix_catch`

3 to 5 raw cod and salmon lie in a row on the bank, with a splash. They are real items. Only a nix rolls it.

Runs when: Water within 16 with a bank of 3 free cells beside it

## `nix_call`

A line of bubbles runs through the water toward the player and a conduit sound plays at its end. Only a nix rolls it.

Runs when: The player is out of water, with water 3 to 12 blocks away that runs 3 or more cells toward them

## `nix_returns`

An item lying in the water nearby is carried through it toward you, dripping, as far as the shore. Only a nix rolls it.

Runs when: An item in water within 16

## `mimic_voice`

An animal makes another mob's sound three times, a second apart: a cow says the villager's "hmm", a sheep groans like a zombie, a chicken mews. Only a mimic rolls it.

Runs when: An adult animal within 12

## `mimic_parrot`

A line you said in chat comes back a few seconds later under your own name, with one word changed. It shows in chat only. Only a mimic rolls it.

Runs when: You said something in chat in the last 10 minutes

## `mimic_chest`

**Harmful.**
A second chest appears beside one of yours. Opening it, the lid snaps shut with the sound of fangs and 2 damage, and the chest is gone. Left alone it is gone after two minutes. Only a mimic rolls it.

Runs when: A chest within 10 with a free cell beside it

## `wraith_taken`

**Harmful.**
A wild animal is gone in a puff of smoke, with no sound and no drops. Lost for good. Named, tamed and leashed animals are left alone. Only a wraith rolls it.

Runs when: Light level under 4 at the player, a wild animal within 16

## `wraith_steps`

In the dark, footsteps follow you for 30 seconds, and stop the moment you stop. Only a wraith rolls it.

Runs when: Light level under 4 at the player

## `wraith_edge`

A figure stands where the dark begins, facing you. It is gone when you look straight at it or come near. Only a wraith rolls it.

Runs when: You stand in light, with dark floor 4 to 6 blocks away

## `mara_dream`

A written book "A dream" turns up in your inventory after waking, about the biome you were in and the last mob you killed. It is a real item. Only a mara rolls it.

Runs when: You woke in the last two minutes, with a free inventory slot

## `mara_stolen_rest`

**Harmful.**
After waking, your sleep does not count: phantoms come as if you had not slept for three days. Only a mara rolls it.

Runs when: You woke in the last two minutes, not on peaceful

## `mara_weight`

**Harmful.**
After waking, slowness and mining fatigue for a minute, and the sound of something heavy getting off the bed. Only a mara rolls it.

Runs when: You woke in the last two minutes

## `revenant_ledger`

A written book on a lectern nearby lists every kind of passive animal you killed today and how many, "Cow: 14". The lectern's own book comes back after two minutes. Only a revenant rolls it.

Runs when: A lectern within 12, 5 or more passive animals killed today

## `revenant_tally`

**Harmful.**
A zombie named after the animal you killed most today and the count, "Cow 14", comes for you. Only a revenant rolls it.

Runs when: Night, not peaceful, a passive animal killed today

## `revenant_rises`

**Harmful.**
A skeleton climbs out of the ground, dirt flying, where you last killed a mob (a skeleton horse for a horse, donkey or mule), and comes for you. Only a revenant rolls it.

Runs when: Not peaceful, you killed a mob within 32 blocks in the last 10 minutes

## `lost_one_sign`

A sign nearby asks the way ("which way is home?") and is gone after five minutes. Only a lost one rolls it.

Runs when: Standing room 3 to 8 blocks away

## `lost_one_wanderer`

A figure in a default skin walks a slow circle 20 to 30 blocks away. When you come near it asks the way in chat and is gone. Only a lost one rolls it.

Runs when: Standing room 20 to 30 blocks away

## `lost_one_death_path`

Footprints appear one by one from the direction of the place you last died, leading toward you, and are wiped after three minutes. Only a lost one rolls it.

Runs when: Your last death was 16 to 64 blocks away in this dimension

## `domovoi_supper`

One thing to eat is gone from a chest, with an eating sound: a bread or cookie, a milk bucket that turns into an empty bucket, a stew that turns into a bowl. Only a domovoi rolls it.

Runs when: A chest within 12 holding bread, a cookie, a milk bucket or a stew

## `domovoi_hearth`

A cold campfire is lit, or one coal goes into a furnace that has something to smelt and no fuel. Only a domovoi rolls it.

Runs when: A cold campfire within 12, or a furnace, smoker or blast furnace with input and no fuel

## `domovoi_tidy`

One chest nearby is sorted by item with like stacks merged, and the open doors, trapdoors and gates nearby shut. Only a domovoi rolls it.

Runs when: A chest within 12 that is not tidy, or an open wooden door, trapdoor or gate within 12

## `hoarder_payment`

**Harmful.**
One stack in a chest turns into junk of the same size: dirt, sticks, rotten flesh or string. The hoarder keeps the stack for its nest. Only a hoarder rolls it.

Runs when: A chest within 10 holding something that is not junk

## `hoarder_shiny`

**Harmful.**
Gold, diamonds, emeralds and netherite in a chest move to another chest nearby. Nothing else does. Only a hoarder rolls it.

Runs when: Something shiny in a chest within 10, and another chest with a free slot

## `hoarder_nest`

After the hoarder has taken five stacks, a barrel appears out of your sight, 14 to 30 blocks away, with all of them in it. The items are real, and yours to find. It is forgotten when the server restarts. Only a hoarder rolls it.

Runs when: The hoarder has taken 5 or more stacks

## `demon_offer`

A crimson sign with an offer ("a diamond for your bed") stands nearby for five minutes. Nothing more happens. Only a demon rolls it.

Runs when: Standing room 3 to 8 blocks away

## `demon_trail`

For 15 seconds the block under each of your steps looks like netherrack, then soul soil, then goes back. Only you see it, unless the swaps setting is real. Only a demon rolls it.

Runs when: You stand on the ground

## `demon_contract`

**Harmful.**
A written book "Contract" with your name at the bottom turns up in your inventory, and your maximum health drops by two hearts until dawn. Only a demon rolls it.

Runs when: A free inventory slot

## `trickster_gift`

A chest of what looks like diamonds, gold and emeralds stands nearby. Whatever you take out turns to plain dirt in your inventory, with a laugh. The chest is gone after three minutes. Only a trickster rolls it.

Runs when: Standing room 3 to 8 blocks away

## `trickster_dig`

Sand or gravel nearby turns suspicious; brushing it gives a joke: a pufferfish, a poisonous potato, a stick named "Treasure". It is put back after ten minutes. Only a trickster rolls it.

Runs when: Sand, red sand or gravel with air above, within 12

## `trickster_dud`

A lit TNT that only you see hisses a few blocks away, then pops into firework sparks with a laugh. Only a trickster rolls it.

Runs when: Standing room 3 to 6 blocks away
