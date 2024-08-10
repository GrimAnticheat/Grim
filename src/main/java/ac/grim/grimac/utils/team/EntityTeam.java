package ac.grim.grimac.utils.team;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import lombok.Getter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class EntityTeam {

    private final GrimPlayer player;
    @Getter private final String name;
    @Getter private final Set<String> entries = new HashSet<>();
    @Getter private WrapperPlayServerTeams.CollisionRule collisionRule;

    public EntityTeam(GrimPlayer player, String name) {
        this.player = player;
        this.name = name;
    }

    public void update(WrapperPlayServerTeams teams) {
        teams.getTeamInfo().ifPresent(info -> this.collisionRule = info.getCollisionRule());

        final WrapperPlayServerTeams.TeamMode mode = teams.getTeamMode();
        if (mode == WrapperPlayServerTeams.TeamMode.ADD_ENTITIES || mode == WrapperPlayServerTeams.TeamMode.CREATE) {
            final TeamHandler teamHandler = player.checkManager.getPacketCheck(TeamHandler.class);
            for (String teamsPlayer : teams.getPlayers()) {
                if (teamsPlayer.equals(player.user.getName())) {
                    player.teamName = name;
                    continue;
                }

                boolean flag = false;
                for (UserProfile profile : player.compensatedEntities.profiles.values()) {
                    if (profile.getName() != null && profile.getName().equals(teamsPlayer)) {
                        teamHandler.addEntityToTeam(profile.getUUID().toString(), this);
                        flag = true;
                    }
                }

                if (flag) continue;

                teamHandler.addEntityToTeam(teamsPlayer, this);
            }
        } else if (mode == WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES) {
            for (String teamsPlayer : teams.getPlayers()) {
                if (teamsPlayer.equals(player.user.getName())) {
                    player.teamName = null;
                    continue;
                }
                entries.remove(teamsPlayer);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityTeam)) return false;
        return Objects.equals(getName(), ((EntityTeam) o).getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}