package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

@CheckData(name = "BadPacketsV")
public class BadPacketsV extends Check implements PacketCheck {
    public BadPacketsV(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            final WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
            // BlockFace.OTHER is USE_ITEM for pre 1.9
            if (packet.getFace() == BlockFace.OTHER && PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) {
                // This packet is always sent at (-1, -1, -1) at (0, 0, 0) on the block
                // except y gets wrapped?
                final int expectedY = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? 4095 : 255;

                final Vector3i pos = packet.getBlockPosition();
                final Vector3f cursor = packet.getCursorPosition();
                final ItemStack item = packet.getItemStack().get();

                if (pos.getX() != -1
                        || pos.getY() != expectedY
                        || pos.getZ() != -1
                        || cursor.getX() != 0
                        || cursor.getY() != 0
                        || cursor.getZ() != 0
                        || item.is(ItemTypes.AIR) // never sent when not holding anything
                ) flagAndAlert();
            }
        }
    }
}
