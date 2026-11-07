package com.orijua.overgearedmod.feature.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatisfyOnlyTest {
    private static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath("overgearedmod", path);
    }

    @Test
    void theCapsuleIsNotASatisfyItem() {
        assertFalse(SatisfyOnly.isSatisfyItem(mod("capsule")));
    }

    @Test
    void theModsToolsAndStationsAreSatisfyItems() {
        assertTrue(SatisfyOnly.isSatisfyItem(mod("smithing_anvil")));
        assertTrue(SatisfyOnly.isSatisfyItem(mod("smelter")));
        assertTrue(SatisfyOnly.isSatisfyItem(mod("apprentice_contract")));
        assertTrue(SatisfyOnly.isSatisfyItem(mod("jaffa_sword")));
        assertTrue(SatisfyOnly.isSatisfyItem(mod("respec_scroll")));
    }

    @Test
    void vanillaItemsAreNeverSatisfyItems() {
        assertFalse(SatisfyOnly.isSatisfyItem(ResourceLocation.withDefaultNamespace("iron_sword")));
        assertFalse(SatisfyOnly.isSatisfyItem(ResourceLocation.fromNamespaceAndPath("othermod", "smelter")));
    }

    @Test
    void theOresAreInertInTheRealWorldToo() {
        for (String path : new String[]{"jaffa_ore", "deepslate_jaffa_ore", "mithril_ore", "deepslate_mithril_ore",
                "blue_orichalcum_ore", "deepslate_blue_orichalcum_ore", "raw_jaffa", "jaffa_ingot", "raw_mithril",
                "mithril_ingot", "raw_blue_orichalcum", "blue_orichalcum_ingot"}) {
            assertTrue(SatisfyOnly.isSatisfyItem(mod(path)), path);
            assertTrue(SatisfyOnly.blocksPlacement(mod(path)), path);
        }
    }

    @Test
    void theOrePlacedFeaturesAreFilteredToSatisfy() throws Exception {
        for (String ore : new String[]{"ore_jaffa", "ore_mithril", "ore_blue_orichalcum"}) {
            try (InputStream in = SatisfyOnlyTest.class.getResourceAsStream("/data/overgearedmod/worldgen/placed_feature/" + ore + ".json")) {
                assertNotNull(in, ore);
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                boolean filtered = false;
                for (var modifier : json.getAsJsonArray("placement")) {
                    filtered |= "overgearedmod:satisfy_only".equals(modifier.getAsJsonObject().get("type").getAsString());
                }
                assertTrue(filtered, ore + " must carry the satisfy_only placement modifier");
            }
        }
    }

    @Test
    void theAnvilAndTheSmelterCannotBePlacedButTheCapsuleAndTheOresCan() {
        assertTrue(SatisfyOnly.blocksPlacement(mod("smithing_anvil")));
        assertTrue(SatisfyOnly.blocksPlacement(mod("smelter")));
        assertTrue(SatisfyOnly.blocksPlacement(mod("golden_table")));
        assertFalse(SatisfyOnly.blocksPlacement(mod("capsule")));
        assertFalse(SatisfyOnly.blocksPlacement(ResourceLocation.withDefaultNamespace("stone")));
    }

    @Test
    void theModsEntitiesAreSatisfyEntitiesButVanillaAndOtherModsAreNot() {
        assertTrue(SatisfyOnly.isSatisfyEntity(mod("blacksmith")));
        assertTrue(SatisfyOnly.isSatisfyEntity(mod("khan")));
        assertFalse(SatisfyOnly.isSatisfyEntity(ResourceLocation.withDefaultNamespace("villager")));
        assertFalse(SatisfyOnly.isSatisfyEntity(ResourceLocation.fromNamespaceAndPath("othermod", "x")));
    }

    @Test
    void theWarningKeyExistsInBothLangFiles() throws Exception {
        for (String file : new String[]{"en_us.json", "es_es.json"}) {
            try (InputStream stream = SatisfyOnlyTest.class.getResourceAsStream("/assets/overgearedmod/lang/" + file)) {
                assertNotNull(stream, "lang file not found: " + file);
                JsonObject lang = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                assertTrue(lang.has(SatisfyOnly.WARNING_KEY), file + " lacks " + SatisfyOnly.WARNING_KEY);
            }
        }
    }
}
