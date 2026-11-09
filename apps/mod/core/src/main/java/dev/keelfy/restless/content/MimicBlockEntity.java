package dev.keelfy.restless.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What a {@link MimicBlock} wears, and until when the Dark Lens has shown it bare. Both reach the client, which draws
 * the block from them.
 */
public final class MimicBlockEntity extends BlockEntity {
    private BlockState look = Blocks.AIR.defaultBlockState();
    private long revealedUntil;

    public MimicBlockEntity(BlockPos pos, BlockState state) {
        super(Content.MIMIC_ENTITY.get(), pos, state);
    }

    public BlockState look() {
        return look;
    }

    public void setLook(BlockState look) {
        this.look = look;
        sync();
    }

    /** Whether its true face shows at this game time. */
    public boolean revealed(long now) {
        return now < revealedUntil;
    }

    public void reveal(long until) {
        revealedUntil = until;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        look = input.read("Look", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
        revealedUntil = input.getLongOr("Revealed", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Look", BlockState.CODEC, look);
        output.putLong("Revealed", revealedUntil);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
