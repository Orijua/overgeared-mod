package com.orijua.overgearedmod.core.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.orijua.overgearedmod.feature.capsule.CapsuleState;
import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import com.orijua.overgearedmod.feature.character.CharacterSnapshot;
import com.orijua.overgearedmod.feature.character.CharacterVault;
import com.orijua.overgearedmod.feature.character.WorldSide;
import com.orijua.overgearedmod.feature.economy.Account;
import com.orijua.overgearedmod.feature.economy.OfferBought;
import com.orijua.overgearedmod.feature.economy.TradeSaturation;
import com.orijua.overgearedmod.feature.insight.LearnedToday;
import com.orijua.overgearedmod.feature.mob.MobLevel;
import com.orijua.overgearedmod.feature.npc.WorkshopEntry;
import com.orijua.overgearedmod.feature.progression.PlayerIdentity;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import com.orijua.overgearedmod.feature.quest.PlayerQuests;
import com.orijua.overgearedmod.feature.quest.goal.PlayerCounters;
import com.orijua.overgearedmod.feature.skill.PlayerSkills;
import com.orijua.overgearedmod.feature.winston.ContestState;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the players' saved data: one fixture per save version ({@code saves/v<N>.snbt}, the saved shape of every
 * attachment with non-default values) is loaded with today's codecs. A codec change that can no longer read an old
 * save fails here, without starting the game; the game test SaveTests checks the same through the real attachments.
 * See {@link SaveMigrations} for the rules.
 */
class SavedDataCompatibilityTest {
    /**
     * The disk codec of every attachment, by its registered name. Keep in step with core.registry.ModAttachments: the
     * game test SaveTests fails if an attachment has no sample in the fixture, and this test if a sample has no codec here.
     */
    private static final Map<String, Codec<?>> CODECS = Map.ofEntries(
            Map.entry("player_stats", PlayerStats.CODEC),
            Map.entry("player_skills", PlayerSkills.CODEC),
            Map.entry("mana", Codec.FLOAT),
            Map.entry("fatigue", Codec.FLOAT),
            Map.entry("account", Account.CODEC),
            Map.entry("trade_saturation", TradeSaturation.CODEC),
            Map.entry("fatigue_grace", Codec.INT),
            Map.entry("fatigue_forced_out", Codec.BOOL),
            Map.entry("fatigue_away_since", Codec.LONG),
            Map.entry("player_identity", PlayerIdentity.CODEC),
            Map.entry("player_counters", PlayerCounters.CODEC),
            Map.entry("player_quests", PlayerQuests.CODEC),
            Map.entry("apprentices", UUIDUtil.CODEC.listOf()),
            Map.entry("workshop", WorkshopEntry.CODEC.listOf()),
            Map.entry("insight", Codec.INT),
            Map.entry("insight_learned", LearnedToday.CODEC),
            Map.entry("talents_found", UUIDUtil.CODEC.listOf()),
            Map.entry("heard_rumors", ResourceLocation.CODEC.listOf()),
            Map.entry("khan_contest", ContestState.CODEC),
            Map.entry("capsule", CapsuleState.CODEC),
            Map.entry("capsule_tier", CapsuleTier.CODEC),
            Map.entry("rested_seconds", Codec.INT),
            Map.entry("rested_cooldown", Codec.INT),
            Map.entry("starter_capsule_given", Codec.BOOL),
            Map.entry("work_day", Codec.LONG),
            Map.entry("offer_bought", OfferBought.CODEC),
            Map.entry("phone_given", Codec.BOOL),
            Map.entry("character_vault", CharacterVault.CODEC),
            Map.entry("save_version", Codec.INT),
            Map.entry("from_spawner", Codec.BOOL),
            Map.entry("mob_level", MobLevel.CODEC));

    /**
     * Attachments that existed in an older save version and were removed since: their fixtures stay but are not loaded.
     * Removing one needs a migration step if its data must move somewhere.
     */
    private static final Set<String> REMOVED = Set.of();

