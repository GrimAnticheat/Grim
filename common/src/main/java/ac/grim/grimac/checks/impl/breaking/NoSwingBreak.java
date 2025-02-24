package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;

@CheckData(name = "NoSwingBreak", description = "Did not swing while breaking block", experimental = true)
public class NoSwingBreak extends Check implements BlockBreakCheck {
    public NoSwingBreak(GrimPlayer playerData) {
        super(playerData);
    }

    private boolean sentAnimation;
    private boolean sentBreak;

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action != DiggingAction.CANCELLED_DIGGING) {
            sentBreak = true;
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            sentAnimation = true;
        }

        if (isTickPacket(event.getPacketType())) {
            if (sentBreak && !sentAnimation) {
                flagAndAlert();
            }

            sentAnimation = sentBreak = false;
        }
    }
}
