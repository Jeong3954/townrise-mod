package kr.glound.townrise;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(TownRiseInfo.MOD_ID)
public final class TownRiseMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    public TownRiseMod() {
        LOGGER.info("{} core mod loaded", TownRiseInfo.DISPLAY_NAME);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            TownRiseSelfUpdater.startAsync();
        }
    }
}
