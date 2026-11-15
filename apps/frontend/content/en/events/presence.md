---
title: Presence
---

# Presence

100 events.

## `dragon_flyover`

Only this player's client is told of an ender dragon that flies a straight 240-block line 40 blocks above the player, up to 30 blocks to one side, on a random bearing and over 10 s, with its growl sounding at the start (the wing flaps are the client's own). The effect exists only on this player's client, and nothing happens on the server.

Runs when: the player is in the Overworld and the sky is visible.

## `edge_figure`

A silent, unhurtable mannequin with the player's own skin stands there facing the player. It vanishes without a sound or particles once the player looks within 25° of it with a clear line, or after 30 s.

Runs when: no such figure exists yet and the player has room to stand 16 blocks away, 45–65° to one side of where they look.

## `fog_silhouette`

A mannequin in the default skin stands far off. It vanishes without a sound once the player comes within 24 blocks, or after 60 s.

Runs when: the sky is open, it is raining or dark outside, and there is room to stand 40 blocks away within 25° of where the player looks.

## `distant_player`

A silent, unhurtable mannequin in a random default skin (Steve, Alex and the rest) stands there facing the player. It vanishes without a sound once the player comes within 35 blocks, or after 60 s.

Runs when: there is room to stand 60 blocks away within 40° of where the player looks, where up to 16 bearings are tried and grass and flowers count as room.

## `friend_double`

A silent, unhurtable mannequin with the skin and name of another player who stands nearby (within 16 blocks, in the same dimension and not a spectator; one is chosen at random) stands 50 blocks away facing the player, with that name shown over its head. It vanishes without a sound once the player comes within 30 blocks, or after 60 s.

Runs when: no such figure exists yet, another such player is nearby, and there is room to stand 50 blocks away within 40° of where the player looks (up to 16 bearings are tried).

## `distant_walker`

A silent, unhurtable mannequin in a random default skin walks across the player's view at a walking pace or, in 40% of cases, sprints at 0.28 blocks/tick and jumps now and then. It jumps up one-block steps, drops off ledges up to 2 blocks deep, goes through grass and flowers, and turns up to 80° aside to go round whatever is in its way. It vanishes without a sound once the player comes within 25 blocks, when no way on is left, or after 30 s.

Runs when: there is room to stand 45 blocks away within 40° of where the player looks, where up to 16 bearings are tried and grass and flowers count as room.

## `nametag`

A nameplate (a text display, seen through walls) with a made-up nickname of the kind spirits get, at most 16 characters, floats 3 blocks below the player's feet or at head height over a spot with room to stand, with nobody beneath it. It vanishes after 10 s.

Runs when: half the time there is room to stand 30 blocks away within 40° of where the player looks, and the other half of the time it does not run.

## `pillar`

