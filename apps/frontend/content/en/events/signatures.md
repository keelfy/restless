---
title: Signatures
---

# Signatures

46 events that only one type of spirit can roll, so that one of them shows which type of spirit is at work. They are likelier than most events. Each type has three or four: the banshee, poltergeist, shade, phantom, nix, mimic, wraith, mara, revenant, lost one, domovoi, hoarder, demon, trickster and boggart.

## `banshee_letter`

A written book "To \<player>" lies on a bed nearby, from the banshee, with a page that is shy, devoted or possessive. It is a real item. Only a banshee rolls it.

Runs when: it is night and a free bed lies within 16 blocks.

## `banshee_gift`

A named item with a line of lore lies on a bed nearby: a wither rose "For you", a gold nugget "Ring", a phantom membrane "Veil", a skeleton skull "Mine" or a poppy. It is a real item. Only a banshee rolls it.

Runs when: a free bed lies within 16 blocks.

## `banshee_serenade`

A soft, low moan comes from just outside a glass window nearby. Only a banshee rolls it.

Runs when: it is night and there is glass or a glass pane within 12 blocks.

## `banshee_jealous`

A villager, wolf, cat or parrot near the player shakes with frost for a few seconds, as in powder snow: a villager shakes its head, a cat hisses and a wolf growls. The animal is not hurt. Only a banshee rolls it.

Runs when: an adult villager, wolf, cat or parrot is within 8 blocks.

## `poltergeist_toss`

A chest nearby opens and tosses three stacks out toward the player, one a second, and then shuts with a giggle. The items stay on the floor. Only a poltergeist rolls it.

Runs when: a chest with something in it is within 10 blocks.

## `poltergeist_play`

Every note block and bell nearby is struck in turn, nearest first and in quick succession, like a child running along them. Only a poltergeist rolls it.

Runs when: 2 or more note blocks or bells are within 12 blocks.

## `poltergeist_tantrum`

**Harmful.**
Every wooden door, trapdoor and fence gate nearby flies open in the same moment and then slams, and up to eight flower pots pop off, leaving pot and plant on the floor. Only a poltergeist rolls it.

Runs when: 3 or more wooden doors, trapdoors or fence gates are within 12 blocks.

## `shade_gathered`

The torches nearby go out one by one, and every one of them ends up in a single pile in a dark spot. Nothing is lost. Only a shade rolls it.

Runs when: 3 or more torches are within 12 blocks and there is a dark spot 3 to 16 blocks away.

## `shade_glance`

For 20 seconds, every torch or lantern the player looks at goes out, up to five. They light again after two minutes. Only a shade rolls it.

Runs when: 3 or more torches or lanterns are within 16 blocks.

## `shade_room`

**Harmful.**
Every torch and lantern in the closed room the player is in goes out, and the edges of the player's view darken for as long as the player stays. They light again when the player leaves, after three minutes at the latest. Only a shade rolls it.

Runs when: the player is in a room shut off from the sky that has a torch or lantern in it.

## `phantom_self_join`

Only this player gets "\<name> joined the game" and a player list entry with their own name and skin. After 40 s the entry goes and "\<name> left the game" follows, though none of this exists on the server. Only a phantom rolls it.

Runs when: there are no conditions.

## `phantom_rerun`

A copy of the player, in the player's skin, walks the path the player took in the last half minute. It vanishes when the player looks at it or comes near. Only a phantom rolls it.

Runs when: the player walked 20 or more blocks in the last 30 seconds.

## `phantom_home`

A copy of the player, in the player's skin, stands by the player's bed when the player is 12 to 32 blocks from it, and vanishes when the player comes close. Only a phantom rolls it.

Runs when: the player's respawn bed is 12 to 32 blocks away.

## `nix_catch`

Between 3 and 5 raw cod and salmon lie in a row on the bank, and a splash is heard. They are real items. Only a nix rolls it.

Runs when: there is water within 16 blocks with a bank of 3 free cells beside it.

