package com.orijua.overgearedmod.feature.insight;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentsTest {
    @Test
    void bairansResidentsAlwaysHideATalent() {
        for (int i = 0; i < 50; i++) {
            assertTrue(Talents.of(UUID.randomUUID(), true).isPresent());
        }
    }

    @Test
    void elsewhereAboutOneInFourAndAlwaysTheSame() {
        int talented = 0;
        for (int i = 0; i < 4000; i++) {
            if (Talents.of(new UUID(i, i * 7L), false).isPresent()) {
                talented++;
            }
        }
        assertEquals(1000, talented, 50, "one in four");
        UUID someone = new UUID(8, 3);
        assertEquals(Talents.of(someone, false), Talents.of(someone, false), "fixed by the UUID");
    }
}
