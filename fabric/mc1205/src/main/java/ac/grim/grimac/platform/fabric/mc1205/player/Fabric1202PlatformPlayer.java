package ac.grim.grimac.platform.fabric.mc1205.player;

import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import net.minecraft.server.level.ServerPlayer;

public class Fabric1202PlatformPlayer extends Fabric1170PlatformPlayer {
    public Fabric1202PlatformPlayer(ServerPlayer player) {
        super(player);
    }

    @Override
    public void kickPlayer(String textReason) {
        fabricPlayer.connection.disconnect(GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(textReason));
    }
}
