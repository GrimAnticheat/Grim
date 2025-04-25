package ac.grim.grimac.platform.fabric.mixins;


import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.network.ServerPlayerEntity;


@Mixin(ServerPlayerEntity.class)
abstract class ServerPlayerEntityMixin {

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void onCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        ((FabricPlatformPlayerFactory) GrimAPI.INSTANCE.getPlatformPlayerFactory()).replaceNativePlayer(oldPlayer.getUuid(), (ServerPlayerEntity) (Object) this);
    }
}
