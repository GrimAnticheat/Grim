package ac.grim.grimac.manager.init.stop;

import ac.grim.grimac.manager.init.Initable;

public interface StoppableInitable extends Initable {
    void stop();
}