## `nix_call`

A line of bubbles runs through the water toward the player, and a conduit sound plays at its end. Only a nix rolls it.

Runs when: the player is out of water and there is water 3 to 12 blocks away that runs 3 or more cells toward them.

## `nix_returns`

An item lying in the water nearby is carried through it toward the player, dripping, as far as the shore. Only a nix rolls it.

Runs when: an item lies in water within 16 blocks.

## `mimic_voice`

An animal makes another mob's sound three times, a second apart: a cow says the villager's "hmm", a sheep groans like a zombie and a chicken mews. Only a mimic rolls it.

Runs when: an adult animal is within 12 blocks.

## `mimic_parrot`

A line the player said in chat comes back a few seconds later under the player's own name, with one word changed, and it shows in chat only. Only a mimic rolls it.

Runs when: the player said something in chat in the last 10 minutes.

## `mimic_chest`

**Harmful.**
A second chest appears beside one of the player's chests. When it is opened, the lid snaps shut with the sound of fangs and deals 2 damage, and the chest vanishes. If left alone, it vanishes after two minutes. Only a mimic rolls it.

Runs when: a chest is within 10 blocks with a free cell beside it.

## `wraith_taken`

**Harmful.**
A wild animal vanishes in a puff of smoke, with no sound and no drops, and is lost for good. Named, tamed and leashed animals are left alone. Only a wraith rolls it.

Runs when: the light level at the player is under 4 and a wild animal is within 16 blocks.

## `wraith_steps`

In the dark, footsteps follow the player for 30 seconds and stop the moment the player stops. Only a wraith rolls it.

Runs when: the light level at the player is under 4.

## `wraith_edge`

A figure stands where the dark begins, facing the player. It vanishes when the player looks straight at it or comes near. Only a wraith rolls it.

Runs when: the player stands in light and there is dark floor 4 to 6 blocks away.

## `mara_dream`

After the player wakes, a written book "A dream" turns up in the inventory, telling of the biome the player was in and the last mob the player killed. It is a real item. Only a mara rolls it.

Runs when: the player woke in the last two minutes and has a free inventory slot.

## `mara_stolen_rest`

**Harmful.**
After waking, the player's sleep does not count, and phantoms come as if the player had not slept for three days. Only a mara rolls it.

Runs when: the player woke in the last two minutes and the difficulty is not peaceful.

## `mara_weight`

**Harmful.**
After waking, the player gets slowness and mining fatigue for a minute, and something heavy is heard getting off the bed. Only a mara rolls it.

Runs when: the player woke in the last two minutes.

## `revenant_ledger`

A written book on a lectern nearby lists every kind of passive animal the player killed today and how many, as in "Cow: 14". The lectern's own book comes back after two minutes. Only a revenant rolls it.

Runs when: a lectern is within 12 blocks and the player has killed 5 or more passive animals today.

## `revenant_tally`

**Harmful.**
A zombie named after the animal the player killed most today, together with the count, as in "Cow 14", comes for the player. Only a revenant rolls it.

Runs when: it is night, the difficulty is not peaceful and the player has killed a passive animal today.

## `revenant_rises`

**Harmful.**
A skeleton climbs out of the ground, dirt flying, where the player last killed a mob (a skeleton horse for a horse, donkey or mule), and comes for the player. Only a revenant rolls it.

Runs when: the difficulty is not peaceful and the player killed a mob within 32 blocks in the last 10 minutes.

## `lost_one_sign`

A sign nearby asks the way ("which way is home?") and vanishes after five minutes. Only a lost one rolls it.

Runs when: there is standing room 3 to 8 blocks away.

## `lost_one_wanderer`

A figure in a default skin walks a slow circle 20 to 30 blocks away. When the player comes near, it asks the way in chat and vanishes. Only a lost one rolls it.

Runs when: there is standing room 20 to 30 blocks away.

## `lost_one_death_path`

Footprints appear one by one from the direction of the place where the player last died, leading toward the player, and are wiped after three minutes. Only a lost one rolls it.

