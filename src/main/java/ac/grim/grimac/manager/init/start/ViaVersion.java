package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.viaversion.viaversion.api.Via;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;

public class ViaVersion implements Initable {
    @Override
    public void start() {
        if (!ViaVersionUtil.isAvailable() || !Via.getConfig().fix1_21PlacementRotation()) {
            return;
        }

        LogUtil.warn("GrimAC has detected that you are using ViaVersion with the `fix-1_21-placement-rotation` option enabled.");
        LogUtil.warn("This option is known to cause issues with GrimAC and may result in false positives and bypasses.");
        LogUtil.warn("Please disable this option in your ViaVersion configuration to prevent these issues.");
    }
}
