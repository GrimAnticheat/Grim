package ac.grim.grimac.manager.init;

import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.manager.init.stop.StoppableInitable;
import ac.grim.grimac.utils.anticheat.LogUtil;

public abstract class OptionalReflectiveInitable implements StartableInitable, StoppableInitable {
    private final String handlerClassName;
    private final String errorMessage;
    private Initable delegate;

    protected OptionalReflectiveInitable(String handlerClassName, String errorMessage) {
        this.handlerClassName = handlerClassName;
        this.errorMessage = errorMessage;
    }

    protected abstract boolean isAvailable();

    @Override
    public final void start() {
        if (!isAvailable()) return;

        try {
            Class<? extends Initable> handlerClass = Class.forName(handlerClassName).asSubclass(Initable.class);
            delegate = handlerClass.getDeclaredConstructor().newInstance();
            if (delegate instanceof StartableInitable startable) {
                startable.start();
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            LogUtil.error(errorMessage, e);
        }
    }

    @Override
    public final void stop() {
        if (delegate instanceof StoppableInitable stoppable) {
            stoppable.stop();
        }
        delegate = null;
    }
}
