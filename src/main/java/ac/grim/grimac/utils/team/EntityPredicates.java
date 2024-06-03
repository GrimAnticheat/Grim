package ac.grim.grimac.utils.team;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;

import java.util.function.Predicate;

public final class EntityPredicates {

    public static Predicate<GrimPlayer> canBePushedBy(GrimPlayer player, PacketEntity entity, TeamHandler teamHandler) {
        if (player.gamemode == GameMode.SPECTATOR) return p -> false;
        final EntityTeam entityTeam = teamHandler.getEntityTeam(entity).orElse(null);
        WrapperPlayServerTeams.CollisionRule collisionRule = entityTeam == null ? WrapperPlayServerTeams.CollisionRule.ALWAYS : entityTeam.getCollisionRule();
        if (collisionRule == WrapperPlayServerTeams.CollisionRule.NEVER) return p -> false;

        return p -> {
            final EntityTeam playersTeam = teamHandler.getPlayersTeam().orElse(null);
            WrapperPlayServerTeams.CollisionRule collisionRule2 = playersTeam == null ? WrapperPlayServerTeams.CollisionRule.ALWAYS : playersTeam.getCollisionRule();
            if (collisionRule2 == WrapperPlayServerTeams.CollisionRule.NEVER) {
                return false;
            } else {
                boolean bl = entityTeam != null && entityTeam.equals(playersTeam);
                if ((collisionRule == WrapperPlayServerTeams.CollisionRule.PUSH_OWN_TEAM || collisionRule2 == WrapperPlayServerTeams.CollisionRule.PUSH_OWN_TEAM) && bl) {
                    return false;
                } else {
                    return collisionRule != WrapperPlayServerTeams.CollisionRule.PUSH_OTHER_TEAMS && collisionRule2 != WrapperPlayServerTeams.CollisionRule.PUSH_OTHER_TEAMS || bl;
                }
            }
        };
    }
}