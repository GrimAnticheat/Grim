package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsG", stableKey = "grim.badpackets.duplicate_sneak", description = "Sent duplicate sneaking status")
public class BadPacketsG extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("state={bool}");

    private boolean lastSneaking, respawn;

    public BadPacketsG(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        // input packet determines sneaking state in 1.21.2+
        return player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2);
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
        // The player may send two START_SNEAKING packets if they respawned
        if (!respawn && lastSneaking == state) {
            if (flag(V.write(verbose()).bool(state)) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        lastSneaking = state;
        respawn = false;
    }

    public void handleRespawn() {
        // Clients could potentially not send a STOP_SNEAKING packet when they die, so we need to track it
        respawn = true;
    }
}
