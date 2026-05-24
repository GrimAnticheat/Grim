package ac.grim.grimac.platform.fabric.mc261;

import ac.grim.grimac.platform.fabric.loader.Grim26ChainEntryPoint;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class Grim26ChainEntrypoint implements Grim26ChainEntryPoint {

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_26_1;
    }
}
