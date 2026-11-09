import Image from "next/image";
import Link from "next/link";

type Spirit = [name: string, id: string, description: string];
type EventGroup = [name: string, id: string];
type Locale = "en" | "ru";

const copy = {
  ru: {
    eyebrow: "Restless / Полевой справочник",
    tagline: "Здесь вы никогда не одни.",
    intro: "Хоррор-мод для Minecraft. Гаснет факел. За спиной открывается дверь. Чьи-то шаги идут за вами до самого дома.",
    start: "Начать выживание", identify: "Узнать своего духа",
    note: "Заметка исследователя", journal: "Сначала — наблюдай.",
    journalText: "У каждого духа свои признаки. Записывайте улики, изучайте его привычки и готовьте защиту.",
    journalLink: "Открыть журнал", edition: "Minecraft 26.3 / NeoForge",
    route: "01 / Первые шаги", routeTitle: "От шороха до ритуала",
    steps: [
      ["Заметить", "Узнайте, как дух набирает силу и почему становится опаснее.", "how-it-works", "spirit_candle"],
      ["Исследовать", "Соберите улики с помощью камеры, пепла и спирит-бокса.", "items/tools#reading-clues", "camera_obscura"],
      ["Защититься", "Подготовьте соль, обереги и безопасное место для сна.", "items/tools#salt", "salt"],
      ["Изгнать", "Выберите подношение и проведите ритуал у алтаря.", "items/tools#altar", "exorcists_censer"],
    ],
    archive: "02 / Атлас духов", archiveTitle: "У присутствия есть имя",
    archiveText: "Узнайте знакомые признаки. Изучите того, кто поселился рядом.",
    personal: "Следуют за вами", personalNote: "Личные духи. У каждого игрока свои.",
    territorial: "Привязаны к месту", territorialNote: "Действуют на любого, кто подойдёт близко.",
    essence: "Эссенция", allSpirits: "Всё о духах",
    events: "03 / Проявления", eventsTitle: "740 способов выдать себя",
    eventsText: "Свет, звуки, вещи и чужое присутствие. Найдите то, что случилось с вами.",
    allEvents: "Все ивенты", spoils: "После встречи остаётся след",
    spoilsText: "Эссенции духов становятся оберегами, расходниками и реликвиями.",
    rewards: [["Эссенции", "essences"], ["Обереги", "charms"], ["Усиленные обереги", "advanced"], ["Расходники", "consumables"], ["Реликвии", "relics"]],
  },
  en: {
    eyebrow: "Restless / Field guide",
    tagline: "You are never alone here.",
    intro: "A horror mod for Minecraft. A torch goes out. A door opens behind you. Someone’s footsteps follow you all the way home.",
    start: "Start surviving", identify: "Know your spirit",
    note: "Researcher’s note", journal: "First, observe.",
    journalText: "Every spirit has its signs. Record the clues, learn its habits, and prepare your defenses.",
    journalLink: "Open the journal", edition: "Minecraft 26.3 / NeoForge",
    route: "01 / First steps", routeTitle: "From a whisper to a ritual",
    steps: [
      ["Notice", "Learn how a spirit gains power and grows more dangerous.", "how-it-works", "spirit_candle"],
      ["Investigate", "Gather clues with a camera, ash, and a spirit box.", "items/tools#reading-the-clues", "camera_obscura"],
      ["Protect", "Prepare salt, wards, and a safe place to sleep.", "items/tools#salt", "salt"],
      ["Banish", "Choose an offering and perform a ritual at the altar.", "items/tools#altar", "exorcists_censer"],
    ],
    archive: "02 / Spirit atlas", archiveTitle: "The presence has a name",
    archiveText: "Recognize the signs. Learn what has settled nearby.",
    personal: "They follow you", personalNote: "Personal spirits. Each player has their own.",
    territorial: "Bound to a place", territorialNote: "They act on anyone who comes near.",
    essence: "Essence", allSpirits: "All about spirits",
    events: "03 / Manifestations", eventsTitle: "740 ways to give themselves away",
    eventsText: "Lights, sounds, belongings, and a presence nearby. Find what happened to you.",
    allEvents: "All events", spoils: "Every encounter leaves a trace",
    spoilsText: "Spirit essences become charms, consumables, and relics.",
    rewards: [["Essences", "essences"], ["Charms", "charms"], ["Advanced charms", "advanced"], ["Consumables", "consumables"], ["Relics", "relics"]],
  },
};

