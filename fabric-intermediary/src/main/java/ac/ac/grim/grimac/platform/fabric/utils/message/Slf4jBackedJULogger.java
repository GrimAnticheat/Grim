package ac.grim.grimac.platform.fabric.utils.message;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Slf4jBackedJULogger extends Logger {
    private final org.slf4j.Logger slf4jLogger;
    private static final Marker MARKER = MarkerFactory.getMarker("JUL");

    public Slf4jBackedJULogger(String name) {
        super(name, null);
        this.slf4jLogger = LoggerFactory.getLogger(name);
    }

    @Override
    public void log(Level level, String msg) {
        if (level == Level.SEVERE) {
            slf4jLogger.error(MARKER, msg);
        } else if (level == Level.WARNING) {
            slf4jLogger.warn(MARKER, msg);
        } else if (level == Level.INFO) {
            slf4jLogger.info(MARKER, msg);
        } else if (level == Level.CONFIG || level == Level.FINE) {
            slf4jLogger.debug(MARKER, msg);
        } else if (level == Level.FINER || level == Level.FINEST) {
            slf4jLogger.trace(MARKER, msg);
        } else {
            slf4jLogger.info(MARKER, msg);
        }
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
        if (level.equals(Level.SEVERE)) {
            slf4jLogger.error(MARKER, msg, thrown);
        } else if (level.equals(Level.WARNING)) {
            slf4jLogger.warn(MARKER, msg, thrown);
        } else if (level.equals(Level.INFO)) {
            slf4jLogger.info(MARKER, msg, thrown);
        } else if (level.equals(Level.CONFIG) || level.equals(Level.FINE)) {
            slf4jLogger.debug(MARKER, msg, thrown);
        } else if (level.equals(Level.FINER) || level.equals(Level.FINEST)) {
            slf4jLogger.trace(MARKER, msg, thrown);
        } else {
            slf4jLogger.info(MARKER, msg, thrown);
        }
    }
}
