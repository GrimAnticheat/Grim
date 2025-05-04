package ac.grim.grimac.platform.fabric.utils.message;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Log4jBackedJULogger extends Logger {
    private final org.apache.logging.log4j.Logger log4jLogger;

    protected Log4jBackedJULogger(String name) {
        super(name, null);
        this.log4jLogger = org.apache.logging.log4j.LogManager.getLogger(name);
    }

    @Override
    public void log(Level level, String msg) {
        if (level == Level.SEVERE) {
            log4jLogger.error(msg);
        } else if (level == Level.WARNING) {
            log4jLogger.warn(msg);
        } else if (level == Level.INFO) {
            log4jLogger.info(msg);
        } else if (level == Level.CONFIG || level == Level.FINE) {
            log4jLogger.debug(msg);
        } else if (level == Level.FINER || level == Level.FINEST) {
            log4jLogger.trace(msg);
        } else {
            log4jLogger.info(msg);
        }
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
        if (level == Level.SEVERE) {
            log4jLogger.error(msg, thrown);
        } else if (level == Level.WARNING) {
            log4jLogger.warn(msg, thrown);
        } else if (level == Level.INFO) {
            log4jLogger.info(msg, thrown);
        } else if (level == Level.CONFIG || level == Level.FINE) {
            log4jLogger.debug(msg, thrown);
        } else if (level == Level.FINER || level == Level.FINEST) {
            log4jLogger.trace(msg, thrown);
        } else {
            log4jLogger.info(msg, thrown);
        }
    }
}
