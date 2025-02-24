package ac.grim.grimac.platform.fabric.initables;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.platform.fabric.utils.metrics.MetricsFabric;

public class FabricBStats implements Initable {
    @Override
    public void start() {
        int pluginId = 12820; // <-- Replace with the id of your plugin!
        try {
            new MetricsFabric(GrimAPI.INSTANCE.getGrimPlugin(), pluginId);
        } catch (Exception ignored) {
        }
    }
}
