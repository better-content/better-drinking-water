package com.bettercontent.betterdrinkingwater.fixture;

import java.util.List;

/** Nonshipping identity check for the actual public Mixin0.8.5 postApply metadata entry. */
public final class ThirstRecursionFixIdentity {
    public static final String MIXIN_CLASS =
            "com.bettercontent.bettercompatfixes.mixin.thirst.AddLootTableModifierMixin";
    private ThirstRecursionFixIdentity() {}
    public static boolean matches(String appliedMixin, List<String> targets, String nativeTarget) {
        return MIXIN_CLASS.equals(appliedMixin)
                && List.of(nativeTarget.replace('.', '/')).equals(targets);
    }
}
