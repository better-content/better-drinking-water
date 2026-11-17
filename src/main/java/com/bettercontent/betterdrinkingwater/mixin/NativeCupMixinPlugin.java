package com.bettercontent.betterdrinkingwater.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** KC-only optional native hooks; active Cooking is excluded by the pack release owner. */
public final class NativeCupMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String packageName) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (!mixin.contains("NativeTeacup") && !mixin.contains("NativeMilkTea")
                && !mixin.contains("NativeTeapotFacts") && !mixin.contains("NativeStockpotFacts")
                && !mixin.contains("NativePortableTeapotFacts")
                && !mixin.contains("NativeCupEntity") && !mixin.contains("NativeCupPlacement")
                && !mixin.contains("NativeCupUseScope") && !mixin.contains("NativeMilkCupPickup")
                && !mixin.contains("NativeEmptyCupScope") && !mixin.contains("NativeMilkCupDrops")
                && !mixin.contains("NativeCardPayment") && !mixin.contains("NativeCupChunkLoad")
                && !mixin.contains("NativeCupThirstFinish")) return true;
        ClassLoader loader = getClass().getClassLoader();
        return loader.getResource("com/github/ysbbbbbb/kaleidoscopecookery/item/TeacupItem.class") != null;
    }
    @Override public void acceptTargets(Set<String> own, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
