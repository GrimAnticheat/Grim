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

        // 26.X port: ServerTickEvents.END_SERVER_TICK is intermediary-bound. The
        // FabricServerEvents shim is driven by a MinecraftServer.tickServer() mixin
        // (Phase B) — until that mixin lands, end-tick fires nothing.
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