A silent, unhurtable mannequin in a random default skin holds a block in its hand (cobblestone, stone, dirt or any planks). Every 0.5 s it looks down at its feet and jumps (along a player's jump arc), places that block (with its place sound) where its feet were once it is above that spot, swings its arm and lands on the block; it does this 8 times, and at the top it turns to face the player. It waits at the top and vanishes 7 s after it appeared, or sooner if the player comes within 10 blocks or the way up is blocked. Each block turns back to air after 2 min if it is still that block, and under hardcore the pillar stays.

Runs when: there is room to stand 24 blocks away within 40° of where the player looks, and the space above it is open for 8 blocks.

## `harvest`

A silent, unhurtable mannequin in a random default skin stands there and, every 0.4 s, turns to the nearest ripe crop within 4 blocks, swings and breaks it (with particles and sound, but no drops). It vanishes when no crop is left, once the player comes within 5 blocks, or after 10 s. Each crop comes back at its previous age after 2 min if its spot is still empty, and under hardcore the crops stay gone.

Runs when: a ripe crop lies within 16 blocks, with room to stand within 4 blocks of the crop and at least 8 blocks from the player.

## `footprints`

For 6 s, puffs of the ground rise and a step sound plays where the player stood a second ago. Only particles appear, and nothing is placed.

Runs when: the player stands on or in sand or snow.

## `cold_breath`

A puff of breath appears in front of the player's face.

Runs when: the biome is warm.

## `local_rain`

Rain particles and rain sound appear over the player for 6 s.

Runs when: the sky is visible and it is not raining.

## `lost_time`

**Harmful.**
The Overworld clock jumps 1000 ticks forward, which affects everyone.

Runs when: it is night.

## `guardian_face`

The elder guardian's face and sound appear on the client only.

Runs when: at any time.

## `false_totem`

The totem of undying animation plays, as a plain totem if the player carries none.

Runs when: at any time.

## `subliminal_title`

A dark red title from `texts` shows for 3 ticks with no fade, after which the fade times are reset.

Runs when: at any time.

## `red_vignette`

The client draws a red tint at the screen's edges that peaks at 80% and fades in and out over 5 s (`RedVignette`).

Runs when: at any time.

## `false_death`

The player gets their own death message, "was slain by ???", and it is sent only to them.

Runs when: at any time.

## `private_rain`

Rain falls for this client only, as a storm in 30% of cases, and lasts 20 s, after which the real weather returns.

Runs when: the dimension has weather, it is not raining and the sky is visible.

## `silent_lightning`

A visual-only lightning bolt strikes 24 blocks away.

Runs when: the dimension has weather and the sky is visible.

## `false_night`

Only this player's client is told that it is midnight, with the clock stopped. After 15 s it gets the real time back, and nothing happens on the server.

Runs when: the player is in the Overworld and it is daytime.

## `false_slot`

Only this player's client is told that the slot holds an item from a list of 25 (skulls and heads, bones, rotten flesh, spider eyes, a totem, disc 11 and more; a player head is the player's own). After 20 s, or as soon as the player interacts with the inventory, the real slot comes back. Nothing happens on the server.

Runs when: an inventory or hotbar slot is empty.

## `false_advancement`

A hidden advancement (`restless:whisper/*`, with an impossible criterion and no chat line) is granted so that its toast shows, and it is revoked 2 ticks later.

Runs when: there are no conditions.

## `turned_head`

**Harmful.**
A step sound of the ground plays at the spirit's position (or 6 blocks away at a random bearing). The camera then turns toward it, up or down as well, smoothly over 30 ticks and a little every frame on the client (`TurnPayload`, `HeadTurn`), while the mouse keeps working.

Runs when: there are no conditions.

## `stranger_joins`

Only this player sees "\<name> joined the game" and a player list entry for a made-up nickname of the kind spirits get, at most 16 characters, in a default skin. After 40 s the entry disappears and "\<name> left the game" follows. Nothing happens on the server.

Runs when: there are no conditions.

## `stranger_death`

Only this player gets a vanilla death message (fell, drowned, suffocated, fell out of the world, died) for a made-up nickname.

Runs when: there are no conditions.

## `false_whisper`

A gray italic line, "X whispers to you", appears from a real online player. The event is off by default, since it impersonates a real person.

Runs when: another player is online.

## `pumpkin_head`

Only this player's client is told that the head slot holds a carved pumpkin, so the pumpkin blur covers the screen. After 4 s the real slot comes back, and nothing happens on the server. In hardcore, a real pumpkin is put on an empty head slot and taken off again.

Runs when: at any time.

## `xp_drain`

The client is told that the experience bar and level are zero for 3 s, after which the real values return.

Runs when: the player has experience points or levels.

## `phantom_flames`

The client is told that the player is on fire for 3 s, so flames show on the screen. Nothing actually burns, and the real flags return afterwards. In hardcore, the player is really set on fire for 1 s.

Runs when: the player is not on fire and not in water.

## `phantom_drowning`

The client is told that the air supply drains from full to 20% over 4 s, so bubbles show on land, after which the real value returns.

Runs when: the air supply is full.

## `phantom_frost`

The client is told that the frozen ticks rise to full over 2.5 s, so frost creeps over the screen and is held until 5 s, after which the real value returns. In hardcore, the player really freezes.

Runs when: the player is not freezing.

## `moon_phase`

Only this player's client is shown the clock moved a random 1 to 7 whole days ahead, so the moon has another phase while the time of day stays the same. After 20 s it gets the real time back, and nothing happens on the server.

Runs when: the player is in the Overworld and it is night.

## `false_boss_bar`

A purple boss bar with a name from `names` is sent to this player only. It fills over 3 s, empties over 2 s and is then removed. No boss exists.

