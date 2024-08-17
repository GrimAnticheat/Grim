package ac.grim.grimac.utils.team;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TeamHandler extends Check implements PacketCheck {

    private final Map<String, EntityTeam> entityTeams = new Object2ObjectOpenHashMap<>();
    private final Map<String, EntityTeam> entityToTeam = new Object2ObjectOpenHashMap<>();

    public TeamHandler(GrimPlayer player) {
        super(player);
    }

    public void addEntityToTeam(String entityTeamRepresentation, EntityTeam team) {
        entityToTeam.put(entityTeamRepresentation, team);
    }

    public Optional<EntityTeam> getPlayersTeam() {
        final String teamName = player.teamName;
        if (teamName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityTeams.get(teamName));
    }

    public Optional<EntityTeam> getEntityTeam(PacketEntity entity) {
        // TODO in what cases is UUID null in 1.9+?
        final UUID uuid = entity.getUuid();
        return uuid == null ? Optional.empty() : Optional.ofNullable(entityToTeam.get(uuid.toString()));
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.TEAMS) {
            WrapperPlayServerTeams teams = new WrapperPlayServerTeams(event);
            final String teamName = teams.getTeamName();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                switch (teams.getTeamMode()) {
                    case CREATE:
                        entityTeams.put(teamName, new EntityTeam(player, teamName));
                        break;
                    case REMOVE:
                        entityTeams.remove(teamName);
                        break;
                }

                entityTeams.computeIfPresent(teamName, (s, team) -> {
                    team.update(teams);
                    return team;
                });
            });
        }
    }
}