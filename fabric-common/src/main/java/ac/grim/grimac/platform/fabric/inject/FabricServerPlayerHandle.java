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
