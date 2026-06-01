package ac.grim.grimac.platform.fabric.initables;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.AbstractTickEndEvent;
import ac.grim.grimac.platform.fabric.FabricServerEvents;
import ac.grim.grimac.player.GrimPlayer;
import net.minecraft.server.MinecraftServer;

public class FabricTickEndEvent extends AbstractTickEndEvent {

    @Override
    public void start() {
        if (!super.shouldInjectEndTick()) {
            return;
        }

        // End-of-tick is delivered by the FabricServerEvents shim (MinecraftServerMixin on
        // tickServer()), avoiding a fabric-api dependency for ServerTickEvents.END_SERVER_TICK.
        FabricServerEvents.onEndTick(this::onEndServerTick);
    }

    private void onEndServerTick(MinecraftServer server) {
        tickAllPlayers();
    }

    private void tickAllPlayers() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player.disableGrim) continue;
            super.onEndOfTick(player, true);
        }
    }
}
