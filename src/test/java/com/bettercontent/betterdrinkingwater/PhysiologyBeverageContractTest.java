package com.bettercontent.betterdrinkingwater;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PhysiologyBeverageContractTest {
    @Test void providerOwnsProfileAndPaidFractionButCoreAloneWritesDiet() throws Exception {
        String adapter = Files.readString(Path.of("src/main/java/com/bettercontent/betterdrinkingwater/integration/PhysiologyBeverages.java"));
        String startup = Files.readString(Path.of("src/main/java/com/bettercontent/betterdrinkingwater/WaterSurvival.java"));
        String commit = Files.readString(Path.of("../better-survival-physiology/src/main/java/com/bettercontent/bettersurvivalphysiology/nutrition/CommittedDietIntake.java"));
        assertTrue(startup.contains("ModList.get().isLoaded(\"better_survival_physiology\")"));
        assertTrue(adapter.contains("BeverageProfiles.register("));
        assertTrue(adapter.contains("BeverageEvents.Profile event"));
        assertTrue(adapter.contains("BeverageEvents.Check event"));
        assertTrue(adapter.contains("BeverageEvents.Sip event"));
        assertTrue(adapter.contains("BeverageNutrition.fraction(event.from(), event.to()) == 0.0"));
        assertTrue(adapter.contains("BeverageSpiceDose.decode("));
        assertTrue(adapter.contains("CommittedDietIntake.commit("));
        assertFalse(adapter.contains("diet.setValue("));
        assertFalse(adapter.contains("diet.sync()"));
        assertTrue(commit.contains("gains.forEach((group, gain) -> diet.setValue"));
        assertTrue(commit.contains("if (eatenItem != null)"));
        assertTrue(adapter.contains("if (ownsCompleted(event.getStack())) event.setCanceled(true)"));
        assertFalse(adapter.contains("diet.consume("));
    }
}
