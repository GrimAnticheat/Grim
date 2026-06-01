package ac.grim.grimac.platform.fabric.mc1205.player;

import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import net.minecraft.server.level.ServerPlayer;

public class Fabric1202PlatformPlayer extends Fabric1170PlatformPlayer {
    public Fabric1202PlatformPlayer(ServerPlayer player) {
        super(player);
    }

    // disconnect(Component) signature changed at 1.20.2; recompiled here so 1.20.2+ resolves
    // the current overload (the 1.16.1-compiled base body targets the pre-1.20.2 one).
    @Override
    public void kickPlayer(String textReason) {
        serverPlayer().connection.disconnect(GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(textReason));
    }
}