    @Test
    void thereIsAFixtureForEverySaveVersion() {
        for (int version = SaveMigrations.BASELINE; version <= SaveMigrations.CURRENT; version++) {
            assertNotNull(SavedDataCompatibilityTest.class.getResourceAsStream("/saves/v" + version + ".snbt"),
                    "saves/v" + version + ".snbt is missing: raising the save version needs the fixture of the new shape");
        }
    }

    @Test
    void theCurrentFixtureCoversEveryAttachment() throws Exception {
        CompoundTag fixture = fixture(SaveMigrations.CURRENT);

        assertEquals(new TreeSet<>(CODECS.keySet()), new TreeSet<>(fixture.getAllKeys()));
    }

    @Test
    void everyOldSaveStillLoads() throws Exception {
        List<String> failures = new ArrayList<>();
        for (int version = SaveMigrations.BASELINE; version <= SaveMigrations.CURRENT; version++) {
            CompoundTag fixture = fixture(version);
            for (String name : fixture.getAllKeys()) {
                if (REMOVED.contains(name)) {
                    continue;
                }
                Codec<?> codec = CODECS.get(name);
                if (codec == null) {
                    failures.add("v" + version + " " + name + ": no codec (if the attachment was removed, list it in REMOVED)");
                    continue;
                }
                DataResult<?> result = codec.parse(NbtOps.INSTANCE, fixture.get(name));
                if (result.error().isPresent()) {
                    failures.add("v" + version + " " + name + ": " + result.error().get().message());
                }
            }
        }

        assertTrue(failures.isEmpty(), "saved data that no longer loads:\n" + String.join("\n", failures));
    }

    /**
     * Loading the current fixture and saving it again gives the same data: the fixture really is today's shape (no
     * field of it is ignored) and nothing is lost in a save and load.
     */
    @Test
    void theCurrentFixtureSurvivesALoadAndSave() throws Exception {
        CompoundTag fixture = fixture(SaveMigrations.CURRENT);
        List<String> failures = new ArrayList<>();
        for (String name : fixture.getAllKeys()) {
            Tag saved = roundTrip(CODECS.get(name), fixture.get(name));
            if (!fixture.get(name).equals(saved)) {
                failures.add(name + ": fixture " + fixture.get(name) + " but saved again as " + saved);
            }
        }

        assertTrue(failures.isEmpty(), "the fixture is not today's saved shape:\n" + String.join("\n", failures));
    }

    // ---- Partial, extra and extreme saves (the fixtures above are always complete) ----

    /** A field a newer version added is ignored by an older reader: the save loads and the field is not written back. */
    @Test
    void anUnknownExtraFieldIsIgnored() throws Exception {
        CompoundTag stats = fixture(SaveMigrations.CURRENT).getCompound("player_stats").copy();
        stats.putInt("field_from_the_future", 99);

        PlayerStats loaded = PlayerStats.CODEC.parse(NbtOps.INSTANCE, stats).getOrThrow();

        assertEquals(12, loaded.level());
        assertTrue(!((CompoundTag) roundTrip(PlayerStats.CODEC, stats)).contains("field_from_the_future"));
    }

    /**
     * A required field that is missing makes the whole value fail to parse. In the game the attachment then goes back
     * to its default (the player starts that data again), which is the accepted behaviour: if it should be tolerated,
     * that is a change of the codec (optionalFieldOf with a default), not of this test.
     */
    @Test
    void aMissingRequiredFieldFailsToParse() throws Exception {
        CompoundTag stats = fixture(SaveMigrations.CURRENT).getCompound("player_stats").copy();
        stats.remove("level");

        assertTrue(PlayerStats.CODEC.parse(NbtOps.INSTANCE, stats).error().isPresent());
    }

    @Test
    void aCapsuleStateWithACapsuleButNoExitLoads() throws Exception {
        CompoundTag state = new CompoundTag();
        state.put("capsule", fixture(SaveMigrations.CURRENT).getCompound("capsule").get("capsule").copy());

        CapsuleState loaded = CapsuleState.CODEC.parse(NbtOps.INSTANCE, state).getOrThrow();

        assertTrue(loaded.isConnected());
        assertTrue(loaded.exit().isEmpty());
    }

