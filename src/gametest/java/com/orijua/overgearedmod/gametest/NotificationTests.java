package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.core.event.SatisfyPlayerEvent;
import com.orijua.overgearedmod.core.registry.Definitions;
import com.orijua.overgearedmod.core.registry.ModDataComponents;
import com.orijua.overgearedmod.core.registry.ModDatapackRegistries;
import com.orijua.overgearedmod.feature.equipment.ItemRating;
import com.orijua.overgearedmod.feature.notification.SystemNotification;
import com.orijua.overgearedmod.feature.notification.SystemNotificationPayload;
import com.orijua.overgearedmod.feature.npc.BlacksmithNpc;
import com.orijua.overgearedmod.feature.progression.ClassChangedEvent;
import com.orijua.overgearedmod.feature.progression.ClassService;
import com.orijua.overgearedmod.feature.progression.TitleDefinition;
import com.orijua.overgearedmod.feature.progression.TitleEarnedEvent;
import com.orijua.overgearedmod.feature.quest.QuestCompletedEvent;
import com.orijua.overgearedmod.feature.quest.QuestDiscoveredEvent;
import com.orijua.overgearedmod.feature.quest.QuestService;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class NotificationTests {
    private NotificationTests() {
    }

    /** Collects the domain events posted for one player while it is registered on the game bus. */
    public static final class Recorder {
        private final ServerPlayer player;
        final List<SatisfyPlayerEvent> events = new ArrayList<>();

        Recorder(ServerPlayer player) {
            this.player = player;
        }

        @SubscribeEvent
        public void onTitle(TitleEarnedEvent event) {
            record(event);
        }

        @SubscribeEvent
        public void onQuest(QuestCompletedEvent event) {
            record(event);
        }

        @SubscribeEvent
        public void onClass(ClassChangedEvent event) {
            record(event);
        }

        @SubscribeEvent
        public void onDiscovery(QuestDiscoveredEvent event) {
            record(event);
        }

        private void record(SatisfyPlayerEvent event) {
            if (event.player() == player) {
                events.add(event);
            }
        }
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void titleQuestAndClassPostTheirEvents(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        ResourceLocation commission = Overgearedmod.id("first_commission");
        BlacksmithNpc smith = TestSupport.smith(helper);
        TestSupport.completed(player, "smith_jaffa_sword");
        Recorder recorder = new Recorder(player);
        NeoForge.EVENT_BUS.register(recorder);
        try {
            ClassService.choose(player, Overgearedmod.id("warrior"));
            QuestService.accept(player, commission, smith);
            TestSupport.forge(player, ItemRating.EPIC); // also earns the epic_forger title
            QuestService.claim(player, commission, smith);
        } finally {
            NeoForge.EVENT_BUS.unregister(recorder);
        }
        helper.assertTrue(recorder.events.stream().anyMatch(e -> e instanceof ClassChangedEvent c
                && c.playerClass().equals(Overgearedmod.id("warrior")) && !c.granted()), "choosing a class: " + recorder.events);
        helper.assertTrue(recorder.events.stream().anyMatch(e -> e instanceof TitleEarnedEvent t
                && t.title().equals(Overgearedmod.id("epic_forger"))), "an epic forge earns epic_forger: " + recorder.events);
        helper.assertTrue(recorder.events.stream().anyMatch(e -> e instanceof QuestCompletedEvent q
                && q.quest().equals(commission)), "claiming the quest: " + recorder.events);
        helper.succeed();
    }

    @GameTest(template = TestSupport.EMPTY)
    public static void discoveringAHiddenQuestPostsItsEventOnce(GameTestHelper helper) {
        ServerPlayer player = TestSupport.fakePlayer(helper);
        Recorder recorder = new Recorder(player);
        NeoForge.EVENT_BUS.register(recorder);
        try {
            for (int i = 0; i < 32; i++) {
                TestSupport.kill(helper, player, EntityType.SKELETON);
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(recorder);
        }
        long discoveries = recorder.events.stream().filter(e -> e instanceof QuestDiscoveredEvent d
                && d.quest().equals(Overgearedmod.id("call_of_the_night"))).count();
        helper.assertTrue(discoveries == 1, "one discovery at the 30th kill, not one per kill after it: " + discoveries);
        helper.succeed();
    }

    /** The window goes through the network as a packet: it must decode to what was sent. */
    @GameTest(template = TestSupport.EMPTY)
    public static void notificationSurvivesTheNetwork(GameTestHelper helper) {
        SystemNotification sent = new SystemNotification(SystemNotification.Kind.CLASS,
                List.of(Component.translatable("class.overgearedmod.warrior"), Component.literal("Line two")));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        SystemNotificationPayload.STREAM_CODEC.encode(buffer, new SystemNotificationPayload(sent));
        SystemNotification received = SystemNotificationPayload.STREAM_CODEC.decode(buffer).notification();
        helper.assertTrue(received.equals(sent), "sent " + sent + " but got " + received);
        helper.succeed();
    }

    /** A grand window carries its icon (the forged piece, a nether star...) to the client. */
    @GameTest(template = TestSupport.EMPTY)
    public static void aGrandNotificationKeepsItsIcon(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.ITEM_RATING, ItemRating.LEGENDARY);
        SystemNotification sent = new SystemNotification(SystemNotification.Kind.LEGENDARY_ITEM, List.of(Component.literal("Fracaso")), Optional.of(sword));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        SystemNotificationPayload.STREAM_CODEC.encode(buffer, new SystemNotificationPayload(sent));
        SystemNotification received = SystemNotificationPayload.STREAM_CODEC.decode(buffer).notification();
        helper.assertTrue(received.equals(sent) && received.kind().grand(), "sent " + sent + " but got " + received);
        helper.assertTrue(received.icon().orElseThrow().get(ModDataComponents.ITEM_RATING) == ItemRating.LEGENDARY, "with its rating");
        helper.succeed();
    }

    /** The canon's big titles are rare (grand window); the everyday ones are not. */
    @GameTest(template = TestSupport.EMPTY)
    public static void theBigTitlesAreRare(GameTestHelper helper) {
        helper.assertTrue(title(helper, "became_legend").rare() && title(helper, "noble_killer").rare(), "legend and noble killer");
        helper.assertFalse(title(helper, "epic_forger").rare(), "an epic forge is not");
        helper.succeed();
    }

    private static TitleDefinition title(GameTestHelper helper, String id) {
        return Definitions.get(helper.getLevel(), ModDatapackRegistries.TITLE, Overgearedmod.id(id)).orElseThrow();
    }
}
