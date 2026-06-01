package ac.grim.grimac.platform.fabric.inject;

import ac.grim.grimac.platform.api.player.PlatformPlayer;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.UUID;

/**
 * PROTOTYPE (refactor/fabric-dedupe spike): the NMS-free, single-sourced half of
 * {@code AbstractFabricPlatformPlayer}, living in fabric-common.
 *
 * <p>Today {@code AbstractFabricPlatformPlayer} (~164 lines) is copy-pasted into BOTH
 * aggregators (fabric-official/src and fabric-intermediary/src) and the two copies
 * differ in only two lines. The reason it cannot already live in fabric-common is that
 * its method bodies call NMS directly: {@code fabricPlayer.isShiftKeyDown()},
 * {@code fabricPlayer.getName().getString()}, {@code fabricPlayer.gameMode...}, etc.
 *
 * <p>This class proves those calls can be re-expressed against the Loom-injected
 * {@link GrimInjectedServerPlayer} bridge instead. The native handle is stored as
 * {@link Object} (so this file imports zero net.minecraft types) and accessed via:
 * <pre>{@code injected().grim$isSneaking()}</pre>
 * Loom has grafted {@code GrimInjectedServerPlayer} onto {@code ServerPlayer} (see
 * {@code injected_interfaces} in each aggregator fabric.mod.json), so the double cast
 * through {@link Object} is legal and dispatches into that aggregator's mixin body.
 *
 * <p>The handful of methods kept here are exactly the version-INVARIANT,
 * NMS-touching ones. Methods needing other grim subsystems (sender, inventory,
 * vehicle, kick/sendMessage which route through the loader plugin) are intentionally
 * left to the concrete subclass for this spike to keep the surface minimal and the
 * compile signal clean.
 */
public abstract class InjectedFabricPlatformPlayerBase implements PlatformPlayer {

    /** Stored as Object so this NMS-free module never imports net.minecraft. */
    protected volatile Object fabricPlayerHandle;

    protected InjectedFabricPlatformPlayerBase(Object serverPlayer) {
        this.fabricPlayerHandle = serverPlayer;
    }

    /**
     * The crux of the spike: cast the NMS ServerPlayer to the injected bridge. This
     * line type-checks ONLY because Loom applied {@code injected_interfaces} to the
     * compile classpath -- if the injection silently failed, this is a compile error,
     * which is the falsifiable signal we want.
     */
    private GrimInjectedServerPlayer injected() {
        return (GrimInjectedServerPlayer) this.fabricPlayerHandle;
    }

    @Override
    public boolean isSneaking() {
        return injected().grim$isSneaking();
    }

    @Override
    public void setSneaking(boolean sneaking) {
        injected().grim$setSneaking(sneaking);
    }

    @Override
    public boolean isOnline() {
        return injected().grim$isOnline();
    }

    @Override
    public String getName() {
        // The getName() clash, dodged: PlatformPlayer.getName() is String, but the
        // injected bridge method is grim$name() (not getName()), so it never collides
        // with NMS ServerPlayer.getName():Component.
        return injected().grim$name();
    }

    @Override
    public void updateInventory() {
        injected().grim$broadcastInventoryChanges();
    }

    @Override
    public Vector3d getPosition() {
        return injected().grim$position();
    }

    @Override
    public GameMode getGameMode() {
        return injected().grim$gameMode();
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        injected().grim$setGameMode(gameMode);
    }

    @Override
    public UUID getUniqueId() {
        return injected().grim$uuid();
    }

    @Override
    public boolean isDead() {
        return injected().grim$isDead();
    }

    @Override
    public boolean isExternalPlayer() {
        return false;
    }

    @Override
    public void replaceNativePlayer(Object nativePlayerObject) {
        this.fabricPlayerHandle = nativePlayerObject;
    }
}
