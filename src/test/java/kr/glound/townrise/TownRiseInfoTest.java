package kr.glound.townrise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TownRiseInfoTest {
    @Test
    void exposesStableModIdentity() {
        assertEquals("townrise", TownRiseInfo.MOD_ID);
        assertEquals("TownRise", TownRiseInfo.DISPLAY_NAME);
    }
}
