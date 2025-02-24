package ac.grim.grimac.platform.fabric.player;


import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class FabricPlatformPlayerFactory implements PlatformPlayerFactory {

    @Override
    public PlatformPlayer getFromUUID(UUID uuid) {
        ServerPlayerEntity player = GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerManager().getPlayer(uuid);
        if (player == null) {
            return null;
        } else {
            return new FabricPlatformPlayer(player);
        }
    }

    @Override
    public PlatformPlayer getFromNativePlayerType(Object playerObject) {
        if (playerObject instanceof ServerPlayerEntity) {
            return new FabricPlatformPlayer((ServerPlayerEntity) playerObject);
        } else {
            throw new IllegalStateException("playerObject was not of type " + ServerPlayerEntity.class.getPackageName() + "." + ServerPlayerEntity.class.getName());
        }
    }
}
