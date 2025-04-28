package ac.grim.grimac.platform.fabric.mc1171.player;

import ac.grim.grimac.platform.fabric.mc1161.player.Fabric1161PlatformPlayer;
import ac.grim.grimac.platform.fabric.utils.convert.FabricConversionUtil;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import net.minecraft.server.network.ServerPlayerEntity;


public class Fabric1170PlatformPlayer extends Fabric1161PlatformPlayer {
    public Fabric1170PlatformPlayer(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        fabricPlayer.changeGameMode(FabricConversionUtil.toFabricGameMode(gameMode));
    }
}
