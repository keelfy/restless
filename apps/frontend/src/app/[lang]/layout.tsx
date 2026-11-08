import type { Metadata } from "next";
import { Alegreya_Sans, EB_Garamond, IM_Fell_English } from "next/font/google";
import { notFound } from "next/navigation";
import { Footer, Layout, Navbar } from "nextra-theme-docs";
import { Head, Search } from "nextra/components";
import { getPageMap } from "nextra/page-map";

import { dictionary, isLocale, locales } from "@/lib/i18n";

import "nextra-theme-docs/style.css";
import "../globals.css";

const display = IM_Fell_English({ weight: "400", style: ["normal", "italic"], subsets: ["latin"], variable: "--font-display" });
const displayRu = EB_Garamond({ weight: "400", style: ["normal", "italic"], subsets: ["cyrillic", "latin"], variable: "--font-display" });
const body = Alegreya_Sans({ weight: ["400", "500", "700"], subsets: ["latin", "cyrillic"], variable: "--font-body" });

export const dynamicParams = false;

export function generateStaticParams() {
  return locales.map((lang) => ({ lang }));
}

type Props = { children: React.ReactNode; params: Promise<{ lang: string }> };

export async function generateMetadata({ params }: Pick<Props, "params">): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) return {};
  return {
    title: { default: "Restless", template: "%s - Restless" },
    description: dictionary[lang].description,
  };
}

export default async function RootLayout({ children, params }: Props) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const t = dictionary[lang];
  return (
    <html lang={lang} dir="ltr" className={`${lang === "ru" ? displayRu.variable : display.variable} ${body.variable}`} suppressHydrationWarning>
      <Head
        color={{ hue: 177, saturation: 58, lightness: 52 }}
        backgroundColor={{ dark: "#202226", light: "#202226" }}
      />
      <body>
        <Layout
          navbar={<Navbar logo={<span className="logo">Restless</span>} projectLink="https://github.com/keelfy/restless" />}
          pageMap={await getPageMap(`/${lang}`)}
          docsRepositoryBase="https://github.com/keelfy/restless/tree/master/apps/frontend"
          footer={<Footer>{t.footer}</Footer>}
          i18n={[
            { locale: "en", name: "English" },
            { locale: "ru", name: "Русский" },
          ]}
          search={<Search placeholder={t.search} />}
          toc={{ title: t.toc }}
          editLink={t.edit}
          nextThemes={{ forcedTheme: "dark", defaultTheme: "dark" }}
          darkMode={false}
          feedback={{ content: null }}
        >
          {children}
        </Layout>
      </body>
    </html>
  );
}
