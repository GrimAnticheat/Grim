package ac.grim.grimac.platform.fabric.utils;

import ac.grim.grimac.utils.anticheat.LogUtil;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

/**
 * Binds a Polymer/fabric-api {@code PacketContext} around Grim's out-of-pipeline {@code ItemStack}
 * encoding so that Polymer can encode items without crashing. See GrimAnticheat/Grim#2701.
 *
 * <p>Polymer 0.16+ (Minecraft 26.x) routes vanilla {@code ItemStack} stream-codec encoding through
 * fabric-api's {@code net.fabricmc.fabric.api.networking.v1.context.PacketContext}. Its
 * {@code ItemStackPacketCodecMixin} calls {@code PacketContext.orElseThrow()} at the head of every
 * encode, which throws {@code "PacketContext is required, but it wasn't set up!"} whenever no context
 * is bound to the current thread.
 *
 * <p>Grim encodes {@code ItemStack}s outside of the Netty pipeline when it reads a player's inventory
 * to convert it into a PacketEvents {@code ItemStack} ({@code IFabricConversionUtil#fromFabricItemStack}).
 * No context is bound there, so under Polymer every read threw, was caught, and returned
 * {@code ItemStack.EMPTY} while spamming the log.
 *
 * <p>This hook reflectively wraps such reads in {@code PacketContext.supplyWithContext(provider, supplier)}
 * using the inventory owner as the provider, so Polymer resolves that player's view of the item — the same
 * representation the client received, which is what an anticheat simulating the client wants, and is
 * consistent with the per-player block translation done by the {@code Fabric*PolymerHook}s.
 *
 * <p>It is a no-op pass-through when Polymer is absent or the fabric-api context API is unavailable
 * (e.g. on pre-26.x where Polymer used PacketTweaker, whose codec mixin degrades gracefully on a missing
 * context). Resolution is done by reflection because the parent fabric module compiles against MC 1.16.1,
 * which predates this API; keying off the API's runtime presence means this transparently covers mc261
 * and any later 26.x module without per-variant wiring.
 */
public final class FabricItemContextHook {
    private static final MethodHandle SUPPLY_WITH_CONTEXT;

    /**
     * True only when Polymer is present and the fabric-api context API resolved, i.e. when binding is
     * actually needed. It is a {@code static final} primitive so the JIT folds it to a constant and
     * dead-code-eliminates the binding path on the (overwhelmingly common) servers without Polymer —
     * letting callers keep their original allocation-free direct read on hot paths. Check this at the
     * call site <em>before</em> building the {@link Supplier} so the lambda is never allocated when
     * inactive.
     */
    public static final boolean ACTIVE;

    static {
        MethodHandle supplyWithContext = null;

        if (FabricLoader.getInstance().isModLoaded("polymer-core")) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                Class<?> contextClass = Class.forName("net.fabricmc.fabric.api.networking.v1.context.PacketContext");
                Class<?> providerClass = Class.forName("net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider");

                MethodHandle raw = lookup.findStatic(contextClass, "supplyWithContext",
                        MethodType.methodType(Object.class, providerClass, Supplier.class));
                // The inventory owner (ServerPlayer) implements PacketContextProvider at runtime via
                // fabric-api's own net.fabricmc.fabric.mixin.networking.ServerPlayerMixin (it delegates
                // getPacketContext() to player.connection). We accept it as a plain Object here and let
                // the handle's runtime checkcast validate it, so the parent module needn't compile against
                // the API. Verified live: the bind resolves "via net.minecraft.server.level.ServerPlayer".
                supplyWithContext = raw.asType(MethodType.methodType(Object.class, Object.class, Supplier.class));
            } catch (Throwable t) {
                // Older fabric-api/Polymer (PacketTweaker-based) tolerates a missing context, so failing to
                // resolve the new API here is not fatal — degrade to a no-op pass-through.
                LogUtil.info("[GrimAC] Polymer is present but fabric-api's PacketContext API is unavailable; "
                        + "leaving item encoding to Polymer's own context handling.");
            }
        }

        SUPPLY_WITH_CONTEXT = supplyWithContext;
        ACTIVE = supplyWithContext != null;
    }

    private FabricItemContextHook() {
    }

    /**
     * Runs {@code supplier} with {@code player}'s Polymer/fabric-api {@code PacketContext} bound to the
     * current thread, so any {@code ItemStack} encoding inside it can be transformed by Polymer for that
     * player. Falls back to running the supplier directly when the hook is inactive, the player is
     * unavailable, or the context cannot be bound.
     */
    @SuppressWarnings("unchecked")
    public static <T> T supply(Object player, Supplier<T> supplier) {
        if (SUPPLY_WITH_CONTEXT == null || player == null) {
            return supplier.get();
        }
        try {
            // The handle is pre-shaped to (Object, Supplier)Object in <clinit>, so invokeExact matches the
            // call-site descriptor exactly and skips the per-call argument adaptation plain invoke() does.
            return (T) (Object) SUPPLY_WITH_CONTEXT.invokeExact(player, (Supplier<Object>) supplier);
        } catch (Throwable t) {
            // Binding failed (e.g. the player is not yet a context provider). Run without it; the encode's
            // own try/catch handles any downstream Polymer failure.
            return supplier.get();
        }
    }
}
