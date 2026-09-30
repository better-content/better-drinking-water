package com.bettercontent.betterdrinkingwater;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WaterSafetyEventsTest {
    @Test void onlyAnActualThirstDecreaseRevealsTheLesson() {
        assertTrue(WaterSafetyEvents.revealsOnDrop(20, 19));
        assertTrue(WaterSafetyEvents.revealsOnDrop(7, 6));
        assertFalse(WaterSafetyEvents.revealsOnDrop(19, 19));
        assertFalse(WaterSafetyEvents.revealsOnDrop(10, 14));
        assertFalse(WaterSafetyEvents.revealsOnDrop(0, -1));
    }

    @Test void completionRequiresTheExactMaximumPurityTier() {
        assertTrue(WaterSafetyEvents.isSafePurity(3, 3));
        assertFalse(WaterSafetyEvents.isSafePurity(2, 3));
        assertFalse(WaterSafetyEvents.isSafePurity(4, 3));
    }

    @Test void onlyPortableNonEmptyEpisodeTokensCanComplete() {
        assertTrue(WaterSafetyEpisodes.validToken("player:thirst:123"));
        assertFalse(WaterSafetyEpisodes.validToken(""));
        assertFalse(WaterSafetyEpisodes.validToken("contains a space"));
        assertFalse(WaterSafetyEpisodes.validToken("x".repeat(129)));
    }

    @Test void repeatedThirstDropsDoNotRestartAnActiveEpisode() {
        assertTrue(WaterSafetyEpisodes.shouldStartEpisode(""));
        assertTrue(WaterSafetyEpisodes.shouldStartEpisode("invalid token"));
        assertFalse(WaterSafetyEpisodes.shouldStartEpisode("player:thirst:123"));
    }
}
