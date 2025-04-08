package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "CrashB", description = "Sent creative mode inventory click packets while not in creative mode")
public class CrashB extends AbstractPacketCheck {
    public CrashB(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            if (player.gamemode != GameMode.CREATIVE) {
                event.setCancelled(true);
                player.onPacketCancel();
                flagAndAlert(); // Could be transaction split, no need to setback though
            }
        }, PacketType.Play.Client.CREATIVE_INVENTORY_ACTION);
    }
}
