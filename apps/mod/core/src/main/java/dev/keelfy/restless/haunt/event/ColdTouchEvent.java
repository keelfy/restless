package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Damages the player but never kills: lethal events come later and must be telegraphed. */
public final class ColdTouchEvent implements HauntEvent {
    @Override
    public String id() {
        return "cold_touch";
    }

    @Override
    public HauntTier tier() {
        return HauntTier.HARMFUL;
    }

    @Override
    public Set<EventTag> tags() {
        return Set.of(EventTag.ASSAULT);
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return player.getHealth() > Config.event("cold_touch").num("min_health");
    }

    @Override
    public void run(ServerPlayer player) {
        Config.Section config = Config.event("cold_touch");
        float damage = Math.min(config.integer("damage_min") + player.getRandom().nextInt(config.integer("damage_spread")),
            player.getHealth() - (float) config.num("keep_health"));
        Config.Tone tone = config.tone("sound");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, tone.volume(), tone.pitch());
        player.hurtServer(player.level(), player.damageSources().magic(), damage);
    }
}
