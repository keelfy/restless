import type { NextConfig } from "next";
import nextra from "nextra";

const withNextra = nextra({});

const config: NextConfig = {
  output: "export",
  images: { unoptimized: true },
  agentRules: false,
};

export default withNextra(config);
