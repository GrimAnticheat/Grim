package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsY", stableKey = "grim.badpackets.oob_slot", description = "Sent out of bounds slot id")
public class BadPacketsY extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("slot={sint}");

    public BadPacketsY(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            final int slot = new WrapperPlayClientHeldItemChange(event).getSlot();
            if (slot > 8 || slot < 0) { // ban
                if (flag(V.write(verbose()).sint(slot)) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }, PacketType.Play.Client.HELD_ITEM_CHANGE);
    }
}