Runs when: at any time.

## `action_bar_whisper`

A dark red line from `lines` ("Don't turn around" and others) shows on the action bar and is cleared after 2 s.

Runs when: at any time.

## `false_absorption`

The client is told that the player has 8 absorption (4 golden hearts), which drains to 0 over 4 s, after which the real value returns.

Runs when: the player has no absorption.

## `empty_armor_bar`

The client is told that the armor attribute is 0 for 4 s, so the armor bar vanishes, after which the real attribute returns.

Runs when: the player is wearing armor.

## `hurt_tilt`

The hurt animation (a screen tilt) plays from a random side, with no damage.

Runs when: at any time.

## `own_hand_swings`

The player's own main hand swings twice, 14 ticks apart.

Runs when: at any time.

## `night_eyes`

The client is told of 1.5 s of night vision followed by 4 s of darkness, after which the real effects return. No such effect exists on the server.

Runs when: the player has neither night vision nor darkness.

## `all_on_cooldown`

Every item on the hotbar and in the hands is shown on a 2 s cooldown, which the client counts down by itself.

Runs when: there is an item on the hotbar or in the hands.

## `noon_moon`

Only this player's client is told that it is midnight under a full moon, with the clock stopped. After 10 s it gets the real time back, and nothing happens on the server.

Runs when: the player is in the Overworld and it is daytime.

## `black_petals`

For 10 s, ash particles fall like petals in a cloud above and around the player.

Runs when: the player is in a cherry grove.

## `roof_figure`

A silent, unhurtable mannequin in a random default skin crouches on top of a roof block, facing the player. It vanishes without a sound once the player comes within 10 blocks, or after 30 s. A faint dark haze stays for 3 s where it stood.

Runs when: a stair, slab or planks block open to the sky lies within 20 blocks and at least 12 blocks from the player.

## `hill_waver`

A silent, unhurtable mannequin in a random default skin stands on a hilltop facing the player and swings its arm every 0.4 s. It vanishes without a sound once the player comes within 20 blocks, or after 20 s. A faint dark haze stays for 3 s where it stood.

Runs when: there is room to stand, open to the sky, 45 blocks away within 40° of where the player looks and at least 6 blocks above the player.

## `wall_miner`

A silent, unhurtable mannequin in a random default skin stands behind a wall and swings at it every 0.4 s. Cracks run up the block over 10 steps, only on this player's client and with a digging sound, and then clear. The wall is never broken, and the mannequin vanishes when it is done, once the player comes within 2.5 blocks, or after 10 s.

Runs when: a wall one block thick and two blocks high stands in front of the player, with room to stand behind it.

## `table_figure`

A silent, unhurtable mannequin in a random default skin stands at a crafting table, looking down at it. It vanishes without a sound when the table is gone, once the player comes within 4 blocks, or after 15 s.

Runs when: a crafting table lies within 16 blocks, with room to stand within 2 blocks of it and at least 6 blocks from the player.

## `cave_torches`

Only this player's client is told that a line of floor spots along a dark tunnel holds torches. After 1 min it gets the real blocks back.

Runs when: the player is under a roof and there is a bearing with at least 3 dark floor spots 6 to 28 blocks away that are shut off from the sky.

## `own_walker`

A silent, unhurtable mannequin in the player's own skin walks across their view, jumping up one-block steps, dropping off ledges up to 2 blocks deep and going round whatever is in its way. It vanishes without a sound once the player comes within 25 blocks, when no way on is left, or after 30 s.

Runs when: there is room to stand 45 blocks away within 40° of where the player looks.

## `night_runner`

A silent, unhurtable mannequin in a random default skin sprints across the view at 0.36 blocks/tick and jumps now and then. It vanishes without a sound once the player comes within 7 blocks, when no way on is left, or after 15 s.

Runs when: the light at the player is 4 or less and there is room to stand 22 blocks away within 35° of where the player looks.

## `bed_sleeper`

A silent, unhurtable mannequin in a random default skin lies asleep in a free bed, which is not marked as taken. It vanishes without a sound once the player comes within 6 blocks, or after 20 s.

Runs when: a free bed lies within 24 blocks with room above it, at least 8 blocks from the player.

