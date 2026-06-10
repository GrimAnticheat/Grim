package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;

@CheckData(name = "PacketOrderD", stableKey = "grim.packetorder.interact_hand_order", verboseVersion = 1, description = "Sent offhand entity interaction before the matching mainhand interaction", experimental = true)
public class PacketOrderD extends Check implements PacketCheck {
    public static final VerboseSchema V = VerboseSchema.of(
            "kind:vi", "requiredEntity:zz", "entity:zz", "requiredSneaking:bool", "sneaking:bool");

    static final int KIND_SKIPPED_MAINHAND = 0;
    static final int KIND_MISMATCH = 1;

    public PacketOrderD(final GrimPlayer player) {
        super(player);
    }

    static String literal(int kind) {
        return switch (kind) {
            case KIND_SKIPPED_MAINHAND -> "Skipped Mainhand";
            default -> "unknown";
        };
    }

    private boolean sentMainhand;
    private int requiredEntity;
    private boolean requiredSneaking;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            final WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            InteractAction action = packet.getAction();
            if (action != InteractAction.ATTACK) {
                final boolean sneaking = packet.isSneaking().orElse(false);
                final int entity = packet.getEntityId();

                if (packet.getHand() == InteractionHand.OFF_HAND) {
                    if (action == InteractAction.INTERACT || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_1)) {
                        if (!sentMainhand) {
                            if (flagAndAlert(V.write(verbose())
                                    .vi(KIND_SKIPPED_MAINHAND)
                                    .zz(0)
                                    .zz(0)
                                    .bool(false)
                                    .bool(false)) && shouldModifyPackets()) {
                                event.setCancelled(true);
                                player.onPacketCancel();
                            }
                        }
                        sentMainhand = false;
                    }

                    if (action == InteractAction.INTERACT_AT) {
                        if (sneaking != requiredSneaking || entity != requiredEntity) {
                            if (flagAndAlert(V.write(verbose())
                                    .vi(KIND_MISMATCH)
                                    .zz(requiredEntity)
                                    .zz(entity)
                                    .bool(requiredSneaking)
                                    .bool(sneaking)) && shouldModifyPackets()) {
                                event.setCancelled(true);
                                player.onPacketCancel();
                            }
                        }
                    }
                } else {
                    requiredEntity = entity;
                    requiredSneaking = sneaking;
                    sentMainhand = true;
                }
            }
        }

        if (isTickPacket(event.getPacketType())) {
            sentMainhand = false;
        }
    }
}