package com.bettercontent.betterdrinkingwater;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The native block/state is unchanged; only the actual filled-cup originals persist. */
public final class NativeCupBlockEntity extends BlockEntity {
    public static final String KEY = "better_drinking_water:native_cup_fifo";
    private final String block;
    private NativeCupQueue queue;
    private boolean corrupt;
    private CompoundTag poison;

    public NativeCupBlockEntity(BlockPos pos, BlockState state) {
        super(NativeCupEntities.CUPS.get(), pos, state);
        block = NativePlacedCups.blockId(state);
        queue = NativeCupQueue.unknown(NativePlacedCups.count(state));
    }
    public NativeCupQueue snapshot(BlockState expected) {
        if (corrupt) throw new IllegalStateException("Corrupt claimed native cup BE");
        if (!block.equals(NativePlacedCups.blockId(expected))) throw new IllegalArgumentException("Native cup block identity drift");
        NativeCupQueue preview = NativePlacedCups.preview(this, expected, queue);
        if (preview.size() != NativePlacedCups.count(expected)) throw new IllegalArgumentException("Native cup state/queue count drift");
        return preview;
    }
    public void commit(BlockState state, NativeCupQueue next) {
        if (corrupt || !block.equals(NativePlacedCups.blockId(state)) || next.size() != NativePlacedCups.count(state))
            throw new IllegalArgumentException("Native filled-cup commit/count disagreement");
        next.save(block); queue = next;
        if (level != null) level.getChunkAt(worldPosition).setUnsaved(true);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (!tag.contains(KEY)) {
            if (tag.getString("id").equals("better_drinking_water:native_drink_cups")) {
                corrupt = true; poison = new CompoundTag(); poison.putString("reason", "missing claimed native cup FIFO");
            } else { queue = NativeCupQueue.unknown(NativePlacedCups.count(getBlockState())); corrupt = false; }
            return;
        }
        try {
            if (!tag.contains(KEY, Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Wrong claimed native FIFO type");
            queue = NativeCupQueue.load(tag.getCompound(KEY), block); corrupt = false; poison = null;
        } catch (RuntimeException invalid) {
            // Vanilla may replace a BE after a load exception. Keep the invalid known
            // row visible and do not reinterpret it as an old, unknown native block.
            corrupt = true; poison = new CompoundTag(); poison.putBoolean("corrupt", true);
            try { poison.put("claimed", NativeDrinkFacts.bounded(tag.getCompound(KEY))); }
            catch (RuntimeException oversized) { poison.putString("reason", "oversized claimed FIFO"); }
        }
    }
    /** Wrong saved BE id must not become a fresh UNKNOWN via vanilla lazy factory. */
    public static NativeCupBlockEntity rejectedSavedType(BlockPos pos, BlockState state, CompoundTag rejected) {
        NativeCupBlockEntity body = new NativeCupBlockEntity(pos, state);
        body.corrupt = true;
        body.poison = new CompoundTag();
        body.poison.putString("reason", "foreign or missing saved native cup BE id");
        if (rejected.contains(KEY, Tag.TAG_COMPOUND)) try {
            body.poison.put("claimed", NativeDrinkFacts.bounded(rejected.getCompound(KEY)));
        } catch (RuntimeException oversized) { body.poison.putString("claimed_reason", "oversized"); }
        return body;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(KEY, corrupt ? poison.copy() : queue.save(block));
    }
}