## `shrieker_figure`

A silent, unhurtable mannequin in a random default skin crouches by a sculk shrieker, looking at it. It vanishes without a sound when the shrieker shrieks, once the player comes within 6 blocks, or after 20 s.

Runs when: the player is in the deep dark biome and a sculk shrieker lies within 24 blocks, with room to stand beside it and at least 8 blocks from the player.

## `hello_chat`

Only this player gets a chat line, `<name> hello?` (from `lines`), under a made-up nickname.

Runs when: a line is left that the spirit has not said yet.

## `ghost_teleport`

Only this player gets the command feedback `Teleported ??? to <player>`. Nobody is moved.

Runs when: a name is left that the spirit has not said yet.

## `stranger_advancement`

Only this player gets the chat line `<name> has made the advancement [Who's There?]` under a made-up nickname.

Runs when: a title is left that the spirit has not said yet.

## `grave_figure`

A silent, unhurtable mannequin in a random default skin stands where the player last died, facing the player. It vanishes without a sound once the player comes within 12 blocks, or after 20 s. A faint dark haze stays for 3 s where it stood.

Runs when: the last death was in this dimension, 20 to 60 blocks away, in a loaded chunk with room to stand.

## `bedside_figure`

The event waits up to 20 min for the player to sleep and wake. Then a silent, unhurtable mannequin in a random default skin stands next to the bed for 1.25 s, facing the player, and a faint dark haze stays for 3 s where it stood.

Runs when: no such wait is already pending.

## `black_ash`

Dark ash falls around the player from 2 blocks up, in 24 bursts 0.25 s apart.

Runs when: the player is indoors.

## `border_closes`

Only this player's client is shown a world border centered on the player, which shrinks from 80 to 40 blocks wide over 4 s and is held for 2 s, after which the real border returns. Nothing happens on the server.

Runs when: no real border lies within 80 blocks.

## `false_recipe`

Only this player's client gets a "recipe unlocked" toast for a made-up bone and skull recipe (the result is a skull, bone block, soul lantern or wither rose). After 10 s the recipe is removed from the recipe book, and no such recipe ever exists.

Runs when: at any time.

## `view_narrows`

The client is told that the movement speed attribute is 0.02 for 1 s, so the field of view pinches, after which the real attribute returns.

Runs when: at any time.

## `mob_eyes`

Only this player's client is sent to see through a spider, cave spider, creeper or enderman within 24 blocks for 2.5 s, with the vanilla shader of that mob, and then returns to the player's own view. The view also ends early if the mob dies or vanishes, or if the player is hurt or changes dimension; death, logout and server stop reset it too. Nothing changes on the server.

Runs when: the player is on the ground and not in a fluid, and one of those mobs is within 24 blocks.

## `out_of_body`

For 3 s the view moves to a packet-only entity 3.5 blocks behind the player (nearer if blocked) and 0.6 blocks above their eyes, looking at the back of their head, and then the view returns and the entity is removed. The same early resets apply as for `mob_eyes`.

Runs when: the player is on the ground and not in a fluid.

## `spirit_sight`

For 1 s the view moves to the spirit's point (10 blocks away at a random bearing when no spirit sent it), looking at the player. The event does nothing if that point is over 40 blocks away or not loaded, and the same early resets apply as for `mob_eyes`.

Runs when: the player is on the ground and not in a fluid.

## `lake_fisher`

A silent, unhurtable mannequin in a random default skin stands on the bank with a fishing rod, facing the water, and casts. Every 4th second it swings the rod and the water splashes (with fishing particles and sounds), and no bobber is drawn. It vanishes without a sound once the player comes within 6 blocks, or after 30 s.

Runs when: a lake (a water source with water on three sides for 3 blocks) lies within 16 blocks, with room to stand on its bank at least 8 blocks from the player.

## `window_figure`

A silent, unhurtable mannequin in a random default skin stands behind the glass, facing the player. It vanishes without a sound once the player comes within 3 blocks, or after 20 s.

Runs when: the player is under open sky and a glass block or glass pane within 14 blocks has a roofed room behind it (with room to stand one block below or level with it, at least 4 blocks from the player) and open air on the player's side.

## `trailing_figure`

