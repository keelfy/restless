---
name: player-text
description: Style and rules for player-facing text in Restless — item and block tooltips (`item.restless.*.tip`) and the in-game journal (`journal.restless.*`: guide chapters, bestiary, "where to meet", chronicle notes). Use whenever writing, rewriting or adding such text in `ru_ru.json` / `en_us.json`, including when a new item, spirit or journal chapter is added.
---

# Player-facing text

Player text is part of the game, not a chat reply. Never write it in the `caveman` register, even when the rest of
the session uses caveman: no clipped fragments, no colon-chopped lists, no familiar tone. Write full, polished
sentences in Russian and English.

## Files

| What | Where |
|---|---|
| Russian | `apps/mod/core/src/main/resources/assets/restless/lang/ru_ru.json` |
| English | `apps/mod/core/src/main/resources/assets/restless/lang/en_us.json` |
| Event descriptions (not covered here) | `apps/mod/core/src/main/resources/restless-config/lang/{ru,en}.json` |

Every key exists in both files with the same meaning. Keep key order. Keep `%s` / `%1$s` placeholders intact, in the
same count and order.

## Rules for all player text

- **Facts are exact.** Keep every number, radius, duration, charge count, "once a day", "may lie" / "never lies".
  Do not invent mechanics or lore. When unsure, read the code; if still unsure, keep the old meaning literally.
- **Nobody is addressed.** No «ты», «твой», "you", "your", no imperatives («Используй», «Выложи», "Use it", "Hold it").
  Name the person instead: «игрок», «владелец», «спящий», "the player", "the wearer", "the holder", "the sleeper".
