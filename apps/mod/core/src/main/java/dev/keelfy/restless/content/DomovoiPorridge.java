package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scan;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

/**
 * The domovoi's porridge: eaten, it gives {@link Content#GROWTH}, which has no end of its own (death or milk takes it
 * away). While it lasts, the crops and saplings around the eater grow faster and the young animals grow up sooner and
 * breed again sooner. It also mends a House Key in an anvil, see {@link #mend}.
 */
public final class DomovoiPorridge extends Item {
    private static final int EAT_TICKS = 32;
    /** How often the glow of the effect works on its surroundings. */
    private static final int INTERVAL = 40;

    public DomovoiPorridge(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel world)) {
            return stack;
        }
        if (player.hasEffect(Charms.holder(Content.GROWTH))) {
            player.sendSystemMessage(Component.translatable("message.restless.porridge_eaten"), true);
            return stack;
        }
        player.addEffect(new MobEffectInstance(
                Charms.holder(Content.GROWTH), MobEffectInstance.INFINITE_DURATION, 0, true, true));
        world.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.0f, 0.6f);
        return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BOWL));
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return EAT_TICKS;
    }

    /** Call every server tick: whoever carries the growth is a source of it. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL == 0) {
            pass(server);
        }
    }

    /** One pass over every carrier of the growth. */
    public static void pass(MinecraftServer server) {
        Config.Section config = Config.section("consumables");
        int radius = config.integer("porridge_radius");
        double extra = config.num("porridge_boost") - 1.0;
        for (ServerLevel level : server.getAllLevels()) {
            double speed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
            double chance = extra * INTERVAL * speed / 4096.0;
            for (ServerPlayer player : level.players()) {
                if (player.hasEffect(Charms.holder(Content.GROWTH))) {
                    feed(level, player, radius, chance, (int) (extra * INTERVAL));
                }
            }
        }
    }

    /** A pass around one carrier: plants may take a random tick more, the young age faster. */
    private static void feed(ServerLevel level, ServerPlayer player, int radius, double chance, int boost) {
        Scan.eachRandomTicking(level, player.blockPosition(), radius, (at, plant) -> {
            if (grows(plant) && level.getRandom().nextDouble() < chance) {
                plant.randomTick(level, at, level.getRandom());
            }
        });
        for (AgeableMob young : level.getEntitiesOfClass(
                AgeableMob.class, new AABB(player.blockPosition()).inflate(radius), m -> m.getAge() != 0)) {
            young.setAge(
                    young.getAge() < 0 ? Math.min(0, young.getAge() + boost) : Math.max(0, young.getAge() - INTERVAL));
        }
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.5, player.getZ(), 2, 0.4, 0.3, 0.4, 0.0);
    }

    /** What gets the extra growth: crops, saplings and a few plants besides. */
    private static boolean grows(BlockState state) {
        return state.isRandomlyTicking()
                && (state.is(BlockTags.CROPS)
                        || state.is(BlockTags.SAPLINGS)
                        || state.is(Blocks.SWEET_BERRY_BUSH)
                        || state.is(Blocks.NETHER_WART)
                        || state.is(Blocks.COCOA));
    }

    /** What the anvil makes of a worn house key and a bowl: the key mended by the config's number of uses. */
    public static ItemStack mend(ItemStack key, ItemStack porridge) {
        if (!key.is(Content.HOUSE_KEY.get())
                || !porridge.is(Content.DOMOVOIS_PORRIDGE_ITEM.get())
                || !key.isDamaged()) {
            return ItemStack.EMPTY;
        }
        ItemStack mended = key.copy();
        mended.setDamageValue(
                Math.max(0, key.getDamageValue() - Config.section("consumables").integer("porridge_repair")));
        return mended;
    }
}