A silent, unhurtable mannequin in a random default skin faces the player and, every 0.1 s, steps toward them by as far as the player moved (up to 1 block). It therefore keeps its distance while the player walks, stops when they stop, and sprints when they do. It vanishes without a sound once the player looks within 12° of it, comes within 8 blocks, or after 60 s.

Runs when: there is room to stand 30 blocks behind the player (150-180° from where they look).

## `deep_swimmer`

A silent, unhurtable mannequin in a random default skin lies in the swimming pose in the water there and swims across the view at 2.4 blocks/s, trailing bubbles. It vanishes without a sound when the water ends, once the player comes within 2 blocks, or after 15 s.

Runs when: the player's head is underwater and there is water 8 blocks away within 15-60° of where they look (12 bearings are tried).

## `note_dropper`

A silent, unhurtable mannequin in a random default skin faces the player for 1.6 s, then drops a torch or, half the time, paper named with a short note, and sprints away from the player. The item is real and stays. The mannequin vanishes without a sound once the player comes within 2 blocks, when no way on is left, or after 20 s.

Runs when: there is room to stand 14 blocks away within 30° of where the player looks.

## `horizon_choir`

Six silent, unhurtable mannequins in random default skins stand there, all facing the player. All of them vanish at once and without a sound when the player looks within 10° of any of them, comes within 20 blocks of any of them, or after 30 s.

Runs when: the player is under open sky and room to stand is found at the surface for at least 4 of 6 points 45-51 blocks away, spread over 60° of where they look.

## `blade_stalker`

A silent, unhurtable mannequin in a random default skin crouches with an iron sword and creeps toward the player at 1.3 blocks/s, facing them. It does not move while the player looks within 25° of it. It vanishes after 2 s of being looked at in all, when it comes within 3 blocks, or after 30 s.

Runs when: there is room to stand 16 blocks behind the player (140-180° from where they look).

## `cliff_faller`

A silent, unhurtable mannequin in a random default skin sprints along that line off the edge and falls. After 5 blocks of falling it makes the player death sound and vanishes without a sound and without a body. It also vanishes if the player comes within 6 blocks, or after 15 s.

Runs when: there is room to stand 12-22 blocks away, with flat ground within 6 blocks of it in some direction that ends in an 8-40 block drop onto solid ground (24 tries).

## `lake_ferry`

A silent, unhurtable mannequin in a random default skin sits in an invulnerable, silent oak boat there (real entities, removed with the figure) and sails across the player's view along the longer stretch of open water at 2.4 blocks/s, with paddle sounds. It vanishes along with the boat when the player comes within 6 blocks, when the water ends, or after 30 s.

Runs when: a lake (as for `lake_fisher`) lies within 16 blocks, with a surface point at least 10 blocks from the player.

## `cave_cart`

A silent, unhurtable mannequin in a random default skin sits in an invulnerable, silent minecart (real entities, no rails needed) that rolls along the ground across the view at 6 blocks/s, going round small obstacles or up and down over them, with rolling sounds. It vanishes along with the cart when the player comes within 5 blocks, when no way on is left, or after 20 s.

Runs when: the player is below y 50 with no open sky above, and there is cave room to stand 14 blocks away within 10-40° of where they look.

## `spyglass_figure`

The event waits up to 2 min for the player to raise a spyglass, then tells their client of a villager 48 blocks away along where they look. The villager is removed as soon as the spyglass is lowered, or after 1 min. Nothing happens on the server.

Runs when: the player carries a spyglass.

## `flash_figure`

Four visual-only lightning bolts strike 2 to 4.5 s apart. At each one, only this player's client is told of a wither skeleton standing on one bearing for 4 ticks, 34 blocks away at the first flash and 7 blocks closer each time, with the bolt striking beyond it. Nothing happens on the server.

Runs when: the player is in the Overworld, it is raining and the sky is visible.

## `jolt_behind`

A loud bang sounds 1.6 blocks behind the player, at 150–180° from where they look, and only this player's client is told of a black figure of no known kind standing there, in one of ten wrong shapes (tall and thin with arms to the knees; low and wide on all fours; a head hung on one shoulder; a small child with arms up; many thin arms; a stooped giant; bent double with its head held up; a tall one and a small one joined by the hands; a body on a very long neck; hung off the ground with its head dropped on a shoulder). The figure is not shown until the player looks at it; 12 ticks after they first do, it is gone, or after 6 s if they never turn. Nothing happens on the server. The figure twitches in sudden small jerks, its head sways, and its two pale eyes glow in the dark. This is a jump scare: after one has run for a player (this or any other jump scare), none rolls for them again for 10 min.