- **No key names.** Every key is rebindable, so never write «ПКМ», «ЛКМ», «Shift», "Right-click", "Sneak key". Name
  the action: «при использовании», «крадучись», «при ударе», "when used", "while sneaking". A click inside a mod
  screen (e.g. the journal's archive button) may be called «нажатие» / "clicking".
- **No crafting ingredients.** Recipes are shown by the game and the journal's recipe line
  (`journal.restless.recipe`). Text describes behaviour only.
- **Russian grammar.** Use «ё». Participles and adjectives agree with the gender of the item name (check the item's
  name key: «Соль» is feminine, so «Выложенная кольцом, соль…»; «Оберег» is masculine, so «Прикреплённый к блоку…»).
- **RU and EN match.** Same facts, same voice. If one language gains a detail, add it to the other.

## Item and block tooltips — `item.restless.<id>.tip`

One short line under the item name (`Content.tipped`). The journal also shows it on the item's codex page
(`Codex.pages`). Neutral and impersonal, like a reference book: it describes what the item does, not what the player
should do.

Open with the condition, then the effect:

| Situation | RU | EN |
|---|---|---|
| Used from hand | «При использовании …» | "When used, it …" |
| Worn charm | «Будучи надетым, …» | "While worn, it …" |
| Held | «Находясь в руке, …» / «Когда свеча в руке, …» | "While held, …" |
| Placed | «Выложенная кольцом, …», «Подвешенный, …» | "When placed in a ring, …", "When hung, …" |
| Drunk | «Выпитый перед сном, …» | "When drunk before bed, it …" |
| Limits | «Срабатывает раз в сутки.», «Ломается после 5 срабатываний.» | "Works once a day.", "Breaks after 5 triggers." |

Length: one or two sentences, about 60–140 characters. Do not explain lore here.

Examples:

- «При использовании делает снимок, показывающий тип духа поблизости.» / "When used, it takes a photo that shows the
  type of a nearby spirit."
- «Будучи надетым, отбрасывает врагов, когда здоровье падает ниже 3 сердец. Срабатывает раз в сутки.» / "While worn,
  it throws hostile mobs back once health drops below 3 hearts. Works once a day."
- «При использовании открывает слот на один стак. Лёжа в инвентаре, переживает смерть владельца.» / "When used, it
  opens a slot for one stack. Kept in the inventory, it survives the owner's death."

Bad: «Используй: 25 секунд враждебные мобы принимают тебя за своего.», «ПКМ: открывает слот…»,
«Запечатывает обереги. Соты, соль и пепел.»

## Journal — `journal.restless.*`

The journal (`journal/JournalPages.java`, `journal/Codex.java`) has two voices.

### Reference entries: impersonal, like tooltips

- `bestiary.<kind>`: what the spirit kind does and how dangerous it is, in one to three sentences.
  `bestiary.<kind>.name` is the kind's title. `bestiary.grown` is the "grows into" line.
  Example: «Садится на спящих. Ночью она опаснее, а опаснее всего становится, когда игрок лежит в постели.»
- `met.<kind>`: where the kind is found and how to lure it. Keep the label «Где встретить:» / "Where to meet:", then a
  full sentence. Example: «Где встретить: встречается в лесных особняках и руинах троп. Намеренно привлечь духа можно
  чучелом-приманкой — пугалом, набитым его эссенцией и подожжённым.»
- Item codex pages reuse the `.tip` line; do not write a second description.

### Guide chapters: the hunter's diary

`guide.<chapter>.title` and `guide.<chapter>.1 … N`. Chapters are `Notebook.Chapter`; the number of paragraphs is
the enum's `pages` value (e.g. `WEAKNESS(5)` reads `guide.weakness.1` to `.5`). Adding a paragraph means raising
that number; removing one means lowering it.

The guide is written in first person by an experienced spirit hunter. Use the past tense for what the narrator saw
and the present tense for the rules that still hold. The tone is calm, a little grim and precise, like an old field
journal, in full flowing sentences. The narrator never speaks to the reader: no «ты», "you" or imperatives, and no
imperative titles. «Прочти сначала» became «Прежде всего», and «Твоя голова» became «Рассудок».

Example: «Поначалу они слабы, но чем дольше их оставляют в покое, тем сильнее становятся; это я узнал на собственной
шкуре. Кое-чего я так и не сумел понять, поэтому часть страниц осталась пустой.»

Mechanics in the guide follow the same fact rule. If the narrator says something works, the code must agree.

### Short labels and chronicle notes

`contents`, `section.*`, `tab.*`, `may_lie`, `unknown_*`, `recipe`, and the chronicle templates `note.*`
(e.g. «%s (%s) упокоен.», «Доска Уиджи: %s.»). Keep them short and neutral, in short phrases or plain sentences.
Change them only when they are clumsy or address the player.

`JournalPages` wraps long entries onto the next page, so length is not a hard limit. Still keep a rewrite within about
1.5x of the old text.

### Formatting: lists and bold (journal only)

`JournalView.marked` draws light markup in journal entries, so a page can be scanned instead of read as a wall of text:

- `\n` starts a new line. A line that starts with `• ` is a list item: drawn with a dot and a hanging indent, kept
  tight under the line before it. A plain line after a list gets a paragraph gap.
- `**word**` is bold. The marks must come in pairs.

Use it sparingly; most paragraphs stay plain prose.

- **Lists** only for real enumerations of three or more parallel items: the four natures and their weaknesses, signs
  per nature, prints in ash, ritual steps, consequences, tools. Open the list with a full sentence ending in a colon.
  Items start lowercase and end with «;» / ";", the last one with «.». In a label-style list the bold name comes
  first, followed by a colon: «• **Тьма**: тянется к тьме, холоду и ночи;».
- **Bold** only for the terms a reader looks for: nature names, tool and item names when they are introduced, the altar
  forms, and the labels «Где встретить:» / «Вырастает в:». Use at most about three bold spots per paragraph, never
  whole sentences.
- Never use markup in item tooltips (`.tip`): the tooltip line and the journal's item pages render it raw. Markup is
  allowed only in `journal.restless.*` keys.
- Russian and English carry the same lists and the same bold terms.

## Checks before finishing

```bash
cd apps/mod/core/src/main/resources/assets/restless/lang
python3 -c "import json,sys;[json.load(open(f)) for f in sys.argv[1:]]" ru_ru.json en_us.json
grep -nE '"(item|journal)\.restless\.[^"]*": "[^"]*(ПКМ|ЛКМ|Shift|[Cc]lick|Используй|Выложи| ты | тебя| твой| твоя| you | your )' ru_ru.json en_us.json
python3 -c "
import json,sys
for f in sys.argv[1:]:
    d=json.load(open(f))
    print(f,[k for k,v in d.items() if v.count('**')%2 or (('**' in v or '\n' in v) and not k.startswith('journal.'))])
" ru_ru.json en_us.json
```

The last command must print empty lists: every `**` is paired, and markup appears only in journal keys.

The grep must print nothing, except for false positives you have checked. If the change also alters player-visible
behaviour, update the wiki (`apps/frontend/content/{en,ru}`) as `AGENTS.md` requires. A pure rewording does not
need a wiki change.
