package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.BlockTranslator;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.entity.AbstractFabricGrimEntity;
import ac.grim.grimac.platform.fabric.utils.PolymerHook;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AbstractFabricPlatformPlayer extends AbstractFabricGrimEntity implements PlatformPlayer {
    protected volatile ServerPlayer fabricPlayer;
    protected final AbstractFabricPlatformInventory inventory;
    private final @Nullable User user;
    @Getter private final BlockTranslator blockTranslator;

    public AbstractFabricPlatformPlayer(ServerPlayer player) {
        super(player);
        this.fabricPlayer = player;
        this.inventory = GrimACFabricLoaderPlugin.LOADER.getPlatformPlayerFactory().getPlatformInventory(this);
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value()) {
            Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(fabricPlayer.getUUID());
            this.user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
        } else {
            this.user = null;
        }

        this.blockTranslator = PolymerHook.createTranslator(this.fabricPlayer);
    }

    @Override
    public void kickPlayer(String textReason) {
        fabricPlayer.connection.disconnect(GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(textReason));
    }

    // PROTOTYPE (refactor/fabric-dedupe spike): the methods below now delegate to the
    // Loom-injected GrimInjectedServerPlayer bridge instead of calling NMS directly.
    // After this change their bodies are byte-identical to the intermediary aggregator's
    // copy (the bridge hides the only difference -- message-send -- which is NOT routed
    // through the bridge and stays below). The cast mirrors AbstractFabricGrimEntity's
    // proven `(PlatformWorld) (Object) entity.level()` pattern.
    private ac.grim.grimac.platform.fabric.inject.GrimInjectedServerPlayer grim$injected() {
        return (ac.grim.grimac.platform.fabric.inject.GrimInjectedServerPlayer) (Object) fabricPlayer;
    }

    @Override
    public boolean isSneaking() {
        return grim$injected().grim$isSneaking();
    }

    @Override
    public void setSneaking(boolean isSneaking) {
        grim$injected().grim$setSneaking(isSneaking);
    }

    @Override
    public boolean hasPermission(String permission) {
        return getSender().hasPermission(permission);
    }

    @Override
    public boolean hasPermission(String permission, boolean defaultIfUnset) {
        return getSender().hasPermission(permission, defaultIfUnset);
    }

    @Override
    public void sendMessage(String message) {
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else {
            var messageUtils = GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils();
            messageUtils.sendSystemMessageToPlayer(fabricPlayer, messageUtils.textLiteral(message));
        }
    }

    @Override
    public void sendMessage(Component message) {
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else {
            GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils()
                    .sendSystemMessageToPlayer(fabricPlayer, GrimACFabricLoaderPlugin.LOADER.getFabricConversionUtil().toNativeText(message));
        }
    }

    @Override
    public boolean isOnline() {
        return grim$injected().grim$isOnline();
    }

    @Override
    public String getName() {
        // getName() clash dodged: the bridge method is grim$name(), never getName(),
        // so it cannot collide with NMS ServerPlayer.getName():Component.
        return grim$injected().grim$name();
    }

    @Override
    public void updateInventory() {
        grim$injected().grim$broadcastInventoryChanges();
    }

    @Override
    public Vector3d getPosition() {
        return grim$injected().grim$position();
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
        return grim$injected().grim$gameMode();
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        grim$injected().grim$setGameMode(gameMode);
    }

    @Override
    public UUID getUniqueId() {
        return grim$injected().grim$uuid();
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
        this.fabricPlayer = (ServerPlayer) nativePlayerObject;
    }

    @Override
    public @NotNull ServerPlayer getNative() {
        return this.fabricPlayer;
    }

    @Override
    public boolean isDead() {
        return grim$injected().grim$isDead();
    }
}
