package ac.grim.grimac.platform.fabric.inject;

import java.util.UUID;

/**
 * A Loom-injected handle interface grafted onto the NMS
 * {@code net.minecraft.server.level.ServerPlayer} via {@code loom:injected_interfaces}
 * in each Fabric aggregator's {@code fabric.mod.json}. The bodies are supplied per
 * mapping family by a {@code @Mixin(ServerPlayer.class)
 * @Implements(@Interface(iface = FabricServerPlayerHandle.class, prefix = "grim$"))}
 * (see {@code ServerPlayerMixin} in fabric-official and fabric-intermediary).
 *
 * <p>WHY THIS LIVES IN fabric-common: this module is a plain {@code java-library} with
 * no Minecraft and no Loom (see fabric-common/build.gradle.kts). Every type referenced
 * below is JDK-only, so the contract holds. This is what lets the shared Fabric
 * inventory wrapper ({@code AbstractFabricPlatformInventory}, now a single copy in
 * fabric-common) and both copies of the player wrapper reference each per-version NMS
 * call once instead of inlining a cast in each aggregator. It mirrors the existing
 * {@code Level -> PlatformWorld} injection (PlatformWorld lives in the equally NMS-free
 * {@code common} module).
 *
 * <p>NAMING CONTRACT (do not break — this is what crashed a prior spike): the methods
 * here are declared with BARE names. Mixin strips the {@code grim$} {@code @Implements}
 * prefix from each mixin body and validates the resulting BARE name against this
 * interface, then grafts that bare name onto {@code ServerPlayer}. Declaring the
 * methods here as {@code grim$isSneaking()} (prefixed) would make the stripped body
 * name {@code isSneaking} fail to match, producing an {@code InvalidMixinException} at
 * boot. The proven convention is {@code LevelMixin}/{@code PlatformWorld}: bare names
 * in the interface, {@code grim$}-prefixed bodies in the mixin.
 *
 * <p>COLLISION CONTRACT (issue #2568): a prefix-stripped bridge whose name+descriptor
 * matches a real {@code ServerPlayer}/{@code Player}/{@code LivingEntity}/{@code Entity}
 * method would OVERRIDE the vanilla method and can infinite-recurse. Every bare name
 * below was checked against the official 26.1.2 jar and the layered 1.16.1 / 1.21.11
 * jars and collides with NOTHING. Vanilla names the bridges deliberately AVOID: vanilla
 * uses {@code isShiftKeyDown}/{@code setShiftKeyDown} (not {@code isSneaking}), {@code
 * isDeadOrDying} (not {@code isDead}), {@code getName}/{@code getUUID}/{@code getX/getY/getZ}/{@code
 * getVehicle} (all return NMS types or collide on descriptor, hence the grim-distinct
 * {@code usernameString}/{@code uuid}/{@code posX..posZ}/{@code vehicleEntity}), and
 * {@code getSelected}/{@code getSelectedItem} (the per-version inventory divergence,
 * unified here as {@code heldItemStack}).
 */
public interface FabricServerPlayerHandle {

    /** @see net.minecraft.world.entity.Entity#isShiftKeyDown() (bridged as a non-colliding name) */
    boolean isSneaking();

    /** @see net.minecraft.world.entity.Entity#setShiftKeyDown(boolean) (bridged as a non-colliding name) */
    void setSneaking(boolean sneaking);

    /** @see net.minecraft.world.entity.LivingEntity#isDeadOrDying() (bridged as a non-colliding name) */
    boolean isDead();

    /**
     * Sends a native text component to this player as a plain (non-overlay) system message.
     *
     * <p>The per-version mixin body supplies the version-specific NMS call: official 26.x
     * uses {@code sendSystemMessage(component, false)}; the intermediary line uses
     * {@code displayClientMessage(component, false)}. Both reach
     * {@code ClientboundSystemChatPacket(component, /*overlay=*&#47;false)} at {@code false}.
     * The argument is typed {@code Object} because fabric-common cannot reference the NMS
     * {@code net.minecraft.network.chat.Component} type; the mixin body casts it back.
     *
     * @param nativeComponent the native {@code net.minecraft.network.chat.Component} to send
     */
    void sendSystemText(Object nativeComponent);

    /** @see net.minecraft.server.level.ServerPlayer#hasDisconnected() (bridged as a non-colliding name) */
    boolean isDisconnected();

    /** Player display name as a plain {@code String} (was {@code getName().getString()}). */
    String usernameString();

    /** Pushes pending container slot changes to the client (was {@code containerMenu.broadcastChanges()}). */
    void broadcastInventoryChanges();

    /** @see net.minecraft.world.entity.Entity#getX() (bridged; bare {@code getX} would collide). */
    double posX();

    /** @see net.minecraft.world.entity.Entity#getY() (bridged; bare {@code getY} would collide). */
    double posY();

    /** @see net.minecraft.world.entity.Entity#getZ() (bridged; bare {@code getZ} would collide). */
    double posZ();

    /** This player's UUID (was {@code getUUID()}; bare {@code getUUID} would collide). */
    UUID uuid();

    /**
     * This player's current vehicle, or {@code null} (was {@code getVehicle()}).
     *
     * <p>Returns {@code Object} (the NMS {@code net.minecraft.world.entity.Entity}) because
     * fabric-common cannot reference it; the caller wraps it through the platform entity
     * factory. Named {@code vehicleEntity} because bare {@code getVehicle} collides.
     */
    Object vehicleEntity();

    // Deliberately NOT bridged: gamemode set. ServerPlayer.setGameMode(GameType) returns
    // void on 1.16.1 but boolean on 1.17+, so a single 1.16.1-compiled intermediary mixin
    // body would emit a (GameType)V invocation that fails to link on 1.17+. Gamemode stays a
    // per-version NMS call in the wrapper (Fabric1170PlatformPlayer overrides it for 1.17+).

    /**
     * The item in this player's selected hotbar slot, as the NMS {@code ItemStack}
     * (returned as {@code Object} since fabric-common cannot reference it). THIS bridge
     * removes the #14 inventory divergence: the per-version body calls {@code
     * inventory.getSelected()} (intermediary, &le;1.21.x) or {@code
     * inventory.getSelectedItem()} (official, 26.x).
     */
    Object heldItemStack();

    /**
     * The NMS {@code ItemStack} (as {@code Object}) at the given player-inventory slot
     * (was {@code inventory.getItem(slot)}). Version-stable across all three families.
     */
    Object inventoryItemAt(int slot);

    /** This player's inventory size (was {@code inventory.getContainerSize()}). Version-stable. */
    int inventorySlotCount();
}
