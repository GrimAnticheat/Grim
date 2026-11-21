package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsI", stableKey = "grim.badpackets.spoofed_abilities", description = "Claimed to be flying while unable to fly")
public class BadPacketsI extends Check implements PacketListener {
    public BadPacketsI(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            if (new WrapperPlayClientPlayerAbilities(event).isFlying() && !player.canFly
                    && flag() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.PLAYER_ABILITIES);
    }
}
