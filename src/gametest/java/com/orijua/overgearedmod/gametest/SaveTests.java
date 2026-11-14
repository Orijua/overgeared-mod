package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.registry.ModAttachments;
import com.orijua.overgearedmod.core.save.SaveMigrations;
import com.orijua.overgearedmod.core.save.SaveMigrations.Step;
import com.orijua.overgearedmod.feature.capsule.CapsuleTier;
import com.orijua.overgearedmod.feature.economy.Account;
import com.orijua.overgearedmod.feature.progression.PlayerStats;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The players' saved data through the real attachments: the fixtures of every save version ({@code saves/v<N>.snbt})
 * are loaded into a player as a saved file would be, and the version steps run at login (see save.SaveMigrations).
 */
@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class SaveTests {
    private SaveTests() {
    }

    /** A new attachment must have its sample in the fixture of the current version: that is what guards its saved shape. */
    @GameTest(template = TestSupport.EMPTY)
    public static void everyAttachmentHasASampleInTheCurrentFixture(GameTestHelper helper) {
        Set<String> registered = ModAttachments.ATTACHMENT_TYPES.getEntries().stream()
                .map(entry -> entry.getId().getPath())
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> samples = new TreeSet<>(fixture(SaveMigrations.CURRENT).getAllKeys());

        helper.assertValueEqual(samples, registered, "attachments with a sample in saves/v" + SaveMigrations.CURRENT + ".snbt");
        helper.succeed();
    }

    /** Every version's fixture, loaded as the player's saved file, leaves every attachment readable. */
    @GameTest(template = TestSupport.EMPTY)
    public static void everyOldSaveLoadsIntoAPlayer(GameTestHelper helper) {
        List<String> lost = new ArrayList<>();
        for (int version = SaveMigrations.BASELINE; version <= SaveMigrations.CURRENT; version++) {
            ServerPlayer player = TestSupport.fakePlayer(helper);
            CompoundTag fixture = fixture(version);

            loadAttachments(player, fixture);

            for (String name : fixture.getAllKeys()) {
                var type = NeoForgeRegistries.ATTACHMENT_TYPES.get(id(name));
                if (type != null && !player.hasData(type)) {
                    lost.add("v" + version + " " + name);
                }
            }
            if (version == 1) {
                // Not only present: the values of the fixture are what the player reads back.
                PlayerStats stats = stats(player);
                if (stats.level() != 12 || stats.xp() != 340 || stats.smithingXp() != 1500) {
                    lost.add("v1 player_stats read back as " + stats);
                }
                Account account = player.getData(ModAttachments.ACCOUNT.get());
                if (account.won() != 6400L || account.debt() != 250000L) {
                    lost.add("v1 account read back as " + account);
                }
                if (player.getData(ModAttachments.CAPSULE_TIER.get()) != CapsuleTier.DIAMOND) {
                    lost.add("v1 capsule_tier read back as " + player.getData(ModAttachments.CAPSULE_TIER.get()));
                }
                if (!player.getData(ModAttachments.HEARD_RUMORS.get()).equals(List.of(id("north_end_cave")))) {
                    lost.add("v1 heard_rumors read back as " + player.getData(ModAttachments.HEARD_RUMORS.get()));
                }
            }
        }

        helper.assertTrue(lost.isEmpty(), "saved data dropped on load: " + lost);
        helper.succeed();
    }

    /** Loading the current fixture and saving the player again writes the same data: nothing is lost or reshaped. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theCurrentSaveSurvivesALoadAndSave(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        CompoundTag fixture = fixture(SaveMigrations.CURRENT);

        loadAttachments(player, fixture);
        CompoundTag saved = player.saveWithoutId(new CompoundTag()).getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY);

        List<String> changed = new ArrayList<>();
        for (String name : fixture.getAllKeys()) {
            if (!fixture.get(name).equals(saved.get(id(name).toString()))) {
                changed.add(name + ": " + fixture.get(name) + " saved again as " + saved.get(id(name).toString()));
            }
        }
        helper.assertTrue(changed.isEmpty(), "saved data that changes in a load and save: " + changed);
        helper.assertValueEqual(player.getData(ModAttachments.PLAYER_STATS.get()).level(), 12, "level read from the fixture");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aPlayerWithoutVersionIsAtTheBaselineAndEndsAtTheCurrentOne(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        helper.assertValueEqual(player.getData(ModAttachments.SAVE_VERSION.get()), SaveMigrations.BASELINE, "version of a save without one");

        SaveMigrations.upgrade(player);

        helper.assertValueEqual(player.getData(ModAttachments.SAVE_VERSION.get()), SaveMigrations.CURRENT, "version after joining");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void theMissingStepsRunInOrderAndOnlyOnce(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.PLAYER_STATS.get(), PlayerStats.DEFAULT.withProgress(5, 0, 0));
        // Each step doubles the level and adds one: the order shows in the result.
        List<Step> steps = List.of(
                new Step(2, "level x2", p -> p.setData(ModAttachments.PLAYER_STATS.get(), stats(p).withProgress(stats(p).level() * 2, 0, 0))),
                new Step(3, "level +1", p -> p.setData(ModAttachments.PLAYER_STATS.get(), stats(p).withProgress(stats(p).level() + 1, 0, 0))));

        int ran = SaveMigrations.upgrade(player, steps, 3);
        int again = SaveMigrations.upgrade(player, steps, 3);

        helper.assertValueEqual(ran, 2, "steps run the first time");
        helper.assertValueEqual(again, 0, "steps run the second time");
        helper.assertValueEqual(stats(player).level(), 11, "level after x2 then +1");
        helper.assertValueEqual(player.getData(ModAttachments.SAVE_VERSION.get()), 3, "version after the steps");
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void aStepThatFailsIsRetriedWithoutRepeatingTheEarlierOnes(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.PLAYER_STATS.get(), PlayerStats.DEFAULT.withProgress(5, 0, 0));
        boolean[] broken = {true};
        List<Step> steps = List.of(
                new Step(2, "level +1", p -> p.setData(ModAttachments.PLAYER_STATS.get(), stats(p).withProgress(stats(p).level() + 1, 0, 0))),
                new Step(3, "breaks once", p -> {
                    if (broken[0]) {
                        throw new IllegalStateException("the step broke");
                    }
                }));

        try {
            SaveMigrations.upgrade(player, steps, 3);
            helper.fail("the failing step should have thrown");
        } catch (IllegalStateException expected) {
            helper.assertValueEqual(player.getData(ModAttachments.SAVE_VERSION.get()), 2, "version after the step that worked");
        }
        broken[0] = false;
        int ran = SaveMigrations.upgrade(player, steps, 3);

        helper.assertValueEqual(ran, 1, "steps run on the retry");
        helper.assertValueEqual(stats(player).level(), 6, "level: the first step ran once");
        helper.assertValueEqual(player.getData(ModAttachments.SAVE_VERSION.get()), 3, "version after the retry");
        helper.succeed();
    }

    /** A save written by a newer version of the mod is left alone: no step runs and the version is not lowered. */
    @GameTest(template = TestSupport.EMPTY)
    public static void aNewerSaveIsLeftAlone(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.SAVE_VERSION.get(), SaveMigrations.CURRENT + 5);

        int ran = SaveMigrations.upgrade(player);

        helper.assertValueEqual(ran, 0, "steps run on a newer save");
        helper.assertValueEqual(player.getData(ModAttachments.SAVE_VERSION.get()), SaveMigrations.CURRENT + 5, "version of the newer save");
        helper.succeed();
    }

    /** The debug reset puts the data at today's defaults, so the version must stay. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theDebugResetKeepsTheSaveVersion(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        player.setData(ModAttachments.SAVE_VERSION.get(), 7);
        player.setData(ModAttachments.INSIGHT.get(), 3);

        ModAttachments.resetAll(player);

        helper.assertValueEqual(player.getData(ModAttachments.SAVE_VERSION.get()), 7, "version after the reset");
        helper.assertValueEqual(player.getData(ModAttachments.INSIGHT.get()), 0, "insight after the reset");
        helper.succeed();
    }

    private static PlayerStats stats(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_STATS.get());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Overgearedmod.MODID, name);
    }

    /** Loads the player from their own saved data with the fixture's attachments in it, as reading their file would. */
    private static void loadAttachments(ServerPlayer player, CompoundTag fixture) {
        CompoundTag attachments = new CompoundTag();
        for (String name : fixture.getAllKeys()) {
            attachments.put(id(name).toString(), fixture.get(name).copy());
        }
        CompoundTag file = player.saveWithoutId(new CompoundTag());
        file.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, attachments);
        player.load(file);
    }

    private static CompoundTag fixture(int version) {
        try (InputStream in = SaveTests.class.getResourceAsStream("/saves/v" + version + ".snbt")) {
            if (in == null) {
                throw new IllegalStateException("saves/v" + version + ".snbt is missing");
            }
            return TagParser.parseTag(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("saves/v" + version + ".snbt cannot be read", e);
        }
    }
}
