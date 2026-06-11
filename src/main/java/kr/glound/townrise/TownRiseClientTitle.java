package kr.glound.townrise;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class TownRiseClientTitle {
    private static final String TITLE = "TownRise";
    private static boolean applied;

    private TownRiseClientTitle() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return;
        }
        if (!applied || minecraft.level == null) {
            minecraft.getWindow().setTitle(TITLE);
            applied = true;
        }
    }
}
