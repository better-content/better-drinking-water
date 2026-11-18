package com.bettercontent.betterdrinkingwater.integration;

/** Completion ownership is narrower than the plant-food classifier. */
public final class NativeCupOwnership {
    private NativeCupOwnership() {}
    public static boolean typedWholeCompletion(boolean plant, boolean claimedNativeCup, boolean completePaidFacts) {
        return plant || claimedNativeCup && completePaidFacts;
    }
}