Runs when: there is room to stand 1.6 blocks behind the player.

## `jolt_side`

A loud bang sounds 1.8 blocks away at 60–100° to one side, and a black figure in one of the ten shapes stands there for this player's client only. It stays unseen until the player looks at it and is gone 10 ticks later, or after 5 s if it is never seen. Nothing happens on the server.

Runs when: there is room to stand 1.8 blocks to the side.

## `jolt_dark`

A black figure in one of the ten shapes stands 2 blocks ahead, within 20° of the player's view, in a spot no brighter than light level 4, and is shown only to this player's client at the same instant as a loud bang. It is gone 8 ticks later. Nothing happens on the server.

Runs when: there is a dark spot to stand 2 blocks ahead.

## `jolt_lunge`

Out of sight, 8–12 blocks away, in a spot no brighter than light level 6 behind a wall or a corner, a faint rustle sounds every 2 s for up to 30 s. If the player comes within 4.5 blocks of the spot, a black figure in one of the ten shapes appears there for this player's client only and is at their face (0.7 blocks ahead) 3 ticks later, with a loud bang as it arrives. It is gone 6 ticks after that. If nobody comes near, the rustling simply stops. Nothing happens on the server. This is a jump scare, so it shares the 10 min pause with the other jump scares.

Runs when: there is a dark spot to stand 8–12 blocks away that the player cannot see.

## `mined_face`

The next time the player breaks a block at eye level in a wall (underground, within 6 blocks, with rock two blocks deep behind it), the rock behind the hole shows as open space for 8 ticks, and a bare black face with pale eyes looks out of it, with a loud bang. Only this player's client is told. This is a jump scare, so it shares the 10 min pause with the other jump scares. If the player breaks no such block within 5 min, it lapses.

Runs when: the player is underground and no other waiting scare is marked for them.

## `dig_down`

The next time the player breaks a floor block beside them (1–4.5 blocks off, not the one under their feet, with rock two blocks deep under it), the rock below shows as open for 8 ticks, and a black figure lies in the hole on its back, face up, with a loud bang. Only this player's client is told. This is a jump scare. It lapses after 5 min.

Runs when: the player is underground and no other waiting scare is marked for them.

## `eating`

The next meal the player finishes ends with a loud bang and a black figure 1.3 blocks ahead of them for 10 ticks. Only this player's client is told. This is a jump scare. It lapses after 5 min.

Runs when: no other waiting scare is marked for the player.

## `container_close`

A chest, barrel, furnace or crafting table lies within 6 blocks of the player. When the player closes any container that they opened and kept open for 3 s, a black figure stands 1.3 blocks ahead for 10 ticks, with a loud bang. Only this player's client is told. This is a jump scare, and the event watches for 5 min.

Runs when: a chest, barrel, furnace or crafting table lies within 6 blocks.

## `slow_turn`

A black figure stands 24 blocks ahead with its back turned. Each time the player looks at it and then away, it has turned 45° more. After four turns it faces them, and the next time they look away it is gone from there and stands 1.6 blocks behind them with a loud bang, unseen until they turn. It is gone if they walk within 8 blocks of it, or after 60 s. Only this player's client is told. This is a jump scare.

Runs when: there is room to stand 24 blocks ahead.

## `echo_answer`

In a cave (no sky overhead), each block the player breaks is answered 1.5 s later by the same break sound, first from 20 blocks away, then from 12, then from 6, and the fourth comes from 1.5 blocks right behind them. The event lapses after 5 min.

Runs when: the player is underground and no other waiting scare is marked for them.

## `name_left_right`

A whisper sounds 2 blocks to one side every 1.5 s. Once the player has turned 45° toward it, a black figure stands 1.5 blocks away on the other side for 8 ticks, with a loud bang. Only this player's client is told. This is a jump scare, and the event gives up after 20 s.

Runs when: at any time.

## `door_slam_face`

