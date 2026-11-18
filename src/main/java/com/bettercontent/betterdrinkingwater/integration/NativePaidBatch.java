package com.bettercontent.betterdrinkingwater.integration;

import com.bettercontent.bettersurvivalphysiology.nutrition.ServingVectors;

import java.util.HashMap;
import java.util.Map;

/** Distinct actual paid ingredients add BEFORE dividing batch yield and final cap. */
public final class NativePaidBatch {
    private NativePaidBatch() {}
    public static Map<String, Double> add(Map<String, Double> accumulated, Map<String, Double> ingredient) {
        Map<String, Double> sum = new HashMap<>(accumulated);
        new ServingVectors.CanonicalServing(ingredient).gains().forEach((group, amount) -> {
            double total = sum.merge(group, amount, Double::sum);
            if (!Double.isFinite(total) || total < 0) throw new IllegalArgumentException("Invalid paid batch total");
        });
        return Map.copyOf(sum);
    }
    public static Map<String, Double> perCup(Map<String, Double> whole, int yield) {
        if (yield < 1 || yield > 12) throw new IllegalArgumentException("Invalid native cup yield");
        Map<String, Double> result = new HashMap<>();
        new ServingVectors.CanonicalServing(whole).gains().forEach((group, total) -> {
            double share = total / yield;
            if (!Double.isFinite(share) || share < 0) throw new IllegalArgumentException("Invalid per-cup native gain");
            result.put(group, Math.min(1, share));
        });
        return Map.copyOf(result);
    }
}
