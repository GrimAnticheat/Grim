package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.commands.GrimVersion;
import ac.grim.grimac.utils.anticheat.LogUtil;

public class UpdateChecker implements StartableInitable {
    @Override
    public void start() {
        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("check-for-updates", true)) {
            try {
                GrimVersion.checkForUpdatesAsync(GrimAPI.INSTANCE.getPlatformServer().getConsoleSender());
            } catch (LinkageError error) {
                LogUtil.warn("Failed to check for GrimAC updates due to an incompatible Adventure dependency.", error);
            }
        }
    }
}
