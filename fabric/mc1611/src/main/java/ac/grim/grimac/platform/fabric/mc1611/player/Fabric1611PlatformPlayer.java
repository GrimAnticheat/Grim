package ac.grim.grimac.platform.fabric.mc1611.player;

import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayer;
import ac.grim.grimac.platform.fabric.utils.convert.FabricConversionUtil;
import net.kyori.adventure.text.Component;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class Fabric1611PlatformPlayer extends FabricPlatformPlayer {
    public Fabric1611PlatformPlayer(ServerPlayerEntity player) {
        super(player);
    }

    @Override
    public void kickPlayer(String textReason) {
        fabricPlayer.networkHandler.disconnect(new LiteralText(textReason));
    }

    @Override
    public void sendMessage(String message) {
        fabricPlayer.sendMessage(new LiteralText(message), false);
    }

    @Override
    public void sendMessage(Component message) {
        fabricPlayer.sendMessage(FabricConversionUtil.toNativeText(message), false);
    }

}
