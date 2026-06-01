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

    // Mirror vanilla names. Per-runtime asymmetry is REQUIRED: official mixin must NOT body these
    // (vanilla ServerPlayer.isUsingItem satisfies the interface; a same-named body self-recurses),
    // intermediary mixin MUST body them (vanilla is method_6115/method_6021, no clash). TODO: if a
    // future MC renames these, update the intermediary ServerPlayerMixin bodies.
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
