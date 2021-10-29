package ac.grim.grimac.manager.init.load;

import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.latency.CompensatedWorldFlat;
import ac.grim.grimac.utils.nmsutil.XMaterial;

public class PaletteInit implements Initable {
    @Override
    public void start() {
        LogUtil.info("Initializing async packet chunk reader...");

        if (XMaterial.isNewVersion())
            CompensatedWorldFlat.init();
        CompensatedWorld.init();
    }
}
