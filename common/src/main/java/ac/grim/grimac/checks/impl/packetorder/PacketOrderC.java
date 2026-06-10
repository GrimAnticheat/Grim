package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
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

@CheckData(name = "PacketOrderC", stableKey = "grim.packetorder.interact_order", verboseVersion = 2, description = "Sent INTERACT and INTERACT_AT entity packets in the wrong order")
public class PacketOrderC extends Check implements PacketCheck {
    public static final VerboseSchema V = VerboseSchema.of(2,
            "kind:vi",
            "requiredEntity:zz",
            "entity:zz",
            "requiredHand:enum",
            "hand:enum",
            "requiredSneaking:bool",
            "sneaking:bool");

    static final int KIND_SKIPPED_INTERACT_AT = 0;
    static final int KIND_MISMATCH = 1;
    static final int KIND_SKIPPED_INTERACT = 2;
    static final int KIND_SKIPPED_INTERACT_TICK = 3;

    private final boolean exempt = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10) // 1.7 players do not send INTERACT_AT
            || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_1); // 26.1 players do not send INTERACT
    private boolean sentInteractAt = false;
    private int requiredEntity;
    private InteractionHand requiredHand;
    private boolean requiredSneaking;

    public PacketOrderC(final GrimPlayer player) {
        super(player);
    }

    static String literal(int kind) {
        return switch (kind) {
            case KIND_SKIPPED_INTERACT_AT -> "Skipped Interact-At";
            case KIND_SKIPPED_INTERACT -> "Skipped Interact";
            case KIND_SKIPPED_INTERACT_TICK -> "Skipped Interact (Tick)";
            default -> "unknown";
        };
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
                        if (flagAndAlert(V.write(verbose())
                                .vi(KIND_SKIPPED_INTERACT_AT)
                                .zz(0)
                                .zz(0)
                                .vi(0)
                                .vi(0)
                                .bool(false)
                                .bool(false)) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else if (packet.getEntityId() != requiredEntity || packet.getHand() != requiredHand || sneaking != requiredSneaking) {
                        if (flagAndAlert(V.write(verbose())
                                .vi(KIND_MISMATCH)
                                .zz(requiredEntity)
                                .zz(packet.getEntityId())
                                .vi(VerboseCodecs.enumOrdinal(requiredHand))
                                .vi(VerboseCodecs.enumOrdinal(packet.getHand()))
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
                        if (flagAndAlert(V.write(verbose())
                                .vi(KIND_SKIPPED_INTERACT)
                                .zz(0)
                                .zz(0)
                                .vi(0)
                                .vi(0)
                                .bool(false)
                                .bool(false)) && shouldModifyPackets()) {
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
                flagAndAlert(V.write(verbose())
                        .vi(KIND_SKIPPED_INTERACT_TICK)
                        .zz(0)
                        .zz(0)
                        .vi(0)
                        .vi(0)
                        .bool(false)
                        .bool(false));
            }
        }
    }
}