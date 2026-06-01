package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.fabric.inject.GrimInjectedServerPlayer;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.utils.convert.FabricConversionUtil;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * PROTOTYPE addition (refactor/fabric-dedupe spike), INTERMEDIARY/Yarn mappings.
 *
 * <p>FINDING worth noting: although this module compiles against Yarn mappings, Loom
 * remaps NMS members to the SAME source-level names the official module uses
 * (isShiftKeyDown, getName, containerMenu, gameMode, isDeadOrDying, ...). So the bridge
 * bodies below are byte-for-byte identical to the official ServerPlayerMixin -- the
 * only thing that genuinely differs between the two aggregators' player code is the
 * message-send call (sendSystemMessage vs displayClientMessage), which the spike
 * deliberately leaves OUT of the injected bridge.
 */
@Mixin(ServerPlayer.class)
@Implements(@Interface(iface = GrimInjectedServerPlayer.class, prefix = "grim$"))
abstract class ServerPlayerMixin {
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void onRestoreFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ((FabricPlatformPlayerFactory) GrimAPI.INSTANCE.getPlatformPlayerFactory()).replaceNativePlayer(oldPlayer.getUUID(), (ServerPlayer) (Object) this);
    }

    // --- PROTOTYPE: GrimInjectedServerPlayer bridge bodies (prefix-stripped to grim$*) ---

    // See official ServerPlayerMixin: containerMenu (declared on Player) must be
    // @Shadow-ed for mixin-context access; the access widener alone is not enough here.
    @Shadow
    public AbstractContainerMenu containerMenu;

    private ServerPlayer grim$self() {
        return (ServerPlayer) (Object) this;
    }

    public boolean grim$isSneaking() {
        return grim$self().isShiftKeyDown();
    }

    public void grim$setSneaking(boolean sneaking) {
        grim$self().setShiftKeyDown(sneaking);
    }

    public boolean grim$isOnline() {
        return !grim$self().hasDisconnected();
    }

    public String grim$name() {
        return grim$self().getName().getString();
    }

    public void grim$broadcastInventoryChanges() {
        this.containerMenu.broadcastChanges();
    }

    public Vector3d grim$position() {
        ServerPlayer p = grim$self();
        return new Vector3d(p.getX(), p.getY(), p.getZ());
    }

    public GameMode grim$gameMode() {
        return FabricConversionUtil.fromFabricGameMode(grim$self().gameMode.getGameModeForPlayer());
    }

    public void grim$setGameMode(GameMode gameMode) {
        grim$self().setGameMode(FabricConversionUtil.toFabricGameMode(gameMode));
    }

    public UUID grim$uuid() {
        return grim$self().getUUID();
    }

    public boolean grim$isDead() {
        return grim$self().isDeadOrDying();
    }
}
