package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsF", stableKey = "grim.badpackets.duplicate_sprint", description = "Sent duplicate sprinting status")
public class BadPacketsF extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("state={bool}");

    public boolean lastSprinting;
    public boolean exemptNext = true; // Support 1.14+ clients starting on either true or false sprinting, we don't know

    public BadPacketsF(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);

            switch (packet.getAction()) {
                case START_SPRINTING -> doCheck(event, true);
                case STOP_SPRINTING -> doCheck(event, false);
            }
        }, PacketType.Play.Client.ENTITY_ACTION);
    }

    private void doCheck(@NotNull PacketReceiveEvent event, boolean state) {
        if (lastSprinting == state) {
            if (exemptNext) {
                exemptNext = false;
                return;
            }

            if (flag(V.write(verbose()).bool(state)) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        lastSprinting = state;
    }
}
