package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.badpackets.BadPacketsE;
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
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateHealth;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;

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
            //
            if (player.packetStateData.lastFood == health.getFood()
                    && player.packetStateData.lastHealth == health.getHealth()
                    && player.packetStateData.lastSaturation == health.getFoodSaturation()
                    && PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) return;

            player.packetStateData.lastFood = health.getFood();
            player.packetStateData.lastHealth = health.getHealth();
            player.packetStateData.lastSaturation = health.getFoodSaturation();

            player.sendTransaction();

            if (health.getFood() == 20) { // Split so transaction before packet
                player.latencyUtils.addRealTimeTask(player.lastTransactionReceived.get(), () -> player.food = 20);
            } else { // Split so transaction after packet
                player.latencyUtils.addRealTimeTask(player.lastTransactionReceived.get() + 1, () -> player.food = health.getFood());
            }

            if (health.getHealth() <= 0) {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.compensatedEntities.getSelf().isDead = true);
            } else {
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> player.compensatedEntities.getSelf().isDead = false);
            }

            event.getTasksAfterSend().add(player::sendTransaction);
        }

        if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerJoinGame joinGame = new WrapperPlayServerJoinGame(event);
            player.gamemode = joinGame.getGameMode();
            player.entityID = joinGame.getEntityId();
            player.dimension = joinGame.getDimension();

            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_17)) return;
            player.compensatedWorld.setDimension(joinGame.getDimension().getDimensionName(), event.getUser());
        }

        if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            WrapperPlayServerRespawn respawn = new WrapperPlayServerRespawn(event);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            List<Runnable> tasks = event.getTasksAfterSend();
            tasks.add(player::sendTransaction);

            // Force the player to accept a teleport before respawning
            // (We won't process movements until they accept a teleport, we won't let movements though either)
            // Also invalidate previous positions
            player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport = false;
            player.getSetbackTeleportUtil().lastKnownGoodPosition = null;

            // TODO: What does keep all metadata do?
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
                player.isSneaking = false;
                player.lastOnGround = false;
                player.isInBed = false;
                player.packetStateData.packetPlayerOnGround = false; // If somewhere else pulls last ground to fix other issues
                player.packetStateData.lastClaimedPosition = new Vector3d();
                player.filterMojangStupidityOnMojangStupidity = new Vector3d();
                player.lastSprintingForSpeed = false; // This is reverted even on 1.18 clients

                player.checkManager.getPacketCheck(BadPacketsE.class).handleRespawn(); // Reminder ticks reset

                // EVERYTHING gets reset on a cross dimensional teleport, clear chunks and entities!
                if (respawn.getDimension().getId() != player.dimension.getId() || !Objects.equals(respawn.getDimension().getDimensionName(), player.dimension.getDimensionName()) || !Objects.equals(respawn.getDimension().getAttributes(), player.dimension.getAttributes())) {
                    player.compensatedEntities.entityMap.clear();
                    player.compensatedWorld.activePistons.clear();
                    player.compensatedWorld.openShulkerBoxes.clear();
                    player.compensatedWorld.chunks.clear();
                    player.compensatedWorld.isRaining = false;
                }
                player.dimension = respawn.getDimension();

                player.compensatedEntities.serverPlayerVehicle = null; // All entities get removed on respawn
                player.compensatedEntities.playerEntity = new PacketEntitySelf(player);
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
                if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
                    player.compensatedWorld.setDimension(respawn.getDimension().getDimensionName(), event.getUser());
                }
            });
        }
    }
}
