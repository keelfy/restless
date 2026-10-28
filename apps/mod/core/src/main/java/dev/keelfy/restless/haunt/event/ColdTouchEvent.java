package dev.keelfy.restless.haunt.event;

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
        return player.getHealth() > 2.0F;
    }

    @Override
    public void run(ServerPlayer player) {
        float damage = Math.min(2.0F + player.getRandom().nextInt(3), player.getHealth() - 1.0F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.0F, 0.6F);
        player.hurtServer(player.level(), player.damageSources().magic(), damage);
    }
}
