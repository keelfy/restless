import type { NextConfig } from "next";
import nextra from "nextra";

const withNextra = nextra({ unstable_shouldAddLocaleToLinks: true });

const config: NextConfig = {
  output: "export",
  i18n: { locales: ["en", "ru"], defaultLocale: "en" },
  images: { unoptimized: true },
  agentRules: false,
};

export default withNextra(config);
