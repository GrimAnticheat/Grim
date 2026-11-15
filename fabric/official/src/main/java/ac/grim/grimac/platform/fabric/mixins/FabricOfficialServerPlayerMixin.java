package ac.grim.grimac.platform.fabric.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(ServerPlayer.class)
@Implements(@Interface(iface = ac.grim.grimac.platform.fabric.inject.FabricServerPlayerHandle.class, prefix = "grim$"))
abstract class FabricOfficialServerPlayerMixin extends Player {

    public FabricOfficialServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    public void grim$resyncSharedFlags() {
        this.getEntityData().getItem(DATA_SHARED_FLAGS_ID).setDirty(true);
        this.getEntityData().isDirty = true;
    }

    public boolean grim$isDead() {
        return this.isDeadOrDying();
    }

    public void grim$sendSystemText(Object nativeComponent) {
        ((ServerPlayer) (Object) this).sendSystemMessage((Component) nativeComponent, false);
    }

    public boolean grim$isDisconnected() {
        return ((ServerPlayer) (Object) this).hasDisconnected();
    }

    public String grim$usernameString() {
        return this.getName().getString();
    }

    public void grim$broadcastInventoryChanges() {
        this.containerMenu.broadcastChanges();
    }

    public double grim$posX() {
        return this.getX();
    }

    public double grim$posY() {
        return this.getY();
    }

    public double grim$posZ() {
        return this.getZ();
    }

    public UUID grim$uuid() {
        return this.getUUID();
    }

    public Object grim$vehicleEntity() {
        return this.getVehicle();
    }

    public Object grim$gameMode() {
        return ((ServerPlayer) (Object) this).gameMode.getGameModeForPlayer();
    }

    public Object grim$heldItemStack() {
        return this.inventory.getSelectedItem();
    }

    public Object grim$inventoryItemAt(int slot) {
        return this.inventory.getItem(slot);
    }

    public Object grim$usedItemHand() {
        return this.getUsedItemHand();
    }

    public int grim$inventorySlotCount() {
        return this.inventory.getContainerSize();
    }

    // NOTE: isUsingItem()/stopUsingItem() are intentionally NOT bodied here. On the mojmap
    // runtime the vanilla ServerPlayer methods of the same name satisfy the injected interface
    // directly; a grim$ body would graft a same-named method and self-recurse (StackOverflow).
    // The intermediary mixin DOES body them (vanilla is method_6115/method_6021 there, so no clash).
}
