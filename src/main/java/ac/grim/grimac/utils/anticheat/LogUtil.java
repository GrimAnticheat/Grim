package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import lombok.experimental.UtilityClass;

import java.util.logging.Level;
import java.util.logging.Logger;

@UtilityClass
public class LogUtil {
    public void info(final String info) {
        getLogger().info(info);
    }

    public void warn(final String warn) {
        getLogger().warning(warn);
    }

    public void error(final String error) {
        getLogger().severe(error);
    }

    public void error(final String error, Throwable throwable) {
        getLogger().log(Level.SEVERE, error, throwable);
    }

    public Logger getLogger() {
        return GrimAPI.INSTANCE.getPlugin().getLogger();
    }
}