Runs when: the player's last death was 16 to 64 blocks away in this dimension.

## `domovoi_supper`

One thing to eat vanishes from a chest, with an eating sound: a bread or cookie, a milk bucket that turns into an empty bucket, or a stew that turns into a bowl. Only a domovoi rolls it.

Runs when: a chest within 12 blocks holds bread, a cookie, a milk bucket or a stew.

## `domovoi_hearth`

A cold campfire is lit, or one coal goes into a furnace that has something to smelt and no fuel. Only a domovoi rolls it.

Runs when: there is a cold campfire within 12 blocks, or a furnace, smoker or blast furnace with input and no fuel.

## `domovoi_tidy`

One chest nearby is sorted by item with like stacks merged, and the open doors, trapdoors and gates nearby shut. Only a domovoi rolls it.

Runs when: a chest within 12 blocks is not tidy, or an open wooden door, trapdoor or gate is within 12 blocks.

## `hoarder_payment`

**Harmful.**
One stack in a chest turns into junk of the same size: dirt, sticks, rotten flesh or string. The hoarder keeps the stack for its nest. Only a hoarder rolls it.

Runs when: a chest within 10 blocks holds something that is not junk.

## `hoarder_shiny`

**Harmful.**
Gold, diamonds, emeralds and netherite in a chest move to another chest nearby, and nothing else does. Only a hoarder rolls it.

Runs when: there is something shiny in a chest within 10 blocks and another chest with a free slot.

## `hoarder_nest`

After the hoarder has taken five stacks, a barrel appears out of the player's sight, 14 to 30 blocks away, with all of them in it. The items are real and the player can find them, but the barrel is forgotten when the server restarts. Only a hoarder rolls it.

Runs when: the hoarder has taken 5 or more stacks.

## `demon_offer`

A crimson sign with an offer ("a diamond for your bed") stands nearby for five minutes, and nothing more happens. Only a demon rolls it.

Runs when: there is standing room 3 to 8 blocks away.

## `demon_trail`

For 15 seconds the block under each of the player's steps looks like netherrack, then soul soil, and then goes back. Only the player sees it, unless the swaps setting is real. Only a demon rolls it.

Runs when: the player stands on the ground.

## `demon_contract`

**Harmful.**
A written book "Contract" with the player's name at the bottom turns up in the player's inventory, and the player's maximum health drops by two hearts until dawn. Only a demon rolls it.

Runs when: the player has a free inventory slot.

## `trickster_gift`

A chest of what looks like diamonds, gold and emeralds stands nearby. Whatever the player takes out turns to plain dirt in the player's inventory, with a laugh, and the chest vanishes after three minutes. Only a trickster rolls it.

Runs when: there is standing room 3 to 8 blocks away.

## `trickster_dig`

Sand or gravel nearby turns suspicious, and brushing it reveals a joke: a pufferfish, a poisonous potato or a stick named "Treasure". It is put back after ten minutes. Only a trickster rolls it.

Runs when: there is sand, red sand or gravel with air above it within 12 blocks.

## `trickster_dud`

A lit TNT that only the player sees hisses a few blocks away, then pops into firework sparks with a laugh. Only a trickster rolls it.

Runs when: there is standing room 3 to 6 blocks away.

## `boggart_spiders`

Forty tiny spiders that only the player sees run out of the dark from every side, swarm to the player's feet and circle the player for a few seconds, and then scatter. Nothing is really there. Only a boggart rolls it.

Runs when: the player is not in water.

## `boggart_crawl`

Two dozen tinier spiders that only the player sees run up close and climb in circles up the player's body to the player's face, and then drop and scatter. Nothing is really there. Only a boggart rolls it.

Runs when: the player is not in water.

## `boggart_silverfish`

Forty silverfish that only the player sees pour across the floor, circle round the player's feet and scatter. Nothing is really there. Only a boggart rolls it.

Runs when: the player is not in water.
