package ac.grim.grimac.platform.fabric.mixins;

import ac.grim.grimac.platform.fabric.FabricServerEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

// Drives the FabricServerEvents shim from MinecraftServer lifecycle points instead of
// taking a hard fabric-api dependency for two hooks: STARTING at HEAD of runServer(),
// STOPPING at HEAD of stopServer(), END_TICK at TAIL of tickServer(BooleanSupplier).
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
