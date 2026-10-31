package com.orijua.overgearedmod.gametest;

import com.orijua.overgearedmod.Overgearedmod;
import com.orijua.overgearedmod.validation.ContentIndex;
import com.orijua.overgearedmod.validation.ContentScanner;
import com.orijua.overgearedmod.validation.ContentValidator;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Overgearedmod.MODID)
@PrefixGameTestTemplate(false)
public final class ContentTests {
    private ContentTests() {
    }

    /** Fails CI when a JSON points to a skill or class that doesn't exist. */
    @GameTest(template = TestSupport.EMPTY)
    public static void realContentHasNoBrokenReferences(GameTestHelper helper) {
        List<String> problems = ContentValidator.validate(ContentScanner.index(helper.getLevel().registryAccess()));
        helper.assertTrue(problems.isEmpty(), "content problems:\n" + String.join("\n", problems));
        helper.succeed();
    }

    /** Guards the test above: an index that read nothing would pass it without checking anything. */
    @GameTest(template = TestSupport.EMPTY)
    public static void scannerReadsTheRealContent(GameTestHelper helper) {
        ContentIndex content = ContentScanner.index(helper.getLevel().registryAccess());
        helper.assertTrue(content.skills().contains(Overgearedmod.id("pagma_link")), "skills should be read");
        helper.assertTrue(content.advancedClasses().contains(Overgearedmod.id("legendary_smith_successor")), "advanced classes should be read");
        helper.assertTrue(content.rewardedClasses().contains(Overgearedmod.id("legendary_smith_successor")), "class rewards should be read");
        helper.assertTrue(content.references().stream().anyMatch(r -> r.from().startsWith("class ")), "class → skill references");
        helper.assertTrue(content.references().stream().anyMatch(r -> r.from().startsWith("quest ")
                && r.kind() == ContentIndex.Kind.SKILL), "quest skill-book rewards");
        helper.assertTrue(content.references().stream().anyMatch(r -> r.from().equals("blacksmith trades")), "blacksmith books");
        helper.assertTrue(Overgearedmod.id("smith_firewood").equals(content.questRequirements().get(Overgearedmod.id("smith_iron"))),
                "quest chains (requires_quest)");
        helper.assertTrue(Overgearedmod.id("blacksmith").equals(content.questGivers().get(Overgearedmod.id("smith_firewood"))),
                "quest givers");
        helper.assertTrue(content.questGiverNpcs().contains(Overgearedmod.id("village_chief")), "NPCs with a quest window");
        helper.succeed();
    }
}