    @Test
    void aCapsuleStateInAnUnknownDimensionStillLoads() throws Exception {
        CompoundTag state = fixture(SaveMigrations.CURRENT).getCompound("capsule").copy();
        CompoundTag capsule = state.getCompound("capsule").copy();
        capsule.putString("dimension", "some_removed_mod:nowhere");
        state.put("capsule", capsule);

        CapsuleState loaded = CapsuleState.CODEC.parse(NbtOps.INSTANCE, state).getOrThrow();

        assertEquals("some_removed_mod:nowhere", loaded.capsule().orElseThrow().dimension().location().toString());
        assertEquals(state, roundTrip(CapsuleState.CODEC, state));
    }

    @Test
    void aVaultWithAFullStoredInventoryRoundTrips() {
        ListTag inventory = new ListTag();
        for (int slot = 0; slot < 41; slot++) {
            CompoundTag item = new CompoundTag();
            item.putByte("Slot", (byte) slot);
            item.putString("id", "minecraft:iron_sword");
            item.putInt("count", 1);
            CompoundTag components = new CompoundTag();
            components.putString("minecraft:custom_name", "\"Sword " + slot + "\"");
            item.put("components", components);
            inventory.add(item);
        }
        CompoundTag stored = new CompoundTag();
        stored.put("ender_chest", new ListTag());
        stored = CharacterSnapshot.withInventory(stored, inventory);
        CharacterVault vault = CharacterVault.of(WorldSide.REAL, stored);

        Tag saved = CharacterVault.CODEC.encodeStart(NbtOps.INSTANCE, vault).getOrThrow();
        CharacterVault loaded = CharacterVault.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();

        assertEquals(vault, loaded);
        assertEquals(41, CharacterSnapshot.inventory(loaded.stored().orElseThrow()).size());
        assertTrue(CharacterSnapshot.isReadable(loaded.stored().orElseThrow()));
    }

    // ---- The sample of a real vault (the v1 fixture's stored character is not one) ----

    /**
     * The {@code stored} of the v1 fixture only has a few loose values: it has no inventory or Ender chest, so it is
     * NOT a readable snapshot (a swap would be refused). That is fine for what v1 guards (the shape of the vault), and
     * v1 is never edited; the readable sample is saves/samples/character_vault_full.snbt.
     */
    @Test
    void theStoredCharacterOfV1IsNotAReadableSnapshot() throws Exception {
        CharacterVault v1 = CharacterVault.CODEC.parse(NbtOps.INSTANCE, fixture(1).get("character_vault")).getOrThrow();

        assertTrue(!CharacterSnapshot.isReadable(v1.stored().orElseThrow()));
    }

    @Test
    void theFullVaultSampleIsReadable() throws Exception {
        CharacterVault vault = CharacterVault.CODEC.parse(NbtOps.INSTANCE, sample("character_vault_full")).getOrThrow();

        assertEquals(java.util.Optional.of(WorldSide.SATISFY), vault.live());
        assertTrue(CharacterSnapshot.isReadable(vault.stored().orElseThrow()));
        assertEquals(4, CharacterSnapshot.inventory(vault.stored().orElseThrow()).size());
    }

    private static Tag sample(String name) throws Exception {
        try (InputStream in = SavedDataCompatibilityTest.class.getResourceAsStream("/saves/samples/" + name + ".snbt")) {
            assertNotNull(in, "saves/samples/" + name + ".snbt is missing");
            return TagParser.parseTag(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static <T> Tag roundTrip(Codec<T> codec, Tag tag) {
        T value = codec.parse(NbtOps.INSTANCE, tag).getOrThrow();
        return codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow();
    }

    private static CompoundTag fixture(int version) throws Exception {
        try (InputStream in = SavedDataCompatibilityTest.class.getResourceAsStream("/saves/v" + version + ".snbt")) {
            assertNotNull(in, "saves/v" + version + ".snbt is missing");
            return TagParser.parseTag(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
