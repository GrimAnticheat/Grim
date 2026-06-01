package ac.grim.grimac.platform.fabric.inject;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.UUID;

/**
 * PROTOTYPE (refactor/fabric-dedupe spike). Narrow Loom-injected bridge grafted onto
 * NMS {@code ServerPlayer} via {@code loom:injected_interfaces} in each aggregator's
 * {@code fabric.mod.json}, with bodies supplied by a per-aggregator
 * {@code @Mixin(ServerPlayer.class) @Implements(@Interface(prefix = "grim$"))}.
 *
 * <p>WHY THIS LIVES IN fabric-common: this module is a plain {@code java-library} with
 * NO Minecraft and NO Loom (hard contract in fabric-common/build.gradle.kts). The only
 * types referenced below are grim-api / packetevents / adventure / JDK -- all NMS-free.
 * That is what lets the single-sourced player wrapper move OUT of the two aggregators
 * (where {@code AbstractFabricPlatformPlayer} is currently duplicated) and INTO here.
 *
 * <p>WHY EVERY METHOD IS grim$-PREFIXED: the maintainer rejected a wide
 * {@code PlatformPlayer} injection because {@code PlatformPlayer.getName()} returns
 * {@code String} while NMS {@code ServerPlayer.getName()} returns
 * {@code net.minecraft.network.chat.Component} -- a hard return-type clash, plus the
 * risk of future Mojang/Yarn method-name collisions. A {@code grim$}-prefixed name can
 * never collide with a vanilla member, so {@code grim$name()} is safe where a bare
 * {@code getName()} is not. This mirrors the existing proven {@code grimac$}-prefixed
 * {@code @Implements} bridge used for {@code Level -> PlatformWorld}.
 *
 * <p>This interface intentionally only covers the subset of {@code ServerPlayer}
 * operations that {@code AbstractFabricPlatformPlayer} performs directly on the NMS
 * handle AND whose API shape is identical across all supported versions (1.16.1 .. 26.1).
 * Version-divergent operations (teleport signature, command-source creation) are NOT
 * here -- they remain in the thin per-version subclasses.
 */
public interface GrimInjectedServerPlayer {

    boolean grim$isSneaking();

    void grim$setSneaking(boolean sneaking);

    boolean grim$isOnline();

    /** Vanilla {@code getName()} returns NMS Component; this returns the plain string. */
    String grim$name();

    void grim$broadcastInventoryChanges();

    Vector3d grim$position();

    GameMode grim$gameMode();

    void grim$setGameMode(GameMode gameMode);

    UUID grim$uuid();

    boolean grim$isDead();
}
