package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.entity.AbstractFabricGrimEntity;
import ac.grim.grimac.platform.fabric.utils.convert.FabricConversionUtil;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

public abstract class AbstractFabricPlatformPlayer extends AbstractFabricGrimEntity implements PlatformPlayer {
    @Getter
    protected ServerPlayerEntity fabricPlayer;
    protected final AbstractFabricPlatformInventory inventory;

    public AbstractFabricPlatformPlayer(ServerPlayerEntity player) {
        super(player);
        this.fabricPlayer = player;
        this.inventory = GrimACFabricLoaderPlugin.LOADER.getPlatformPlayerFactory().getPlatformInventory(player);
    }

    @Override
    public void kickPlayer(String textReason) {
        fabricPlayer.networkHandler.disconnect(GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(textReason));
    }

    @Override
    public boolean isSneaking() {
        return fabricPlayer.isSneaking();
    }

    @Override
    public void setSneaking(boolean isSneaking) {
        fabricPlayer.setSneaking(isSneaking);
    }

    @Override
    public void sendMessage(String message) {
        fabricPlayer.sendMessage(GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(message), false);
    }

    @Override
    public void sendMessage(Component message) {
        fabricPlayer.sendMessage(GrimACFabricLoaderPlugin.LOADER.getFabricConversionUtil().toNativeText(message), false);
    }

    @Override
    public boolean isOnline() {
        return !fabricPlayer.isDisconnected();
    }

    @Override
    public String getName() {
        return fabricPlayer.getName().getString();
    }

    @Override
    public void updateInventory() {
        fabricPlayer.currentScreenHandler.sendContentUpdates();
    }

    @Override
    public Vector3d getPosition() {
        return new Vector3d(fabricPlayer.getX(), fabricPlayer.getY(), fabricPlayer.getZ());
    }

    @Override
    public PlatformInventory getInventory() {
        return inventory;
    }

    @Override
    public GrimEntity getVehicle() {
        Entity vehicle = fabricPlayer.getVehicle();
        return vehicle != null ? GrimACFabricLoaderPlugin.LOADER.getPlatformPlayerFactory().getPlatformEntity(vehicle) : null;
    }

    @Override
    public GameMode getGameMode() {
        return FabricConversionUtil.fromFabricGameMode(fabricPlayer.interactionManager.getGameMode());
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        fabricPlayer.setGameMode(FabricConversionUtil.toFabricGameMode(gameMode));
    }

    @Override
    public UUID getUniqueId() {
        return fabricPlayer.getUuid();
    }

    @Override
    public boolean isExternalPlayer() {
        return false;
    }

    @Override
    public void sendPluginMessage(String channelName, byte[] byteArray) {
        // You might want to use Fabric's networking system here
//        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(
//                Identifier.of(channelName),
//                new PacketByteBuf(Unpooled.wrappedBuffer(byteArray))
//        );
//        fabricPlayer.networkHandler.sendPacket(packet);
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceNativePlayer(Object nativePlayerObject) {
        this.inventory.fabricPlayer = (ServerPlayerEntity) nativePlayerObject;
        this.inventory.inventory = ((ServerPlayerEntity) nativePlayerObject).inventory;
        this.fabricPlayer = (ServerPlayerEntity) nativePlayerObject;
    }

    @Override @NonNull
    public ServerPlayerEntity getNative() {
        return this.fabricPlayer;
    }

    @Override
    public boolean isDead() {
        return fabricPlayer.isDead();
    }
}
