package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The artifact of a spirit's essence: using it gives its holder a few effects, and some also touch the hostile things
 * around. Poltergeist and shade have artifacts of their own ({@link TelekinesisAmulet}, {@link DarkEye}); every other
 * type is here, one {@link Kind} each, crafted from the essence the spirit leaves.
 */
public final class Relic extends Item {
    /** One effect; the level is how far above the first it goes. */
    public record Fx(Holder<MobEffect> effect, int seconds, int level) {
        MobEffectInstance instance() {
            return new MobEffectInstance(effect, seconds * 20, level);
        }
    }

    /**
     * @param self what the holder gets
     * @param around what every hostile creature in reach gets
     * @param one only one of {@code self}, drawn at random, instead of all of them
     * @param cleanse the holder is rid of every harmful effect first
     */
    public record Spec(List<Fx> self, List<Fx> around, boolean one, boolean cleanse) {}

    /** @param id the item; its essence is {@code <spirit>_essence} */
    public record Kind(String id, String spirit, Spec spec) {}

    private static Spec self(Fx... fx) {
        return new Spec(List.of(fx), List.of(), false, false);
    }

    private static Fx fx(Holder<MobEffect> effect, int seconds, int level) {
        return new Fx(effect, seconds, level);
    }

    /** Each spirit's gift answers what it is: the nix gives the water, the demon strength at a price, and so on. */
    public static final List<Kind> ALL = List.of(
            new Kind(
                    "tide_charm",
                    "nix",
                    self(fx(MobEffects.WATER_BREATHING, 180, 0), fx(MobEffects.DOLPHINS_GRACE, 180, 0))),
            new Kind("mimic_skin", "mimic", self(fx(MobEffects.ABSORPTION, 60, 2))),
            new Kind(
                    "wraith_fang",
                    "wraith",
                    self(
                            fx(MobEffects.SPEED, 30, 1),
                            fx(MobEffects.STRENGTH, 30, 0),
                            fx(MobEffects.NIGHT_VISION, 30, 0))),
            new Kind(
                    "mara_lullaby",
                    "mara",
                    new Spec(List.of(fx(MobEffects.REGENERATION, 20, 1)), List.of(), false, true)),
            new Kind(
                    "revenant_brand",
                    "revenant",
                    self(fx(MobEffects.STRENGTH, 15, 1), fx(MobEffects.RESISTANCE, 15, 1))),
            new Kind(
                    "hearth_charm",
                    "domovoi",
                    self(fx(MobEffects.SATURATION, 10, 0), fx(MobEffects.REGENERATION, 10, 0))),
            new Kind(
                    "trickster_coin",
                    "trickster",
                    new Spec(
                            List.of(
                                    fx(MobEffects.SPEED, 30, 1),
                                    fx(MobEffects.JUMP_BOOST, 30, 2),
                                    fx(MobEffects.LUCK, 60, 1),
                                    fx(MobEffects.INVISIBILITY, 30, 0),
                                    fx(MobEffects.REGENERATION, 15, 1),
                                    fx(MobEffects.SLOWNESS, 10, 1),
                                    fx(MobEffects.BLINDNESS, 5, 0)),
                            List.of(),
                            true,
                            false)));

    private final Spec spec;

    public Relic(Item.Properties properties, Spec spec) {
        super(properties);
        this.spec = spec;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Config.Section config = Config.section("artifacts");
        if (spec.cleanse()) {
            for (MobEffectInstance active : new ArrayList<>(player.getActiveEffects())) {
                if (active.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    player.removeEffect(active.getEffect());
                }
            }
        }
        List<Fx> self = spec.one()
                ? List.of(spec.self().get(player.getRandom().nextInt(spec.self().size())))
                : spec.self();
        self.forEach(e -> player.addEffect(e.instance()));
        if (!spec.around().isEmpty()) {
            for (LivingEntity foe : level.getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(config.num("relic_radius")),
                    e -> e instanceof Enemy)) {
                spec.around().forEach(e -> foe.addEffect(e.instance()));
            }
        }
        level.playSound(
                null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.8f);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), config.integer("relic_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
