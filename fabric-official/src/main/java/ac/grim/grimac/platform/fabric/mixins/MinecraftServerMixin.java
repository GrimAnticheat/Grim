package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.platform.fabric.FabricServerEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

// Drives the FabricServerEvents shim from MinecraftServer lifecycle points,
// standing in for fabric-api's ServerLifecycleEvents / ServerTickEvents so Grim
// doesn't take a hard fabric-api dependency for two lifecycle hooks. (Dependency-
// surface choice, not a namespace one: fabric-api's events are mojmap on 26.1 and
// would link fine.) Hook points mirror fabric-api's:
//   STARTING fires at @Inject HEAD of runServer() — fabric-api's SERVER_STARTING
//     also fires before initServer() runs (initServer is the first instruction
//     inside runServer in 26.1.2 bytecode). For "after init succeeds, before
//     first tick" semantics use SERVER_STARTED instead — not wired today
//     because Grim's start path doesn't need that ordering.
//   STOPPING fires at the head of stopServer().
//   END_TICK fires at the tail of tickServer(BooleanSupplier).
@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {

    @Inject(method = "runServer", at = @At("HEAD"))
    private void grim$fireStarting(CallbackInfo ci) {
        FabricServerEvents.fireServerStarting((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void grim$fireStopping(CallbackInfo ci) {
        FabricServerEvents.fireServerStopping((MinecraftServer) (Object) this);
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void grim$fireEndTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        FabricServerEvents.fireEndTick((MinecraftServer) (Object) this);
    }
}
