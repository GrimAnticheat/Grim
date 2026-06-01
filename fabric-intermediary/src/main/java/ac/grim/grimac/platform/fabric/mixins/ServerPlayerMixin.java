package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.platform.fabric.inject.FabricServerPlayerHandle;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * INTERMEDIARY mappings (Minecraft 1.16.1 .. 1.21.11). Supplies the
 * {@link FabricServerPlayerHandle} bodies on {@code ServerPlayer}, exactly mirroring the
 * proven {@code LevelMixin} -> {@code PlatformWorld} pattern (which uses prefix
 * {@code grimac$}). The interface methods are BARE-named; each body below is
 * {@code grim$}-prefixed and Mixin strips the prefix, validates the bare name against
 * {@link FabricServerPlayerHandle}, and grafts it onto {@code ServerPlayer}.
 *
 * <p>The official (26.x) copy of this mixin differs only where the NMS API renamed across
 * mappings: this one calls {@code displayClientMessage} (vs {@code sendSystemMessage}) for
 * the system message, and {@code getSelected} (vs {@code getSelectedItem}) for the held
 * item. Both resolve to the same behaviour per family.
 *
 * <p>No {@code grim$}-prefixed helper method is declared: Mixin would strip the prefix off
 * such a helper and fail to find the bare name in the interface. The cast
 * {@code (ServerPlayer) (Object) this} is used inline instead, matching {@code LevelMixin}.
 */
@Mixin(ServerPlayer.class)
@Implements(@Interface(iface = FabricServerPlayerHandle.class, prefix = "grim$"))
abstract class ServerPlayerMixin {

    public boolean grim$isSneaking() {
        return ((ServerPlayer) (Object) this).isShiftKeyDown();
    }

    public void grim$setSneaking(boolean sneaking) {
        ((ServerPlayer) (Object) this).setShiftKeyDown(sneaking);
    }

    // Bare name is isDead (not isDeadOrDying): a bridge named isDeadOrDying would override
    // LivingEntity.isDeadOrDying()Z and recurse. isDead has no vanilla collision.
    public boolean grim$isDead() {
        return ((ServerPlayer) (Object) this).isDeadOrDying();
    }

    // Intermediary-line non-overlay system message. nativeComponent is the NMS Component
    // (Object in the NMS-free interface); cast it back here.
    public void grim$sendSystemText(Object nativeComponent) {
        ((ServerPlayer) (Object) this).displayClientMessage((Component) nativeComponent, false);
    }

    public boolean grim$isDisconnected() {
        return ((ServerPlayer) (Object) this).hasDisconnected();
    }

    public String grim$usernameString() {
        return ((ServerPlayer) (Object) this).getName().getString();
    }

    public void grim$broadcastInventoryChanges() {
        ((ServerPlayer) (Object) this).containerMenu.broadcastChanges();
    }

    public double grim$posX() {
        return ((ServerPlayer) (Object) this).getX();
    }

    public double grim$posY() {
        return ((ServerPlayer) (Object) this).getY();
    }

    public double grim$posZ() {
        return ((ServerPlayer) (Object) this).getZ();
    }

    public UUID grim$uuid() {
        return ((ServerPlayer) (Object) this).getUUID();
    }

    public Object grim$vehicleEntity() {
        return ((ServerPlayer) (Object) this).getVehicle();
    }

    // Intermediary line: Inventory.getSelected() (renamed getSelectedItem() in 26.x). #14 divergence.
    public Object grim$heldItemStack() {
        return ((ServerPlayer) (Object) this).inventory.getSelected();
    }

    public Object grim$inventoryItemAt(int slot) {
        return ((ServerPlayer) (Object) this).inventory.getItem(slot);
    }

    public int grim$inventorySlotCount() {
        return ((ServerPlayer) (Object) this).inventory.getContainerSize();
    }
}
