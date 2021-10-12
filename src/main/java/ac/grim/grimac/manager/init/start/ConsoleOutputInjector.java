package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.manager.init.Initable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class ConsoleOutputInjector implements Initable {
    @Override
    public void start() {
        Logger coreLogger = (Logger) LogManager.getRootLogger();

        ConsoleOutputAppender appender = new ConsoleOutputAppender();
        appender.start();

        coreLogger.addAppender(appender);
    }
}
