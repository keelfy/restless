import type { Metadata } from "next";
import { Footer, Layout, Navbar } from "nextra-theme-docs";
import { Head } from "nextra/components";
import { getPageMap } from "nextra/page-map";

import "nextra-theme-docs/style.css";

export const metadata: Metadata = {
  title: { default: "Restless", template: "%s - Restless" },
  description: "Wiki for Restless, a Minecraft horror mod",
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en-US" dir="ltr" suppressHydrationWarning>
      <Head />
      <body>
        <Layout
          navbar={<Navbar logo={<b>Restless</b>} projectLink="https://github.com/keelfy/restless" />}
          pageMap={await getPageMap()}
          docsRepositoryBase="https://github.com/keelfy/restless/tree/master/apps/frontend"
          footer={<Footer>Restless wiki</Footer>}
        >
          {children}
        </Layout>
      </body>
    </html>
  );
}
