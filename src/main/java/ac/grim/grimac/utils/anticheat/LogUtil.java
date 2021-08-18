package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LogUtil {
    public void info(final String info) {
        GrimAPI.INSTANCE.getPlugin().getLogger().info(info);
    }

    public void warn(final String warn) {
        GrimAPI.INSTANCE.getPlugin().getLogger().info(warn);
    }

    public void error(final String error) {
        GrimAPI.INSTANCE.getPlugin().getLogger().info(error);
    }
}
