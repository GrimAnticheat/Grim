package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.badpackets.BadPacketsF;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.TrackerData;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySelf;
import ac.grim.grimac.utils.enums.Pose;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateHealth;
import org.bukkit.util.Vector;

import java.util.List;

public class PacketPlayerRespawn extends PacketListenerAbstract {

    public PacketPlayerRespawn() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.UPDATE_HEALTH) {
            WrapperPlayServerUpdateHealth health = new WrapperPlayServerUpdateHealth(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            List<Runnable> tasks = event.getPostTasks();
            tasks.add(player::sendTransaction);

            if (health.getFood() == 20) { // Split so transaction before packet
                player.latencyUtils.addRealTimeTask(player.lastTransactionReceived.get(), () -> player.food = 20);
            } else { // Split so transaction after packet
                player.latencyUtils.addRealTimeTask(player.lastTransactionReceived.get() + 1, () -> player.food = health.getFood());
            }

            if (health.getHealth() <= 0) {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.isDead = true);
            } else {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.isDead = false);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.slowedByUsingItem = false);
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.packetStateData.slowedByUsingItem = false);
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerJoinGame joinGame = new WrapperPlayServerJoinGame(event);
            player.gamemode = joinGame.getGameMode();
            player.entityID = joinGame.getEntityId();

            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_17)) return;
            player.compensatedWorld.setDimension(joinGame.getDimension().getType().getName(), event.getUser());
        }

        if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            WrapperPlayServerRespawn respawn = new WrapperPlayServerRespawn(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            List<Runnable> tasks = event.getPostTasks();
            tasks.add(player::sendTransaction);

            // Force the player to accept a teleport before respawning
            player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport = false;

            // TODO: What does keep all metadata do?
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                // Client creates a new entity on respawn
                player.isDead = false;
                player.isSneaking = false;
                player.lastOnGround = false;
                player.packetStateData.packetPlayerOnGround = false; // If somewhere else pulls last ground to fix other issues
                player.lastSprintingForSpeed = false; // This is reverted even on 1.18 clients

                // EVERYTHING gets reset on a cross dimensional teleport, clear chunks and entities!
                player.compensatedEntities.entityMap.clear();
                player.compensatedWorld.activePistons.clear();
                player.compensatedWorld.openShulkerBoxes.clear();
                player.compensatedWorld.chunks.clear();
                player.compensatedEntities.serverPlayerVehicle = null; // All entities get removed on respawn
                player.compensatedEntities.playerEntity = new PacketEntitySelf();
                player.compensatedEntities.selfTrackedEntity = new TrackerData(0, 0, 0, 0, 0, EntityTypes.PLAYER, player.lastTransactionSent.get());

                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14)) { // 1.14+ players send a packet for this, listen for it instead
                    player.isSprinting = false;
                    ((BadPacketsF) player.checkManager.getPacketCheck(BadPacketsF.class)).lastSprinting = false; // Pre 1.14 clients set this to false when creating new entity
                    // TODO: What the fuck viaversion, why do you throw out keep all metadata?
                    // The server doesn't even use it... what do we do?
                    player.compensatedEntities.hasSprintingAttributeEnabled = false;
                }
                player.pose = Pose.STANDING;
                player.clientVelocity = new Vector();
                player.gamemode = respawn.getGameMode();
                player.compensatedWorld.setDimension(respawn.getDimension().getType().getName(), event.getUser());
            });
        }
    }
}
