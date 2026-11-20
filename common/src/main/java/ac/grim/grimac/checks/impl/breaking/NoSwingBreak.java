package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.BlockBreakListener;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "NoSwingBreak", stableKey = "grim.breaking.no_swing_break", description = "Did not swing while breaking block", experimental = true)
public class NoSwingBreak extends Check implements BlockBreakListener, PacketListener {
    private boolean sentAnimation;
    private boolean sentBreak;

    public NoSwingBreak(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action != DiggingAction.CANCELLED_DIGGING) {
            sentBreak = true;
        }
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> sentAnimation = true, ANIMATION_PACKETS);
        registry.registerPreViaReceiveHandler(event -> {
            if (isTickPacket(event.getPacketType())) {
                if (sentBreak && !sentAnimation) {
                    flag();
                }

                sentAnimation = sentBreak = false;
            }
        }, TICK_PACKET_CANDIDATES);
    }
}
