export const locales = ["en", "ru"] as const;
export type Locale = (typeof locales)[number];

export const dictionary = {
  en: {
    description: "Wiki for Restless, a Minecraft horror mod",
    footer: "Restless wiki",
    toc: "On this page",
    search: "Search...",
    edit: "Edit this page",
  },
  ru: {
    description: "Вики Restless, хоррор-мода для Minecraft",
    footer: "Вики Restless",
    toc: "На этой странице",
    search: "Поиск...",
    edit: "Править страницу",
  },
} satisfies Record<Locale, Record<string, string>>;

export function isLocale(value: string): value is Locale {
  return (locales as readonly string[]).includes(value);
}
