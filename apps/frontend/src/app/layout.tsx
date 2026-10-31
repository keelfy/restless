import type { Metadata } from "next";
import { Alegreya_Sans, IM_Fell_English } from "next/font/google";
import { Footer, Layout, Navbar } from "nextra-theme-docs";
import { Head } from "nextra/components";
import { getPageMap } from "nextra/page-map";

import "nextra-theme-docs/style.css";
import "./globals.css";

const display = IM_Fell_English({ weight: "400", style: ["normal", "italic"], subsets: ["latin"], variable: "--font-display" });
const body = Alegreya_Sans({ weight: ["400", "500", "700"], subsets: ["latin"], variable: "--font-body" });

export const metadata: Metadata = {
  title: { default: "Restless", template: "%s - Restless" },
  description: "Wiki for Restless, a Minecraft horror mod",
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en-US" dir="ltr" className={`${display.variable} ${body.variable}`} suppressHydrationWarning>
      <Head
        color={{ hue: 177, saturation: 58, lightness: 52 }}
        backgroundColor={{ dark: "#202226", light: "#202226" }}
      />
      <body>
        <Layout
          navbar={<Navbar logo={<span className="logo">Restless</span>} projectLink="https://github.com/keelfy/restless" />}
          pageMap={await getPageMap()}
          docsRepositoryBase="https://github.com/keelfy/restless/tree/master/apps/frontend"
          footer={<Footer>Restless wiki</Footer>}
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