A door lies within 4 blocks of the player. When a door that was open shuts while the player is within 3.5 blocks of it, the door shows open again for 10 ticks with a black figure standing in it, and a loud bang sounds. Only this player's client is told. This is a jump scare, and the event watches for 5 min.

Runs when: a door lies within 4 blocks.

## `first_light`

The next torch the player puts up in a dark cave (light level 3 or less at their feet, no sky) lights a black figure standing within 6 blocks ahead of them for 4 ticks, with no sound. Only this player's client is told. This is a jump scare. It lapses after 5 min.

Runs when: the player is underground and no other waiting scare is marked for them.

## `hide`

**Harmful.**
The action bar says "Hide" and a heartbeat sounds every second for 10 s. If the player moves more than 1 block, a black figure that was out of sight is at their face 4 ticks later with a loud bang and hits for 4 (it never takes them below the safe floor of the threat band). If they keep still, four heavy steps pass outside. This is a harmful event, so a lit omen candle warns of it.

Runs when: the player is underground, their health is above the safe floor and a margin, and there is room to stand 5 blocks away.

## `overnight_shrine`

A row of bone blocks with two skulls and lit candles on top, and a sign with a few words in front of it, appears. In its phantom form only this player's client is told of it, the server keeps open ground, and interacting with it shows the real air; it is gone after 10 min. With the swaps setting `REAL`, or under hardcore, the bone, skulls and candles are real blocks, which are put back after that time, or never under hardcore.

Runs when: the player is in the Overworld, there is open ground for a 3×3 shrine 6–14 blocks away, and there is a sign text the spirit has not used.

## `ash_sign`

Ash is laid as a ring of radius 3 or as the initial of the spirit's name, readable from the player's side (only cells on bare floor, at least 70%). It is swept away after 5 min, and under hardcore it stays.

Runs when: the player is under a roof and there is bare floor ahead for the shape (3 blocks away, within 25° of the view).

## `screen_handprint`

A pale handprint appears over the player's view for 3.5 s (peak 80%), quick to come in and slow to fade out. It is a client-side overlay under the HUD.

Runs when: at any time.

## `screen_frost`

Frost spreads along the screen's edges for 8 s (peak 70%), coming in over 2.4 s and going out over 4 s. It is a client-side overlay.

Runs when: at any time.

## `screen_face`

A pale face appears over the view for 2 ticks (90%). Its weight is 0, so it never runs until a player raises it.

Runs when: at any time.

## `eyes_in_dark`

A pair of eyes hangs 1.5 blocks above a dark spot for 2–3 s, blinks once and goes out. It is a client-only particle and is fully lit.

Runs when: there is room to stand 7–14 blocks ahead (within 60°) with a light level of at most 3.

## `frost_prints`

Up to 3 frost handprints appear one by one, 1.5 s apart, on the outer face of the glass, each with a tap on the glass. Each is removed after 2 min, and under hardcore they stay.

Runs when: the player is indoors and there is glass or a glass pane within 8 blocks with a free block on its far side from the player.

## `frost_writing`

A phrase (`let me in`, `cold`, `help`, `open`) forms word by word, 2 s apart, one word per window, and reads left to right from the player's side. Each word is removed after 2 min, and under hardcore they stay.

Runs when: the player is indoors and a run of glass within 8 blocks, long enough for a phrase, has free blocks on the player's side.

## `doll`

A doll appears and, whenever it is not watched, turns to face the player and hops to another spot, up to 4 times. It keeps still while watched, and it is gone after 2 min or when broken.

Runs when: a spot of free floor lies under a roof within 10 blocks, at least 4 blocks away and outside 55° of where the player looks.

## `armor_stand`

**Harmful.**
A real armor stand with arms, in one of four poses (reaching, arms raised, head lolling, lunging), stands facing the player. Every 0.2 s it steps up to 0.6 blocks toward them (climbing 1, dropping up to 3), but only while they are over 2 blocks off and not looking within 50° of it with a clear line; one move in four strikes a new pose. It breaks at the first blow and drops nothing. After 2 min it falls apart with plank particles (under hardcore it stays where it ended up).

Runs when: no such stand exists yet and there is room to stand 8 blocks away at 150–180° to either side of where the player looks, which is behind them (up to 16 bearings are tried).
