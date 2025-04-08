package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;

@CheckData(name = "NoSwingBreak", description = "Did not swing while breaking block", experimental = true)
public class NoSwingBreak extends AbstractPacketCheck implements BlockBreakCheck {
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
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> sentAnimation = true, PacketType.Play.Client.ANIMATION);
        registry.registerHandler(event -> {
            if (!isTickPacket(event.getPacketType())) return;
            if (sentBreak && !sentAnimation) {
                flagAndAlert();
            }

            sentAnimation = sentBreak = false;
        });
    }
}
