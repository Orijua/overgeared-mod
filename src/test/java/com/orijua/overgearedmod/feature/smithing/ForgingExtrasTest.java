package com.orijua.overgearedmod.feature.smithing;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgingExtrasTest {
    private static final ResourceLocation X = ResourceLocation.fromNamespaceAndPath("test", "x");
    private static final ResourceLocation Y = ResourceLocation.fromNamespaceAndPath("test", "y");

    private static ForgingExtras forClass(ResourceLocation required) {
        return new ForgingExtras(Optional.of(required), ForgingExtras.NONE.minRating(), ForgingExtras.NONE.maxRating(), java.util.List.of());
    }

    @Test
    void withoutARequiredClassEveryoneMayForge() {
        assertTrue(ForgingExtras.NONE.allows(Optional.empty()));
        assertTrue(ForgingExtras.NONE.allows(Optional.of(X)));
    }

    @Test
    void aRequiredClassOnlyAllowsThatClass() {
        ForgingExtras onlyX = forClass(X);
        assertTrue(onlyX.allows(Optional.of(X)));
        assertFalse(onlyX.allows(Optional.of(Y)));
        assertFalse(onlyX.allows(Optional.empty()), "a smith with no class can't");
    }

    private static boolean parses(String json) {
        JsonElement element = JsonParser.parseString(json);
        return ForgingExtras.MAP_CODEC.codec().parse(JsonOps.INSTANCE, element).result().isPresent();
    }

    @Test
    void aMinimumRatingAboveTheMaximumIsRejected() {
        assertFalse(parses("{\"min_rating\":\"legendary\",\"max_rating\":\"unique\"}"));
        assertTrue(parses("{\"min_rating\":\"unique\",\"max_rating\":\"legendary\"}"));
        assertTrue(parses("{\"min_rating\":\"legendary\",\"max_rating\":\"legendary\"}"), "equal is fine");
        ForgingExtras defaults = ForgingExtras.MAP_CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).result().orElseThrow();
        assertEquals(ForgingExtras.NONE, defaults, "an empty object means no restrictions");
    }
}
