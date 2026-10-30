package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.equipment.ItemRating;
import com.orijua.overgearedmod.event.ClassChangedEvent;
import com.orijua.overgearedmod.event.QuestCompletedEvent;
import com.orijua.overgearedmod.event.SatisfyPlayerEvent;
import com.orijua.overgearedmod.event.TitleEarnedEvent;
import com.orijua.overgearedmod.network.SystemNotificationPayload;
import com.orijua.overgearedmod.notification.SystemNotification;
import com.orijua.overgearedmod.playerclass.ClassService;
import com.orijua.overgearedmod.quest.QuestService;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

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
        Recorder recorder = new Recorder(player);
        NeoForge.EVENT_BUS.register(recorder);
        try {
            ClassService.choose(player, Overgearedmod.id("warrior"));
            QuestService.accept(player, commission);
            TestSupport.forge(player, ItemRating.EPIC); // also earns the epic_forger title
            QuestService.claim(player, commission);
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
}