function ItemArt({ name, size, alt = "" }: { name: string; size: number; alt?: string }) {
  return <Image className="item-art" src={`/images/items/${name}.png`} width={size} height={size} alt={alt} />;
}

function SpiritRoster({ lang, title, note, spirits }: { lang: Locale; title: string; note: string; spirits: Spirit[] }) {
  return (
    <section className="spirit-roster">
      <div className="roster-heading"><h3>{title}</h3><span className="roster-count">{spirits.length.toString().padStart(2, "0")}</span></div>
      <p className="roster-note">{note}</p>
      <ul className="spirit-grid">
        {spirits.map(([name, id, description]) => (
          <li key={id}>
            <Link className="spirit-card" href={`/${lang}/spirits#${id}`}>
              <ItemArt name={`${id.replaceAll("-", "_")}_essence`} size={48} alt={`${copy[lang].essence}: ${name}`} />
              <span><span className="spirit-name">{name}</span><span className="spirit-description">{description}</span></span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

export function WikiHome({ lang, personal, territorial, events }: { lang: Locale; personal: Spirit[]; territorial: Spirit[]; events: EventGroup[] }) {
  const t = copy[lang];
  return (
    <div className="home">
      <header className="home-hero">
        <div className="hero-copy">
          <p className="home-eyebrow">{t.eyebrow}</p>
          <h1>Restless</h1>
          <p className="hero-tagline">{t.tagline}</p>
          <p className="home-lede">{t.intro}</p>
          <div className="hero-actions">
            <Link className="home-button home-button-primary" href={`/${lang}/how-it-works`}>{t.start}</Link>
            <Link className="home-button" href={`/${lang}/spirits`}>{t.identify}</Link>
          </div>
          <p className="hero-edition">{t.edition}</p>
        </div>
        <aside className="field-note">
          <p className="home-eyebrow">{t.note}</p>
          <div className="journal-art"><ItemArt name="journal" size={160} /></div>
          <h2>{t.journal}</h2>
          <p>{t.journalText}</p>
          <Link className="home-text-link" href={`/${lang}/items/tools#journal`}>{t.journalLink}<span aria-hidden="true"> ↗</span></Link>
        </aside>
      </header>

      <section className="home-section" aria-labelledby="route-title">
        <p className="home-eyebrow">{t.route}</p>
        <h2 id="route-title">{t.routeTitle}</h2>
        <div className="route-grid">
          {t.steps.map(([name, description, path, art], i) => (
            <Link className="route-card" href={`/${lang}/${path}`} key={path}>
              <div className="route-top"><ItemArt name={art} size={48} /><span className="route-number">0{i + 1}</span></div>
              <h3>{name}</h3><p>{description}</p>
              <span className="route-arrow" aria-hidden="true">↗</span>
            </Link>
          ))}
        </div>
      </section>

      <section className="home-section" aria-labelledby="atlas-title">
        <p className="home-eyebrow">{t.archive}</p>
        <div className="section-heading"><h2 id="atlas-title">{t.archiveTitle}</h2><Link className="home-text-link" href={`/${lang}/spirits`}>{t.allSpirits} <span aria-hidden="true">↗</span></Link></div>
        <p className="section-intro">{t.archiveText}</p>
        <div className="home-rosters">
          <SpiritRoster lang={lang} title={t.personal} note={t.personalNote} spirits={personal} />
          <SpiritRoster lang={lang} title={t.territorial} note={t.territorialNote} spirits={territorial} />
        </div>
      </section>

      <section className="home-section manifestations" aria-labelledby="events-title">
        <p className="home-eyebrow">{t.events}</p>
        <h2 id="events-title">{t.eventsTitle}</h2>
        <p className="section-intro">{t.eventsText}</p>
        <div className="event-links">{events.map(([name, id]) => <Link href={`/${lang}/events/${id}`} key={id}>{name}</Link>)}</div>
        <Link className="home-text-link" href={`/${lang}/events`}>{t.allEvents} <span aria-hidden="true">↗</span></Link>
      </section>

      <section className="home-spoils" aria-labelledby="spoils-title">
        <h2 id="spoils-title">{t.spoils}</h2>
        <p>{t.spoilsText}</p>
        <div className="reward-links">{t.rewards.map(([name, id]) => <Link className="home-text-link" href={`/${lang}/items/${id}`} key={id}>{name} <span aria-hidden="true">↗</span></Link>)}</div>
      </section>
    </div>
  );
}
