# Ideas

Backlog of ideas, added by hand. Move an idea out (or delete it) once it is implemented.


- Development plan for the agent orchestrator (spirit model, rituals, mid game, sound, curation): [development-plan.md](development-plan.md).
- 100 haunt event ideas: [event-ideas.md](event-ideas.md).
- Roadmap past the event backlog (procedural spirits, clues, banishing, quests, spread, binding, the Other Side): [design.md](design.md).
- Personal spirit births from the player's deeds (slaughter, death far from home, a pet's death and more): [births.md](births.md).
- Second batch of event and spirit ideas with 1–10 difficulty: [event-ideas-2.md](event-ideas-2.md).
- Third batch, about 330 ideas toward 500 events, with signatures per spirit type: [event-ideas-3.md](event-ideas-3.md).
- Hardcore mode (temporary and visual effects become permanent and real), plan only: [hardcore.md](hardcore.md).
- 100 scenarios, scripted mixes of existing events for older and evolved spirits: [scenarios.md](scenarios.md).
- Signature events, three or four per spirit type that only it rolls, all built: [signature-events.md](signature-events.md).
- From phase 1 (T1–T7), left open:
  - The mirror (T5) unmasks only `Swaps` fakes and `DoubleEvent` doubles; the ~30 `PacketEntity` fakes and real
    swaps (hardcore, `REAL` setting) ignore it.
  - Holy water (T5) ends only `Scheduler` steps of a malice spirit, not `Restore` timers or spawned entities.
  - GameTests are order/placement flaky: `telekinesis_amulet_pulls_experience`, `signature_revenant_tally` and
    `signature_revenant_rises` fail on some runs and pass on others.
  - Grave bones (I3) are laid once: if the bones item is destroyed, a REMAINS grave stays at stage 2 for good. A
    reading could lay them again when no copy is found in the world.
  - A grave's letter (I3) goes to any villager, not one in the nearest village.
