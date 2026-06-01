package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.BlockTranslator;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.entity.AbstractFabricGrimEntity;
import ac.grim.grimac.platform.fabric.inject.FabricServerPlayerHandle;
import ac.grim.grimac.platform.fabric.utils.PolymerHook;
import ac.grim.grimac.platform.fabric.utils.convert.FabricConversionUtil;
import ac.grim.grimac.utils.common.arguments.CommonGrimArguments;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// NOT in fabric-common (the dedup target): this class extends AbstractFabricGrimEntity,
// whose single source-of-truth field is typed net.minecraft.world.entity.Entity (kept as
// Entity per maintainer), and it still needs the raw ServerPlayer for the per-version bits
// the subclasses override (getSender / teleportAsync) plus PolymerHook.createTranslator at
// construction. A fabric-common (java-library, NMS-free) class cannot extend an NMS-bound
// one, so the wrapper stays per-version. The version-stable reads, however, no longer
// duplicate an NMS call per aggregator: they route through handle() (the Loom-injected
// FabricServerPlayerHandle), so the two copies of this file are byte-identical and the
// only real per-version code lives in the thin Fabric<ver>PlatformPlayer subclasses.
public abstract class AbstractFabricPlatformPlayer extends AbstractFabricGrimEntity implements PlatformPlayer {
    protected final AbstractFabricPlatformInventory inventory;
    private final @Nullable User user;
    @Getter private final BlockTranslator blockTranslator;

    public AbstractFabricPlatformPlayer(ServerPlayer player) {
        super(player);
        this.inventory = GrimACFabricLoaderPlugin.LOADER.getPlatformPlayerFactory().getPlatformInventory(this);
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value()) {
            Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(player.getUUID());
            this.user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
        } else {
            this.user = null;
        }

        this.blockTranslator = PolymerHook.createTranslator(player);
    }

    /** The single native handle, narrowed to ServerPlayer (the inherited entity is always one for players). */
    protected ServerPlayer serverPlayer() {
        return (ServerPlayer) this.entity;
    }

    /**
     * The current native player viewed through the Loom-injected {@link FabricServerPlayerHandle}.
     * Resolved fresh from the (volatile) inherited entity each call, so it tracks a
     * respawn/dimension-change rebind, and removes the repeated inline
     * {@code ((FabricServerPlayerHandle) (Object) serverPlayer())} casts (maintainer request).
     */
    protected FabricServerPlayerHandle handle() {
        return (FabricServerPlayerHandle) (Object) this.entity;
    }

    @Override
    public void kickPlayer(String textReason) {
        // disconnect(Component) signature changed at 1.20.2; mc1205 overrides this. Not a
        // handle bridge: a single 1.16.1-compiled mixin body would NoSuchMethodError on 1.20.2+.
        serverPlayer().connection.disconnect(GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(textReason));
    }

    @Override
    public boolean isSneaking() {
        return handle().isSneaking();
    }

    @Override
    public void setSneaking(boolean isSneaking) {
        handle().setSneaking(isSneaking);
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
            var nativeText = GrimACFabricLoaderPlugin.LOADER.getFabricMessageUtils().textLiteral(message);
            handle().sendSystemText(nativeText);
        }
    }

    @Override
    public void sendMessage(Component message) {
        if (CommonGrimArguments.USE_CHAT_FAST_BYPASS.value() && user != null) {
            user.sendMessage(message);
        } else {
            var nativeText = GrimACFabricLoaderPlugin.LOADER.getFabricConversionUtil().toNativeText(message);
            handle().sendSystemText(nativeText);
        }
    }

    @Override
    public boolean isOnline() {
        return !handle().isDisconnected();
    }

    @Override
    public String getUsername() {
        return handle().usernameString();
    }

    @Override
    public void updateInventory() {
        handle().broadcastInventoryChanges();
    }

    @Override
    public Vector3d getPosition() {
        FabricServerPlayerHandle handle = handle();
        return new Vector3d(handle.posX(), handle.posY(), handle.posZ());
    }

    @Override
    public PlatformInventory getPlayerInventory() {
        return inventory;
    }

    @Override
    public GrimEntity getVehicleEntity() {
        // vehicleEntity() returns the NMS Entity as Object; wrap it through the platform factory.
        net.minecraft.world.entity.Entity vehicle = (net.minecraft.world.entity.Entity) handle().vehicleEntity();
        return vehicle != null ? GrimACFabricLoaderPlugin.LOADER.getPlatformPlayerFactory().getPlatformEntity(vehicle) : null;
    }

    @Override
    public GameMode getGameMode() {
        // Per-version NMS: not bridged because ServerPlayer.setGameMode's return type
        // differs across the intermediary line (void on 1.16.1, boolean on 1.17+), so the
        // setter cannot be a single shared mixin body. Kept symmetric with setGameMode.
        return FabricConversionUtil.fromFabricGameMode(serverPlayer().gameMode.getGameModeForPlayer());
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        serverPlayer().setGameMode(FabricConversionUtil.toFabricGameMode(gameMode));
    }

    @Override
    public UUID getUniqueId() {
        return handle().uuid();
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
//        serverPlayer().networkHandler.sendPacket(packet);
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceNativePlayer(Object nativePlayerObject) {
        // Rebinds the ONE native field (inherited entity); handle() reads it fresh afterwards.
        setNativeEntity((ServerPlayer) nativePlayerObject);
    }

    @Override
    public @NotNull ServerPlayer getNative() {
        return serverPlayer();
    }

    @Override
    public boolean isDead() {
        return handle().isDead();
    }
}
