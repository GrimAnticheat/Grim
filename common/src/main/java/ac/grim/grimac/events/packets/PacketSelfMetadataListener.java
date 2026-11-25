package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.SprintingState;
import ac.grim.grimac.utils.nmsutil.WatchableIndexUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class PacketSelfMetadataListener extends PacketListenerAbstract {

    public PacketSelfMetadataListener() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        WrapperPlayServerEntityMetadata entityMetadata = new WrapperPlayServerEntityMetadata(event);
        if (entityMetadata.getEntityId() != player.entityID) return;

        List<EntityData<?>> metadata = entityMetadata.getEntityMetadata();

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
        // Why Mojang, why.  Why are you so incompetent at netcode.
        //
        // Also, Mojang.  This system makes movement ping dependent!
        // A player using or exiting an elytra, or using or exiting sneaking will have different movement
        // to a player because of sending poses!  ViaVersion works fine without sending these poses
        // to the player on old servers... because the player just overrides this pose the very next tick
        //
        // It makes no sense to me why Mojang is doing this, it has to be a bug.
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            // Remove the pose metadata from the list
            metadata.removeIf(element -> element.getIndex() == 6);
            entityMetadata.setEntityMetadata(metadata);
            event.markForReEncode(true);
        }

        if (WatchableIndexUtil.getIndexValue(metadata, 0) instanceof Byte v) {
            byte field = v;
            boolean isGliding = (field & 0x80) != 0 && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);
            boolean isSwimming = (field & 0x10) != 0;
            boolean isSprinting = (field & 0x8) != 0;

            if (!hasSendTransaction) player.sendTransaction();
            hasSendTransaction = true;

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                player.isSwimming = isSwimming;
                player.lastSprinting = isSprinting;
                if (!isSprinting) {
                    player.vehicleData.camelSprintingState = SprintingState.STOPPING;
                }
                // Protect this due to players being able to get the server to spam this packet a lot
                if (player.isGliding != isGliding) {
                    player.pointThreeEstimator.updatePlayerGliding();
                }
                player.isGliding = isGliding;
            });
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_10)) {
            if (WatchableIndexUtil.getIndexValue(metadata, 5) instanceof Boolean hasNoGravity) {
                // Vanilla uses hasNoGravity, which is a bad name IMO
                // hasGravity > hasNoGravity
                boolean hasGravity = !hasNoGravity;

                if (!hasSendTransaction) player.sendTransaction();
                hasSendTransaction = true;
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.playerEntityHasGravity = hasGravity);
            }
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            if (WatchableIndexUtil.getIndexValue(metadata, 7) instanceof Integer v) {
                int ticksFrozen = v;

                if (!hasSendTransaction) player.sendTransaction();
                hasSendTransaction = true;
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.powderSnowFrozenTicks = ticksFrozen);
            }
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            int id;

            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_14_4)) {
                id = 12; // Added in 1.14 with an initial ID of 12
            } else if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_16_4)) {
                id = 13; // 1.15 changed this to 13
            } else {
                id = 14; // 1.17 changed this to 14
            }

            if (WatchableIndexUtil.getIndexValue(metadata, id) instanceof Optional<?> optional) {
                if (!hasSendTransaction) player.sendTransaction();
                hasSendTransaction = true;

                if (optional.orElse(null) instanceof Vector3i bedPos) {
                    player.addRealTimeTaskNow(() -> {
                        player.isInBed = true;
                        player.bedPosition = new Vector3d(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5);
                    });
                } else {
                    // Run when we know the player is not in bed 100%
                    player.addRealTimeTaskNow(() -> player.isInBed = false);
                }
            }
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            int id;

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
                id = 8;
            } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
                id = 7;
            } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_10)) {
                id = 6;
            } else {
                id = 5;
            }

            if (WatchableIndexUtil.getIndexValue(metadata, id) instanceof Byte v) {
                byte data = v;

                if (!hasSendTransaction) player.sendTransaction();
                hasSendTransaction = true;

                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
                    boolean isRiptiding = (data & 0x4) != 0;

                    player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                            () -> player.isRiptidePose = isRiptiding);
                }

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
                boolean isActive = (data & 0x1) != 0;
                boolean isOffhand = (data & 0x2) != 0;

                // Player might have gotten this packet
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                        () -> player.packetStateData.setSlowedByUsingItem(false));

                int markedTransaction = player.lastTransactionSent.get();

                // Player has gotten this packet
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                    // If the player hasn't overridden this packet by using or stopping using an item
                    // Vanilla update order: Receive this -> process new interacts
                    // Grim update order: Process new interacts -> receive this
                    if (player.packetStateData.slowedByUsingItemTransaction < markedTransaction) {
                        player.packetStateData.setSlowedByUsingItem(isActive);

                        if (isActive) {
                            player.packetStateData.itemInUseHand = isOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                        }
                    }
                });

                // Yes, we do have to use a transaction for eating as otherwise it can desync much easier
                event.getTasksAfterSend().add(player::sendTransaction);
            }
        }
    }
}
