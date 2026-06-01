package ac.grim.grimac.platform.fabric.inject;

import java.util.UUID;

public interface FabricServerPlayerHandle {

    boolean isSneaking();

    void setSneaking(boolean sneaking);

    boolean isDead();

    void sendSystemText(Object nativeComponent);

    boolean isDisconnected();

    String usernameString();

    void broadcastInventoryChanges();

    // isUsingItem()/stopUsingItem() deliberately reuse vanilla's own method names, which forces the
    // two runtimes to behave differently:
    //   - Official (Mojang-mapped) builds: ServerPlayer already declares these names, so vanilla's
    //     own methods satisfy this interface for free. The official mixin must NOT add bodies for
    //     them -- a method with the same name would just call itself and StackOverflow.
    //   - Intermediary builds: the real methods are obfuscated (method_6115 / method_6021), so these
    //     names clash with nothing, and the mixin MUST add bodies that forward to vanilla.
    // TODO: if a future Minecraft version renames these, update the intermediary ServerPlayerMixin.
    void stopUsingItem();

    boolean isUsingItem();

    double posX();

    double posY();

    double posZ();

    UUID uuid();

    Object vehicleEntity();

    Object gameMode();

    Object heldItemStack();

    Object inventoryItemAt(int slot);

    Object usedItemHand();

    int inventorySlotCount();
}
