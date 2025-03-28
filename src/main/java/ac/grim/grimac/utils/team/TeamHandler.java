package ac.grim.grimac.utils.team;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

// Reminder: Entities use UUIDs, players use name, for setting teams.
public class TeamHandler extends Check implements PacketCheck {

    private final Map<String, EntityTeam> entityTeams = new Object2ObjectOpenHashMap<>();
    private final Map<String, EntityTeam> entityToTeam = new Object2ObjectOpenHashMap<>();

    private @Getter @Setter @Nullable EntityTeam playerTeam = null;

    public TeamHandler(GrimPlayer player) {
        super(player);
    }

    public void addEntityToTeam(String entityTeamRepresentation, EntityTeam team) {
        entityToTeam.put(entityTeamRepresentation, team);
    }

    public void removeEntityFromTeam(String entityTeamRepresentation) {
        entityToTeam.remove(entityTeamRepresentation);
    }

    public EntityTeam getEntityTeam(PacketEntity entity) {
        // TODO in what cases is UUID null in 1.9+?
        final UUID uuid = entity.getUuid();
        return uuid == null ? null : entityToTeam.get(uuid.toString());
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.TEAMS) {
            WrapperPlayServerTeams teams = new WrapperPlayServerTeams(event);
            final String teamName = teams.getTeamName();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                EntityTeam entityTeam = switch (teams.getTeamMode()) {
                    case CREATE -> {
                        var newTeam = new EntityTeam(player, teamName);
                        entityTeams.put(teamName, newTeam);
                        yield newTeam;
                    }
                    case REMOVE -> entityTeams.remove(teamName);
                    default -> entityTeams.get(teamName);
                };

                if (entityTeam != null) {
                    entityTeam.update(teams);
                }
            });
        }
    }
}
