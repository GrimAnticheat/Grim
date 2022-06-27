package ac.grim.grimac;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

//This is used for grim's external API. It has its own class just for organization.
public class GrimExternalAPI implements GrimAbstractAPI {

    private final GrimAPI api;

    public GrimExternalAPI(GrimAPI api) {
        this.api = api;
    }

    @Nullable
    @Override
    public GrimUser getGrimUser(Player player) {
        return api.getPlayerDataManager().getPlayer(player);
    }

    @Override
    public void setServerName(String name) {
        api.getDiscordManager().setServerName(name);
    }
}
