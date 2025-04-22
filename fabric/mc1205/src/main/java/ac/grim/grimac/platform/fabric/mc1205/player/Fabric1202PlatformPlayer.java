package ac.grim.grimac.platform.fabric.mc1205.player;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1611.player.Fabric1161PlatformPlayer;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.platform.fabric.world.FabricPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public class Fabric1202PlatformPlayer extends Fabric1161PlatformPlayer {
    public Fabric1202PlatformPlayer(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    public void kickPlayer(String textReason) {
        fabricPlayer.networkHandler.disconnect(GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(textReason));
    }
}
