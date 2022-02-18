package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.nmsutil.WatchableIndexUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUseBed;

import java.util.List;

public class PacketSelfMetadataListener extends PacketListenerAbstract {
    public PacketSelfMetadataListener() {
        super(PacketListenerPriority.MONITOR, false);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata entityMetadata = new WrapperPlayServerEntityMetadata(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null)
                return;

            if (entityMetadata.getEntityId() == player.entityID) {
                // If we send multiple transactions, we are very likely to split them
                boolean hasSendTransaction = false;

                // 1.14+ poses:
                // - Client: I am sneaking
                // - Client: I am no longer sneaking
                // - Server: You are now sneaking
                // - Client: Okay, I am now sneaking.
                // - Server: You are no longer sneaking
                // - Client: Okay, I am no longer sneaking
                //
                // 1.13- poses:
                // - Client: I am sneaking
                // - Client: I am no longer sneaking
                // - Server: Okay, got it.
                //
                // Why mojang, why.  Why are you so incompetent at netcode.
                //
                // Also, mojang.  This system makes movement ping dependent!
                // A player using or exiting an elytra, or using or exiting sneaking will have differnet movement
                // to a player because of sending poses!  ViaVersion works fine without sending these poses
                // to the player on old servers... because the player just overrides this pose the very next tick
                //
                // It makes no sense to me why mojang is doing this, it has to be a bug.
                if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14)) {
                    List<EntityData> metadataStuff = entityMetadata.getEntityMetadata();

                    // Remove the pose metadata from the list
                    metadataStuff.removeIf(element -> element.getIndex() == 6);

                    entityMetadata.setEntityMetadata(metadataStuff);
                }

                EntityData watchable = WatchableIndexUtil.getIndex(entityMetadata.getEntityMetadata(), 0);

                if (watchable != null) {
                    Object zeroBitField = watchable.getValue();

                    if (zeroBitField instanceof Byte) {
                        byte field = (byte) zeroBitField;
                        boolean isGliding = (field & 0x80) == 0x80 && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);
                        boolean isSwimming = (field & 0x10) == 0x10;
                        boolean isSprinting = (field & 0x8) == 0x8;

                        // Send transaction BEFORE gliding so that any transition stuff will get removed
                        // by the uncertainty from switching with an elytra
                        player.sendTransaction();
                        hasSendTransaction = true;

                        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                            player.uncertaintyHandler.lastMetadataDesync = 0;
                            player.isSwimming = isSwimming;
                            player.lastSprinting = isSprinting;
                            // Protect this due to players being able to get the server to spam this packet a lot
                            if (player.isGliding != isGliding) {
                                player.pointThreeEstimator.updatePlayerGliding();
                            }
                            player.isGliding = isGliding;
                        });
                    }
                }

                if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13) &&
                        player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                    EntityData riptide = WatchableIndexUtil.getIndex(entityMetadata.getEntityMetadata(), PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? 8 : 7);

                    // This one only present if it changed
                    if (riptide != null && riptide.getValue() instanceof Byte) {
                        boolean isRiptiding = (((byte) riptide.getValue()) & 0x04) == 0x04;

                        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                            player.isRiptidePose = isRiptiding;
                        });

                        // 1.9 eating:
                        // - Client: I am starting to eat
                        // - Client: I am no longer eating
                        // - Server: Got that, you are eating!
                        // - Client: Okay, starting to eat (no response packet because server caused this)
                        // - Server: I got that you aren't eating, you are not eating!
                        // - Client: Okay, I am no longer eating (no response packet because server caused this)
                        //
                        // 1.8 eating:
                        // - Client: I am starting to eat
                        // - Client: I am no longer eating
                        // - Server: Okay, I will not make you eat or stop eating because it makes sense that the server doesn't control a player's eating.
                        //
                        // This was added for stuff like shields, but IMO it really should be all client sided
                        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
                            boolean isActive = (((byte) riptide.getValue()) & 0x01) == 0x01;
                            boolean hand = (((byte) riptide.getValue()) & 0x01) == 0x01;

                            if (!hasSendTransaction) player.sendTransaction();

                            // Player might have gotten this packet
                            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                                    () -> player.packetStateData.slowedByUsingItem = false);

                            int markedTransaction = player.lastTransactionSent.get();

                            // Player has gotten this packet
                            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                                // If the player hasn't overridden this packet by using or stopping using an item
                                // Vanilla update order: Receive this -> process new interacts
                                // Grim update order: Process new interacts -> receive this
                                if (player.packetStateData.slowedByUsingItemTransaction < markedTransaction) {
                                    player.packetStateData.slowedByUsingItem = isActive;

                                    if (isActive) {
                                        player.packetStateData.eatingHand = hand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                                    }
                                }
                            });

                            // Yes, we do have to use a transaction for eating as otherwise it can desync much easier
                            event.getPostTasks().add(player::sendTransaction);
                        }
                    }
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.USE_BED) {
            WrapperPlayServerUseBed bed = new WrapperPlayServerUseBed(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player != null && player.entityID == bed.getEntityId()) {
                // Split so packet received after transaction
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    player.isInBed = true;
                    player.bedPosition = new Vector3d(bed.getPosition().getX() + 0.5, bed.getPosition().getY(), bed.getPosition().getZ() + 0.5);
                });
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_ANIMATION) {
            WrapperPlayServerEntityAnimation animation = new WrapperPlayServerEntityAnimation(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player != null && player.entityID == animation.getEntityId()
                    && animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.LEAVE_BED) {
                // Split so packet received before transaction
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.isInBed = false);
                event.getPostTasks().add(player::sendTransaction);
            }
        }
    }
}
