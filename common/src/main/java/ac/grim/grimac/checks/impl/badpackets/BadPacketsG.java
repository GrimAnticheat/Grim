package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "BadPacketsG", stableKey = "grim.badpackets.duplicate_sneak", description = "Sent duplicate sneaking status")
public class BadPacketsG extends Check implements PreViaPacketReceiveListener {
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
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);

            if (packet.getAction() == WrapperPlayClientEntityAction.Action.START_SNEAKING) {
                // The player may send two START_SNEAKING packets if they respawned
                if (lastSneaking && !respawn) {
                    boolean state = true;
                    if (flag(V.write(verbose()).bool(state)) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    lastSneaking = true;
                }
                respawn = false;
            } else if (packet.getAction() == WrapperPlayClientEntityAction.Action.STOP_SNEAKING) {
                if (!lastSneaking && !respawn) {
                    boolean state = false;
                    if (flag(V.write(verbose()).bool(state)) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    lastSneaking = false;
                }
                respawn = false;
            }
        }
    }

    public void handleRespawn() {
        // Clients could potentially not send a STOP_SNEAKING packet when they die, so we need to track it
        respawn = true;
    }
}
