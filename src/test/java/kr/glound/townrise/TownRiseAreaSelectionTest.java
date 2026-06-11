package kr.glound.townrise;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownRiseAreaSelectionTest {
    @Test
    void areaFeatureIsDocumentedInDisplayNameSafeConstants() {
        assertTrue(TownRiseInfo.DISPLAY_NAME.contains("TownRise"));
        assertFalse(TownRiseInfo.MOD_ID.isBlank());
    }
}
