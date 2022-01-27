package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import lombok.experimental.UtilityClass;

import java.util.logging.Logger;

@UtilityClass
public class LogUtil {
    public void info(final String info) {
        getLogger().info(info);
    }

    public void warn(final String warn) {
        getLogger().info(warn);
    }

    public void error(final String error) {
        getLogger().info(error);
    }

    public Logger getLogger() {
        return GrimAPI.INSTANCE.getPlugin().getLogger();
    }
}
