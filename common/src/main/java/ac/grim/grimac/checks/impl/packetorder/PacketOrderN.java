package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

@CheckData(name = "PacketOrderN", experimental = true)
public class PacketOrderN extends BlockPlaceCheck {
    public PacketOrderN(final GrimPlayer player) {
        super(player);
    }

    private int invalid;
    private boolean usingWithoutPlacing, placing;

    @Override
    public void onBlockPlace(BlockPlace place) {
        placing = true;
        if (usingWithoutPlacing) {
            if (!player.canSkipTicks()) {
                if (flagAndAlert() && shouldModifyPackets() && shouldCancel()) {
                    place.resync();
                }
            } else {
                invalid++;
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM
                || event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                && new WrapperPlayClientPlayerBlockPlacement(event).getFace() == BlockFace.OTHER) {
            if (!placing) {
                usingWithoutPlacing = true;
            }

            placing = false;
        }

        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            usingWithoutPlacing = placing = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (; invalid >= 1; invalid--) {
                flagAndAlert();
            }
        }

        invalid = 0;
    }
}
