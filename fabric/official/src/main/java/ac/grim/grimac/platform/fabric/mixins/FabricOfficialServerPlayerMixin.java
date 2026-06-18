package ac.grim.grimac.platform.fabric.mixins;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(ServerPlayer.class)
@Implements(@Interface(iface = ac.grim.grimac.platform.fabric.inject.FabricServerPlayerHandle.class, prefix = "grim$"))
abstract class FabricOfficialServerPlayerMixin {

    public boolean grim$isSneaking() {
        return ((ServerPlayer) (Object) this).isShiftKeyDown();
    }

    public void grim$setSneaking(boolean sneaking) {
        ((ServerPlayer) (Object) this).setShiftKeyDown(sneaking);
    }

    public boolean grim$isDead() {
        return ((ServerPlayer) (Object) this).isDeadOrDying();
    }

    public void grim$sendSystemText(Object nativeComponent) {
        ((ServerPlayer) (Object) this).sendSystemMessage((Component) nativeComponent, false);
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

    public Object grim$gameMode() {
        return ((ServerPlayer) (Object) this).gameMode.getGameModeForPlayer();
    }

    public Object grim$heldItemStack() {
        return ((ServerPlayer) (Object) this).inventory.getSelectedItem();
    }

    public Object grim$inventoryItemAt(int slot) {
        return ((ServerPlayer) (Object) this).inventory.getItem(slot);
    }

    public Object grim$usedItemHand() {
        return ((ServerPlayer) (Object) this).getUsedItemHand();
    }

    public int grim$inventorySlotCount() {
        return ((ServerPlayer) (Object) this).inventory.getContainerSize();
    }

    // NOTE: isUsingItem()/stopUsingItem() are intentionally NOT bodied here. On the mojmap
    // runtime the vanilla ServerPlayer methods of the same name satisfy the injected interface
    // directly; a grim$ body would graft a same-named method and self-recurse (StackOverflow).
    // The intermediary mixin DOES body them (vanilla is method_6115/method_6021 there, so no clash).
}
