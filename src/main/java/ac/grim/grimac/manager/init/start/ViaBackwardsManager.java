package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.manager.init.Initable;

public class ViaBackwardsManager implements Initable {
    @Override
    public void start() {
        System.setProperty("com.viaversion.handlePingsAsInvAcknowledgements", "true");
    }
}
