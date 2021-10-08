package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ChangeBlockData;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.blockchange.WrappedPacketOutBlockChange;
import io.github.retrooper.packetevents.packetwrappers.play.out.unloadchunk.WrappedPacketOutUnloadChunk;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.vector.Vector3i;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BasePacketWorldReader extends PacketListenerAbstract {
    public static Method getByCombinedID;

    public BasePacketWorldReader() {
        super(PacketListenerPriority.MONITOR);

        getByCombinedID = Reflection.getMethod(NMSUtils.blockClass, "getCombinedId", int.class);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrappedPacketOutUnloadChunk unloadChunk = new WrappedPacketOutUnloadChunk(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            unloadChunk(player, unloadChunk.getChunkX(), unloadChunk.getChunkZ());
        }

        if (packetID == PacketType.Play.Server.BLOCK_CHANGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            handleBlockChange(player, event);
        }

        if (packetID == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            handleMultiBlockChange(player, event);
        }
    }

    public void addChunkToCache(GrimPlayer player, int chunkX, int chunkZ, boolean isSync) {

    }

    public void unloadChunk(GrimPlayer player, int x, int z) {
        if (player == null) return;
        player.compensatedWorld.removeChunkLater(x, z);
    }

    public void handleBlockChange(GrimPlayer player, PacketPlaySendEvent event) {
        WrappedPacketOutBlockChange wrappedBlockChange = new WrappedPacketOutBlockChange(event.getNMSPacket());
        if (player == null) return;
        if (player.compensatedWorld.isResync) return;

        int combinedID = 0;

        // For 1.8 all the way to 1.16, the method for getting combined ID has never changed
        try {
            Object blockObject = wrappedBlockChange.readAnyObject(1);
            combinedID = (int) getByCombinedID.invoke(null, blockObject);
        } catch (InvocationTargetException | IllegalAccessException var4) {
            var4.printStackTrace();
        }

        handleUpdateBlockChange(player, event, wrappedBlockChange, combinedID);
    }

    public void handleMultiBlockChange(GrimPlayer player, PacketPlaySendEvent event) {

    }

    public void handleUpdateBlockChange(GrimPlayer player, PacketPlaySendEvent event, WrappedPacketOutBlockChange wrappedBlockChange, int combinedID) {
        Vector3i blockPosition = wrappedBlockChange.getBlockPosition();

        int range = (player.getTransactionPing() / 100) + 16;
        if (Math.abs(blockPosition.getX() - player.x) < range && Math.abs(blockPosition.getY() - player.y) < range && Math.abs(blockPosition.getZ() - player.z) < range)
            event.setPostTask(player::sendTransaction);

        player.compensatedWorld.worldChangedBlockQueue.add(new ChangeBlockData(player.lastTransactionSent.get() + 1, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), combinedID));
    }
}
