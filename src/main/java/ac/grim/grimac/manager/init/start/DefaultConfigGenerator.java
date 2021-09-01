package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;

public class DefaultConfigGenerator implements Initable {
    @Override
    public void start() {
        GrimAPI.INSTANCE.getPlugin().saveDefaultConfig();
        GrimAPI.INSTANCE.getPlugin().reloadConfig();
    }
}
