package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsC", stableKey = "grim.badpackets.wake_not_sleeping", description = "Tried to wake up while not sleeping", experimental = true)
public class BadPacketsC extends Check implements PacketListener {
    public BadPacketsC(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler((event, wrapper) -> {
            WrapperPlayClientEntityAction packet = wrapper.get();
            WrapperPlayClientEntityAction.Action action = packet.getAction();
            if (action == WrapperPlayClientEntityAction.Action.LEAVE_BED && !player.isInBed) {
                flag();
            }
        }, PacketType.Play.Client.ENTITY_ACTION);
    }
}
