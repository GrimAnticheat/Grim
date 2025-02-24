package ac.grim.grimac.manager.player.handlers;

import ac.grim.grimac.api.handler.ResyncHandler;
import ac.grim.grimac.events.packets.patch.ResyncWorldUtil;
import ac.grim.grimac.player.GrimPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BukkitResyncHandler implements ResyncHandler {

    private final GrimPlayer player;

    @Override
    public void resync(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ) {
        ResyncWorldUtil.resyncPositions(player, minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);
    }

}
