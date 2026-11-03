package com.orijua.overgearedmod.feature.insight;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentsTest {
    @Test
    void bairansResidentsAlwaysHideATalent() {
        for (int i = 0; i < 50; i++) {
            assertTrue(Talents.of(new UUID(i * 31L + 1, i), true).isPresent());
        }
    }

    @Test
    void elsewhereAboutOneInFourAndAlwaysTheSame() {
        int talented = 0;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 4000; i++) {
            Optional<String> talent = Talents.of(new UUID(i, i * 7L), false);
            if (talent.isPresent()) {
                talented++;
                seen.add(talent.get());
            }
        }
        assertEquals(1000, talented, 50, "one in four");
        assertEquals(Set.copyOf(Talents.KINDS), seen, "every kind of talent comes up");
        UUID someone = new UUID(8, 3);
        assertEquals(Talents.of(someone, false), Talents.of(someone, false), "fixed by the UUID");
    }

    @Test
    void theTalentIsFixedByTheUuid() {
        assertEquals(Optional.of("magic"), Talents.of(new UUID(0, -1L), false), "floorMod(-1, 6) = 5");
        assertEquals(Optional.of("mining"), Talents.of(new UUID(0, 0), false));
        assertEquals(Optional.empty(), Talents.of(new UUID(1, 0), false), "floorMod(1, 4) != 0");
        assertEquals(Optional.of("mining"), Talents.of(new UUID(1, 0), true), "a Bairan resident always has one");
    }
}
