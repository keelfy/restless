import Link from "next/link";

import { Button } from "@/components/ui/button";

export default function Home() {
  return (
    <section className="flex flex-col items-center gap-6 py-24 text-center">
      <h1 className="text-5xl font-bold tracking-tight">Restless</h1>
      <p className="text-xl text-muted-foreground">Spirits that won&apos;t let you rest</p>
      <Button asChild size="lg">
        <Link href="/spirits">Spirits</Link>
      </Button>
    </section>
  );
}
