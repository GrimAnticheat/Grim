package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "PacketOrderC", stableKey = "grim.packetorder.interact_order", description = "Sent INTERACT and INTERACT_AT entity packets in the wrong order")
public class PacketOrderC extends Check implements PacketCheck {
    // Shape index == KIND_* constant value.
    private static final Verbose V = Verbose
            .of("Skipped Interact-At")
            .or("Skipped Interact")
            .or("Skipped Interact (Tick)")
            .or("requiredEntity={sint}, entity={sint}, requiredHand={hand}, hand={hand}, requiredSneaking={bool}, sneaking={bool}");

    static final int KIND_SKIPPED_INTERACT_AT = 0;
    static final int KIND_SKIPPED_INTERACT = 1;
    static final int KIND_SKIPPED_INTERACT_TICK = 2;
    static final int KIND_MISMATCH = 3;

    private final boolean exempt = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10) // 1.7 players do not send INTERACT_AT
            || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_1); // 26.1 players do not send INTERACT
    private boolean sentInteractAt = false;
    private int requiredEntity;
    private InteractionHand requiredHand;
    private boolean requiredSneaking;

    public PacketOrderC(final GrimPlayer player) {
        super(player);
    }

    private Verbose.Writer writeKind(int kind) {
        return V.write(verbose(), kind);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (exempt) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            final WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);

            final PacketEntity entity = player.compensatedEntities.entityMap.get(packet.getEntityId());

            // For armor stands, vanilla clients send:
            //  - when renaming the armor stand or in spectator mode: INTERACT_AT + INTERACT
            //  - in all other cases: only INTERACT
            // Just exempt armor stands to be safe
            if (entity != null && entity.getType() == EntityTypes.ARMOR_STAND) return;

            final boolean sneaking = packet.isSneaking().orElse(false);

            switch (packet.getAction()) {
                // INTERACT_AT then INTERACT
                case INTERACT:
                    if (!sentInteractAt) {
                        if (flag(writeKind(KIND_SKIPPED_INTERACT_AT)) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else if (packet.getEntityId() != requiredEntity || packet.getHand() != requiredHand || sneaking != requiredSneaking) {
                        if (flag(V.write(verbose(), KIND_MISMATCH)
                                .sint(requiredEntity)
                                .sint(packet.getEntityId())
                                .uint(VerboseCodecs.enumId(requiredHand))
                                .uint(VerboseCodecs.enumId(packet.getHand()))
                                .bool(requiredSneaking)
                                .bool(sneaking)) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    }

                    sentInteractAt = false;
                    break;
                case INTERACT_AT:
                    if (sentInteractAt) {
                        if (flag(writeKind(KIND_SKIPPED_INTERACT)) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    }

                    requiredHand = packet.getHand();
                    requiredEntity = packet.getEntityId();
                    requiredSneaking = sneaking;
                    sentInteractAt = true;
                    break;
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (sentInteractAt) {
                sentInteractAt = false;
                flag(writeKind(KIND_SKIPPED_INTERACT_TICK));
            }
        }
    }
}