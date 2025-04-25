package ac.grim.grimac.platform.fabric.mc1205.player;

import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1611.player.Fabric1161PlatformPlayer;
import net.minecraft.server.network.ServerPlayerEntity;


public class Fabric1202PlatformPlayer extends Fabric1161PlatformPlayer {
    public Fabric1202PlatformPlayer(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    public void kickPlayer(String textReason) {
        fabricPlayer.networkHandler.disconnect(GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(textReason));
    }
}
