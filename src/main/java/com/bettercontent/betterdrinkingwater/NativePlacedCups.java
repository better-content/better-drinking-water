package com.bettercontent.betterdrinkingwater;

import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.*;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.item.TeapotItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.function.Supplier;

/** Only actual native cup state transitions; neither cup pickup nor placement awards intake. */
public final class NativePlacedCups {
    private NativePlacedCups() {}
    public static String blockId(BlockState state) { return String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())); }
    public static boolean known(BlockState state) {
        return NativeDrinkFacts.isCupId(blockId(state))
                && (state.getBlock() instanceof TeacupBlock || state.getBlock() instanceof ClayPotMilkTeaBlock);
    }
    public static int count(BlockState state) {
        if (!known(state)) return 0;
        if (state.getBlock() instanceof TeacupBlock cups) {
            int filled = state.getValue(cups.getTeaCountProperty());
            int total = state.getValue(cups.getCupCountProperty());
            if (filled < 1 || filled > 4 || total > 4 || filled > total)
                throw new IllegalArgumentException("Invalid actual native filled tea count");
            return filled;
        }
        return 1;
    }
    private static NativeCupBlockEntity owner(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof NativeCupBlockEntity body))
            throw new IllegalStateException("Missing Water-owned native cup BE");
        return body;
    }
    private static final class Use {
        final ServerLevel level; final BlockPos pos; final BlockState before;
        final Player player; final InteractionHand hand; final NativeCupBlockEntity body;
        final NativeCupQueue queue; final ItemStack held; final ItemStack additional;
        final NativeCupQueue filled; final boolean teapot;
        final int beforeCount;
        boolean delivered;
        Use(ServerLevel level, BlockPos pos, BlockState before, Player player, InteractionHand hand) {
            this.level = level; this.pos = pos.immutable(); this.before = before; this.player = player; this.hand = hand;
            body = known(before) ? owner(level, pos) : null;
            queue = body == null ? NativeCupQueue.unknown(0) : body.snapshot(before);
            ItemStack raw = player.getItemInHand(hand);
            teapot = raw.is(ModItems.TEAPOT.get());
            // Unrelated held tools and native UNKNOWN cups need no deep copy. Claimed
            // oversized receipts fail BEFORE KC can debit, while an unclaimed oversized
            // original remains a native-only UNKNOWN slot rather than blocking native use.
            boolean bounded = true;
            if (NativeDrinkFacts.isCup(raw) || teapot) try { NativeDrinkFacts.validateStack(raw); }
            catch (RuntimeException invalid) {
                if (raw.hasTag() && raw.getTag().contains(NativeDrinkFacts.KEY)) throw invalid;
                bounded = false; NativeDrinkProduction.unsupported("unclaimed placed cup original", invalid);
            }
            held = bounded && (teapot || NativeDrinkFacts.isCup(raw)) ? raw.copy() : raw;
            ItemStack source = bounded && teapot ? TeapotItem.getPourOut(held) : raw;
            ItemStack candidate = ItemStack.EMPTY;
            if (NativeDrinkFacts.isCup(source)) try {
                NativeDrinkFacts.validateStack(source);
                candidate = source.copyWithCount(1);
            } catch (RuntimeException invalid) {
                if (source.hasTag() && source.getTag().contains(NativeDrinkFacts.KEY)) throw invalid;
                NativeDrinkProduction.unsupported("unclaimed teapot pour original", invalid);
            }
            additional = candidate;
            beforeCount = teapot && bounded ? source.getCount() : raw.getCount();
            if (queue.size() < 4 && (NativeDrinkFacts.isCup(source) || teapot && !bounded)) {
                NativeCupQueue proposed = candidate.isEmpty() ? queue.appendUnknown() : queue.append(candidate);
                // Full cumulative FIFO, not just a single row, MUST fit before debit.
                if (NativeDrinkFacts.isCup(source)) proposed.save(WaterSources.id(source));
                else if (known(before)) proposed.save(blockId(before));
                filled = proposed;
            } else filled = null;
        }
        boolean paidOne() {
            ItemStack after = player.getItemInHand(hand);
            return teapot ? TeapotItem.getPourOut(after).getCount() == beforeCount - 1
                    : after.getCount() == beforeCount - 1;
        }
    }
    private static final ThreadLocal<Use> ACTIVE = new ThreadLocal<>();
    static NativeCupQueue preview(NativeCupBlockEntity body, BlockState expected, NativeCupQueue stored) {
        Use use = ACTIVE.get();
        if (use == null || body.getLevel() != use.level || !body.getBlockPos().equals(use.pos)) return stored;
        int n = count(expected);
        if (n == stored.size() + 1 && use.filled != null
                && (use.additional.isEmpty() || use.additional.is(expected.getBlock().asItem()))) return use.filled;
        if (n == stored.size() - 1 && use.delivered) return stored.removeFirst();
        return stored;
    }
    public static void requireUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level.isClientSide || hand != InteractionHand.MAIN_HAND) return;
        ItemStack held = player.getItemInHand(hand);
        boolean relevant = known(state) || state.getBlock() instanceof EmptyCupBlock
                && NativeDrinkFacts.isCup(held.is(ModItems.TEAPOT.get()) ? TeapotItem.getPourOut(held) : held);
        if (!relevant) return;
        Use use = ACTIVE.get();
        if (use == null || use.level != level || !use.pos.equals(pos) || use.player != player || use.hand != hand)
            throw new IllegalStateException("Native cup bypassed exact use scope");
    }
    public static InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                        InteractionHand hand, Supplier<InteractionResult> nativeUse) {
        if (hand != InteractionHand.MAIN_HAND || !(level instanceof ServerLevel server)
                || !known(state) && !(state.getBlock() instanceof EmptyCupBlock)) return nativeUse.get();
        Use parent = ACTIVE.get();
        if (parent != null && parent.level == server && parent.pos.equals(pos))
            throw new IllegalStateException("Reentrant native cup at one block");
        Use use = new Use(server, pos, state, player, hand); ACTIVE.set(use);
        Throwable thrown = null;
        try { return nativeUse.get(); }
        catch (RuntimeException | Error failure) { thrown = failure; throw failure; }
        finally {
            try { reconcile(use); }
            catch (RuntimeException | Error invalid) { if (thrown == null) throw invalid; thrown.addSuppressed(invalid); }
            finally { if (parent == null) ACTIVE.remove(); else ACTIVE.set(parent); }
        }
    }
    private static void reconcile(Use use) {
        BlockState after = use.level.getBlockState(use.pos);
        int before = use.queue.size(), now = count(after);
        if (now == before) {
            if (use.delivered) throw new IllegalStateException("Native cup delivered without state debit");
            return;
        }
        if (now == before + 1 && !use.delivered) {
            if (use.filled == null || !use.additional.isEmpty()
                    && (!use.additional.is(after.getBlock().asItem()) || !use.paidOne()))
                throw new IllegalStateException("Native filled cup placed without one paid original");
            owner(use.level, use.pos).commit(after, use.filled); return;
        }
        if (now == before - 1 && use.delivered) {
            if (now > 0) owner(use.level, use.pos).commit(after, use.queue.removeFirst());
            return;
        }
        throw new IllegalStateException("Native cup count/paid FIFO drift: " + before + " -> " + now);
    }
    public static ItemStack pickup(Level level, BlockPos pos, BlockState state, Player player, ItemStack candidate) {
        if (level.isClientSide || !NativeDrinkFacts.isCup(candidate)) return candidate;
        Use use = ACTIVE.get();
        if (use == null || use.level != level || !use.pos.equals(pos) || use.player != player || use.delivered
                || candidate.getCount() != 1 || !candidate.is(state.getBlock().asItem()))
            throw new IllegalStateException("Native filled cup delivered outside paid FIFO use");
        use.delivered = true;
        return use.queue.first().orElseGet(() -> candidate.copyWithCount(1));
    }
    public static void validatePlacement(Level world, ItemStack source) {
        if (world.isClientSide || !NativeDrinkFacts.isCup(source)) return;
        try {
            NativeDrinkFacts.validateStack(source); // raw before copying or native placement debit
            NativeCupQueue.unknown(0).append(source.copyWithCount(1)).save(WaterSources.id(source));
        } catch (RuntimeException invalid) {
            if (source.hasTag() && source.getTag().contains(NativeDrinkFacts.KEY)) throw invalid;
            NativeDrinkProduction.unsupported("unclaimed placed original", invalid);
        }
    }
    /** Called by KC's actual setPlacedBy callsite; oversized unclaimed original uses
     * the BE's native UNKNOWN slot, never a newly minted known paid original. */
    public static ItemStack snapshotPlacement(ItemStack source) {
        if (!NativeDrinkFacts.isCup(source)) return ItemStack.EMPTY;
        try { NativeDrinkFacts.validateStack(source); return source.copyWithCount(1); }
        catch (RuntimeException unsupported) { return ItemStack.EMPTY; }
    }
    public static void placed(Level world, BlockPos pos, BlockState state, ItemStack original) {
        if (!(world instanceof ServerLevel server) || !known(state)) return;
        if (!world.getBlockState(pos).getBlock().equals(state.getBlock()) || count(state) != 1
                || !original.is(state.getBlock().asItem())) throw new IllegalStateException("Native cup placement identity drift");
        owner(server, pos).commit(state, NativeCupQueue.unknown(0).append(original.copyWithCount(1)));
    }
    public static List<ItemStack> drops(BlockState state, LootParams.Builder params, List<ItemStack> nativeDrops) {
        if (!known(state)) return nativeDrops;
        var origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin == null) throw new IllegalStateException("Native cup drop missing origin");
        var old = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(old instanceof NativeCupBlockEntity body)) {
            if (!params.getLevel().getBlockState(BlockPos.containing(origin)).equals(state))
                throw new IllegalStateException("Detached native cup loot omitted its BE");
            old = params.getLevel().getBlockEntity(BlockPos.containing(origin));
        }
        if (!(old instanceof NativeCupBlockEntity body)) throw new IllegalStateException("Missing native cup loot BE");
        NativeCupQueue queue = body.snapshot(state);
        int filled = 0;
        for (ItemStack drop : nativeDrops) if (drop.is(state.getBlock().asItem())) filled += drop.getCount();
        List<ItemStack> recovered = queue.drops(filled, new ItemStack(state.getBlock()));
        int index = 0; List<ItemStack> result = new ArrayList<>();
        for (ItemStack drop : nativeDrops) {
            if (!drop.is(state.getBlock().asItem())) result.add(drop);
            else for (int n = 0; n < drop.getCount(); n++) result.add(recovered.get(index++));
        }
        return List.copyOf(result);
    }
}
