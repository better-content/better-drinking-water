package com.bettercontent.betterdrinkingwater.api;

import net.minecraft.resources.ResourceLocation;

/** Run in a separate plain JVM: production registration is intentionally global and cannot be reset.
 * No item/effect registry or Forge bootstrap is initialized, and no provider is fabricated.
 */
public final class ServingEffectsAbsentControls {
    public static void main(String[] args) {
        var source = new ResourceLocation("example", "paid_source");
        var selected = new ServingEffects.Selected(new ResourceLocation("example", "selected_effect"), 8000, 9);
        if (!ServingEffects.restrict(source, selected).orElseThrow().equals(selected)) throw new AssertionError("Absent provider must preserve public API entitlement exactly");
        if (!ServingEffects.restrict(source, selected).orElseThrow().equals(selected)) throw new AssertionError("Absent provider repeated read must remain identity");
        System.out.println("ServingEffects public API absent-provider controls PASS2 (separate plain JVM, no Forge bootstrap)");
    }
}
