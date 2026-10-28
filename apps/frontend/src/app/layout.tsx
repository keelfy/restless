import type { Metadata } from "next";
import Link from "next/link";

import "./globals.css";

export const metadata: Metadata = {
  title: "Restless",
  description: "Wiki for Restless, a Minecraft horror mod",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en-US">
      <body>
        <header className="border-b">
          <nav className="mx-auto flex h-14 max-w-3xl items-center gap-6 px-6 text-sm">
            <Link href="/" className="font-semibold tracking-wide">
              Restless
            </Link>
            <Link href="/spirits" className="text-muted-foreground hover:text-foreground">
              Spirits
            </Link>
          </nav>
        </header>
        <main className="mx-auto max-w-3xl px-6 py-12">{children}</main>
      </body>
    </html>
  );
}
