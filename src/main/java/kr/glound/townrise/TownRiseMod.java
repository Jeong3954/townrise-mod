package kr.glound.townrise;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(TownRiseInfo.MOD_ID)
public final class TownRiseMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    public TownRiseMod() {
        LOGGER.info("{} core mod loaded", TownRiseInfo.DISPLAY_NAME);

        NeoForge.EVENT_BUS.addListener(TownRiseAreaTool::registerCommands);
        NeoForge.EVENT_BUS.addListener(TownRiseAreaTool::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(TownRiseAreaTool::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(TownRiseAreaTool::onItemToss);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(TownRiseClientTitle::onClientTick);
            TownRiseSelfUpdater.startAsync();
        }
    }
}
