package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.nmsImplementations.WatchableIndexUtil;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.NMSPacket;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedPacketOutEntityMetadata;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.player.Hand;
import io.github.retrooper.packetevents.utils.server.ServerVersion;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class PacketSelfMetadataListener extends PacketListenerAbstract {
    public PacketSelfMetadataListener() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.ENTITY_METADATA) {
            WrappedPacketOutEntityMetadata entityMetadata = new WrappedPacketOutEntityMetadata(event.getNMSPacket());
            if (entityMetadata.getEntityId() == event.getPlayer().getEntityId()) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());

                if (player == null)
                    return;

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
                if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_14)) {
                    // Use a new arraylist to avoid a concurrent modification exception
                    List<Object> metadataStuff = entityMetadata.readList(0);
                    List<Object> metadata = new ArrayList<>(metadataStuff);

                    // Remove the pose metadata from the list
                    metadata.removeIf(element -> {
                        Object dataWatcherObject = new WrappedPacket(new NMSPacket(element)).readAnyObject(0);
                        WrappedPacket wrappedDataWatcher = new WrappedPacket(new NMSPacket(dataWatcherObject));
                        return wrappedDataWatcher.readInt(0) == 6;
                    });

                    // If there was pose metadata in the list
                    if (metadata.size() != metadataStuff.size() && !metadata.isEmpty()) {
                        try {
                            // We need to find a constructor for the entity metadata packet
                            // Warning: Do not modify the current packet being sent as it is being sent to multiple people
                            // You must create a new packet to remove poses from metadata
                            Constructor<?> constructor = event.getNMSPacket().getRawNMSPacket().getClass().getConstructor(int.class, NMSUtils.dataWatcherClass, boolean.class);

                            // Generate a metadata packet using a new data watcher, to avoid concurrent modification exceptions
                            Object nmsEntity = NMSUtils.getNMSEntity(event.getPlayer());
                            Object dataWatcher = NMSUtils.generateDataWatcher(nmsEntity);
                            Object watcherPacket = constructor.newInstance(player.entityID, dataWatcher, true);

                            // Write the modified list to this new packet
                            new WrappedPacket(new NMSPacket(watcherPacket)).writeList(0, metadata);
                            // And send it to the player
                            PacketEvents.get().getPlayerUtils().sendNMSPacket(event.getPlayer(), watcherPacket);

                            // Then cancel this packet to avoid poses getting sent to the player
                            event.setCancelled(true);
                            return;
                        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }

                WrappedWatchableObject watchable = WatchableIndexUtil.getIndex(entityMetadata.getWatchableObjects(), 0);

                if (watchable != null) {
                    Object zeroBitField = watchable.getRawValue();

                    if (zeroBitField instanceof Byte) {
                        byte field = (byte) zeroBitField;
                        boolean isGliding = (field & 0x80) == 0x80 && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9);
                        boolean isSwimming = (field & 0x10) == 0x10;

                        player.sendTransaction();

                        // Send transaction BEFORE gliding so that any transition stuff will get removed
                        // by the uncertainty from switching with an elytra
                        int transactionSent = player.lastTransactionSent.get();
                        player.compensatedElytra.tryAddStatus(transactionSent, isGliding);

                        player.latencyUtils.addRealTimeTask(transactionSent, () -> {
                            player.uncertaintyHandler.lastMetadataDesync = 0;
                            player.isSwimming = isSwimming;
                        });
                    }
                }

                if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13) &&
                        player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9)) {
                    WrappedWatchableObject riptide = WatchableIndexUtil.getIndex(entityMetadata.getWatchableObjects(), ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 8 : 7);

                    // This one only present if it changed
                    if (riptide != null && riptide.getRawValue() instanceof Byte) {
                        boolean isRiptiding = (((byte) riptide.getRawValue()) & 0x04) == 0x04;

                        player.compensatedRiptide.setPose(isRiptiding);

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
                        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9) && ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9)) {
                            boolean isActive = (((byte) riptide.getRawValue()) & 0x01) == 0x01;
                            boolean hand = (((byte) riptide.getRawValue()) & 0x01) == 0x01;

                            player.sendTransaction();

                            // Player might have gotten this packet
                            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                                    () -> player.packetStateData.slowedByUsingItem = AlmostBoolean.MAYBE);

                            int markedTransaction = player.lastTransactionSent.get();

                            // Player has gotten this packet
                            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                                // If the player hasn't overridden this packet by using or stopping using an item
                                // Vanilla update order: Receive this -> process new interacts
                                // Grim update order: Process new interacts -> receive this
                                if (player.packetStateData.slowedByUsingItemTransaction < markedTransaction) {
                                    player.packetStateData.slowedByUsingItem = isActive ? AlmostBoolean.TRUE : AlmostBoolean.FALSE;

                                    if (isActive) {
                                        player.packetStateData.eatingHand = hand ? Hand.MAIN_HAND : Hand.OFF_HAND;
                                    }
                                }
                            });

                            // Yes, we do have to use a transaction for eating as otherwise it can desync much easier
                            event.setPostTask(player::sendTransaction);
                        }
                    }
                }
            }
        }
    }
}
