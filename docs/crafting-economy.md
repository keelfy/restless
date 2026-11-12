# Crafting economy

Target recipes for every crafted item. Implemented. Shaped where the item has a shape; consumables and
upgrades stay shapeless. Resources follow progression: the altar and the first banishing are possible before the
Nether, nothing past the day-one tools is free.

## Intermediate material: Hallowed Wax (`hallowed_wax`)

- **Recipe:** honeycomb + salt + ash → 2, shapeless.
- **Why:** honeycomb needs bees and a campfire, the first step from vanilla into the mod's craft.
- **Used in:** mid-tier tools and **every base charm** (1 wax, the "seal"). Not in upgrades or consumables.
- **Art:** delegated to the `chatgpt-6.1-sol` harness.

## Progression tiers

| Tier | Gate | Items |
|---|---|---|
| Day one | wood, sand, kelp, charcoal | salt, ash, journal, mirror, ouija board, door charm, dreamcatcher, scarecrow |
| Bees, copper, amethyst | early mid game | hallowed wax, spirit box, camera obscura, **altar**, reliquary, effigy |
| Nether | soul sand, glowstone | spirit candle, hallowed lantern |
| Essence | after the first banishing | charms, consumables |
| Mid to late game | catalysts | advanced charms |
| Late game | echo shard, totem, shulker shell, creaking heart | relics |

## Tools

| Item | Recipe |
|---|---|
| salt | sand + dried kelp → 2, shapeless |
| ash | **campfire cooking**: charcoal → 4 |
| hallowed_wax | honeycomb + salt + ash → 2, shapeless |
| journal | book + salt + ink sac, shapeless |
| mirror | shaped `NPN`: N iron nugget, P glass pane |
| ouija_board | shaped `DBD / PPP`: D black dye, B bone (planchette), P `#planks` |
| door_charm | paper + hallowed wax + salt → 2, shapeless |
| dreamcatcher | shaped `" K " / TST / FTF`: K stick, T string, S salt, F feather |
| scarecrow | shaped `" P " / SHS / " K "`: P carved pumpkin, S salt, H hay block, K stick |
| spirit_box | shaped `" R " / CNC / CAC`: R lightning rod (antenna), C copper ingot, N note block, A amethyst shard |
| camera_obscura | shaped `" B " / CAC / CCC`: B black wool (hood), C copper ingot, A amethyst shard (lens) |
| altar | shaped `WGW / SAS / BBB`: W hallowed wax, G gold ingot (bowl), S salt, A amethyst block, B chiseled stone bricks |
| spirit_candle | candle + hallowed wax + soul sand, shapeless |
| hallowed_lantern | shaped `WGW / SLS`: W hallowed wax, G glowstone dust, S salt, L lantern |
| effigy | scarecrow + candle + hallowed wax, shapeless |
| reliquary | shaped `GAG / WBW / GGG`: G gold ingot, A amethyst shard, W hallowed wax, B book |
| keepers_key_rebind | Keeper's Key + campfire, shapeless |

## Charms

Shaped. Each is its essence (E) + a themed vanilla part + 1 hallowed wax (W).

| Charm | Essence | Other ingredients |
|---|---|---|
| Telekinesis Rod | poltergeist | 2 gold ingots, laid as a diagonal rod |
| Dark Lens | shade | ender eye + 2 tinted glass |
| Drowned Stone | nix | nautilus shell + 2 prismarine shards |
| Phantom Double | phantom | 3 phantom membranes |
| Borrowed Face | mimic | 3 leather + 2 string (mask) |
| Wraith's Breath | wraith | 3 cobwebs (shroud) |
| Borrowed Dream | mara | honey bottle + 2 feathers (pillow) |
| Death Wail | banshee | bell + 2 string (necklace) |
| Grudge | revenant | 3 iron ingots (ring) |
| Lost Step | lost one | lantern + compass |
| House Key | domovoi | 2 copper ingots (key) |
| Stash | hoarder | bundle + emerald, shapeless |
| Bargain | demon | blaze rod + 2 gold ingots |
| Trickster's Coin | trickster | 7 gold nuggets (coin) |

## Advanced charms

Shapeless: base charm + essence + **catalyst**. Needs a `catalyst` field in `UpgradeRecipe`.

| Advanced charm | Base | Essence | Catalyst |
|---|---|---|---|
| Black Bargain | Bargain | wraith | crying obsidian |
| Blood Debt | Grudge | demon | ghast tear |
| Changeling Guise | Borrowed Face | **boggart** | fermented spider eye |
| Devil's Coin | Trickster's Coin | demon | gold block |
| Fetch's Path | Lost Step | phantom | ender pearl |
| Grave Satchel | Stash | lost one | echo shard |
| Hearth Dream | Borrowed Dream | domovoi | campfire |
| Keeper's Key | House Key | hoarder | iron door |
| Magpie's Hook | Telekinesis Rod | hoarder | fishing rod |
| Nightwalk Shroud | Wraith's Breath | shade | closed eyeblossom |
| Requital Wail | Death Wail | revenant | goat horn |
| Tidecaller | Drowned Stone | lost one | heart of the sea |
| Truthseer's Eye | Dark Lens | banshee | spyglass |
| Twin's Mirror | Phantom Double | mimic | mirror |

Changeling Guise takes boggart essence, which had no use outside the reliquary. An illusion that turns one mob on
another fits the boggart; no new item or art is needed.

## Consumables

Shapeless. Yield rule: **×4** by default; drinks yield one per vessel in the recipe; Infernal Contract ×1; Mimic Clay
×8.

| Consumable | Recipe |
|---|---|
| Restless Knot | poltergeist essence + string + 2 iron chains → 4 |
| Dusk Salt | shade essence + 2 salt + ash → 4 |
| Still Water | nix essence + 2 holy water + prismarine shard → 2 |
| Sighting Mark | phantom essence + paper + glowstone dust → 4 |
| Mimic Clay | mimic essence + 4 clay balls → 8 |
| Wraith Oil | wraith essence + 2 glass bottles + ink sac → 2 |
| Dreamless Draught | mara essence + 3 honey bottles + poppy → 3 |
| Omen Candle | banshee essence + 4 candles + bone meal → 4 |
| Bloodied Rag | revenant essence + 2 rotten flesh + red dye → 4 |
| Wayfinder's Ash | lost one essence + ash + bone → 4 |
| Domovoi's Porridge | domovoi essence + 4 bowls + wheat + milk bucket → 4 |
| Magpie Feather | hoarder essence + feather + gold nugget → 4 |
| Infernal Contract | demon essence + paper + blaze powder → 1 |
| Changeling Dust | trickster essence + gold nugget + redstone → 4 |

## Relics

Shaped. Three essences of different types + late-game finds.

| Relic | Essences | Finds |
|---|---|---|
| Hollow Lantern | shade, banshee, domovoi | Hallowed Lantern + 2 echo shards |
| Ferryman's Coin | lost one, nix, trickster | totem of undying + gold block |
| Exorcist's Censer | revenant, demon, wraith | soul lantern + blaze rod + 2 iron chains |
| Hoarder's Hollow | hoarder, poltergeist, mimic | ender chest + shulker shell |
| Mara's Cradle | mara, phantom, lost one | any bed (`#beds`) + creaking heart |

## Scope of the change

- About 60 recipe JSONs and one new item (hallowed wax: registration, model, lang, art).
- `catalyst` field in `UpgradeRecipe`.
- Wiki in en and ru: `items/*.md`, tier rule in `index.md`, yield rule in `consumables.md`.
- Proof: `mise run mod:build` and game tests.
